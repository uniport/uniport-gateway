package com.inventage.portal.gateway.core.entrypoint;

import com.inventage.portal.gateway.GatewayRouterInternal;
import com.inventage.portal.gateway.Runtime;
import com.inventage.portal.gateway.core.application.Application;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.model.GatewayMiddleware;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import io.vertx.core.AsyncResult;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the portal gateway.
 */
public class Entrypoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(Entrypoint.class);
    private final Vertx vertx;
    private final String name;
    private final int port;
    private final List<GatewayMiddleware> entryMiddlewares;
    private GatewayRouterInternal router;
    private boolean enabled;
    private Tls tls;

    public Entrypoint(Vertx vertx, String name, int port, List<GatewayMiddleware> entryMiddlewares) {
        this.vertx = vertx;
        this.name = name;
        this.port = port;
        this.enabled = true;
        this.entryMiddlewares = entryMiddlewares;
    }

    public static JsonObject entrypointConfigByName(String name, JsonObject globalConfig) {
        final JsonArray configs = globalConfig.getJsonArray(StaticConfiguration.ENTRYPOINTS);
        return configs.stream().map(object -> new JsonObject(Json.encode(object)))
            .filter(entrypoint -> entrypoint.getString(StaticConfiguration.ENTRYPOINT_NAME).equals(name))
            .findFirst().orElseThrow(() -> {
                throw new IllegalStateException(String.format("Entrypoint '%s' not found!", name));
            });
    }

    public String name() {
        return name;
    }

    public int port() {
        return port;
    }

    public GatewayRouterInternal router() {
        if (router != null) {
            return router;
        }
        router = GatewayRouterInternal.router(vertx, String.format("entrypoint %s", name));
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
                    router().mountSubRouter(application.rootPath(), applicationRouter);
                    LOGGER.info("Application '{}' for '{}' at endpoint '{}'", application, application.rootPath(), name);
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

    public void setJksOptions(JksOptions jksOptions) {
        this.tls = new Tls(jksOptions);
    }

    public JksOptions jksOptions() {
        if (isTls()) {
            return tls.jksOptions();
        }
        return null;
    }

    public boolean enabled() {
        return enabled && port > 0;
    }

    public void disable() {
        enabled = false;
    }

    private void setupEntryMiddlewares(List<GatewayMiddleware> entryMiddlewares, Router router) {

        final List<Future<Middleware>> entryMiddlewaresFuture = new ArrayList<>();
        for (GatewayMiddleware entryMiddleware : entryMiddlewares) {
            entryMiddlewaresFuture.add(createEntryMiddleware(entryMiddleware, router));
        }

        Future.all(entryMiddlewaresFuture)
            .onSuccess(cf -> {
                entryMiddlewaresFuture.forEach(mf -> {
                    final Middleware middleware = mf.result();
                    router.route()
                        .setName(middleware.getClass().getSimpleName())
                        .handler((Handler<RoutingContext>) middleware);
                });
                LOGGER.info("EntryMiddlewares created successfully");
            }).onFailure(err -> {
                Runtime.fatal(vertx, err.getMessage());
            });
    }

    private Future<Middleware> createEntryMiddleware(GatewayMiddleware middlewareConfig, Router router) {
        final Promise<Middleware> promise = Promise.promise();
        createEntryMiddleware(middlewareConfig, router, promise);
        return promise.future();
    }

    private void createEntryMiddleware(GatewayMiddleware middlewareConfig, Router router, Handler<AsyncResult<Middleware>> handler) {
        final String middlewareType = middlewareConfig.getType();
        final GatewayMiddlewareOptions middlewareOptions = middlewareConfig.getOptions();

        final Optional<MiddlewareFactory> middlewareFactory = MiddlewareFactory.Loader.getFactory(middlewareType);
        if (middlewareFactory.isEmpty()) {
            final String errMsg = String.format("Unknown middleware '%s'", middlewareType);
            LOGGER.warn("{}", errMsg);
            handler.handle(Future.failedFuture(errMsg));
            return;
        }

        final String middlewareName = middlewareConfig.getName();
        middlewareFactory.get()
            .create(this.vertx, middlewareName, router, middlewareOptions)
            .onComplete(handler);
    }

    static class Tls {
        private JksOptions jksOptions;

        Tls(JksOptions jksOptions) {
            this.jksOptions = jksOptions;
        }

        public JksOptions jksOptions() {
            return jksOptions;
        }
    }

}
