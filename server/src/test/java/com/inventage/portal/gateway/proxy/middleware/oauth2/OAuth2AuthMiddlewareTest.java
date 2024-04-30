package com.inventage.portal.gateway.proxy.middleware.oauth2;

import static com.inventage.portal.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddleware.OIDC_PARAM_PKCE;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddleware.OIDC_PARAM_REDIRECT_URI;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddleware.OIDC_PARAM_STATE;
import static com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.BrowserConnected;
import com.inventage.portal.gateway.proxy.middleware.KeycloakServer;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder;
import com.inventage.portal.gateway.proxy.middleware.oauth2.relyingParty.StateWithUri;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.RequestOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class OAuth2AuthMiddlewareTest {
    public static final String PKCE_METHOD_PLAIN = "plain";
    public static final String PKCE_METHOD_S256 = "S256";
    public static final String CODE_CHALLENGE_METHOD = "code_challenge_method";
    public static final String CODE_CHALLENGE = "code_challenge";

    @Test
    void redirectForAuthenticationRequest(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, "localhost")
            .startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "protected")
            .build().start();
        final String protectedResource = "http://localhost:8080/protected";
        // when
        gateway.incomingRequest(GET, protectedResource, (outgoingResponse) -> {
            // then
            assertThat(outgoingResponse)
                .isValidAuthenticationRequest(Map.of(
                    "redirect_uri", protectedResource,
                    "scope", "openid test"))
                .isUsingFormPost()
                .hasPKCE();
            testCtx.completeNow();
            keycloakServer.closeServer();
        });
    }

    @Test
    void redirectForTwoAuthenticationRequests(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, "localhost")
            .startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "protected")
            .build().start();
        // when
        gateway.incomingRequest(GET, "http://localhost:8080/protected/one", (outgoingResponse) -> {
            assertThat(outgoingResponse).isValidAuthenticationRequest().isUsingFormPost().hasPKCE();
            // when
            final String protectedResource2 = "http://localhost:8080/protected/two";
            gateway.incomingRequest(GET, protectedResource2, withCookie(cookieFrom(outgoingResponse)),
                (secondOutgoingResponse) -> {
                    // then
                    assertThat(secondOutgoingResponse).isValidAuthenticationRequest()
                        .isUsingFormPost().hasPKCE();
                    testCtx.completeNow();
                    keycloakServer.closeServer();
                });
        });
    }

    @Test
    void redirectForTwoAuthenticationRequests2(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, "localhost")
            .startWithDefaultDiscoveryHandler();
        final BrowserConnected browser = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "protected")
            .build().start().connectBrowser();
        // when
        browser.request(GET, "http://localhost:8080/protected/one")
            .thenCompose(response -> browser.request(GET, "http://localhost:8080/protected/two"))
            .whenComplete((result, error) -> {
                // then
                assertThat(result).isValidAuthenticationRequest().isUsingFormPost().hasPKCE();
                testCtx.completeNow();
                keycloakServer.closeServer();

            });
    }

    @Test
    void redirectForParallelAuthenticationRequests(Vertx vertx, VertxTestContext testCtx)
        throws InterruptedException {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, "localhost")
            .startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "protected")
            .build().start();
        // when
        final String[] sessionCookie = new String[1];
        String initialUri1 = "http://localhost:8080/protected/one";
        gateway.incomingRequest(GET, initialUri1, (outgoingResponse1) -> {
            assertThat(outgoingResponse1).isValidAuthenticationRequest().hasStateWithUri("/protected/one")
                .isUsingFormPost()
                .hasPKCE();
            sessionCookie[0] = cookieFrom(outgoingResponse1);
            String initialUri2 = "http://localhost:8080/protected/two";
            gateway.incomingRequest(GET, initialUri2, withCookie(sessionCookie[0]),
                (outgoingResponse2) -> {
                    // then
                    assertThat(outgoingResponse2).isValidAuthenticationRequest()
                        .hasStateWithUri("/protected/two")
                        .isUsingFormPost().hasPKCE();
                });
            String initialUri3 = "http://localhost:8080/protected/three";
            gateway.incomingRequest(GET, initialUri3, withCookie(sessionCookie[0]),
                (outgoingResponse3) -> {
                    // then
                    assertThat(outgoingResponse3).isValidAuthenticationRequest()
                        .hasStateWithUri("/protected/three")
                        .isUsingFormPost().hasPKCE();
                    testCtx.completeNow();
                    keycloakServer.closeServer();
                });
        });
    }

    @Test
    void redirectForAuthenticationRequestHasPKCE(Vertx vertx, VertxTestContext testCtx)
        throws InterruptedException {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx).startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = MiddlewareServerBuilder.portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "protectedScope")
            .build().start();
        // when
        gateway.incomingRequest(GET, "/protectedScope", (redirectResponse) -> {
            // then
            assertThat(redirectResponse)
                .hasPKCE();
            testCtx.completeNow();
            keycloakServer.closeServer();
        });
    }

    @Test
    void redirectForAuthenticationRequestHasResponseModeFormPost(Vertx vertx, VertxTestContext testCtx)
        throws InterruptedException {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx).startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = MiddlewareServerBuilder.portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "protectedScope")
            .build().start();
        // when
        gateway.incomingRequest(GET, "/protectedScope", (redirectResponse) -> {
            // then
            assertThat(redirectResponse).isUsingFormPost();
            testCtx.completeNow();
            keycloakServer.closeServer();
        });
    }

    @Test
    void callbackRequestWithExistingState(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        // We need to mock the sessionstate to pass the OAuth2Middleware checks
        final Map<String, String> oidcSessionState = oidcSessionState("aState",
            "http://localhost:12345/a/b/c?p1=v1&p2=v2#fragment1", "aPKCE");
        final KeycloakServer keycloakServer = new KeycloakServer(vertx)
            .startWithDefaultDiscoveryHandlerAndCustomTokenBodyHandler((bodyHandler -> {
            }));
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withCustomSessionState(oidcSessionState)
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "test")
            .build().start();
        // when
        gateway.incomingRequest(POST,
            "/callback/test?state=" + oidcSessionState.get(OIDC_PARAM_STATE) + "&code=ghijklmnop",
            httpClientResponse -> {
                // then
                assertThat(httpClientResponse).isRedirectTo("/a/b/c?p1=v1&p2=v2#fragment1");
                testCtx.completeNow();
                keycloakServer.closeServer();
            });
    }

    // Dieser Test überprüft, dass ein Code Callback Request mit ungültigem Session Cookie
    // zu keinem Hänger (PORTAL-1184) führt, sondern Status Code `401` zurückgibt.
    @Test
    void callbackRequestWithNonExistingStateWithoutURI(Vertx vertx, VertxTestContext testCtx)
        throws InterruptedException {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx).startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "test")
            .build().start();
        // when
        gateway.incomingRequest(POST, "http://localhost:8080/callback/test?state=anyInvalidState&code=anyCode",
            (outgoingResponse) -> {
                // then
                assertThat(outgoingResponse)
                    .hasStatusCode(HttpResponseStatus.GONE.code());
                testCtx.completeNow();
                keycloakServer.closeServer();
            });
    }

    // Dieser Test überprüft, dass ein Code Callback Request mit ungültigem Session Cookie
    // zu keinem Hänger (PORTAL-1184) führt, sondern Status Code `307` zurückgibt, falls im state Parameter enthalten.
    @Test
    void callbackRequestWithNonExistingStateWithURI(Vertx vertx, VertxTestContext testCtx)
        throws InterruptedException {
        // given
        String state = new StateWithUri("aNonExistingState", "/test/resource").toStateParameter();
        final KeycloakServer keycloakServer = new KeycloakServer(vertx).startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "test")
            .build().start();
        // when
        gateway.incomingRequest(POST, "http://localhost:8080/callback/test?state=" + state + "&code=anyCode",
            (outgoingResponse) -> {
                // then
                assertThat(outgoingResponse)
                    .hasStatusCode(HttpResponseStatus.TEMPORARY_REDIRECT.code());
                testCtx.completeNow();
                keycloakServer.closeServer();
            });
    }

    @Test
    void testSuccessfulPKCEFlow(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        // We need to mock the sessionstate to pass the OAuth2Middleware checks
        final Map<String, String> oidcSessionState = oidcSessionState("aState", "http://localhost:12345", "aPKCE");
        final KeycloakServer keycloakServer = new KeycloakServer(vertx).startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = MiddlewareServerBuilder.portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withCustomSessionState(oidcSessionState)
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "test")
            .build().start();
        gateway.incomingRequest(GET, "/", (outgoingResponse) -> {
            // when
            gateway.incomingRequest(POST,
                "/callback/test?state=" + oidcSessionState.get(OIDC_PARAM_STATE) + "&code=ghijklmnop",
                httpClientResponse -> {
                    // then
                    assertThat(httpClientResponse).isRedirectTo("");
                    testCtx.completeNow();
                    keycloakServer.closeServer();
                });
        });
    }

    // Dieser Test stellt den kompletten Flow für PORTAL-1184 nach.
    @Test
    void fixed_PORTAL_1184(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String protectedResource1 = "http://localhost:8080/protected1/one";
        final String protectedResource2 = "http://localhost:8080/protected2/two";
        final String protectedResource3 = "http://localhost:8080/protected2/three";
        // We need to mock the sessionstate to pass the OAuth2Middleware checks
        final Map<String, String> oidcSessionState = oidcSessionState("aState", protectedResource1, "aPKCE");
        final KeycloakServer keycloakServer = new KeycloakServer(vertx)
            .startWithDefaultDiscoveryHandlerAndCustomTokenBodyHandler((bodyHandler -> {
            }));
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withCustomSessionState(oidcSessionState)
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "protected1")
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "protected2")
            .build().start();
        String[] sessionCookies = new String[1];
        // 1: callback -> code-to-token -> authenticated session
        gateway.incomingRequest(POST,
            "/callback/protected1?state=" + oidcSessionState.get(OIDC_PARAM_STATE)
                + "&code=THE-CODE",
            response1 -> {
                assertThat(response1).isRedirectTo("/protected1/one");
                sessionCookies[0] = cookieFrom(response1);
                // 2: protected resource -> auth flow 1 startet
                gateway.incomingRequest(GET, protectedResource2, withCookie(sessionCookies[0]),
                    (response2) -> {
                        assertThat(response2).isValidAuthenticationRequest()
                            .hasStateWithUri("/protected2/two");
                        final String state2 = TestUtils
                            .extractParametersFromHeader(response2
                                .getHeader("location"))
                            .get("state");
                        // 3: protected resource -> auth flow 2 startet
                        gateway.incomingRequest(GET, protectedResource3,
                            withCookie(sessionCookies[0]),
                            (response3) -> {
                                assertThat(response3)
                                    .isValidAuthenticationRequest()
                                    .hasStateWithUri(
                                        "/protected2/three");
                                final String state3 = TestUtils
                                    .extractParametersFromHeader(
                                        response3.getHeader(
                                            "location"))
                                    .get("state");
                                // 4: callback for auth flow 2 -> code-to-token -> session with regenerated id
                                gateway.incomingRequest(POST,
                                    "/callback/protected2?state="
                                        + state2
                                        + "&code=THE-CODE",
                                    withCookie(sessionCookies[0]),
                                    (response4) -> {
                                        assertThat(response4)
                                            .isRedirectTo("http://localhost:8080/protected2/two")
                                            .hasSetSessionCookieDifferentThan(
                                                sessionCookies[0]);
                                        // when
                                        // 5:
                                        gateway.incomingRequest(
                                            POST,
                                            "/callback/protected2?state="
                                                + state3
                                                + "&code=THE-CODE",
                                            withCookie(sessionCookies[0]),
                                            (response5) -> {
                                                // then
                                                assertThat(response5)
                                                    .isRedirectTo("/protected2/three")
                                                    .hasSetSessionCookieDifferentThan(
                                                        sessionCookies[0]);
                                                testCtx.completeNow();
                                                keycloakServer.closeServer();
                                            });
                                    });
                            });
                    });
            });
    }

    // --------------------------------- helper functions ---------------------------------

    private Map<String, String> oidcSessionState(String state, String redirect_uri, String pkce) {
        return Map.of(
            OIDC_PARAM_STATE, stateWithUri(state, redirect_uri),
            OIDC_PARAM_REDIRECT_URI, redirect_uri,
            OIDC_PARAM_PKCE, pkce);
    }

    private String stateWithUri(String aState, String protectedResource1) {
        return new StateWithUri(aState, protectedResource1).toStateParameter();
    }

    private String cookieFrom(HttpClientResponse response) {
        String set_cookie = response.getHeader(HttpHeaderNames.SET_COOKIE);
        return Arrays.stream(
            set_cookie.split(";"))
            .filter(element -> element.startsWith(DEFAULT_SESSION_COOKIE_NAME))
            .findFirst()
            .orElse(null);

    }

    private RequestOptions withCookie(String cookieHeaderValue) {
        return new RequestOptions().putHeader(HttpHeaderNames.COOKIE, cookieHeaderValue);
    }

}
