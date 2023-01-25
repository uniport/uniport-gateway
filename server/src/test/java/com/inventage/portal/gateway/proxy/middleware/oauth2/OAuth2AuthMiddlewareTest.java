package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.KeycloakServer;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder;
import io.netty.handler.codec.http.HttpHeaders;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.RequestOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;

import static com.inventage.portal.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static com.inventage.portal.gateway.proxy.middleware.BufferAssert.assertThat;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddleware.OIDC_PARAM_PKCE;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddleware.OIDC_PARAM_REDIRECT_URI;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddleware.OIDC_PARAM_STATE;
import static com.inventage.portal.gateway.proxy.middleware.session.SessionMiddleware.COOKIE_NAME_DEFAULT;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

@ExtendWith(VertxExtension.class)
public class OAuth2AuthMiddlewareTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2AuthMiddlewareTest.class);
    public static final String PKCE_METHOD_PLAIN = "plain";
    public static final String PKCE_METHOD_S256 = "S256";
    public static final String RESPONSE_MODE_FORM_POST = "form_post";
    public static final String CODE_CHALLENGE_METHOD = "code_challenge_method";
    public static final String CODE_CHALLENGE = "code_challenge";

    private static final String PREFIX_STATE = "oauth2_state_";

    @Test
    void redirectForAuthenticationRequest(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, "localhost").startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = portalGateway(vertx)
                .withSessionMiddleware()
                .withOAuth2AuthMiddlewareForScope(keycloakServer, "protected")
                .build().start();
        final String protectedResource = "http://localhost:8080/protected";
        // when
        gateway.incomingRequest(GET, protectedResource, testCtx, (outgoingResponse) -> {
            // then
            assertThat(outgoingResponse)
                    .isValidAuthenticationRequest(Map.of(
                            "redirect_uri", protectedResource,
                            "scope", "openid test")
                    );
            testCtx.completeNow();
            keycloakServer.closeServer();
        });
    }

    @Test
    void blockSecondAuthenticationRequestForSameApplication(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, "localhost", TestUtils.findFreePort())
                .startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
                .withSessionMiddleware()
                .withOAuth2AuthMiddlewareForScope(keycloakServer, "test")
                .build().start();

        gateway.incomingRequest(GET, "http://localhost:8080/test/one", testCtx, (outgoingResponse) -> {
            assertThat(outgoingResponse).isValidAuthenticationRequest().isUsingFormPost().hasPKCE();
            // when
            final String protectedResource2 = "http://localhost:8080/test/two";
            gateway.incomingRequest(GET, protectedResource2, withCookie(cookieFrom(outgoingResponse)), testCtx, (secondOutgoingResponse) -> {
                // then
                assertThat(secondOutgoingResponse)
                        .isRedirectTo(protectedResource2)
                        .hasNotSetCookieForSession();
                testCtx.completeNow();
                keycloakServer.closeServer();
            });
        });
    }

    @Test
    void blockThirdAuthenticationRequestForSameApplication(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, "localhost", TestUtils.findFreePort())
                .startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
                .withSessionMiddleware()
                .withOAuth2AuthMiddlewareForScope(keycloakServer, "test")
                .build().start();

        gateway.incomingRequest(GET, "http://localhost:8080/test/one", testCtx, (outgoingResponse) -> {
            assertThat(outgoingResponse).isValidAuthenticationRequest().isUsingFormPost().hasPKCE();
            // when
            gateway.incomingRequest(GET, "http://localhost:8080/test/two", withCookie(cookieFrom(outgoingResponse)), testCtx, (secondOutgoingResponse) -> {
                assertThat(outgoingResponse).isValidAuthenticationRequest().isUsingFormPost().hasPKCE();
                final String protectedResource3 = "http://localhost:8080/test/three";
                gateway.incomingRequest(GET, protectedResource3, withCookie(cookieFrom(outgoingResponse)), testCtx, (thirdOutgoingResponse) -> {
                    // then
                    assertThat(thirdOutgoingResponse)
                            .isRedirectTo(protectedResource3)
                            .hasNotSetCookieForSession();
                    testCtx.completeNow();
                    keycloakServer.closeServer();
                });

            });
        });
    }

    @Test
    void blockSecondAuthenticationRequestForDifferentApplications(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, "localhost", TestUtils.findFreePort())
                .startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
                .withSessionMiddleware()
                .withOAuth2AuthMiddlewareForScope(keycloakServer, "scope1")
                .withOAuth2AuthMiddlewareForScope(keycloakServer, "scope2")
                .build().start();
        gateway.incomingRequest(GET, "http://localhost:8080/scope1/one", testCtx, (outgoingResponse) -> {
            assertThat(outgoingResponse).isValidAuthenticationRequest().isUsingFormPost().hasPKCE();
            // when
            final String protectedResource2 = "http://localhost:8080/scope2/two";
            gateway.incomingRequest(GET, protectedResource2, withCookie(cookieFrom(outgoingResponse)), testCtx, (secondOutgoingResponse) -> {
                // then
                assertThat(secondOutgoingResponse)
                        .isRedirectTo(protectedResource2)
                        .hasNotSetCookieForSession();
                testCtx.completeNow();
                keycloakServer.closeServer();
            });
        });
    }

    private String cookieFrom(HttpClientResponse response) {
        String set_cookie = response.getHeader(HttpHeaders.Names.SET_COOKIE);
        return Arrays.stream(set_cookie.split(";")).filter(element -> element.startsWith(COOKIE_NAME_DEFAULT)).findFirst().orElse(null);
    }

    private RequestOptions withCookie(String cookieHeaderValue) {
        return new RequestOptions().putHeader(HttpHeaders.Names.COOKIE, cookieHeaderValue);
    }

    @Test
    void redirectForAuthenticationRequestHasPKCE(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx).startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = MiddlewareServerBuilder.portalGateway(vertx)
                    .withSessionMiddleware()
                    .withOAuth2AuthMiddlewareForScope(keycloakServer, "protectedScope")
                    .build().start();
        // when
        gateway.incomingRequest(GET, "/protectedScope", testCtx, (redirectResponse) -> {
            // then
            assertThat(redirectResponse)
                    .hasPKCE();
            testCtx.completeNow();
            keycloakServer.closeServer();
        });
    }

    @Test
    void redirectForAuthenticationRequestHasResponseModeFormPost(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx).startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = MiddlewareServerBuilder.portalGateway(vertx)
                .withSessionMiddleware()
                .withOAuth2AuthMiddlewareForScope(keycloakServer, "protectedScope")
                .build().start();
        // when
        gateway.incomingRequest(GET, "/protectedScope", testCtx, (redirectResponse) -> {
            // then
            assertThat(redirectResponse).isUsingFormPost();
            testCtx.completeNow();
            keycloakServer.closeServer();
        });
    }

    @Test
    void redirectAfterAuthenicationFlow(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        // We need to mock the sessionstate to pass the OAuth2Middleware checks
        Map<String, String> sessionState = Map.of(
                OIDC_PARAM_STATE, "abcdef",
                OIDC_PARAM_REDIRECT_URI, "http://localhost:12345",
                OIDC_PARAM_PKCE, "someValue"
        );
        final KeycloakServer keycloakServer = new KeycloakServer(vertx);
        keycloakServer.startWithDefaultDiscoveryHandlerAndCustomTokenBodyHandler((bodyHandler -> {
            //then
            assertThat(bodyHandler)
                    .hasKeyValue("grant_type", "authorization_code")
                    .hasKeyValue("response_mode", RESPONSE_MODE_FORM_POST)
                    .hasKeyValue("code", "ghijklmnop")
                    .hasKeyValue("code_verifier", sessionState.get(OIDC_PARAM_PKCE));
        }));
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
                .withSessionMiddleware()
                .withCustomSessionState(sessionState)
                .withOAuth2AuthMiddlewareForScope(keycloakServer, "test")
                .build().start();

        // when
        String callback = sessionState.get(OIDC_PARAM_REDIRECT_URI) + "/callback/test?state=" + sessionState.get(OIDC_PARAM_STATE) + "&code=ghijklmnop";
        gateway.incomingRequest(POST, callback, new RequestOptions(), testCtx, httpClientResponse -> {
            // then
            assertThat(httpClientResponse).isRedirectTo(sessionState.get(OIDC_PARAM_REDIRECT_URI));
            testCtx.completeNow();
            keycloakServer.closeServer();
        });
    }

    @Test
    void testSuccessfulPKCEFlow(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        // We need to mock the sessionstate to pass the OAuth2Middleware checks
        Map<String, String> sessionState = Map.of(
                OIDC_PARAM_STATE, "abcdef",
                OIDC_PARAM_REDIRECT_URI, "http://localhost:12345",
                OIDC_PARAM_PKCE, "someValue"
        );
        final KeycloakServer keycloakServer = new KeycloakServer(vertx).startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = MiddlewareServerBuilder.portalGateway(vertx)
                    .withSessionMiddleware()
                    .withCustomSessionState(sessionState)
                    .withOAuth2AuthMiddlewareForScope(keycloakServer, "test")
                    .build().start();
        // when
        gateway.incomingRequest(GET, "/", testCtx, (outgoingResponse) -> {
            // Callback uri must match the sessionstate
            String callback = sessionState.get(OIDC_PARAM_REDIRECT_URI) + "/callback/test?state=" + sessionState.get(OIDC_PARAM_STATE) + "&code=ghijklmnop";
            gateway.incomingRequest(POST, callback, testCtx, httpClientResponse -> {
                // then
                assertThat(httpClientResponse).isRedirectTo(sessionState.get(OIDC_PARAM_REDIRECT_URI));
                testCtx.completeNow();
                keycloakServer.closeServer();
            });
        });
    }

    // Dieser Test überprüft, dass ein Code Callback Request mit ungültigem Session Cookie
    // zu keinem Hänger (PORTAL-1184) führt, sondern Status Code `401` zurückgibt.
    @Test
    void callbackRequestWithInvalidState(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx).startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = portalGateway(vertx)
                    .withSessionMiddleware()
                    .withOAuth2AuthMiddlewareForScope(keycloakServer, "test")
                    .build().start();
        // when
        gateway.incomingRequest(POST, "http://localhost:8080/callback/test?state=anyInvalidState&code=anyCode", testCtx, (outgoingResponse) -> {
            // then
            assertThat(outgoingResponse)
                    .hasStatusCode(401);
            testCtx.completeNow();
            keycloakServer.closeServer();
        });
    }
}