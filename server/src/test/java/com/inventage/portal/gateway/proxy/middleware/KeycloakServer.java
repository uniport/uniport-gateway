package com.inventage.portal.gateway.proxy.middleware;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareFactoryBase;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;
import com.inventage.portal.gateway.proxy.router.RouterFactory;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class KeycloakServer {

    private static final String SESSION_SCOPE = "test";
    private static final String HTTP_PREFIX = "http://";
    private static final String EMPTY_STRING = "";
    private static final String OPENID_DISCOVERY_PATH = "/.well-known/openid-configuration";
    private static final String TEST_REALM_PATH = "/auth/realms/test";
    private static final String ISSUER_KEY = "issuer";
    private static final String AUTHORIZATION_ENDPOINT_KEY = "authorization_endpoint";
    private static final String TOKEN_ENDPOINT_KEY = "token_endpoint";
    private static final String TOKEN_ENDPOINT_PATH = "/protocol/openid-connect/token";
    private static final String ACCESS_TOKEN_KEY = "access_token";
    private static final String RANDOM_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2V4YW1wbGUuYXV0aDAuY29tLyIsImF1ZCI6Imh0dHBzOi8vYXBpLmV4YW1wbGUuY29tL2NhbGFuZGFyL3YxLyIsInN1YiI6InVzcl8xMjMiLCJpYXQiOjE0NTg3ODU3OTYsImV4cCI6MTQ1ODg3MjE5Nn0.CA7eaHjIHz5NxeIJoFK9krqaeZrPLwmMmgI_XiQiIkQ";

    private static final String JWKS_URI_KEY = "jwks_uri";
    private static final String JWKS_URIS_PATH = "/protocol/openid-connect/certs";
    private static final String JWKS_KEYS_KEY = "keys";
    private static final String JWK_KID_KEY = "kid";
    private static final String JWK_KTY_KEY = "kty";
    private static final String JWK_ALG_KEY = "alg";
    private static final String JWK_USE_KEY = "use";
    private static final String JWK_MODULUS_KEY = "n";
    private static final String JWK_PUBLIC_EXPONENT_KEY = "e";

    private static final String RSA_JWK_KTY = "RSA";
    private static final String RS256_JWK_ALG = "RS256";
    private static final String SIGNING_JWK_USE = "sig";
    // RANDOM_JWK_* matches to FOR_DEVELOPMENT_PURPOSE_ONLY-privateKey.pem
    private static final String RANDOM_JWK_KID = "validKID";
    private static final String RANDOM_JWK_MODULUS = "uFJ0A754CTB9-mhomn9Z1aVCiSliTm7Mow3PkWko7PCRVshrqqJEHNg6fgl4KNH-u0ZBjq4L5AKtTuwhsx2vIcJ8aJ3mQNdyxFU02nLaNzOVm-rOwytUPflAnYIgqinmiFpqyQ8vwj_L82F5kN5hnB-G2heMXSep4uoq--2ogdyLtRi4CCr2tuFdPMcdvozsafRJjgJrmKkGggoembuIN5mvuJ_YySMmE3F-TxXOVbhZqAuH4A2-9l0d1rbjghJnv9xCS8Tc7apusoK0q8jWyBHp6p12m1IFkrKSSRiXXCmoMIQO8ZTCzpyqCQEgOXHKvxvSPRWsSa4GZWHzH3hvRQ";
    private static final String RANDOM_JWK_PUBLIC_EXPONENT = "AQAB";
    // ALTERNATIVERANDOM_JWK_* matches to no private key
    private static final String ALTERNATIVE_RANDOM_JWK_KID = "invalidKID";
    private static final String ALTERNATIVE_RANDOM_JWK_MODULUS = "s_7_WfL-gQmQAEv4FmMdntTUMF2_nXYriw8w2euy8AhooCUr33PBBoSkG7R6cEHcTwkgiqn9U7qfRvpwNyAok8GyJA5KhMhsBolNuXsHxBBzMDSRWs5Byd33F2D0Zi8Nv9DjqIY8K56TWHh8MjDGzMIHHVNpLBhjARTzCUuHUg3BfAcohe0G6bgfnuKNNN9gzY2RHNLVsLQrHvhgqjA-Lx5-EpfAUCCB3fSvuhDLmzI_pn14Qqm788WgvjpDzO5lHWA7Ue3aTDz_SaFAuUOww3-2-btboxP_0tozrn8GfsAb5rHWke3s6W5YJcj3yHk1FSu9BQvrUymTkYmZxTTTnQ";
    private static final String ALTERNATIVE_RANDOM_JWK_PUBLIC_EXPONENT = "AQAB";

    private final Vertx vertx;
    private final VertxTestContext testCtx;
    private final String host;
    private final int port;
    private HttpServer server;
    private boolean serveValidPublicKeys = true;
    private String codeChallenge;

    public KeycloakServer(Vertx vertx, VertxTestContext testCtx) {
        this(vertx, testCtx, "localhost");
    }

    public KeycloakServer(Vertx vertx, VertxTestContext testCtx, String host) {
        this(vertx, testCtx, host, TestUtils.findFreePort());
    }

    public KeycloakServer(Vertx vertx, VertxTestContext testCtx, String host, int port) {
        this.vertx = vertx;
        this.testCtx = testCtx;
        this.host = host;
        this.port = port;
    }

    public KeycloakServer setPKCE(String codeChallenge) {
        this.codeChallenge = codeChallenge;
        return this;
    }

    public final void startServerWithCustomHandler(Handler<HttpServerRequest> handler) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        this.server = vertx
            .createHttpServer()
            .requestHandler(handler)
            .listen(this.port, this.host, ready -> {
                if (ready.failed()) {
                    testCtx.failNow(ready.cause());
                }
                // ready
                latch.countDown();
            });
        latch.await();
    }

    public String host() {
        return host;
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
        final JsonObject discoveryResponse = getDefaultDiscoveryResponse();
        final JsonObject tokenResponse = getDefaultTokenEndpointResponse();
        startServerWithCustomHandler(
            req -> {
                if (req.path().equals(TEST_REALM_PATH + OPENID_DISCOVERY_PATH)) {
                    req.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(200)
                        .send(discoveryResponse.encode());
                } else if (req.path().equals(TEST_REALM_PATH + TOKEN_ENDPOINT_PATH)) {
                    req.setExpectMultipart(true);
                    req.endHandler(v -> {
                        final String codeVerifier = req.formAttributes().get("code_verifier");
                        if (codeChallenge != null && !verifyPKCE(codeChallenge, codeVerifier)) {
                            req.response()
                                .setStatusCode(400)
                                .end();
                            return;
                        }

                        req.response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(200)
                            .send(tokenResponse.encode());
                    });
                } else {
                    req.response()
                        .setStatusCode(404)
                        .end();
                }
            });
        return this;
    }

    public KeycloakServer startWithDefaultDiscoveryHandlerAndCustomTokenBodyHandler(Handler<Buffer> bodyHandler)
        throws InterruptedException {
        final JsonObject discoveryResponse = getDefaultDiscoveryResponse();
        final JsonObject tokenResponse = getDefaultTokenEndpointResponse();
        startServerWithCustomHandler(
            req -> {
                if (req.path().equals(TEST_REALM_PATH + OPENID_DISCOVERY_PATH)) {
                    req.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(200)
                        .send(discoveryResponse.encode());
                } else if (req.path().equals(TEST_REALM_PATH + TOKEN_ENDPOINT_PATH)) {
                    req.bodyHandler(bodyHandler);
                    req.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(200)
                        .send(tokenResponse.encode());
                }
            });
        return this;
    }

    public KeycloakServer startWithDiscoveryHandlerWithJWKsURIAndDefaultJWKsURIHandler() throws InterruptedException {
        final JsonObject discoveryResponse = getDiscoveryResponseWithJWKsURI();
        startServerWithCustomHandler(
            req -> {
                if (req.path().equals(TEST_REALM_PATH + OPENID_DISCOVERY_PATH)) {
                    req.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(200)
                        .send(discoveryResponse.encode());
                } else if (req.path().equals(TEST_REALM_PATH + JWKS_URIS_PATH)) {
                    final JsonObject jwksURIResponse;
                    if (this.serveValidPublicKeys) {
                        jwksURIResponse = getDefaultJWKsURIResponse();
                    } else {
                        jwksURIResponse = getAlternativeJWKsURIResponse();
                    }
                    req.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(200)
                        .send(jwksURIResponse.encode());
                }
            });
        return this;
    }

    public KeycloakServer serveValidPublicKeys() {
        this.serveValidPublicKeys = true;
        return this;
    }

    public KeycloakServer serveInvalidPublicKeys() {
        this.serveValidPublicKeys = false;
        return this;
    }

    public JsonObject getDefaultOAuth2AuthConfig() {
        return getOAuth2AuthConfig(SESSION_SCOPE, true, null);
    }

    public JsonObject getOAuth2AuthConfig(String scope) {
        return getOAuth2AuthConfig(scope, true, null);
    }

    public JsonObject getOAuth2AuthConfig(String scope, boolean proxyAuthenticationFlow, String publicUrl) {
        final JsonObject config = new JsonObject();
        config.put(OAuth2MiddlewareFactory.OAUTH2_SESSION_SCOPE, scope);
        config.put(OAuth2MiddlewareFactory.OAUTH2_CLIENTID, EMPTY_STRING);
        config.put(OAuth2MiddlewareFactory.OAUTH2_CLIENTSECRET, EMPTY_STRING);
        config.put(OAuth2MiddlewareFactory.OAUTH2_DISCOVERYURL, getDefaultDiscoveryUrl());
        config.put(OAuth2MiddlewareFactory.OAUTH2_PROXY_AUTHENTICATION_FLOW, proxyAuthenticationFlow);
        if (publicUrl != null) {
            config.put(OAuth2MiddlewareFactory.OAUTH2_PUBLIC_URL, publicUrl);
        }

        config.put(RouterFactory.PUBLIC_PROTOCOL_KEY, "http");
        config.put(RouterFactory.PUBLIC_HOSTNAME_KEY, this.host);
        config.put(RouterFactory.PUBLIC_PORT_KEY, this.port);
        return config;
    }

    public JsonObject getBearerOnlyConfig(String issuer, List<String> audience, JsonArray publicKeys, boolean reconcilationEnabled, long reconcilationIntervalMs) {
        final JsonObject config = new JsonObject();
        config.put(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS, publicKeys);
        config.put(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_ISSUER, issuer);
        config.put(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_AUDIENCE, new JsonArray(audience));
        config.put(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS_RECONCILIATION, JsonObject.of(
            WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS_RECONCILIATION_ENABLED, reconcilationEnabled,
            WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS_RECONCILIATION_INTERVAL_MS, reconcilationIntervalMs));
        config.put(RouterFactory.PUBLIC_PROTOCOL_KEY, "http");
        config.put(RouterFactory.PUBLIC_HOSTNAME_KEY, this.host);
        config.put(RouterFactory.PUBLIC_PORT_KEY, this.port);
        return config;
    }

    private String getDefaultDiscoveryUrl() {
        return formatURL(this.host) + ":" + this.port + TEST_REALM_PATH;
    }

    private JsonObject getDefaultDiscoveryResponse() {
        final JsonObject discoveryResponse = new JsonObject()
            .put(ISSUER_KEY, this.getDefaultDiscoveryUrl())
            .put(AUTHORIZATION_ENDPOINT_KEY, this.getDefaultDiscoveryUrl())
            .put(TOKEN_ENDPOINT_KEY, this.getDefaultDiscoveryUrl() + TOKEN_ENDPOINT_PATH);
        return discoveryResponse;
    }

    private JsonObject getDiscoveryResponseWithJWKsURI() {
        final JsonObject discoveryResponse = new JsonObject();
        discoveryResponse.put(AUTHORIZATION_ENDPOINT_KEY, this.getDefaultDiscoveryUrl());
        discoveryResponse.put(TOKEN_ENDPOINT_KEY, this.getDefaultDiscoveryUrl() + TOKEN_ENDPOINT_PATH);
        discoveryResponse.put(JWKS_URI_KEY, this.getDefaultDiscoveryUrl() + JWKS_URIS_PATH);
        return discoveryResponse;
    }

    private JsonObject getDefaultTokenEndpointResponse() {
        final JsonObject tokenEndpointResponse = new JsonObject();
        tokenEndpointResponse.put(ACCESS_TOKEN_KEY, RANDOM_ACCESS_TOKEN);
        return tokenEndpointResponse;
    }

    private JsonObject getDefaultJWKsURIResponse() {
        final JsonObject jwk = new JsonObject()
            .put(JWK_KID_KEY, RANDOM_JWK_KID)
            .put(JWK_KTY_KEY, RSA_JWK_KTY)
            .put(JWK_ALG_KEY, RS256_JWK_ALG)
            .put(JWK_USE_KEY, SIGNING_JWK_USE)
            .put(JWK_MODULUS_KEY, RANDOM_JWK_MODULUS)
            .put(JWK_PUBLIC_EXPONENT_KEY, RANDOM_JWK_PUBLIC_EXPONENT);

        final JsonArray jwks = new JsonArray().add(jwk);
        final JsonObject jwksURIResponse = new JsonObject().put(JWKS_KEYS_KEY, jwks);
        return jwksURIResponse;
    }

    private JsonObject getAlternativeJWKsURIResponse() {
        final JsonObject jwk = new JsonObject()
            .put(JWK_KID_KEY, ALTERNATIVE_RANDOM_JWK_KID)
            .put(JWK_KTY_KEY, RSA_JWK_KTY)
            .put(JWK_ALG_KEY, RS256_JWK_ALG)
            .put(JWK_USE_KEY, SIGNING_JWK_USE)
            .put(JWK_MODULUS_KEY, ALTERNATIVE_RANDOM_JWK_MODULUS)
            .put(JWK_PUBLIC_EXPONENT_KEY, ALTERNATIVE_RANDOM_JWK_PUBLIC_EXPONENT);

        final JsonArray jwks = new JsonArray().add(jwk);
        final JsonObject jwksURIResponse = new JsonObject().put(JWKS_KEYS_KEY, jwks);
        return jwksURIResponse;
    }

    private String formatURL(String url) {
        if (url.startsWith(HTTP_PREFIX)) {
            return url;
        }
        return HTTP_PREFIX.concat(url);
    }

    private boolean verifyPKCE(String challenge, String verifier) {
        return challenge.equals(toChallenge(verifier));
    }

    private String toChallenge(String codeVerifier) {
        final MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot get instance of SHA-256 MessageDigest", e);
        }

        final byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
        sha256.update(bytes);
        final byte[] digest = sha256.digest();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }
}
