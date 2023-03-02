package com.inventage.portal.gateway.core.entrypoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.core.application.Application;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.log.RequestResponseLogger;
import com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.ReplacedSessionCookieDetectionMiddleware;
import com.inventage.portal.gateway.proxy.middleware.responseSessionCookie.ResponseSessionCookieRemovalMiddleware;
import com.inventage.portal.gateway.proxy.middleware.session.SessionMiddleware;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * Entry point for the portal gateway.
 */
public class Entrypoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(Entrypoint.class);
    private final Vertx vertx;
    private final String name;
    private final int port;
    private final JsonArray entryMiddlewares;
    private Router router;
    private boolean enabled;
    private Tls tls;

    public Entrypoint(Vertx vertx, String name, int port, JsonArray entryMiddlewares) {
        this.vertx = vertx;
        this.name = name;
        this.port = port;
        this.enabled = true;
        this.entryMiddlewares = entryMiddlewares;
    }

    public String name() {
        return name;
    }

    public int port() {
        return port;
    }

    public Router router() {
        if (router != null) {
            return router;
        }
        router = Router.router(vertx);
        if (this.entryMiddlewares != null) {
            LOGGER.info("Setup EntryMiddlewares");
            this.setupEntryMiddlewares(this.entryMiddlewares, router);
        }
        return router;
    }

    public void mount(Application application) {
        final Optional<Router> optionApplicationRouter = application.router();
        optionApplicationRouter.ifPresent(applicationRouter -> {
            if (name.equals(application.entrypoint())) {
                if (enabled()) {
                    router().route(application.rootPath() + "*").subRouter(applicationRouter);
                    LOGGER.info("Application '{}' for '{}' at endpoint '{}'", application,
                            application.rootPath(), name);
                } else {
                    LOGGER.warn("Disabled endpoint '{}' can not mount application '{}' for '{}'", name,
                            application, application.rootPath());
                }
            }
        });
    }

    public boolean isTls() {
        return tls != null;
    }

    public JksOptions jksOptions() {
        if (isTls()) {
            return tls.jksOptions();
        }
        return new JksOptions();
    }

    public boolean enabled() {
        return enabled && port > 0;
    }

    public void disable() {
        enabled = false;
    }

    static class Tls {
        public JksOptions jksOptions() {
            return null;
        }
    }

    public static JsonObject entrypointConfigByName(String name, JsonObject globalConfig) {
        final JsonArray configs = globalConfig.getJsonArray(StaticConfiguration.ENTRYPOINTS);
        return configs.stream().map(object -> new JsonObject(Json.encode(object)))
                .filter(entrypoint -> entrypoint.getString(StaticConfiguration.ENTRYPOINT_NAME).equals(name))
                .findFirst().orElseThrow(() -> {
                    throw new IllegalStateException(String.format("Entrypoint '%s' not found!", name));
                });
    }

    private void setupEntryMiddlewares(JsonArray entryMiddlewares, Router router) {

        final List<Future> entryMiddlewaresFuture = new ArrayList<>();
        for (int i = 0; i < entryMiddlewares.size(); i++) {
            entryMiddlewaresFuture.add(createEntryMiddleware(entryMiddlewares.getJsonObject(i), router));
        }

        CompositeFuture.all(entryMiddlewaresFuture).onSuccess(cf -> {
            entryMiddlewaresFuture
                    .forEach(mf -> router.route().setName("entry middleware")
                            .handler((Handler<RoutingContext>) mf.result()));
            LOGGER.info("EntryMiddlewares created successfully");
        }).onFailure(err -> {
            throw new RuntimeException(
                    String.format("Failed to create EntryMiddlewares. Cause: {}", err.getMessage()));
        });
    }

    private Future<Middleware> createEntryMiddleware(JsonObject middlewareConfig, Router router) {
        final Promise<Middleware> promise = Promise.promise();
        createEntryMiddleware(middlewareConfig, router, promise);
        return promise.future();
    }

    private void createEntryMiddleware(JsonObject middlewareConfig, Router router,
            Handler<AsyncResult<Middleware>> handler) {
        final String middlewareType = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_TYPE);
        final JsonObject middlewareOptions = middlewareConfig.getJsonObject(DynamicConfiguration.MIDDLEWARE_OPTIONS,
                new JsonObject());

        final MiddlewareFactory middlewareFactory = MiddlewareFactory.Loader.getFactory(middlewareType);
        if (middlewareFactory == null) {
            final String errMsg = String.format("Unknown middleware '%s'", middlewareType);
            LOGGER.warn("{}", errMsg);
            handler.handle(Future.failedFuture(errMsg));
            return;
        }

        middlewareFactory.create(this.vertx, router, middlewareOptions).onComplete(handler);
    }

}
