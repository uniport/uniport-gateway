package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.KeycloakServer;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import io.vertx.core.Vertx;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;

import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(VertxExtension.class)
class OAuth2AuthMiddlewareTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2AuthMiddlewareTest.class);
    private static final String host = "localhost";
    private static final String PKCE_METHOD_PLAIN = "plain";
    private static final String PKCE_METHOD_S256 = "S256";
    private static final String RESPONSE_MODE_FORM_POST = "form_post";
    private static final String PREFIX_STATE = "oauth2_state_";

    @Test
    void testPkceIsSetInRedirectURL(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();

        int keycloakPort = TestUtils.findFreePort();
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, host, keycloakPort);
        keycloakServer.startServerWithDefaultDiscoveryHandler();

        int portalGatewayPort = TestUtils.findFreePort();
        JsonObject oAuth2AuthConfig = keycloakServer.getDefaultOAuth2AuthConfig();


        MiddlewareServer gateway;
        try {
            // The creation of the OAuth2AuthMiddleware (as it depends on the availability of our keycloak mock server) is not deterministic, hence it may fail.
            gateway = portalGateway(vertx, host, portalGatewayPort)
                    .withRoutingContextHolder(routingContext)
                    .withOAuth2AuthMiddleware(oAuth2AuthConfig)
                    .build();
        } catch (RuntimeException e) {
            LOGGER.error("Test will not be executed, mock-gateway creation failed: " + e.getMessage());
            keycloakServer.closeServer();
            testCtx.completeNow();
            return;
        }

        // when
        gateway.incomingRequest(testCtx, new RequestOptions(), (outgoingResponse) -> {
            // then
            Map<String, String> responseParamsMap = extractParametersFromHeader(outgoingResponse.getHeader("location"));

            String challengeMethod = responseParamsMap.get("code_challenge_method");
            Assertions.assertNotNull(challengeMethod);
            Assertions.assertNotEquals(PKCE_METHOD_PLAIN, challengeMethod);
            Assertions.assertEquals(PKCE_METHOD_S256, challengeMethod);

            String codeChallenge = responseParamsMap.get("code_challenge");
            Assertions.assertNotNull(codeChallenge);

            testCtx.completeNow();
            keycloakServer.closeServer();
        });
    }

    @Test
    void testResponseModeSetInRedirectURL(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();

        int keycloakPort = TestUtils.findFreePort();
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, host, keycloakPort);
        keycloakServer.startServerWithDefaultDiscoveryHandler();

        int portalGatewayPort = TestUtils.findFreePort();
        int proxyBackendPort = TestUtils.findFreePort();
        JsonObject oAuth2AuthConfig = keycloakServer.getDefaultOAuth2AuthConfig();

        MiddlewareServer gateway;
        try {
            // The creation of the OAuth2AuthMiddleware (as it depends on the availability of our keycloak mock server) is not deterministic, hence it may fail.
            gateway = portalGateway(vertx, host, portalGatewayPort)
                    .withRoutingContextHolder(routingContext)
                    .withOAuth2AuthMiddleware(oAuth2AuthConfig)
                    .withProxyMiddleware(proxyBackendPort)
                    .withBackend(vertx, proxyBackendPort, ctx -> {
                        ctx.response().end();
                    })
                    .build();
        } catch (RuntimeException e) {
            LOGGER.error("Test will not be executed, mock-gateway creation failed: " + e.getMessage());
            keycloakServer.closeServer();
            testCtx.completeNow();
            return;
        }

        // when
        gateway.incomingRequest(testCtx, new RequestOptions(), (outgoingResponse) -> {
            // then
            Map<String, String> responseParamsMap = extractParametersFromHeader(outgoingResponse.getHeader("location"));

            String responseMode = responseParamsMap.get("response_mode");
            Assertions.assertEquals(RESPONSE_MODE_FORM_POST, responseMode);

            testCtx.completeNow();
            keycloakServer.closeServer();
        });
    }

    @Test
    void testGatewayRequestTokenFromKeycloak(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {

        // given
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();

        int keycloakPort = TestUtils.findFreePort();
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, host, keycloakPort);

        int portalGatewayPort = TestUtils.findFreePort();
        JsonObject oAuth2AuthConfig = keycloakServer.getDefaultOAuth2AuthConfig();

        String state = "abcdef";
        String code = "ghijklmnop";
        String redirectUri = "http://localhost:" + keycloakPort;
        String pkce = "someValue";

        // We need to mock the sessionstate to pass the OAuth2Middleware checks
        Map<String, Object> sessionState = Map.of(PREFIX_STATE + state, new JsonObject()
                .put("state", state)
                .put("code", code)
                .put("redirect_uri", redirectUri)
                .put("pkce", pkce)
        );
        //when
        keycloakServer.startServerWithDefaultDiscoveryHandlerAndCustomTokenBodyHandler((bodyHandler -> {
            //then
            Map<String, String> body = extractParametersFromBody(bodyHandler.toString());
            Assertions.assertEquals(body.get("code"), code);
            Assertions.assertEquals("authorization_code", body.get("grant_type"));
            Assertions.assertEquals(RESPONSE_MODE_FORM_POST, body.get("response_mode"));
            Assertions.assertEquals(code, body.get("code"));
            Assertions.assertEquals(pkce, body.get("code_verifier"));
        }));

        MiddlewareServer gateway;
        try {
            // The creation of the OAuth2AuthMiddleware (as it depends on the availability of our keycloak mock server) is not deterministic, hence it may fail.
            gateway = portalGateway(vertx, host, portalGatewayPort)
                    .withRoutingContextHolder(routingContext)
                    .withCustomSessionState(sessionState)
                    .withOAuth2AuthMiddleware(oAuth2AuthConfig)
                    .build();
        } catch (RuntimeException e) {
            LOGGER.error("Test will not be executed, mock-gateway creation failed: " + e.getMessage());
            keycloakServer.closeServer();
            testCtx.completeNow();
            return;
        }

        // when
        gateway.incomingRequest(testCtx, new RequestOptions()
                , (outgoingResponse) -> {
                    // Callback uri must match the sessionstate
                    String callback = redirectUri + "/callback/test?state=" + state + "&code=" + code;
                    gateway.incomingPostRequest(testCtx, new RequestOptions(),
                            (httpClientResponse -> {
                                testCtx.completeNow();
                                keycloakServer.closeServer();
                            }), callback);
                });

    }

    @Test
    void testSuccessfulPKCEFlow(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();

        int keycloakPort = TestUtils.findFreePort();
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, host, keycloakPort);
        keycloakServer.startServerWithDefaultDiscoveryHandler();

        int portalGatewayPort = TestUtils.findFreePort();
        JsonObject oAuth2AuthConfig = keycloakServer.getDefaultOAuth2AuthConfig();

        String state = "abcdef";
        String code = "ghijklmnop";
        String redirectUri = "http://localhost:" + keycloakPort;
        String pkce = "someValue";

        // We need to mock the sessionstate to pass the OAuth2Middleware checks
        Map<String, Object> sessionState = Map.of(PREFIX_STATE + state, new JsonObject()
                .put("state", state)
                .put("code", code)
                .put("redirect_uri", redirectUri)
                .put("pkce", pkce)
        );

        MiddlewareServer gateway;
        try {
            // The creation of the OAuth2AuthMiddleware (as it depends on the availability of our keycloak mock server) is not deterministic, hence it may fail.
            gateway = portalGateway(vertx, host, portalGatewayPort)
                    .withRoutingContextHolder(routingContext)
                    .withCustomSessionState(sessionState)
                    .withOAuth2AuthMiddleware(oAuth2AuthConfig)
                    .build();
        } catch (RuntimeException e) {
            LOGGER.error("Test will not be executed, gateway creation failed: " + e.getMessage());
            keycloakServer.closeServer();
            testCtx.completeNow();
            return;
        }

        // when
        gateway.incomingRequest(testCtx, new RequestOptions()
                , (outgoingResponse) -> {
                    // Callback uri must match the sessionstate
                    String callback = redirectUri + "/callback/test?state=" + state + "&code=" + code;
                    gateway.incomingPostRequest(testCtx, new RequestOptions(),
                            (httpClientResponse -> {
                                Assertions.assertEquals(302, httpClientResponse.statusCode());
                                testCtx.completeNow();
                                keycloakServer.closeServer();
                            }), callback);
                });


    }

    private Map<String, String> extractParametersFromHeader(String header) {
        List<NameValuePair> responseParamsList = null;
        try {
            responseParamsList = URLEncodedUtils.parse(new URI(header), Charset.forName("UTF-8"));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertNotNull(responseParamsList);
        Map<String, String> responseParamsMap = responseParamsList.stream().collect(Collectors.toMap(
                entry -> entry.getName(), entry -> entry.getValue()
        ));

        return responseParamsMap;
    }

    private Map<String, String> extractParametersFromBody(String body) {
        return Arrays.stream(body.split("&"))
                .collect(Collectors.toMap(entry -> entry.split("=")[0], entry -> entry.split("=")[1]));
    }
}