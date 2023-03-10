package com.inventage.portal.gateway.proxy.middleware;

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

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class KeycloakServer {

    private final static String SESSION_SCOPE = "test";
    private final static String HTTP_PREFIX = "http://";
    private final static String EMPTY_STRING = "";
    private final static String OPENID_DISCOVERY_PATH = "/.well-known/openid-configuration";
    private final static String TEST_REALM_PATH = "/auth/realms/test";
    private final static String AUTHORIZATION_ENDPOINT_KEY = "authorization_endpoint";
    private final static String TOKEN_ENDPOINT_KEY = "token_endpoint";
    private final static String TOKEN_ENDPOINT_PATH = "/protocol/openid-connect/token";
    private final static String ACCESS_TOKEN_KEY = "access_token";
    private final static String RANDOM_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2V4YW1wbGUuYXV0aDAuY29tLyIsImF1ZCI6Imh0dHBzOi8vYXBpLmV4YW1wbGUuY29tL2NhbGFuZGFyL3YxLyIsInN1YiI6InVzcl8xMjMiLCJpYXQiOjE0NTg3ODU3OTYsImV4cCI6MTQ1ODg3MjE5Nn0.CA7eaHjIHz5NxeIJoFK9krqaeZrPLwmMmgI_XiQiIkQ";
    private final static String JWKs_URI_KEY = "jwks_uri";
    private final static String JWKS_URIS_PATH = "/protocol/openid-connect/certs";
    private final static String JWKS_KEYS_KEY = "keys";
    private final static String JWK_KID_KEY = "kid";
    private final static String JWK_KTY_KEY = "kty";
    private final static String JWK_ALG_KEY = "alg";
    private final static String JWK_USE_KEY = "use";
    private final static String JWK_MODULUS_KEY = "n";
    private final static String JWK_PUBLIC_EXPONENT_KEY = "e";
    private final static String RANDOM_JWK_KID = "-xQUHcerDnrhFl6deB8Vw0f4GKPsY6BZMDyHTIbOnL4";
    private final static String RSA_JWK_KTY = "RSA";
    private final static String RS256_JWK_ALG = "RS256";
    private final static String SIGNING_JWK_USE = "sig";
    private final static String RANDOM_JWK_MODULUS = "uFJ0A754CTB9-mhomn9Z1aVCiSliTm7Mow3PkWko7PCRVshrqqJEHNg6fgl4KNH-u0ZBjq4L5AKtTuwhsx2vIcJ8aJ3mQNdyxFU02nLaNzOVm-rOwytUPflAnYIgqinmiFpqyQ8vwj_L82F5kN5hnB-G2heMXSep4uoq--2ogdyLtRi4CCr2tuFdPMcdvozsafRJjgJrmKkGggoembuIN5mvuJ_YySMmE3F-TxXOVbhZqAuH4A2-9l0d1rbjghJnv9xCS8Tc7apusoK0q8jWyBHp6p12m1IFkrKSSRiXXCmoMIQO8ZTCzpyqCQEgOXHKvxvSPRWsSa4GZWHzH3hvRQ";
    private final static String RANDOM_JWK_PUBLIC_EXPONENT = "AQAB";
    private final Vertx vertx;
    private final String host;
    private final int port;
    private HttpServer server;

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
                    if (req.path().equals(TEST_REALM_PATH + OPENID_DISCOVERY_PATH)) {
                        req.response()
                                .putHeader("content-type", "application/json")
                                .setStatusCode(200)
                                .send(discoveryResponse.encode());
                    }
                    else if (req.path().equals(TEST_REALM_PATH + TOKEN_ENDPOINT_PATH)) {
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
                    if (req.path().equals(TEST_REALM_PATH + OPENID_DISCOVERY_PATH)) {
                        req.response()
                                .putHeader("content-type", "application/json")
                                .setStatusCode(200)
                                .send(discoveryResponse.encode());
                    }
                    else if (req.path().equals(TEST_REALM_PATH + TOKEN_ENDPOINT_PATH)) {
                        req.bodyHandler(bodyHandler);
                        req.response()
                                .putHeader("content-type", "application/json")
                                .setStatusCode(200)
                                .send(tokenResponse.encode());
                    }
                });
        return this;
    }

    public KeycloakServer startDiscoveryHandlerWithJWKsURIAndDefaultJWKsURIHandler() throws InterruptedException {
        JsonObject discoveryResponse = getDiscoveryResponseWithJWKsURI();
        JsonObject jwksURIResponse = getDefaultJWKsURIResponse();
        startServerWithCustomHandler(
                req -> {
                    if (req.path().equals(TEST_REALM_PATH + OPENID_DISCOVERY_PATH)) {
                        req.response()
                                .putHeader("content-type", "application/json")
                                .setStatusCode(200)
                                .send(discoveryResponse.encode());
                    }
                    else if (req.path().equals(TEST_REALM_PATH + JWKS_URIS_PATH)) {
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
        return formatURL(this.host) + ":" + this.port + TEST_REALM_PATH;
    }

    private JsonObject getDefaultDiscoveryResponse() {
        JsonObject discoveryResponse = new JsonObject();
        discoveryResponse.put(AUTHORIZATION_ENDPOINT_KEY, this.getDefaultDiscoveryUrl());
        discoveryResponse.put(TOKEN_ENDPOINT_KEY, this.getDefaultDiscoveryUrl() + TOKEN_ENDPOINT_PATH);
        return discoveryResponse;
    }

    private JsonObject getDiscoveryResponseWithJWKsURI() {
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
                .put(JWK_ALG_KEY, RS256_JWK_ALG)
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
