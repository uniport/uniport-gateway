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
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.auth.impl.jose.JWK;
import io.vertx.ext.auth.impl.jose.JWT;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.jwt.impl.JWTAuthProviderImpl;
import net.minidev.json.JSONArray;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * We extend the built-in JWTAuth verifier to support further claims verification.
 */
public class JWTAuthClaimProviderImpl extends JWTAuthProviderImpl {

    private static final JsonArray EMPTY_ARRAY = new JsonArray();

    private final JWT jwt = new JWT();

    private final String permissionsClaimKey;
    private final JWTOptions jwtOptions;

    private static final String ERROR_MESSAGE = "Invalid JWT token: Payload does not comply to claims.";
    public JWTAuthClaimProviderImpl(Vertx vertx, JWTAuthOptions config) {
        super(vertx, config);
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

           if(this.jwtOptions instanceof JWTClaimOptions){
                final List otherClaims = ((JWTClaimOptions) this.jwtOptions).getClaims();

                var jsonPath = JsonPath.parse(payload.toString());

                for(int i = 0; i < otherClaims.size(); i++){
                    //Claims provided by the dynamic configuration file. We verify that each payload complies with the claims defined in the configuration
                    JWTClaim claim = (JWTClaim) otherClaims.get(i);

                    //Throws an exception if the path does not exist in the payload
                    var payloadValue = JsonPath.read(payload.toString(),  claim.path);

                    //Verify if the value stored in that path complies to the claim.
                    if (!verifyClaim(payloadValue, claim.value, claim.operator)){
                        resultHandler.handle(Future.failedFuture(ERROR_MESSAGE));
                        return;
                    }
                }
            }
            super.authenticate(credentials, resultHandler);
        } catch (RuntimeException | JsonProcessingException e) {
            resultHandler.handle(Future.failedFuture(e));
        }
    }

    private static boolean verifyClaim(Object payloadValue, Object claimValue, JWTClaimOperator operator) throws JsonProcessingException {

        //We need to convert the dynamic type of the payload to ensure compatibility when using method calls from external libraries.
        payloadValue = convertPayloadType(payloadValue);

        if(operator == JWTClaimOperator.EQUALS) {
            return verifyClaimEquals(payloadValue, claimValue);
        }
        else if(operator == JWTClaimOperator.CONTAINS){
            return verifyClaimContains(payloadValue, claimValue);
        }
        else{
            throw new RuntimeException(String.format("%s. We do not support the following operator: %s", ERROR_MESSAGE, operator));
        }
    }
    private static boolean verifyClaimEquals(Object payloadValue, Object claimValue) throws JsonProcessingException {

        if((claimValue instanceof String && payloadValue instanceof String)
                || (claimValue instanceof Number && payloadValue instanceof Number)
                || (claimValue instanceof Boolean && payloadValue instanceof Boolean)) {
            return (claimValue.equals(payloadValue));
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(payloadValue.toString()).equals(mapper.readTree(claimValue.toString()));
    }
    private static boolean verifyClaimContains(Object payloadValue, Object claimValue) throws JsonProcessingException{
        //By definition, we require that contains can only work with an array/list.
        JsonArray claimArray = (JsonArray) claimValue;

        if(payloadValue instanceof JsonArray){
            JsonArray payloadArray = (JsonArray) payloadValue;
            //Every entry in the payload array must be contained in the claimed array
            for(int i = 0; i < payloadArray.size(); i++){
                var pl_i = payloadArray.getValue(i);
                boolean found = false;
                for(int j = 0; j < claimArray.size(); j++){
                    var c_j = claimArray.getValue(j);
                    if(verifyClaimEquals(pl_i, c_j)){
                        found = true;
                        break;
                    }
                }
                //If the entry has been found, the code will terminate before reaching this statement
                if(!found){
                    throw new RuntimeException(ERROR_MESSAGE);
                }
            }
            return true;
        }
        else{
            for(int j = 0; j < claimArray.size(); j++){
                var c_j = claimArray.getValue(j);
                if(verifyClaimEquals(payloadValue, c_j)){
                    return true;
                }
            }
            throw new RuntimeException(ERROR_MESSAGE);
        }
    }
    private static Object convertPayloadType(Object payloadValue){
        //The JsonPath library converts a Json Object directly to a map data structure. To use external libraries to perform equality check between
        //two JSON objects, we convert the payload to a JsonObject, such that we get the same structure when calling toString
        if (payloadValue instanceof Map){
            payloadValue = new JsonObject((Map<String, Object>) payloadValue);
        }
        //Potentially payloadValue has type JSONArray, but for our solution we require type JsonArray
        if(payloadValue instanceof JSONArray){
            payloadValue = new JsonArray( payloadValue.toString());
        }

        return payloadValue;
    }
    private static JsonArray getJsonPermissions(JsonObject jwtToken, String permissionsClaimKey) {
        if (permissionsClaimKey.contains("/")) {
            return getNestedJsonValue(jwtToken, permissionsClaimKey);
        }
        return jwtToken.getJsonArray(permissionsClaimKey, null);
    }

    private static final Collection<String> SPECIAL_KEYS = Arrays.asList("access_token", "exp", "iat", "nbf");

    /**
     * @deprecated This method is deprecated as it introduces an exception to the internal representation of {@link User}
     * object data.
     * In the future a simple call to User.create() should be used
     */
    @Deprecated
    private User createUser(String accessToken, JsonObject jwtToken, String permissionsClaimKey) {
        User result = User.fromToken(accessToken);

        // update the attributes
        result.attributes()
                .put("accessToken", jwtToken);

        // copy the expiration check properties + sub to the attributes root
        copyProperties(jwtToken, result.attributes(), "exp", "iat", "nbf", "sub");
        // as the token is immutable, the decoded values will be added to the principal
        // with the exception of the above ones
        for (String key : jwtToken.fieldNames()) {
            if (!SPECIAL_KEYS.contains(key)) {
                result.principal().put(key, jwtToken.getValue(key));
            }
        }

        // root claim meta data for JWT AuthZ
        result.attributes()
                .put("rootClaim", "accessToken");

        JsonArray jsonPermissions = getJsonPermissions(jwtToken, permissionsClaimKey);
        if (jsonPermissions != null) {
            for (Object item : jsonPermissions) {
                if (item instanceof String) {
                    String permission = (String) item;
                    result.authorizations().add("jwt-authentication", PermissionBasedAuthorization.create(permission));
                }
            }
        }
        return result;
    }

    private static void copyProperties(JsonObject source, JsonObject target, String... keys) {
        if (source != null && target != null) {
            for (String key : keys) {
                if (source.containsKey(key) && !target.containsKey(key)) {
                    target.put(key, source.getValue(key));
                }
            }
        }
    }

    private static JsonArray getNestedJsonValue(JsonObject jwtToken, String permissionsClaimKey) {
        String[] keys = permissionsClaimKey.split("/");
        JsonObject obj = null;
        for (int i = 0; i < keys.length; i++) {
            if (i == 0) {
                obj = jwtToken.getJsonObject(keys[i]);
            } else if (i == keys.length - 1) {
                if (obj != null) {
                    return obj.getJsonArray(keys[i]);
                }
            } else {
                if (obj != null) {
                    obj = obj.getJsonObject(keys[i]);
                }
            }
        }
        return null;
    }
}

