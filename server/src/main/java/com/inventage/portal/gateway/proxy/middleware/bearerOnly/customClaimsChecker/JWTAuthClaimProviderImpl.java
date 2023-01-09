package com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystemException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.impl.jose.JWK;
import io.vertx.ext.auth.impl.jose.JWT;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.jwt.impl.JWTAuthProviderImpl;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;
import net.minidev.json.JSONArray;

/*
 * In order for our custom jwt claim check to work, some classes of the vertx library are copied and modified.
 * This class contains some copy-pastes of its superclass. The reason being is that we need to have access to the jwt field to parse the signed token.
 * Unfortunately this field is private in the superclass. Hence the constructor is mostly copied to init a local JWT instance.
 * https://github.com/vert-x3/vertx-auth/blob/4.3.7/vertx-auth-jwt/src/main/java/io/vertx/ext/auth/jwt/impl/JWTAuthProviderImpl.java
 */
public class JWTAuthClaimProviderImpl extends JWTAuthProviderImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(JWTAuthClaimProviderImpl.class);

    private final JWT jwt = new JWT();

    private final String permissionsClaimKey;
    private final JWTOptions jwtOptions;

    private final List<JWTClaim> additionalJWTClaims;

    private static final String ERROR_MESSAGE = "Invalid JWT token: Payload does not comply to claims.";

    public JWTAuthClaimProviderImpl(Vertx vertx, JWTAuthOptions config) {
        super(vertx, config);

        // NOTE: the following is exactly the same as the super.constructor() but we need access to private the super.jwt field
        this.permissionsClaimKey = config.getPermissionsClaimKey();
        this.jwtOptions = config.getJWTOptions();
        // set the nonce algorithm
        jwt.nonceAlgorithm(jwtOptions.getNonceAlgorithm());

        final KeyStoreOptions keyStore = config.getKeyStore();

        // attempt to load a Key file
        try {
            if (keyStore != null) {
                final KeyStore ks;
                if (keyStore.getProvider() == null) {
                    ks = KeyStore.getInstance(keyStore.getType());
                } else {
                    ks = KeyStore.getInstance(keyStore.getType(), keyStore.getProvider());
                }

                // synchronize on the class to avoid the case where multiple file accesses will overlap
                synchronized (JWTAuthProviderImpl.class) {
                    String path = keyStore.getPath();
                    if (path != null) {
                        final Buffer keystore = vertx.fileSystem().readFileBlocking(keyStore.getPath());

                        try (InputStream in = new ByteArrayInputStream(keystore.getBytes())) {
                            ks.load(in, keyStore.getPassword().toCharArray());
                        }
                    } else {
                        ks.load(null, keyStore.getPassword().toCharArray());
                    }
                }
                // load all available keys in the keystore
                for (JWK key : JWK.load(ks, keyStore.getPassword(), keyStore.getPasswordProtection())) {
                    jwt.addJWK(key);
                }
            }
            // attempt to load pem keys
            final List<PubSecKeyOptions> keys = config.getPubSecKeys();

            if (keys != null) {
                for (PubSecKeyOptions pubSecKey : config.getPubSecKeys()) {
                    jwt.addJWK(new JWK(pubSecKey));
                }
            }

            // attempt to load jwks
            final List<JsonObject> jwks = config.getJwks();

            if (jwks != null) {
                for (JsonObject jwk : jwks) {
                    try {
                        jwt.addJWK(new JWK(jwk));
                    } catch (Exception e) {
                        LOGGER.warn("Unsupported JWK", e);
                    }
                }
            }

        } catch (KeyStoreException | IOException | FileSystemException | CertificateException | NoSuchAlgorithmException
                | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }

        // initialize additional claims
        if (config.getJWTOptions() != null && config.getJWTOptions() instanceof JWTClaimOptions) {
            additionalJWTClaims = ((JWTClaimOptions) config.getJWTOptions()).getClaims();
        } else {
            additionalJWTClaims = List.of();
        }
    }

    /**
     * This method conducts checks if the payload complies to the custom claims.
     */
    @Override
    public Future<User> authenticate(Credentials credentials) {
        Future<User> user = super.authenticate(credentials);

        if (user.failed()) {
            return user;
        }

        final TokenCredentials authInfo;
        try {
            // cast
            authInfo = (TokenCredentials) credentials;
            // check
            authInfo.checkValid(null);
        } catch (RuntimeException e) {
            return Future.failedFuture(e);
        }

        final JsonObject payload;
        try {
            payload = jwt.decode(authInfo.getToken());
        } catch (RuntimeException e) {
            return Future.failedFuture(e);
        }

        // Check that all required additional claims are present
        try {
            for (JWTClaim additionalClaim : additionalJWTClaims) {
                // Claims are provided by the dynamic configuration file.
                // We verify that each payload complies with the claims defined in the configuration
                // Throws an exception if the path does not exist in the payload
                final var payloadValue = JsonPath.read(payload.toString(), additionalClaim.path);

                // Verify if the value stored in that path complies to the claim.
                if (!verifyClaim(payloadValue, additionalClaim.value, additionalClaim.operator)) {
                    throw new IllegalStateException(String.format(
                            "%s Claim verification failed. Path: %s, Operator: %s, claim: %s, payload: %s",
                            ERROR_MESSAGE, additionalClaim.path, additionalClaim.operator, additionalClaim.value,
                            payloadValue));
                }
            }
        } catch (RuntimeException | JsonProcessingException e) {
            return Future.failedFuture(e);
        }

        return user;
    }

    private static boolean verifyClaim(Object payloadValue, Object claimValue, JWTClaimOperator operator)
            throws JsonProcessingException {

        //We need to convert the dynamic type of the payload to ensure compatibility when using method calls from external libraries.
        payloadValue = convertPayloadType(payloadValue);

        if (operator == JWTClaimOperator.EQUALS) {
            return verifyClaimEquals(payloadValue, claimValue);
        } else if (operator == JWTClaimOperator.CONTAINS) {
            return verifyClaimContains(payloadValue, claimValue);
        } else if (operator == JWTClaimOperator.EQUALS_SUBSTRING_WHITESPACE) {
            final String[] array = payloadValue.toString().split(" ");
            final JsonArray payloadArray = new JsonArray(Arrays.asList(array));
            return verifyClaimEquals(payloadArray, claimValue);
        } else if (operator == JWTClaimOperator.CONTAINS_SUBSTRING_WHITESPACE) {
            final String[] array = payloadValue.toString().split(" ");
            final JsonArray payloadArray = new JsonArray(Arrays.asList(array));
            return verifyClaimContains(payloadArray, claimValue);
        } else {
            throw new IllegalStateException(
                    String.format("%s. No support for the following operator: %s", ERROR_MESSAGE, operator));
        }
    }

    private static boolean verifyClaimEquals(Object payloadValue, Object claimValue) throws JsonProcessingException {
        if ((claimValue instanceof String && payloadValue instanceof String)
                || (claimValue instanceof Number && payloadValue instanceof Number)
                || (claimValue instanceof Boolean && payloadValue instanceof Boolean)) {
            return claimValue.equals(payloadValue);
        }

        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(payloadValue.toString()).equals(mapper.readTree(claimValue.toString()));
    }

    private static boolean verifyClaimContains(Object payloadValue, Object claimValue) throws JsonProcessingException {
        //By definition, we require that contains can only work with an array/list.

        final JsonArray claimArray = new JsonArray(claimValue.toString());

        if (payloadValue instanceof JsonArray) {
            final JsonArray payloadArray = (JsonArray) payloadValue;
            return verifyClaimContainsArray(payloadArray, claimArray);
        } else {
            for (Object claimItem : claimArray) {
                if (verifyClaimEquals(payloadValue, claimItem)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static boolean verifyClaimContainsArray(JsonArray payloadArray, JsonArray claimArray)
            throws JsonProcessingException {
        //Every entry in the payload array must be contained in the claimed array
        boolean found = false;
        for (Object payloadItem : payloadArray) {
            for (Object claimItem : claimArray) {
                if (verifyClaimEquals(payloadItem, claimItem)) {
                    found = true;
                    break;
                }
            }
        }
        //If the entry has been found, the code will terminate before reaching this statement
        return found;
    }

    private static Object convertPayloadType(Object payloadValue) {
        //The JsonPath library converts a Json Object directly to a map data structure. To use external libraries to perform equality check between
        //two JSON objects, we convert the payload to a JsonObject, such that we get the same structure when calling toString
        if (payloadValue instanceof Map) {
            payloadValue = new JsonObject((Map<String, Object>) payloadValue);
        }
        //Potentially payloadValue has type JSONArray, but for our solution we require type JsonArray
        if (payloadValue instanceof JSONArray) {
            payloadValue = new JsonArray(payloadValue.toString());
        }

        return payloadValue;
    }

}
