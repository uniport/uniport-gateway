package com.inventage.portal.gateway.proxy.middleware.authorizationBearer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class AuthorizationBearerMiddlewareTest {

    String host = "localhost";

    @Test
    void setIdToken(Vertx vertx, VertxTestContext testCtx) {
        int port = TestUtils.findFreePort();
        String tokenName = OAuth2MiddlewareFactory.ID_TOKEN;
        String token = "alterEgo";

        Checkpoint serverStarted = testCtx.checkpoint();
        Checkpoint requestServed = testCtx.checkpoint();
        Checkpoint responseReceived = testCtx.checkpoint();

        // needed to populate a session
        Handler<RoutingContext> sessionHandler = SessionHandler.create(LocalSessionStore.create(vertx));

        // mock OAuth2 authentication
        Handler<RoutingContext> injectTokenHandler = ctx -> {
            ctx.session().put(tokenName, token);
            ctx.next();
        };

        AuthorizationBearerMiddleware authBearer = new AuthorizationBearerMiddleware(
                DynamicConfiguration.MIDDLEWARE_OAUTH2_SESSION_SCOPE_ID);

        Handler<RoutingContext> endHandler = ctx -> ctx.response().end("ok");
        ;

        Router router = Router.router(vertx);
        router.route().handler(sessionHandler).handler(injectTokenHandler).handler(authBearer).handler(endHandler);
        vertx.createHttpServer().requestHandler(req -> {
            router.handle(req);
            testCtx.verify(() -> {
                assertTrue(req.headers().contains(HttpHeaders.AUTHORIZATION), "should contain auth header");
                assertEquals(String.format("Bearer %s", token), req.headers().get(HttpHeaders.AUTHORIZATION),
                        "should match token");
            });
            requestServed.flag();
        }).listen(port).onComplete(testCtx.succeeding(s -> {
            serverStarted.flag();
            // server is started, we can proceed
            vertx.createHttpClient().request(HttpMethod.GET, port, host, "/blub").compose(req -> req.send())
                    .onComplete(testCtx.succeeding(resp -> {
                        testCtx.verify(() -> {
                            assertFalse(resp.headers().contains(HttpHeaders.AUTHORIZATION),
                                    "should not contain auth header");
                        });
                        responseReceived.flag();
                    }));
        }));
    }

    @Test
    void setAccessToken(Vertx vertx, VertxTestContext testCtx) {
        int port = TestUtils.findFreePort();
        String sessionScope = "testScope";
        String token = "mayIAccessThisRessource";

        Checkpoint serverStarted = testCtx.checkpoint();
        Checkpoint requestServed = testCtx.checkpoint();
        Checkpoint responseReceived = testCtx.checkpoint();

        // needed to populate a session
        Handler<RoutingContext> sessionHandler = SessionHandler.create(LocalSessionStore.create(vertx));

        // mock OAuth2 authentication
        Handler<RoutingContext> injectTokenHandler = ctx -> {
            ctx.session().put(String.format(OAuth2MiddlewareFactory.SESSION_SCOPE_ACCESS_TOKEN_FORMAT, sessionScope),
                    token);
            ctx.next();
        };

        AuthorizationBearerMiddleware authBearer = new AuthorizationBearerMiddleware(sessionScope);

        Handler<RoutingContext> endHandler = ctx -> ctx.response().end("ok");
        ;

        Router router = Router.router(vertx);
        router.route().handler(sessionHandler).handler(injectTokenHandler).handler(authBearer).handler(endHandler);
        vertx.createHttpServer().requestHandler(req -> {
            router.handle(req);
            testCtx.verify(() -> {
                assertTrue(req.headers().contains(HttpHeaders.AUTHORIZATION), "should contain auth header");
                assertEquals(String.format("Bearer %s", token), req.headers().get(HttpHeaders.AUTHORIZATION),
                        "should match token");
            });
            requestServed.flag();
        }).listen(port).onComplete(testCtx.succeeding(s -> {
            serverStarted.flag();
            // server is started, we can proceed
            vertx.createHttpClient().request(HttpMethod.GET, port, host, "/blub").compose(req -> req.send())
                    .onComplete(testCtx.succeeding(resp -> {
                        testCtx.verify(() -> {
                            assertFalse(resp.headers().contains(HttpHeaders.AUTHORIZATION),
                                    "should not contain auth header");
                        });
                        responseReceived.flag();
                    }));
        }));

    }

    @Test
    void setNoToken(Vertx vertx, VertxTestContext testCtx) {
        int port = TestUtils.findFreePort();
        String token = "mayIAccessThisRessource";
        String sessionScope = "someScope";
        String someOtherSessionScope = "anotherScope";
        testCtx.verify(() -> assertNotEquals(sessionScope, someOtherSessionScope));

        Checkpoint serverStarted = testCtx.checkpoint();
        Checkpoint requestServed = testCtx.checkpoint();
        Checkpoint responseReceived = testCtx.checkpoint();

        // needed to populate a session
        Handler<RoutingContext> sessionHandler = SessionHandler.create(LocalSessionStore.create(vertx));

        // mock OAuth2 authentication
        Handler<RoutingContext> injectTokenHandler = ctx -> {
            ctx.session().put(someOtherSessionScope, token);
            ctx.next();
        };

        AuthorizationBearerMiddleware authBearer = new AuthorizationBearerMiddleware(sessionScope);

        Handler<RoutingContext> endHandler = ctx -> ctx.response().end("ok");
        ;

        Router router = Router.router(vertx);
        router.route().handler(sessionHandler).handler(injectTokenHandler).handler(authBearer).handler(endHandler);
        vertx.createHttpServer().requestHandler(req -> {
            router.handle(req);
            testCtx.verify(() -> {
                assertFalse(req.headers().contains(HttpHeaders.AUTHORIZATION), "should not contain auth header");
            });
            requestServed.flag();
        }).listen(port).onComplete(testCtx.succeeding(s -> {
            serverStarted.flag();
            // server is started, we can proceed
            vertx.createHttpClient().request(HttpMethod.GET, port, host, "/blub").compose(req -> req.send())
                    .onComplete(testCtx.succeeding(resp -> {
                        testCtx.verify(() -> {
                            assertFalse(resp.headers().contains(HttpHeaders.AUTHORIZATION),
                                    "should not contain auth header");
                        });
                        responseReceived.flag();
                    }));
        }));

    }
}
