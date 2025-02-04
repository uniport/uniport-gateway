package com.inventage.portal.gateway.proxy.middleware;

import static com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.ReplacedSessionCookieDetectionMiddlewareFactory.DEFAULT_DETECTION_COOKIE_NAME;
import static com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.ReplacedSessionCookieDetectionMiddlewareFactory.DEFAULT_MAX_REDIRECT_RETRIES;
import static com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.ReplacedSessionCookieDetectionMiddlewareFactory.DEFAULT_WAIT_BEFORE_RETRY_MS;
import static com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;

import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareFactoryBase;
import com.inventage.portal.gateway.proxy.middleware.authorization.authorizationBearer.AuthorizationBearerMiddleware;
import com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.BearerOnlyMiddleware;
import com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.BearerOnlyMiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.customClaimsChecker.JWTAuthAdditionalClaimsHandler;
import com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.customClaimsChecker.JWTAuthAdditionalClaimsOptions;
import com.inventage.portal.gateway.proxy.middleware.authorization.passAuthorization.PassAuthorizationMiddleware;
import com.inventage.portal.gateway.proxy.middleware.authorizationBearer.MockOAuth2Auth;
import com.inventage.portal.gateway.proxy.middleware.bodyHandler.BodyHandlerMiddleware;
import com.inventage.portal.gateway.proxy.middleware.checkRoute.CheckRouteMiddleware;
import com.inventage.portal.gateway.proxy.middleware.claimToHeader.ClaimToHeaderMiddleware;
import com.inventage.portal.gateway.proxy.middleware.controlapi.ControlApiAction;
import com.inventage.portal.gateway.proxy.middleware.controlapi.ControlApiMiddleware;
import com.inventage.portal.gateway.proxy.middleware.cors.CorsMiddleware;
import com.inventage.portal.gateway.proxy.middleware.csp.CSPMiddleware;
import com.inventage.portal.gateway.proxy.middleware.csp.CSPViolationReportingServerMiddleware;
import com.inventage.portal.gateway.proxy.middleware.csp.CSPViolationReportingServerMiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP.CSPMergeStrategy;
import com.inventage.portal.gateway.proxy.middleware.csrf.CSRFMiddleware;
import com.inventage.portal.gateway.proxy.middleware.csrf.CSRFMiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.customResponse.CustomResponseMiddleware;
import com.inventage.portal.gateway.proxy.middleware.headers.HeaderMiddleware;
import com.inventage.portal.gateway.proxy.middleware.languageCookie.LanguageCookieMiddleware;
import com.inventage.portal.gateway.proxy.middleware.languageCookie.LanguageCookieMiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.log.RequestResponseLoggerMiddleware;
import com.inventage.portal.gateway.proxy.middleware.matomo.MatomoMiddleware;
import com.inventage.portal.gateway.proxy.middleware.oauth2.AuthenticationUserContext;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.proxy.ProxyMiddleware;
import com.inventage.portal.gateway.proxy.middleware.proxy.ProxyMiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.replacePathRegex.ReplacePathRegexMiddleware;
import com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.ReplacedSessionCookieDetectionMiddleware;
import com.inventage.portal.gateway.proxy.middleware.responseSessionCookie.ResponseSessionCookieRemovalMiddleware;
import com.inventage.portal.gateway.proxy.middleware.session.SessionMiddleware;
import com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.sessionBag.SessionBagMiddleware;
import com.inventage.portal.gateway.proxy.middleware.sessionLogoutFromBackchannel.BackChannelLogoutMiddleware;
import com.inventage.portal.gateway.proxy.middleware.sessionLogoutFromBackchannel.MockJWKAuthHandler;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.impl.UserImpl;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.junit5.VertxTestContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.event.Level;

public final class MiddlewareServerBuilder {

    private static final int TIMEOUT_SERVER_START_SECONDS = 5;

    private final String host;
    private final Vertx vertx;
    private final Router router;
    private final VertxTestContext testCtx;