/**
 * Solution without json path
 *    if(claim.path.length() == 0){
 *                         resultHandler.handle(Future.failedFuture("Invalid JWT: Claimed path must not be empty!"));
 *                         return;
 *                     }
 *
 *                     String[] claimedPath = claim.path.split(".");
 *
 *                     JsonObject jsonToVerify = payload.copy();
 *
 *                     //(1) check if claimed path exists on the payload
 *                     for(int j = 0; j < claimedPath.length-1; j++){
 *                         jsonToVerify = jsonToVerify.getJsonObject(claimedPath[j]);
 *                         if(jsonToVerify == null){
 *                             resultHandler.handle(Future.failedFuture("Invalid JWT: Claimed path does not exist"));
 *                             return;
 *                         }
 *                     }
 *
 *                     //(2) check existence and type of the entry in the claimed path
 *                     String entryKey = claimedPath[claimedPath.length-1];
 *                     if(jsonToVerify.containsKey(entryKey)){
 *                         //TODO: Make more beautiful code, only temporary non-elegant solution
 *                         if(jsonToVerify.getString(entryKey) != null){
 *
 *                         }else if(jsonToVerify.getJsonArray(entryKey) != null){
 *
 *                         }else if(jsonToVerify.getJsonObject(entryKey) != null){
 *
 *                         }else if(jsonToVerify.getNumber(entryKey) != null){
 *
 *                         }else if(jsonToVerify.getBoolean(entryKey) != null){
 *
 *                         }
 *
 *                     }else{
 *                         resultHandler.handle(Future.failedFuture("Invalid JWT: Claimed path does not exist"));
 *                         return;
 *                     }
 */

