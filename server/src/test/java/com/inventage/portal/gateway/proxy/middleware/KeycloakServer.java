package com.inventage.portal.gateway.proxy.middleware;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.router.RouterFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class KeycloakServer {

    private final Vertx vertx;
    private final String host;
    private final int port;

    private HttpServer server;

    private final static String SESSION_SCOPE = "test";
    private final static String HTTP_PREFIX = "http://";
    private final static String EMPTY_STRING = "";
    private final static String OPENID_DISCOVERY_PATH = "/.well-known/openid-configuration";

    private final static String AUTHORIZATION_ENDPOINT_KEY = "authorization_endpoint";

    private final static String TOKEN_ENDPOINT_KEY = "token_endpoint";
    private final static String TOKEN_ENDPOINT_PATH = "/auth/realms/test/protocol/openid-connect/token";
    private final static String ACCESS_TOKEN_KEY = "access_token";
    private final static String RANDOM_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2V4YW1wbGUuYXV0aDAuY29tLyIsImF1ZCI6Imh0dHBzOi8vYXBpLmV4YW1wbGUuY29tL2NhbGFuZGFyL3YxLyIsInN1YiI6InVzcl8xMjMiLCJpYXQiOjE0NTg3ODU3OTYsImV4cCI6MTQ1ODg3MjE5Nn0.CA7eaHjIHz5NxeIJoFK9krqaeZrPLwmMmgI_XiQiIkQ";

    private final static String JWKs_URI_KEY = "jwks_uri";
    private final static String JWKS_URIS_PATH = "/auth/realms/test/protocol/openid-connect/certs";
    private final static String JWKS_KEYS_KEY = "keys";
    private final static String JWK_KID_KEY = "kid";
    private final static String JWK_KTY_KEY = "kty";
    private final static String JWK_ALG_KEY = "alg";
    private final static String JWK_USE_KEY = "use";
    private final static String JWK_MODULUS_KEY = "n";
    private final static String JWK_PUBLIC_EXPONENT_KEY = "e";
    private final static String RANDOM_JWK_KID = "-xQUHcerDnrhFl6deB8Vw0f4GKPsY6BZMDyHTIbOnL4";
    private final static String RSA_JWK_KTY = "RSA";
    private final static String RSA256_JWK_ALG = "RSA256";
    private final static String SIGNING_JWK_USE = "sig";
    private final static String RANDOM_JWK_MODULUS = "xiuRv-X8jt5nKmq0CFv2YZjBfr5MZsdFIBA_MScN2JHxul8kB_zKdgJJ23U-K4vlAsBLVSc0JgInqUHl8un4Nk6_L0Fip9qRJ0TDt7gpDCEVH-FyvXgnlNo7tV94ALuByPt-dO94eqIlnoG_BLWe6u4sdJop7q1GsZ7S0NIlW-X9V1GSH-V0bTfX-9a6VvzAuKy3Yl6WlT6PD7T5waY--oZnnQocI2x9wDpI7lDrP5uZlAiJORoJdlKWNlQnENMTHtRu8wFo71fePynxxhr2ScMjgbER87U0b5mMXH2RBI25EMuYwNdLQB5rEQs5uE99bY3NYRe3Z7mRhE_KYS3qlw";
    private final static String RANDOM_JWK_PUBLIC_EXPONENT = "AQAB";

    public KeycloakServer(Vertx vertx) {
        this(vertx, "localhost", TestUtils.findFreePort());
    }

    public KeycloakServer(Vertx vertx, String host) {
        this(vertx, host, TestUtils.findFreePort());
    }

    public KeycloakServer(Vertx vertx, String host, int port) {
        this.vertx = vertx;
        this.host = host;
        this.port = port;
    }

    public final void startServerWithCustomHandler(Handler<HttpServerRequest> handler) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        this.server = vertx.createHttpServer().requestHandler(handler)
                .listen(this.port, this.host, ready -> {
                    if (ready.failed()) {
                        throw new RuntimeException(ready.cause());
                    }
                    // ready
                    latch.countDown();
                });
        latch.await();
    }

    public int port() {
        return port;
    }

    public final void closeServer() {
        if (this.server != null) {
            this.server.close();
        }
    }

    public KeycloakServer startWithDefaultDiscoveryHandler() throws InterruptedException {
        JsonObject discoveryResponse = getDefaultDiscoveryResponse();
        JsonObject tokenResponse = getDefaultTokenEndpointResponse();
        startServerWithCustomHandler(
                req -> {
                    if (req.path().equals(OPENID_DISCOVERY_PATH)) {
                        req.response()
                                .putHeader("content-type", "application/json")
                                .setStatusCode(200)
                                .send(discoveryResponse.encode());
                    } else if (req.path().equals(TOKEN_ENDPOINT_PATH)) {
                        req.response()
                                .putHeader("content-type", "application/json")
                                .setStatusCode(200)
                                .send(tokenResponse.encode());
                    }
                });
        return this;
    }

    public KeycloakServer startWithDefaultDiscoveryHandlerAndCustomTokenBodyHandler(Handler<Buffer> bodyHandler)
            throws InterruptedException {
        JsonObject discoveryResponse = getDefaultDiscoveryResponse();
        JsonObject tokenResponse = getDefaultTokenEndpointResponse();
        startServerWithCustomHandler(
                req -> {
                    if (req.path().equals(OPENID_DISCOVERY_PATH)) {
                        req.response()
                                .putHeader("content-type", "application/json")
                                .setStatusCode(200)
                                .send(discoveryResponse.encode());
                    } else if (req.path().equals(TOKEN_ENDPOINT_PATH)) {
                        req.bodyHandler(bodyHandler);
                        req.response()
                                .putHeader("content-type", "application/json")
                                .setStatusCode(200)
                                .send(tokenResponse.encode());
                    }
                });
        return this;
    }

    public KeycloakServer startWithDefaultDiscoveryHandlerAndDefaultJWKsURIHandler() throws InterruptedException {
        JsonObject discoveryResponse = getDefaultDiscoveryResponse();
        JsonObject jwksURIResponse = getDefaultJWKsURIResponse();
        startServerWithCustomHandler(
                req -> {
                    if (req.path().equals(OPENID_DISCOVERY_PATH)) {
                        req.response()
                                .putHeader("content-type", "application/json")
                                .setStatusCode(200)
                                .send(discoveryResponse.encode());
                    } else if (req.path().equals(JWKS_URIS_PATH)) {
                        req.response()
                                .putHeader("content-type", "application/json")
                                .setStatusCode(200)
                                .send(jwksURIResponse.encode());
                    }
                });
        return this;
    }

    public JsonObject getDefaultOAuth2AuthConfig() {
        JsonObject config = new JsonObject();
        config.put(DynamicConfiguration.MIDDLEWARE_OAUTH2_SESSION_SCOPE, SESSION_SCOPE);
        config.put(DynamicConfiguration.MIDDLEWARE_OAUTH2_CLIENTID, EMPTY_STRING);
        config.put(DynamicConfiguration.MIDDLEWARE_OAUTH2_CLIENTSECRET, EMPTY_STRING);
        config.put(DynamicConfiguration.MIDDLEWARE_OAUTH2_DISCOVERYURL, getDefaultDiscoveryUrl());
        config.put(RouterFactory.PUBLIC_PROTOCOL_KEY, "http");
        config.put(RouterFactory.PUBLIC_HOSTNAME_KEY, this.host);
        config.put(RouterFactory.PUBLIC_PORT_KEY, this.port);
        return config;
    }

    public JsonObject getOAuth2AuthConfig(String scope) {
        JsonObject config = new JsonObject();
        config.put(DynamicConfiguration.MIDDLEWARE_OAUTH2_SESSION_SCOPE, scope);
        config.put(DynamicConfiguration.MIDDLEWARE_OAUTH2_CLIENTID, EMPTY_STRING);
        config.put(DynamicConfiguration.MIDDLEWARE_OAUTH2_CLIENTSECRET, EMPTY_STRING);
        config.put(DynamicConfiguration.MIDDLEWARE_OAUTH2_DISCOVERYURL, getDefaultDiscoveryUrl());
        config.put(RouterFactory.PUBLIC_PROTOCOL_KEY, "http");
        config.put(RouterFactory.PUBLIC_HOSTNAME_KEY, this.host);
        config.put(RouterFactory.PUBLIC_PORT_KEY, this.port);
        return config;
    }

    public JsonObject getBearerOnlyConfig(String issuer, List<String> audience, JsonArray publicKeys) {
        JsonObject config = new JsonObject();
        config.put(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEYS, publicKeys);
        config.put(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_ISSUER, issuer);
        config.put(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_AUDIENCE, new JsonArray(audience));
        config.put(RouterFactory.PUBLIC_PROTOCOL_KEY, "http");
        config.put(RouterFactory.PUBLIC_HOSTNAME_KEY, this.host);
        config.put(RouterFactory.PUBLIC_PORT_KEY, this.port);
        return config;
    }

    private String getDefaultDiscoveryUrl() {
        return formatURL(this.host) + ":" + this.port;
    }

    private JsonObject getDefaultDiscoveryResponse() {
        JsonObject discoveryResponse = new JsonObject();
        discoveryResponse.put(AUTHORIZATION_ENDPOINT_KEY, this.getDefaultDiscoveryUrl());
        discoveryResponse.put(TOKEN_ENDPOINT_KEY, this.getDefaultDiscoveryUrl() + TOKEN_ENDPOINT_PATH);
        discoveryResponse.put(JWKs_URI_KEY, this.getDefaultDiscoveryUrl() + JWKS_URIS_PATH);
        return discoveryResponse;
    }

    private JsonObject getDefaultTokenEndpointResponse() {
        JsonObject tokenEndpointResponse = new JsonObject();
        tokenEndpointResponse.put(ACCESS_TOKEN_KEY, RANDOM_ACCESS_TOKEN);
        return tokenEndpointResponse;
    }

    private JsonObject getDefaultJWKsURIResponse() {

        JsonObject JWK = new JsonObject()
                .put(JWK_KID_KEY, RANDOM_JWK_KID)
                .put(JWK_KTY_KEY, RSA_JWK_KTY)
                .put(JWK_ALG_KEY, RSA256_JWK_ALG)
                .put(JWK_USE_KEY, SIGNING_JWK_USE)
                .put(JWK_MODULUS_KEY, RANDOM_JWK_MODULUS)
                .put(JWK_PUBLIC_EXPONENT_KEY, RANDOM_JWK_PUBLIC_EXPONENT);

        JsonArray JWKs = new JsonArray().add(JWK);

        JsonObject JWKsURIResponse = new JsonObject().put(JWKS_KEYS_KEY, JWKs);
        return JWKsURIResponse;
    }

    private String formatURL(String url) {
        if (url.startsWith(HTTP_PREFIX)) {
            return url;
        }
        return HTTP_PREFIX.concat(url);
    }
}