    private MiddlewareServerBuilder(Vertx vertx, String host, VertxTestContext testCtx) {
        this.vertx = vertx;
        this.host = host;
        this.testCtx = testCtx;
        router = Router.router(vertx);
    }

    public static MiddlewareServerBuilder portalGateway(Vertx vertx, VertxTestContext testCtx) {
        return portalGateway(vertx, "localhost", testCtx);
    }

    public static MiddlewareServerBuilder portalGateway(Vertx vertx, String host, VertxTestContext testCtx) {
        return new MiddlewareServerBuilder(vertx, host, testCtx);
    }

    public MiddlewareServerBuilder withSessionMiddleware() {
        return withSessionMiddleware(false, false);
    }

    public MiddlewareServerBuilder withSessionMiddleware(String uriWithoutSessionTimeoutReset) {
        return withSessionMiddleware(uriWithoutSessionTimeoutReset, SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_HEADER, false);
    }

    public MiddlewareServerBuilder withSessionMiddleware(boolean withLifetimeHeader, boolean withLifetimeCookie) {
        return withSessionMiddleware(null, withLifetimeHeader, withLifetimeCookie);
    }

    public MiddlewareServerBuilder withSessionMiddleware(String uriWithoutSessionTimeoutReset, boolean withLifetimeHeader, boolean withLifetimeCookie) {
        return withMiddleware(
            new SessionMiddleware(
                vertx,
                "session",
                SessionMiddlewareFactory.DEFAULT_SESSION_ID_MINIMUM_LENGTH,
                SessionMiddlewareFactory.DEFAULT_SESSION_IDLE_TIMEOUT_IN_MINUTE,
                uriWithoutSessionTimeoutReset,
                SessionMiddlewareFactory.DEFAULT_NAG_HTTPS,
                SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME,
                SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_HTTP_ONLY,
                SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_SECURE,
                SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_SAME_SITE,
                withLifetimeHeader,
                SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_HEADER_NAME,
                withLifetimeCookie,
                SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_COOKIE_NAME,
                SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_COOKIE_PATH,
                SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_COOKIE_HTTP_ONLY,
                SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_COOKIE_SECURE,
                SessionMiddlewareFactory.DEFAULT_SESSION_LIFETIME_COOKIE_SAME_SITE,
                SessionMiddlewareFactory.DEFAULT_CLUSTERED_SESSION_STORE_RETRY_TIMEOUT_MILISECONDS));
    }

    public MiddlewareServerBuilder withCorsMiddleware(String allowedOrigin) {
        return withCorsMiddleware(List.of(allowedOrigin));
    }

    public MiddlewareServerBuilder withCorsMiddleware(List<String> allowedOrigins) {
        return withMiddleware(new CorsMiddleware("cors", allowedOrigins, null, null, null, null, -1, false, false));
    }

    public MiddlewareServerBuilder withCorsMiddleware(List<String> allowedOrigins, List<String> allowedOriginPatterns) {
        return withMiddleware(new CorsMiddleware("cors", allowedOrigins, allowedOriginPatterns, null, null, null, -1, false, false));
    }

    public MiddlewareServerBuilder withCorsMiddleware(List<String> allowedOrigins, Set<HttpMethod> allowedMethods, Set<String> allowedHeaders) {
        return withMiddleware(new CorsMiddleware("cors", allowedOrigins, null, allowedMethods, allowedHeaders, null, -1, false, false));
    }

    public MiddlewareServerBuilder withCorsMiddleware(List<String> allowedOrigins, Set<String> exposedHeaders) {
        return withMiddleware(new CorsMiddleware("cors", allowedOrigins, null, null, null, exposedHeaders, -1, false, false));
    }

    public MiddlewareServerBuilder withCorsMiddleware(List<String> allowedOrigins, int maxAge) {
        return withMiddleware(new CorsMiddleware("cors", allowedOrigins, null, null, null, null, maxAge, false, false));
    }

    public MiddlewareServerBuilder withCorsMiddleware(List<String> allowedOrigins, boolean allowCredentials) {
        return withMiddleware(new CorsMiddleware("cors", allowedOrigins, null, null, null, null, -1, allowCredentials, false));
    }

