package ch.uniport.gateway.proxy.middleware.sessionLogoutFromBackchannel;

import ch.uniport.gateway.proxy.middleware.authorization.JWKAccessibleAuthHandler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.impl.jose.JWK;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.RoutingContext;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class MockJWKAuthHandler implements JWKAccessibleAuthHandler {

    private final List<JsonObject> jwks = new ArrayList<JsonObject>();

    public MockJWKAuthHandler(JWTAuthOptions config) {
        // attempt to load pem keys
        final List<PubSecKeyOptions> keys = config.getPubSecKeys();
        if (keys != null) {
            for (PubSecKeyOptions pubSecKey : config.getPubSecKeys()) {
                jwks.add(toJWKJsonObject(new JWK(pubSecKey)));
            }
        }

        // attempt to load jwks
        final List<JsonObject> jwks = config.getJwks();
        if (jwks != null) {
            for (JsonObject jwk : jwks) {
                try {
                    jwks.add(toJWKJsonObject(new JWK(jwk)));
                } catch (Exception e) {
                    throw new RuntimeException("Unsupported JWK", e);
                }
            }
        }
    }

    @Override
    public void handle(RoutingContext event) {
        // not used in test
        throw new UnsupportedOperationException("Unimplemented method 'handle'");
    }

    @Override
    public List<JsonObject> getJwks() {
        return this.jwks;
    }

    private JsonObject toJWKJsonObject(JWK jwk) {
        final String kty = jwk.kty();
        if (!kty.equals("RSA")) {
            throw new IllegalArgumentException(String.format("RSA key expected, got '%s'", kty));
        }

        final RSAPublicKey publicKey = (RSAPublicKey) jwk.publicKey();
        base64UrlEncode(publicKey.getModulus().toByteArray());

        return JsonObject.of()
            .put("kid", "some-kid")
            .put("alg", jwk.getAlgorithm())
            .put("kty", kty)
            .put("use", "sig")
            .put("n", base64UrlEncode(publicKey.getModulus().toByteArray()))
            .put("e", base64UrlEncode(publicKey.getPublicExponent().toByteArray()));
    }

    private String base64UrlEncode(byte[] b) {
        return Base64.getUrlEncoder().encodeToString(b);
    }
}
