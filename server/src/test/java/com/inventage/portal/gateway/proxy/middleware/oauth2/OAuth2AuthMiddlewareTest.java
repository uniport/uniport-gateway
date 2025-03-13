package com.inventage.portal.gateway.proxy.middleware.oauth2;

import static com.inventage.portal.gateway.TestUtils.buildConfiguration;
import static com.inventage.portal.gateway.TestUtils.withMiddleware;
import static com.inventage.portal.gateway.TestUtils.withMiddlewareOpts;
import static com.inventage.portal.gateway.TestUtils.withMiddlewares;
import static com.inventage.portal.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddleware.OIDC_PARAM_PKCE;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddleware.OIDC_PARAM_REDIRECT_URI;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddleware.OIDC_PARAM_STATE;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddleware.PREFIX_STATE;
import static com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.BrowserConnected;
import com.inventage.portal.gateway.proxy.middleware.KeycloakServer;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareTestBase;
import com.inventage.portal.gateway.proxy.middleware.oauth2.relyingParty.StateWithUri;
import com.inventage.portal.gateway.proxy.router.RouterFactory;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(VertxExtension.class)
public class OAuth2AuthMiddlewareTest extends MiddlewareTestBase {

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {
        final JsonObject simple = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", OAuth2MiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        OAuth2MiddlewareFactory.CLIENT_ID, "foo",
                        OAuth2MiddlewareFactory.CLIENT_SECRET, "bar",
                        OAuth2MiddlewareFactory.DISCOVERY_URL, "localhost:1234",
                        OAuth2MiddlewareFactory.SESSION_SCOPE, "blub",
                        OAuth2MiddlewareFactory.PROXY_AUTHENTICATION_FLOW, false)))));

        final JsonObject invalidResponseMode = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", OAuth2MiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        OAuth2MiddlewareFactory.CLIENT_ID, "foo",
                        OAuth2MiddlewareFactory.CLIENT_SECRET, "bar",
                        OAuth2MiddlewareFactory.DISCOVERY_URL, "localhost:1234",
                        OAuth2MiddlewareFactory.SESSION_SCOPE, "blub",
                        OAuth2MiddlewareFactory.PROXY_AUTHENTICATION_FLOW, false,
                        OAuth2MiddlewareFactory.RESPONSE_MODE, "blub")))));

        final JsonObject missingOptions = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", OAuth2MiddlewareFactory.TYPE)));

        final JsonObject missingRequiredProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", OAuth2MiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        OAuth2MiddlewareFactory.CLIENT_ID, "foo",
                        OAuth2MiddlewareFactory.DISCOVERY_URL, "localhost:1234",
                        OAuth2MiddlewareFactory.SESSION_SCOPE, "blub",
                        OAuth2MiddlewareFactory.PROXY_AUTHENTICATION_FLOW, false)))));

        final JsonObject unknownProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", OAuth2MiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        "bar", "blub")))));

        return Stream.of(
            Arguments.of("accept simple config", simple, complete, expectedTrue),
            Arguments.of("reject config with no options", missingOptions, complete, expectedFalse),
            Arguments.of("reject config with invalid response mode", invalidResponseMode, complete, expectedFalse),
            Arguments.of("reject config with missing required property", missingRequiredProperty, complete, expectedFalse),
            Arguments.of("reject config with unknown property", unknownProperty, complete, expectedFalse)

        );
    }

    public static final String PKCE_METHOD_PLAIN = "plain";
    public static final String PKCE_METHOD_S256 = "S256";
    public static final String CODE_CHALLENGE_METHOD = "code_challenge_method";
    public static final String CODE_CHALLENGE = "code_challenge";
    public static final String SCOPE = "scope";

    @Test
    void discoveryFailure(Vertx vertx, VertxTestContext testCtx) throws Throwable {
        final OAuth2MiddlewareOptions config = OAuth2MiddlewareOptions.builder()
            .withSessionScope("scope")
            .withClientId("id")
            .withClientSecret("secret")
            .withDiscoveryURL("http://inexistent.host")
            .withEnv(Map.<String, String>of(
                RouterFactory.PUBLIC_PROTOCOL_KEY, "http",
                RouterFactory.PUBLIC_HOSTNAME_KEY, "host",
                RouterFactory.PUBLIC_PORT_KEY, "1234"))
            .build();

        try {
            portalGateway(vertx, testCtx)
                .withSessionMiddleware()
                .withOAuth2AuthMiddleware(config)
                .build()
                .start();
        } catch (Exception e) {
            testCtx.completeNow();
            return;
        }
        testCtx.failNow("Exception expected");
    }

    @Test
    void redirectForAuthenticationRequest(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx, "localhost")
            .startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "test")
            .build()
            .start();
        final String protectedResource = "http://localhost:8080/test";
        final RequestOptions reqOpts = new RequestOptions()
            .addHeader(HttpHeaders.ACCEPT, HttpHeaders.TEXT_HTML.toString());
        // when
        gateway.incomingRequest(GET, protectedResource, reqOpts, (outgoingResponse) -> {
            // then
            assertThat(testCtx, outgoingResponse)
                .isValidAuthenticationRequest(Map.of(
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
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx, "localhost")
            .startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "protected")
            .build().start();
        final RequestOptions reqOpts = new RequestOptions().addHeader(HttpHeaders.ACCEPT, HttpHeaders.TEXT_HTML.toString());
        // when
        gateway.incomingRequest(GET, "http://localhost:8080/protected/one", reqOpts, (outgoingResponse) -> {
            assertThat(testCtx, outgoingResponse)
                .isValidAuthenticationRequest()
                .isUsingFormPost()
                .hasPKCE();
            // when
            final String protectedResource2 = "http://localhost:8080/protected/two";
            gateway.incomingRequest(GET, protectedResource2, withCookie(reqOpts, cookieFrom(outgoingResponse)),
                (secondOutgoingResponse) -> {
                    // then
                    assertThat(testCtx, secondOutgoingResponse)
                        .isValidAuthenticationRequest()
                        .isUsingFormPost()
                        .hasPKCE();
                    testCtx.completeNow();
                    keycloakServer.closeServer();
                });
        });
    }

    @Test
    void redirectForTwoAuthenticationRequests2(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx, "localhost")
            .startWithDefaultDiscoveryHandler();
        final BrowserConnected browser = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "protected")
            .build().start().connectBrowser();
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap().add(HttpHeaders.ACCEPT, HttpHeaders.TEXT_HTML.toString());
        // when
        browser.request(GET, "http://localhost:8080/protected/one")
            .thenCompose(response -> browser.request(GET, "http://localhost:8080/protected/two", headers))
            .whenComplete((result, error) -> {
                // then
                assertThat(testCtx, result)
                    .isValidAuthenticationRequest()
                    .isUsingFormPost()
                    .hasPKCE();
                testCtx.completeNow();
                keycloakServer.closeServer();

            });
    }

    @Test
    void redirectForParallelAuthenticationRequests(Vertx vertx, VertxTestContext testCtx)
        throws InterruptedException {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx, "localhost")
            .startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "protected")
            .build()
            .start();
        // when
        final String[] sessionCookie = new String[1];
        final RequestOptions reqOpts = new RequestOptions().addHeader(HttpHeaders.ACCEPT, HttpHeaders.TEXT_HTML.toString());
        String initialUri1 = "http://localhost:8080/protected/one";
        gateway.incomingRequest(GET, initialUri1, reqOpts, (outgoingResponse1) -> {
            // then
            assertThat(testCtx, outgoingResponse1)
                .isValidAuthenticationRequest()
                .hasStateWithUri("/protected/one")
                .isUsingFormPost()
                .hasPKCE();

            sessionCookie[0] = cookieFrom(outgoingResponse1);
            String initialUri2 = "http://localhost:8080/protected/two";
            gateway.incomingRequest(GET, initialUri2, withCookie(reqOpts, sessionCookie[0]), (outgoingResponse2) -> {
                // then
                assertThat(testCtx, outgoingResponse2)
                    .isValidAuthenticationRequest()
                    .hasStateWithUri("/protected/two")
                    .isUsingFormPost()
                    .hasPKCE();
            });

            String initialUri3 = "http://localhost:8080/protected/three";
            gateway.incomingRequest(GET, initialUri3, withCookie(reqOpts, sessionCookie[0]), (outgoingResponse3) -> {
                // then
                assertThat(testCtx, outgoingResponse3)
                    .isValidAuthenticationRequest()
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
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx).startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = MiddlewareServerBuilder.portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "protectedScope")
            .build()
            .start();
        // when
        gateway.incomingRequest(GET, "/protectedScope", (redirectResponse) -> {
            // then
            assertThat(testCtx, redirectResponse)
                .isValidAuthenticationRequest()
                .hasPKCE();
            testCtx.completeNow();
            keycloakServer.closeServer();
        });
    }

    @Test
    void redirectForAuthenticationRequestHasResponseModeFormPost(Vertx vertx, VertxTestContext testCtx)
        throws InterruptedException {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx).startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = MiddlewareServerBuilder.portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "protectedScope")
            .build()
            .start();
        final RequestOptions reqOpts = new RequestOptions().addHeader(HttpHeaders.ACCEPT, HttpHeaders.TEXT_HTML.toString());
        // when
        gateway.incomingRequest(GET, "/protectedScope", reqOpts, (redirectResponse) -> {
            // then
            assertThat(testCtx, redirectResponse)
                .isValidAuthenticationRequest()
                .isUsingFormPost();
            testCtx.completeNow();
            keycloakServer.closeServer();
        });
    }

    @Test
    void redirectWithExtraParametersForAuthenticationRequest(Vertx vertx, VertxTestContext testCtx) throws Throwable {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx, "localhost")
            .startWithDefaultDiscoveryHandler();
        final OAuth2MiddlewareOptions middlewareConfig = keycloakServer.getOAuth2AuthConfig("test")
            .withAdditionalAuthRequestParameters(Map.<String, String>of(
                "extra", "parameter"));

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddleware(middlewareConfig)
            .build()
            .start();
        final String protectedResource = "http://localhost:8080/protected";
        final RequestOptions reqOpts = new RequestOptions().addHeader(HttpHeaders.ACCEPT, HttpHeaders.TEXT_HTML.toString());
        // when
        gateway.incomingRequest(GET, protectedResource, reqOpts, (outgoingResponse) -> {
            // then
            assertThat(testCtx, outgoingResponse)
                .isValidAuthenticationRequest(Map.of("extra", "parameter"));
            testCtx.completeNow();
            keycloakServer.closeServer();
        });
    }

    @Test
    void redirectWithAdditionalScope(Vertx vertx, VertxTestContext testCtx) throws Throwable {
        // given
        final List<String> scopes = List.of("scopeA", "scopeB", "scopeC");
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx, "localhost")
            .startWithDefaultDiscoveryHandler();
        final OAuth2MiddlewareOptions middlewareConfig = keycloakServer.getOAuth2AuthConfig("test")
            .withAdditionalScopes(scopes);

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddleware(middlewareConfig)
            .build()
            .start();
        final String protectedResource = "http://localhost:8080/protected";
        final RequestOptions reqOpts = new RequestOptions().addHeader(HttpHeaders.ACCEPT, HttpHeaders.TEXT_HTML.toString());
        // when
        gateway.incomingRequest(GET, protectedResource, reqOpts, (outgoingResponse) -> {
            // then
            assertThat(testCtx, outgoingResponse)
                .isValidAuthenticationRequest()
                .hasScopes(scopes);
            testCtx.completeNow();
            keycloakServer.closeServer();
        });
    }

    @Test
    void redirectForAuthenticationRequestRefusingTextHtml(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx, "localhost")
            .startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "protected")
            .build()
            .start();
        final String protectedResource = "http://localhost:8080/protected";
        final RequestOptions reqOpts = new RequestOptions()
            .addHeader(HttpHeaders.ACCEPT, HttpHeaderValues.APPLICATION_JSON.toString());
        // when
        gateway.incomingRequest(GET, protectedResource, reqOpts, (outgoingResponse) -> {
            // then
            assertThat(testCtx, outgoingResponse)
                .isValidAuthenticationRequest(Map.of(
                    "prompt", "none",
                    "response_mode", "query"));
            testCtx.completeNow();
            keycloakServer.closeServer();
        });
    }

    @Test
    void redirectForAuthenticationRequestWithMultipleAcceptHeaders(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx, "localhost")
            .startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "protected")
            .build()
            .start();
        final String protectedResource = "http://localhost:8080/protected";
        final RequestOptions reqOpts = new RequestOptions()
            .addHeader(HttpHeaders.ACCEPT, HttpHeaderValues.APPLICATION_JSON.toString())
            .addHeader(HttpHeaders.ACCEPT, HttpHeaderValues.TEXT_HTML.toString());
        // when
        gateway.incomingRequest(GET, protectedResource, reqOpts, (outgoingResponse) -> {
            // then
            assertThat(testCtx, outgoingResponse)
                .isValidAuthenticationRequest(Map.of(
                    "response_mode", "form_post"));
            testCtx.completeNow();
            keycloakServer.closeServer();
        });
    }

    static Stream<Arguments> provideForPassthroughParamTest() {
        return Stream.of(
            Arguments.of(
                List.of("paramA"),
                Map.of("paramA", List.of("valueA")),
                Map.of("paramA", "valueA")),
            Arguments.of(
                List.of("paramA"),
                Map.of("paramA", List.of("valueA", "valueB")),
                Map.of("paramA", "valueA")),
            Arguments.of(
                List.of("paramA"),
                Map.of("paramB", List.of("valueB")),
                null),
            Arguments.of(
                List.of("paramA", "paramB"),
                Map.of("paramA", List.of("valueA"), "paramB", List.of("valueB")),
                Map.of("paramA", "valueA", "paramB", "valueB")));
    }

    @ParameterizedTest
    @MethodSource("provideForPassthroughParamTest")
    void redirectForAuthenticationRequestWithPassthroughParam(List<String> passthrough, Map<String, List<String>> in, Map<String, String> expected, Vertx vertx, VertxTestContext testCtx) throws Throwable {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx, "localhost")
            .startWithDefaultDiscoveryHandler();
        final OAuth2MiddlewareOptions middlewareConfig = keycloakServer.getOAuth2AuthConfig("test")
            .withPassthroughParameters(passthrough);

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddleware(middlewareConfig)
            .build()
            .start();
        final String protectedResource = "http://localhost:8080/protected";
        final RequestOptions reqOpts = new RequestOptions()
            .addHeader(HttpHeaders.ACCEPT, HttpHeaderValues.TEXT_HTML.toString());

        final String query = in.entrySet().stream()
            .flatMap(entry -> entry.getValue().stream()
                .map(value -> entry.getKey() + "=" + value))
            .reduce((p1, p2) -> p1 + "&" + p2)
            .map(s -> "?" + s)
            .orElse("");

        // when
        gateway.incomingRequest(GET, protectedResource + query, reqOpts, (outgoingResponse) -> {
            // then
            assertThat(testCtx, outgoingResponse)
                .isValidAuthenticationRequest(expected);
            testCtx.completeNow();
            keycloakServer.closeServer();
        });
    }

    @Test
    void callbackRequestWithExistingState(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String url = "http://localhost:12345";
        // We need to mock the sessionstate to pass the OAuth2Middleware checks
        final JsonObject oidcSessionState = oidcSessionState("aState", url, "aPKCE");
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx)
            .startWithDefaultDiscoveryHandlerAndCustomTokenBodyHandler((bodyHandler -> {
                // ?
            }));
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withCustomSessionState(PREFIX_STATE + oidcSessionState.getString(OIDC_PARAM_STATE), oidcSessionState)
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "test")
            .build().start();
        // when
        gateway.incomingRequest(POST, "/callback/test?state=" + oidcSessionState.getString(OIDC_PARAM_STATE) + "&code=ghijklmnop",
            httpClientResponse -> {
                // then
                assertThat(testCtx, httpClientResponse)
                    .isRedirectTo(url);
                testCtx.completeNow();
                keycloakServer.closeServer();
            });
    }

    @Test
    void callbackRequestWithErrorLoginRequired(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx).startWithDefaultDiscoveryHandler();
        // We need to mock the sessionstate to pass the OAuth2Middleware checks
        final JsonObject oidcSessionState = oidcSessionState("aState", "http://localhost:12345", "aPKCE");
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withCustomSessionState(PREFIX_STATE + oidcSessionState.getString(OIDC_PARAM_STATE), oidcSessionState)
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "test")
            .build().start();
        // when
        gateway.incomingRequest(POST, "/callback/test?error=login_required&state=" + oidcSessionState.getString(OIDC_PARAM_STATE) + "&code=ghijklmnop",
            outgoingResponse -> {
                // then
                assertThat(testCtx, outgoingResponse)
                    .hasStatusCode(HttpResponseStatus.UNAUTHORIZED.code());
                testCtx.completeNow();
                keycloakServer.closeServer();
            });
    }

    // Dieser Test überprüft, dass ein Code Callback Request mit ungültigem Session Cookie
    // zu keinem Hänger (PORTAL-1184) führt, sondern Status Code `401` zurückgibt.
    @Test
    void callbackRequestWithNonExistingStateWithoutURI(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx).startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "test")
            .build().start();
        // when
        gateway.incomingRequest(POST, "/callback/test?state=anyInvalidState&code=anyCode",
            (outgoingResponse) -> {
                // then
                assertThat(testCtx, outgoingResponse)
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
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx).startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "test")
            .build().start();
        // when
        gateway.incomingRequest(POST, "/callback/test?state=" + state + "&code=anyCode",
            (outgoingResponse) -> {
                // then
                assertThat(testCtx, outgoingResponse)
                    .hasStatusCode(HttpResponseStatus.SEE_OTHER.code());
                testCtx.completeNow();
                keycloakServer.closeServer();
            });
    }

    @Test
    void callbackRequestWithPKCE(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String url = "http://localhost:12345";
        final String codeVerifier = "aCodeVerifier";
        // We need to mock the sessionstate to pass the OAuth2Middleware checks
        final JsonObject oidcSessionState = oidcSessionState("aState", url, codeVerifier);
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx)
            .setPKCE(createCodeChallenge(codeVerifier)) // is normally set on the authorization request, so we mimic it here
            .startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = MiddlewareServerBuilder.portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withCustomSessionState(PREFIX_STATE + oidcSessionState.getString(OIDC_PARAM_STATE), oidcSessionState)
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "test")
            .build().start();
        gateway.incomingRequest(GET, "/", (outgoingResponse) -> {
            // when
            gateway.incomingRequest(POST,
                "/callback/test?state=" + oidcSessionState.getString(OIDC_PARAM_STATE) + "&code=ghijklmnop",
                httpClientResponse -> {
                    // then
                    assertThat(testCtx, httpClientResponse)
                        .isRedirectTo(url);
                    testCtx.completeNow();
                    keycloakServer.closeServer();
                });
        });
    }

    @Test
    void callbackRequestWithInvalidPKCE(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String url = "http://localhost:12345";
        // We need to mock the sessionstate to pass the OAuth2Middleware checks
        final JsonObject oidcSessionState = oidcSessionState("aState", url, "aCodeVerifier");
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx)
            .setPKCE("anInvalidCodeChallenge") // is normally set on the authorization request, so we mimic it here
            .startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = MiddlewareServerBuilder.portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withCustomSessionState(PREFIX_STATE + oidcSessionState.getString(OIDC_PARAM_STATE), oidcSessionState)
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "test")
            .build().start();
        gateway.incomingRequest(GET, "/", (outgoingResponse) -> {
            // when
            gateway.incomingRequest(POST,
                "/callback/test?state=" + oidcSessionState.getString(OIDC_PARAM_STATE) + "&code=ghijklmnop",
                httpClientResponse -> {
                    // then
                    assertThat(testCtx, httpClientResponse)
                        .hasStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()); // Keycloak serves a 400, but the Oauth2 middleware will convert this into a 500
                    testCtx.completeNow();
                    keycloakServer.closeServer();
                });
        });
    }

    @Test
    void authorizationPathShouldBePatched(Vertx vertx, VertxTestContext testCtx) throws Throwable {
        // given
        final String publicUrl = "http://some.domain/some/path";
        final String scope = "test";
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx, "localhost")
            .startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddleware(keycloakServer.getOAuth2AuthConfig(scope, true, publicUrl))
            .build()
            .start();
        final RequestOptions reqOpts = new RequestOptions().addHeader(HttpHeaders.ACCEPT, HttpHeaders.TEXT_HTML.toString());
        // when
        gateway.incomingRequest(GET, "http://localhost:8080", reqOpts, (outgoingResponse) -> {
            // then
            assertThat(testCtx, outgoingResponse)
                .isRedirectToWithoutParameters(publicUrl + "/auth/realms/test") // redirect path to OP should be patched
                .isValidAuthenticationRequest(Map.of(
                    "redirect_uri", publicUrl + "/callback/" + scope, // callback should be patched as well
                    "scope", "openid test"))
                .isUsingFormPost()
                .hasPKCE();
            testCtx.completeNow();
            keycloakServer.closeServer();
        });
    }

    @Test
    void callbackPathShouldBePatched(Vertx vertx, VertxTestContext testCtx) throws Throwable {
        // given
        final String publicUrl = "http://some.domain/some/path";
        final String scope = "test";
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx, "localhost")
            .startWithDefaultDiscoveryHandler();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withOAuth2AuthMiddleware(keycloakServer.getOAuth2AuthConfig(scope, false, publicUrl))
            .build()
            .start();
        final RequestOptions reqOpts = new RequestOptions().addHeader(HttpHeaders.ACCEPT, HttpHeaders.TEXT_HTML.toString());
        // when
        gateway.incomingRequest(GET, "http://localhost:8080", reqOpts, (outgoingResponse) -> {
            // then
            assertThat(testCtx, outgoingResponse)
                .isRedirectToWithoutParameters("http://" + keycloakServer.host() + ":" + keycloakServer.port() + "/auth/realms/test") // redirect should point directly to Keycloak
                .isValidAuthenticationRequest(Map.of(
                    "redirect_uri", publicUrl + "/callback/" + scope, // callback should still point to the gateway
                    "scope", "openid test"))
                .isUsingFormPost()
                .hasPKCE();
            testCtx.completeNow();
            keycloakServer.closeServer();
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
        final JsonObject oidcSessionState = oidcSessionState("aState", protectedResource1, "aPKCE");
        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx)
            .startWithDefaultDiscoveryHandlerAndCustomTokenBodyHandler((bodyHandler -> {
            }));
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withCustomSessionState(PREFIX_STATE + oidcSessionState.getString(OIDC_PARAM_STATE), oidcSessionState)
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "protected1")
            .withOAuth2AuthMiddlewareForScope(keycloakServer, "protected2")
            .build().start();

        final AtomicReference<String> sessionCookies = new AtomicReference<String>();
        final RequestOptions reqOpts = new RequestOptions();
        // 1: callback -> code-to-token -> authenticated session
        gateway.incomingRequest(POST, "/callback/protected1?state=" + oidcSessionState.getString(OIDC_PARAM_STATE) + "&code=THE-CODE",
            response1 -> {
                assertThat(testCtx, response1)
                    .isRedirectTo(protectedResource1);
                sessionCookies.set(cookieFrom(response1));

                // 2: protected resource -> auth flow 1 startet
                gateway.incomingRequest(GET, protectedResource2, withCookie(reqOpts, sessionCookies.get()),
                    (response2) -> {
                        assertThat(testCtx, response2)
                            .isValidAuthenticationRequest()
                            .hasStateWithUri("/protected2/two");
                        final String state2 = TestUtils.extractParametersFromHeader(
                            response2.getHeader("location")).get("state");

                        // 3: protected resource -> auth flow 2 startet
                        gateway.incomingRequest(GET, protectedResource3, withCookie(reqOpts, sessionCookies.get()),
                            (response3) -> {
                                assertThat(testCtx, response3)
                                    .isValidAuthenticationRequest()
                                    .hasStateWithUri("/protected2/three");
                                final String state3 = TestUtils.extractParametersFromHeader(
                                    response3.getHeader("location")).get("state");

                                // 4: callback for auth flow 2 -> code-to-token -> session with regenerated id
                                gateway.incomingRequest(POST, "/callback/protected2?state=" + state2 + "&code=THE-CODE", withCookie(reqOpts, sessionCookies.get()),
                                    (response4) -> {
                                        assertThat(testCtx, response4)
                                            .isRedirectTo(protectedResource2)
                                            .hasSetSessionCookieDifferentThan(sessionCookies.get());

                                        // when
                                        // 5:
                                        gateway.incomingRequest(POST, "/callback/protected2?state=" + state3 + "&code=THE-CODE", withCookie(reqOpts, sessionCookies.get()),
                                            (response5) -> {
                                                // then
                                                assertThat(testCtx, response5)
                                                    .isRedirectTo("/protected2/three")
                                                    .hasSetSessionCookieDifferentThan(sessionCookies.get());
                                                testCtx.completeNow();
                                                keycloakServer.closeServer();
                                            });
                                    });
                            });
                    });
            });
    }

    // --------------------------------- helper functions ---------------------------------

    private JsonObject oidcSessionState(String state, String redirect_uri, String pkce) {
        return JsonObject.of(
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

    private RequestOptions withCookie(RequestOptions reqOpts, String cookieHeaderValue) {
        return reqOpts.putHeader(HttpHeaderNames.COOKIE, cookieHeaderValue);
    }

    private String createCodeChallenge(String codeVerifier) {
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
