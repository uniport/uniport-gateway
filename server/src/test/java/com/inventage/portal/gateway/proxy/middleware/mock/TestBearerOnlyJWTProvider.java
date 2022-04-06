package com.inventage.portal.gateway.proxy.middleware.mock;


import io.smallrye.jwt.build.Jwt;

import java.util.HashMap;
import java.util.Map;

/**
 * Signing JWTs for Unit tests
 */
public class TestBearerOnlyJWTProvider {

    static {
        // JWT signing is normally done in Keycloak
        System.setProperty("smallrye.jwt.sign.key.location", "FOR_DEVELOPMENT_PURPOSE_ONLY-privateKey.pem");
        // token life time in seconds: 1 year
        System.setProperty("smallrye.jwt.new-token.lifespan", "" + (1 * 60 * 60 * 24 * 365));
    }

    public static void main(String[] args) {
        String token = signToken(new HashMap<>());
        System.out.println(token);
    }

    /**
     * @param jsonMap, map of the JsonObject to be signed.
     * @return jwt string token, signed with the private key stored under /resources/FOR_DEVELOPMENT_PURPOSE_ONLY-privateKey.pem
     */
    public static String signToken(Map<String, Object> jsonMap) {
        return Jwt.sign(jsonMap);
    }
}
