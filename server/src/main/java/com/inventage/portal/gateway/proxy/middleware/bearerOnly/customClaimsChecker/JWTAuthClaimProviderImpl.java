package com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
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
import net.minidev.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;

/**
 * We extend the built-in JWTAuth verifier to support further claims verification.
 */
public class JWTAuthClaimProviderImpl extends JWTAuthProviderImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(JWTAuthClaimProviderImpl.class);

    private final JWT jwt = new JWT();

    private final JWTOptions jwtOptions;

    private static final String ERROR_MESSAGE = "Invalid JWT token: Payload does not comply to claims.";
    public JWTAuthClaimProviderImpl(Vertx vertx, JWTAuthOptions config) {
        super(vertx, config);
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
                    final Buffer keystore = vertx.fileSystem().readFileBlocking(keyStore.getPath());

                    try (InputStream in = new ByteArrayInputStream(keystore.getBytes())) {
                        ks.load(in, keyStore.getPassword().toCharArray());
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
                    this.jwt.addJWK(new JWK(jwk));
                }
            }

        } catch (KeyStoreException | IOException | FileSystemException | CertificateException | NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {
        authenticate(new TokenCredentials(authInfo.getString("token")), resultHandler);
    }

    @Override
    public void authenticate(Credentials credentials, Handler<AsyncResult<User>> resultHandler) {
        try {

            TokenCredentials authInfo = (TokenCredentials) credentials;
            authInfo.checkValid(null);

            final JsonObject payload = jwt.decode(authInfo.getToken());

            if (this.jwtOptions instanceof JWTClaimOptions) {
                final List<JWTClaim> otherClaims = ((JWTClaimOptions) this.jwtOptions).getClaims();

                for (JWTClaim otherClaim : otherClaims) {
                    //Claims provided by the dynamic configuration file. We verify that each payload complies with the claims defined in the configuration
                    //Throws an exception if the path does not exist in the payload
                    var payloadValue = JsonPath.read(payload.toString(), otherClaim.path);

                    //Verify if the value stored in that path complies to the claim.
                    if (!verifyClaim(payloadValue, otherClaim.value, otherClaim.operator)) {
                        resultHandler.handle(Future.failedFuture(ERROR_MESSAGE));
                        throw new RuntimeException(String.format("%s Claim verification failed. Operator: %s, claim: %s, payload: %s",
                                ERROR_MESSAGE, otherClaim.operator, otherClaim.value, payloadValue));
                    }
                }
            }
            super.authenticate(credentials, resultHandler);
        } catch (RuntimeException | JsonProcessingException e) {
            LOGGER.debug(e.getMessage());
            resultHandler.handle(Future.failedFuture(e));
        }
    }

    private static boolean verifyClaim(Object payloadValue, Object claimValue, JWTClaimOperator operator) throws JsonProcessingException {

        //We need to convert the dynamic type of the payload to ensure compatibility when using method calls from external libraries.
        payloadValue = convertPayloadType(payloadValue);

        if (operator == JWTClaimOperator.EQUALS) {
            return verifyClaimEquals(payloadValue, claimValue);
        } else if (operator == JWTClaimOperator.CONTAINS) {
            return verifyClaimContains(payloadValue, claimValue);
        } else {
            throw new RuntimeException(String.format("%s. We do not support the following operator: %s", ERROR_MESSAGE, operator));
        }
    }

    private static boolean verifyClaimEquals(Object payloadValue, Object claimValue) throws JsonProcessingException {

        if ((claimValue instanceof String && payloadValue instanceof String)
                || (claimValue instanceof Number && payloadValue instanceof Number)
                || (claimValue instanceof Boolean && payloadValue instanceof Boolean)) {
            return claimValue.equals(payloadValue);
        }

        ObjectMapper mapper = new ObjectMapper();
        if (mapper.readTree(payloadValue.toString()).equals(mapper.readTree(claimValue.toString()))){
            return true;
        } else{
            throw new RuntimeException(String.format("%s. Claim and payload value are not equal. Claim: %s, Payload: %s", ERROR_MESSAGE, payloadValue, claimValue));
        }
    }

    private static boolean verifyClaimContains(Object payloadValue, Object claimValue) throws JsonProcessingException {
        //By definition, we require that contains can only work with an array/list.
        JsonArray claimArray = (JsonArray) claimValue;

        if (payloadValue instanceof JsonArray) {
            JsonArray payloadArray = (JsonArray) payloadValue;
            return verifyClaimContainsArray(payloadArray, claimArray);
        } else {
            for (Object claimItem : claimArray) {
                if (verifyClaimEquals(payloadValue, claimItem)) {
                    return true;
                }
            }
            throw new RuntimeException(String.format("%s. Payload is not contained in claim. Claim: %s, Payload: %s", ERROR_MESSAGE, claimArray, claimValue));
        }
    }

    private static boolean verifyClaimContainsArray(JsonArray payloadArray, JsonArray claimArray) throws JsonProcessingException {
        //Every entry in the payload array must be contained in the claimed array
        for (int i = 0; i < payloadArray.size(); i++) {
            Object payloadItem = payloadArray.getValue(i);
            boolean found = false;
            for (int j = 0; j < claimArray.size(); j++) {
                Object claimItem = claimArray.getValue(j);
                if (verifyClaimEquals(payloadItem, claimItem)) {
                    found = true;
                    break;
                }
            }
            //If the entry has been found, the code will terminate before reaching this statement
            if (!found) {
                throw new RuntimeException(String.format("%s. Payload is not a subset of claim. Claim: %s, Payload: %s", ERROR_MESSAGE, claimArray, payloadArray));
            }
        }
        return true;
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