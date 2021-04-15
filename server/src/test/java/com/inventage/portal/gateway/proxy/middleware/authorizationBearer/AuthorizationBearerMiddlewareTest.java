package com.inventage.portal.gateway.proxy.middleware.authorizationBearer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class AuthorizationBearerMiddlewareTest {

    @Test
    void setIdToken(Vertx vertx, VertxTestContext testCtx) {
        String idToken = "alterEgo";
        String host = "localhost";
        int port = 8888;

        Checkpoint serverStarted = testCtx.checkpoint();
        Checkpoint requestServed = testCtx.checkpoint();
        Checkpoint responseReceived = testCtx.checkpoint();

        Handler<RoutingContext> injectTokenHandler = ctx -> {
            ctx.session().put(OAuth2MiddlewareFactory.ID_TOKEN, idToken);
            System.out.println(ctx.session().toString());
            ctx.next();
        };

        AuthorizationBearerMiddleware authBearer = new AuthorizationBearerMiddleware(
                DynamicConfiguration.MIDDLEWARE_OAUTH2_SESSION_SCOPE_ID);

        Handler<RoutingContext> endHandler = ctx -> ctx.response().end("ok");

        Router router = Router.router(vertx);
        router.route().handler(injectTokenHandler).handler(authBearer).handler(endHandler);
        vertx.createHttpServer().requestHandler(req -> {
            router.handle(req);
            testCtx.verify(() -> {
                assertTrue(req.headers().contains(HttpHeaders.AUTHORIZATION));
                assertEquals(req.headers().get(HttpHeaders.AUTHORIZATION), idToken);
            });
            requestServed.flag();
        }).listen(port).onComplete(testCtx.succeeding(httpServer -> serverStarted.flag()));

        vertx.createHttpClient().request(HttpMethod.GET, port, host, "/blub")
                .compose(req -> req.send())
                .onComplete(testCtx.succeeding(resp -> testCtx.verify(() -> {
                    assertFalse(resp.headers().contains(HttpHeaders.AUTHORIZATION));
                    responseReceived.flag();
                })));
    }

    @Test
    void setAccessToken(Vertx vertx, VertxTestContext testCtx) {
        testCtx.completeNow();
    }

    @Test
    void setNoToken(Vertx vertx, VertxTestContext testCtx) {
        testCtx.completeNow();
    }
}