    public MiddlewareServerBuilder withBearerOnlyMiddleware(JWTAuth authProvider, boolean optional) {
        return withMiddleware(new BearerOnlyMiddleware("bearerOnly", JWTAuthHandler.create(authProvider), optional));
    }

    public MiddlewareServerBuilder withBearerOnlyMiddlewareOtherClaims(
        JWTAuth authProvider,
        JWTAuthAdditionalClaimsOptions options, boolean optional
    ) {
        return withMiddleware(
            new BearerOnlyMiddleware("bearerOnly", JWTAuthAdditionalClaimsHandler.create(authProvider, options),
                optional));
    }

    /**
     * @return this
     */
    public MiddlewareServerBuilder withBearerOnlyMiddleware(
        KeycloakServer mockKeycloakServer,
        String issuer, List<String> audience, JsonArray publicKeys, long reconcilationIntervalMs
    ) {
        try {
            withBearerOnlyMiddleware(mockKeycloakServer.getBearerOnlyConfig(issuer, audience, publicKeys, true, reconcilationIntervalMs));
        } catch (Throwable t) {
            if (mockKeycloakServer != null) {
                mockKeycloakServer.closeServer();
            }
            if (testCtx != null) {
                testCtx.failNow(t);
            }
        }
        return this;
    }

    public MiddlewareServerBuilder withBearerOnlyMiddleware(
        KeycloakServer mockKeycloakServer,
        String issuer, List<String> audience, JsonArray publicKeys
    ) {
        return withBearerOnlyMiddleware(mockKeycloakServer.getBearerOnlyConfig(issuer, audience, publicKeys, false, WithAuthHandlerMiddlewareFactoryBase.DEFAULT_RECONCILIATION_INTERVAL_MS));
    }

    public MiddlewareServerBuilder withBearerOnlyMiddleware(JsonObject bearerOnlyConfig) {
        final BearerOnlyMiddlewareFactory factory = new BearerOnlyMiddlewareFactory();
        final Future<Middleware> middlewareFuture = factory.create(vertx, "bearerOnly", router, bearerOnlyConfig);
        final int atMost = 50;
        int counter = 0;
        while (!middlewareFuture.isComplete() && atMost > counter) {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            counter++;
        }
        if (middlewareFuture.failed() || counter >= atMost) {
            throw new IllegalStateException("BearerOnly Middleware could not be instantiated");
        }
        return withMiddleware(middlewareFuture.result());
    }

    public MiddlewareServerBuilder withCspMiddleware(JsonArray directives, boolean reportOnly) {
        return withMiddleware(new CSPMiddleware("csp", directives, reportOnly));
    }

    public MiddlewareServerBuilder withCspMiddleware(JsonArray directives, boolean reportOnly, CSPMergeStrategy mergeStrategy) {
        return withMiddleware(new CSPMiddleware("csp", directives, reportOnly, mergeStrategy));
    }

    public MiddlewareServerBuilder withCspViolationReportingServerMiddleware() {
        return withCspViolationReportingServerMiddleware(CSPViolationReportingServerMiddlewareFactory.DEFAULT_LOG_LEVEL);
    }

    public MiddlewareServerBuilder withCspViolationReportingServerMiddleware(String logLevel) {
        return withMiddleware(new CSPViolationReportingServerMiddleware("cspViolationReportingServer", Level.valueOf(logLevel)));
    }

    public MiddlewareServerBuilder withCsrfMiddleware(String secret, String cookieName, String headerName) {
        return withMiddleware(
            new CSRFMiddleware(this.vertx, "csrf", secret, cookieName, null, CSRFMiddlewareFactory.DEFAULT_COOKIE_SECURE, headerName, CSRFMiddlewareFactory.DEFAULT_TIMEOUT_IN_MINUTES, null, CSRFMiddlewareFactory.DEFAULT_NAG_HTTPS));
    }

    public MiddlewareServerBuilder withHeaderMiddleware(MultiMap requestHeaders, MultiMap responseHeaders) {
        return withMiddleware(new HeaderMiddleware("header", requestHeaders, responseHeaders));
    }

