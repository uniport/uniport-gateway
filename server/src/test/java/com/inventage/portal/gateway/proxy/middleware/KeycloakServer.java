package com.inventage.portal.gateway.proxy.middleware;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.router.RouterFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CountDownLatch;

public class KeycloakServer {

    private final Vertx vertx;
    private final String host;
    private final int port;

    private HttpServer server;

    private final static String SESSION_SCOPE = "test";
    private final static String HTTP_PREFIX = "http://";
    private final static String EMPTY_STRING = "";
    private final static String AUTHORIZATION_ENDPOINT_KEY = "authorization_endpoint";
    private final static String TOKEN_ENDPOINT_KEY = "token_endpoint";
    private final static String OPENID_DISCOVERY_PATH = "/.well-known/openid-configuration";
    private final static String TOKEN_ENDPOINT_PATH = "/auth/realms/test/protocol/openid-connect/token";
    private final static String ACCESS_TOKEN_KEY = "access_token";
    private final static String RANDOM_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2V4YW1wbGUuYXV0aDAuY29tLyIsImF1ZCI6Imh0dHBzOi8vYXBpLmV4YW1wbGUuY29tL2NhbGFuZGFyL3YxLyIsInN1YiI6InVzcl8xMjMiLCJpYXQiOjE0NTg3ODU3OTYsImV4cCI6MTQ1ODg3MjE5Nn0.CA7eaHjIHz5NxeIJoFK9krqaeZrPLwmMmgI_XiQiIkQ";

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

    private String getDefaultDiscoveryUrl() {
        return formatURL(this.host) + ":" + this.port;
    }

    private JsonObject getDefaultDiscoveryResponse() {
        JsonObject discoveryResponse = new JsonObject();
        discoveryResponse.put(AUTHORIZATION_ENDPOINT_KEY, this.getDefaultDiscoveryUrl());
        discoveryResponse.put(TOKEN_ENDPOINT_KEY, this.getDefaultDiscoveryUrl() + TOKEN_ENDPOINT_PATH);
        return discoveryResponse;
    }

    private JsonObject getDefaultTokenEndpointResponse() {
        JsonObject tokenEndpointResponse = new JsonObject();
        tokenEndpointResponse.put(ACCESS_TOKEN_KEY, RANDOM_ACCESS_TOKEN);
        return tokenEndpointResponse;
    }

    private String formatURL(String url) {
        if (url.startsWith(HTTP_PREFIX)) {
            return url;
        }
        return HTTP_PREFIX.concat(url);
    }
}
