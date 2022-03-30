package com.inventage.portal.gateway.proxy.middleware;

import com.inventage.portal.gateway.proxy.middleware.bearerOnly.BearerOnlyMiddleware;
import com.inventage.portal.gateway.proxy.middleware.cors.CorsMiddleware;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.JWTAuthHandler;

import java.util.concurrent.CountDownLatch;

public class MiddlewareServerBuilder {

    private final Vertx vertx;
    private final int port;
    private final Router router;

    public static MiddlewareServerBuilder httpServer(Vertx vertx, int port) throws InterruptedException {
        return new MiddlewareServerBuilder(vertx, port);
    }

    private MiddlewareServerBuilder(Vertx vertx, int port) throws InterruptedException {
        this.vertx = vertx;
        this.port = port;
        router = Router.router(vertx);
    }

    public MiddlewareServer withCorsMiddleware(String allowedOrigin) throws InterruptedException {
        return withMiddleware(new CorsMiddleware(router, allowedOrigin));
    }

    public MiddlewareServer withBearerOnlyMiddleware(JWTAuth authProvider, boolean optional) throws InterruptedException {
        return withMiddleware(new BearerOnlyMiddleware(JWTAuthHandler.create(authProvider), optional));
    }

    public MiddlewareServer withMiddleware(Middleware middleware) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        HttpServer httpServer = vertx.createHttpServer().requestHandler(router::handle).listen(port, ready -> {
            if (ready.failed()) {
                throw new RuntimeException(ready.cause());
            }
            latch.countDown();
        });

        latch.await();

        router.route().handler(middleware);
        router.route().handler(ctx -> ctx.response().setStatusCode(200).end("ok"));

        return new MiddlewareServer(vertx, httpServer, port);
    }
}