    public MiddlewareServerBuilder withCustomResponseMiddleware(String content, Integer statusCode, MultiMap headers) {
        return withMiddleware(new CustomResponseMiddleware("customResponse", content, statusCode, headers));
    }

    public MiddlewareServerBuilder withReplacePathRegexMiddleware(String regex, String replacement) {
        return withMiddleware(new ReplacePathRegexMiddleware("replacePath", regex, replacement));
    }

    public MiddlewareServerBuilder withBodyHandlerMiddleware() {
        return withMiddleware(new BodyHandlerMiddleware(this.vertx, "bodyHandler"));
    }

    public MiddlewareServerBuilder withPassAuthorizationMiddleware(String sessionScope, JWTAuth authProvider) {
        return withMiddleware(new PassAuthorizationMiddleware(vertx, "passAuthorization", sessionScope, JWTAuthHandler.create(authProvider)));
    }

    public MiddlewareServerBuilder withAuthorizationBearerMiddleware(String sessionScope) {
        return withMiddleware(new AuthorizationBearerMiddleware(vertx, "authorizationBearer", sessionScope));
    }

    public MiddlewareServerBuilder withLanguageCookieMiddleware() {
        return withMiddleware(new LanguageCookieMiddleware("languageCookie", LanguageCookieMiddlewareFactory.DEFAULT_LANGUAGE_COOKIE_NAME));
    }

    public MiddlewareServerBuilder withControlApiMiddleware(ControlApiAction action, WebClient client) {
        return withMiddleware(new ControlApiMiddleware(vertx, "controlAPI", action, client));
    }

    public MiddlewareServerBuilder withControlApiMiddleware(ControlApiAction action, String resetUri, WebClient client) {
        return withMiddleware(new ControlApiMiddleware(vertx, "controlAPI", action, resetUri, client));
    }

    public MiddlewareServerBuilder withSessionBagMiddleware(JsonArray whitelistedCookies) {
        return withMiddleware(
            new SessionBagMiddleware("sessionBag", whitelistedCookies, "uniport.session"));
    }

    public MiddlewareServerBuilder withSessionBagMiddleware(JsonArray whitelistedCookies, String sessionCookieName) {
        return withMiddleware(new SessionBagMiddleware("sessionBag", whitelistedCookies, sessionCookieName));
    }

    public MiddlewareServerBuilder withResponseSessionCookieRemovalMiddleware() {
        return withMiddleware(new ResponseSessionCookieRemovalMiddleware("responseSessionCookieRemoval", null));
    }

    public MiddlewareServerBuilder withRequestResponseLoggerMiddleware(String uriPatternForIgnoringRequests) {
        return withMiddleware(new RequestResponseLoggerMiddleware("requestResponseLogger", uriPatternForIgnoringRequests, null, null, null));
    }

    public MiddlewareServerBuilder withAuthenticationTriggerMiddleware() {
        return withMiddleware(new CheckRouteMiddleware("checkRoute"));
    }

    public MiddlewareServerBuilder withMatomoMiddleware(String jwtPathRoles, String jwtPathGroup, String jwtPathUsername, String jwtPathEmail) {
        return withMiddleware(new MatomoMiddleware("matomo", jwtPathRoles, jwtPathGroup, jwtPathUsername, jwtPathEmail));
    }

    /**
     * @param mockKeycloakServer
     * @param scope
     *            will be used as the path prefix of incoming requests (e.g. /scope/*)
     * @return this
     */
    public MiddlewareServerBuilder withOAuth2AuthMiddlewareForScope(KeycloakServer mockKeycloakServer, String scope) {
        try {
            return withOAuth2AuthMiddleware(mockKeycloakServer.getOAuth2AuthConfig(scope), scope);
        } catch (Throwable t) {
            if (mockKeycloakServer != null) {
                mockKeycloakServer.closeServer();
            }
            if (testCtx != null) {
                testCtx.failNow(t);
            }
        }
        return this;
    }

