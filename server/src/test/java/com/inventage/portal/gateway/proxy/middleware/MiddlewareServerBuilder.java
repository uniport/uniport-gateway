package com.inventage.portal.gateway.proxy.middleware;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.inventage.portal.gateway.proxy.middleware.bearerOnly.BearerOnlyMiddleware;
import com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker.JWTAuthAdditionalClaimsHandler;
import com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker.JWTAuthAdditionalClaimsOptions;
import com.inventage.portal.gateway.proxy.middleware.controlapi.ControlApiMiddleware;
import com.inventage.portal.gateway.proxy.middleware.cors.CorsMiddleware;
import com.inventage.portal.gateway.proxy.middleware.languageCookie.LanguageCookieMiddleware;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.proxy.ProxyMiddleware;
import com.inventage.portal.gateway.proxy.middleware.sessionBag.SessionBagMiddleware;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.junit5.VertxTestContext;

public class MiddlewareServerBuilder {

    private static final String SESSION_COOKIE_NAME = "test.session.cookie";
    private static final int TIMEOUT_SERVER_START_SECONDS = 5;

    private final String host;
    private final Vertx vertx;
    private final int port;
    private final Router router;

    private MiddlewareServerBuilder(Vertx vertx, String host, int port) {
        this.vertx = vertx;
        this.port = port;
        this.host = host;
        router = Router.router(vertx);

        final SessionStore sessionStore = LocalSessionStore.create(vertx);
        final SessionHandler sessionHandler = SessionHandler.create(sessionStore)
                .setSessionCookieName(SESSION_COOKIE_NAME);
        router.route().handler(sessionHandler);
    }

    public static MiddlewareServerBuilder portalGateway(Vertx vertx, String host, int port) {
        return new MiddlewareServerBuilder(vertx, host, port);
    }

    public MiddlewareServerBuilder withCorsMiddleware(String allowedOrigin) {
        return withMiddleware(new CorsMiddleware(allowedOrigin));
    }

    public MiddlewareServerBuilder withBearerOnlyMiddleware(JWTAuth authProvider, boolean optional) {
        return withMiddleware(new BearerOnlyMiddleware(JWTAuthHandler.create(authProvider), optional));
    }

    public MiddlewareServerBuilder withBearerOnlyMiddlewareOtherClaims(JWTAuth authProvider,
            JWTAuthAdditionalClaimsOptions options, boolean optional) {
        return withMiddleware(
                new BearerOnlyMiddleware(JWTAuthAdditionalClaimsHandler.create(authProvider, options), optional));
    }

    public MiddlewareServerBuilder withLanguageCookieMiddleware() {
        return withMiddleware(new LanguageCookieMiddleware());
    }

    public MiddlewareServerBuilder withControlApiMiddleware(String action) {
        return withMiddleware(new ControlApiMiddleware(action, WebClient.create(vertx)));
    }

    public MiddlewareServerBuilder withSessionBagMiddleware(JsonArray whitelistedCookies) {
        return withMiddleware(new SessionBagMiddleware(whitelistedCookies, "inventage-portal-gateway.session"));
    }

    public MiddlewareServerBuilder withSessionBagMiddleware(JsonArray whitelistedCookies, String sessionCookieName) {
        return withMiddleware(new SessionBagMiddleware(whitelistedCookies, sessionCookieName));
    }

    public MiddlewareServerBuilder withOAuth2AuthMiddleware(JsonObject oAuth2AuthConfig) {
        OAuth2MiddlewareFactory factory = new OAuth2MiddlewareFactory();
        Future<Middleware> middlewareFuture = factory.create(vertx, router, oAuth2AuthConfig);
        while (!middlewareFuture.isComplete()) { // eventually aborted by the test context
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (!middlewareFuture.isComplete() || middlewareFuture.failed()) {
            throw new IllegalStateException("OAuth2Auth Middleware could not be instantiated");
        }
        return withMiddleware(middlewareFuture.result());
    }

    public MiddlewareServerBuilder withProxyMiddleware(int port) {
        return withMiddleware(new ProxyMiddleware(vertx, host, port));
    }

    public MiddlewareServerBuilder withBackend(Vertx vertx, int port) throws InterruptedException {
        VertxTestContext testContext = new VertxTestContext();
        Router serviceRouter = Router.router(vertx);

        serviceRouter.route().handler(ctx -> {
            ctx.response().end();
        });

        vertx.createHttpServer().requestHandler(serviceRouter).listen(port)
                .onComplete(testContext.succeedingThenComplete());

        if (!testContext.awaitCompletion(TIMEOUT_SERVER_START_SECONDS, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timeout: Server did not start in time.");
        }

        return this;
    }

    public MiddlewareServerBuilder withBackend(Vertx vertx, int port, Handler<RoutingContext> handler)
            throws InterruptedException {
        VertxTestContext testContext = new VertxTestContext();
        Router serviceRouter = Router.router(vertx);

        serviceRouter.route().handler(handler);

        vertx.createHttpServer().requestHandler(serviceRouter).listen(port)
                .onComplete(testContext.succeedingThenComplete());

        if (!testContext.awaitCompletion(TIMEOUT_SERVER_START_SECONDS, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timeout: Server did not start in time.");
        }

        return this;
    }

    public MiddlewareServerBuilder withMockOAuth2Middleware() {
        final String sessionScope = "testScope";
        final String rawAccessToken = "mayIAccessThisRessource";
        final User user = User.create(new JsonObject().put("access_token", rawAccessToken));
        final Pair<OAuth2Auth, User> authPair = ImmutablePair.of(null, user);

        // mock OAuth2 authentication
        Handler<RoutingContext> injectTokenHandler = ctx -> {
            String key = String.format("%s%s", sessionScope, OAuth2MiddlewareFactory.SESSION_SCOPE_SUFFIX);
            ctx.session().put(key, authPair);
            ctx.next();
        };
        router.route().handler(injectTokenHandler);
        return this;
    }

    public MiddlewareServerBuilder withCustomSessionState(Map<String, Object> sessionEntries) {
        Handler<RoutingContext> handler = ctx -> {
            sessionEntries.forEach((key, value) -> ctx.session().put(key, value));
            ctx.next();
        };
        router.route().handler(handler);
        return this;
    }

    public MiddlewareServerBuilder withRoutingContextHolder(AtomicReference<RoutingContext> routingContext) {
        final Handler<RoutingContext> holdRoutingContext = ctx -> {
            routingContext.set(ctx);
            ctx.next();
        };

        router.route().handler(holdRoutingContext);
        return this;
    }

    public MiddlewareServerBuilder withMiddleware(Handler<RoutingContext> middleware) {
        router.route().handler(middleware);
        return this;
    }

    public MiddlewareServer build() {
        router.route().handler(ctx -> ctx.response().setStatusCode(200).end("ok"));

        HttpServer httpServer = vertx.createHttpServer().requestHandler(router::handle);

        return new MiddlewareServer(vertx, httpServer, port, host);
    }
}