    public MiddlewareServerBuilder withOAuth2AuthMiddleware(JsonObject oAuth2AuthConfig) throws Throwable {
        return withOAuth2AuthMiddleware(oAuth2AuthConfig, null);
    }

    private MiddlewareServerBuilder withOAuth2AuthMiddleware(JsonObject oAuth2AuthConfig, String scope) throws Throwable {
        final OAuth2MiddlewareFactory factory = new OAuth2MiddlewareFactory();
        final Future<Middleware> middlewareFuture = factory.create(vertx, "oauth", router, oAuth2AuthConfig);

        final CountDownLatch latch = new CountDownLatch(1);
        middlewareFuture.onComplete(fut -> {
            latch.countDown();
        });
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException("OAuth2Auth Middleware timed out to be instantiated");
        }

        if (middlewareFuture.failed()) {
            throw middlewareFuture.cause();
        }
        if (middlewareFuture.result() == null) {
            throw new IllegalStateException("OAuth2Auth Middleware failed to be instantiated and is null");
        }
        if (scope == null) {
            return withMiddleware(middlewareFuture.result());
        } else {
            return withMiddlewareOnPath(middlewareFuture.result(), "/" + scope + "/*");
        }
    }

    public MiddlewareServerBuilder withProxyMiddleware(int port) {
        return withProxyMiddleware(host, port);
    }

    public MiddlewareServerBuilder withProxyMiddleware(String host, int port) {
        return withMiddleware(new ProxyMiddleware(vertx, "proxy",
            host, port,
            ProxyMiddlewareFactory.DEFAULT_SERVER_PROTOCOL,
            ProxyMiddlewareFactory.DEFAULT_HTTPS_TRUST_ALL,
            ProxyMiddlewareFactory.DEFAULT_HTTPS_VERIFY_HOSTNAME,
            ProxyMiddlewareFactory.DEFAULT_HTTPS_TRUST_STORE_PATH,
            ProxyMiddlewareFactory.DEFAULT_HTTPS_TRUST_STORE_PASSWORD));
    }

    public MiddlewareServerBuilder withProxyMiddleware(String host, int port, String serverProtocol, boolean httpsTrustAll, boolean verifyHost, String httpsTrustStorePath, String httpsTrustStorePassword) {
        return withMiddleware(new ProxyMiddleware(vertx, "proxy", host, port, serverProtocol, httpsTrustAll, verifyHost, httpsTrustStorePath, httpsTrustStorePassword));
    }

    public MiddlewareServerBuilder withBackend(Vertx vertx, int port) throws InterruptedException {
        final Handler<RoutingContext> defaultHandler = ctx -> ctx.response().end();
        return withBackend(vertx, port, defaultHandler);
    }

    public MiddlewareServerBuilder withBackend(Vertx vertx, int port, HttpServerOptions serverOptions) throws InterruptedException {
        final Handler<RoutingContext> defaultHandler = ctx -> ctx.response().end();
        return withBackend(vertx, port, serverOptions, defaultHandler);
    }

    public MiddlewareServerBuilder withBackend(Vertx vertx, int port, Handler<RoutingContext> handler) throws InterruptedException {
        return withBackend(vertx, port, new HttpServerOptions(), handler);
    }

    public MiddlewareServerBuilder withBackend(Vertx vertx, int port, HttpServerOptions serverOptions, Handler<RoutingContext> handler)
        throws InterruptedException {
        final VertxTestContext testContext = new VertxTestContext();
        final Router serviceRouter = Router.router(vertx);

        serviceRouter.route().handler(handler);

        vertx.createHttpServer(serverOptions)
            .requestHandler(serviceRouter)
            .listen(port)
            .onComplete(testContext.succeedingThenComplete());

        if (!testContext.awaitCompletion(TIMEOUT_SERVER_START_SECONDS, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timeout: Server did not start in time.");
        }

        return this;
    }

    public MiddlewareServerBuilder withMockOAuth2Middleware() {
        return withMockOAuth2Middleware("mayIAccessThisRessource");
    }

    public MiddlewareServerBuilder withMockOAuth2Middleware(String rawAccessToken) {
        return withMockOAuth2Middleware("localhost", 1234, rawAccessToken);
    }

    public MiddlewareServerBuilder withMockOAuth2Middleware(String host, int port) {
        return withMockOAuth2Middleware(host, port, "mayIAccessThisRessource");
    }

    public MiddlewareServerBuilder withMockOAuth2Middleware(String host, int port, String rawAccessToken) {
        final String sessionScope = "testScope";

        final JsonObject principal = new JsonObject().put("access_token", rawAccessToken);
        final OAuth2Auth authProvider = new MockOAuth2Auth(host, port, principal, 0);
        final User user = MockOAuth2Auth.createUser(principal);
        AuthenticationUserContext authContext = AuthenticationUserContext.of(authProvider, user);

        // mock OAuth2 authentication
        final Handler<RoutingContext> injectTokenHandler = ctx -> {
            authContext.toSessionAtScope(ctx.session(), sessionScope);
            ctx.next();
        };
        withMiddleware(injectTokenHandler);
        return this;
    }

    public MiddlewareServerBuilder withCustomSessionState(String key, Object value) {
        return withCustomSessionState(Map.of(key, value));
    }

    public MiddlewareServerBuilder withCustomSessionState(Map<String, Object> sessionEntries) {
        final Handler<RoutingContext> handler = ctx -> {
            sessionEntries.forEach((key, value) -> ctx.session().put(key, value));
            ctx.next();
        };
        withMiddleware(handler);
        return this;
    }

    public MiddlewareServerBuilder withUser() {
        final Handler<RoutingContext> handler = ctx -> {
            ctx.setUser(new UserImpl());
            ctx.next();
        };
        withMiddleware(handler);
        return this;
    }

    public MiddlewareServerBuilder withRoutingContextHolder(AtomicReference<RoutingContext> routingContextHolder) {
        final Handler<RoutingContext> holdRoutingContext = ctx -> {
            routingContextHolder.set(ctx);
            ctx.next();
        };
        withMiddleware(holdRoutingContext);
        return this;
    }

    public MiddlewareServerBuilder withMiddleware(Handler<RoutingContext> middleware) {
        router.route().handler(middleware);
        return this;
    }

    public MiddlewareServerBuilder withMiddlewareOnPath(Handler<RoutingContext> middleware, String path) {
        router.route().path(path).handler(middleware);
        return this;
    }

    public MiddlewareServerBuilder withClaimToMiddleware(String claim, String headerName) {
        return withMiddleware(new ClaimToHeaderMiddleware("withClaimToMiddleware", claim, headerName));
    }

    public MiddlewareServerBuilder withReplacedSessionCookieDetectionMiddleware() {
        return withMiddleware(
            new ReplacedSessionCookieDetectionMiddleware(
                "withReplacedSessionMiddleware",
                DEFAULT_DETECTION_COOKIE_NAME,
                DEFAULT_SESSION_COOKIE_NAME,
                DEFAULT_WAIT_BEFORE_RETRY_MS,
                DEFAULT_MAX_REDIRECT_RETRIES));
    }

    public MiddlewareServerBuilder withBackChannelLogoutMiddleware(String path, JWTAuthOptions jwtAuthOptions) {
        return withMiddlewareOnPath(
            new BackChannelLogoutMiddleware(
                this.vertx,
                "withBackChannelLogoutMiddleware",
                new MockJWKAuthHandler(jwtAuthOptions)),
            path);
    }

    public MiddlewareServer build() {
        final Handler<RoutingContext> defaultBackendMockHandler = ctx -> ctx.response().setStatusCode(200).end("ok");
        return build(defaultBackendMockHandler);
    }

    public MiddlewareServer build(Handler<RoutingContext> backendMockHandler) {
        router.route().handler(backendMockHandler);
        final HttpServer httpServer = vertx.createHttpServer().requestHandler(router);
        return new MiddlewareServer(vertx, httpServer, host, testCtx);
    }
}
