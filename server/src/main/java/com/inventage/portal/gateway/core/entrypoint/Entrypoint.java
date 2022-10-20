package com.inventage.portal.gateway.core.entrypoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.log.RequestResponseLogger;
import com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.ReplacedSessionCookieDetectionMiddleware;
import com.inventage.portal.gateway.proxy.middleware.responseSessionCookie.ResponseSessionCookieRemovalMiddleware;
import com.inventage.portal.gateway.proxy.middleware.session.SessionMiddleware;
import io.vertx.core.*;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.core.application.Application;
import com.inventage.portal.gateway.core.config.StaticConfiguration;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;

/**
 * Entry point for the portal gateway.
 */
public class Entrypoint {

    private static Logger LOGGER = LoggerFactory.getLogger(Entrypoint.class);
    private final Vertx vertx;
    private final String name;
    private final int port;
    // sessionIdleTimeout is how many minutes an idle session is kept before deletion

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
        if (this.entryMiddlewares == null) {
            LOGGER.info("No custom EntryMiddlewares defined. Setup default EntryMiddlewares");
            this.setupDefaultEntryMiddlewares();
        } else {
            LOGGER.info("No EntryMiddlewares defined");
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

    class Tls {

        public JksOptions jksOptions() {
            return null;
            // final String keyStorePath = config.getString(CONFIG_PREFIX +
            // CONFIG_HTTPS_KEY_STORE_PATH);
            // if (keyStorePath == null || keyStorePath.isEmpty()) {
            // throw new IllegalStateException("When using https the path to the key store must be
            // configured by variable: '" +
            // CONFIG_PREFIX + "https-key-store-path'. To disable the https port configuration use
            // '-1' as port.");
            // }
            // return new JksOptions()
            // .setPath(keyStorePath)
            // .setPassword(config.getString(CONFIG_PREFIX + CONFIG_HTTPS_KEY_STORE_PASSWORD));
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

    private void setupDefaultEntryMiddlewares() {
        router.route().handler(new ResponseSessionCookieRemovalMiddleware(null));
        router.route().handler(new SessionMiddleware(null, null, null, null, null, null, null, null));
        router.route().handler(new RequestResponseLogger());
        router.route().handler(new ReplacedSessionCookieDetectionMiddleware(null, null));
    }

    private void setupEntryMiddlewares(JsonArray entryMiddlewares, Router router) {

        List<Future> entryMiddlewaresFuture = new ArrayList<>();
        for (int i = 0; i < entryMiddlewares.size(); i++) {
            entryMiddlewaresFuture.add(createEntryMiddleware(entryMiddlewares.getJsonObject(i), router));
        }

        CompositeFuture.all(entryMiddlewaresFuture).onSuccess(cf -> {
            entryMiddlewaresFuture.forEach(mf -> router.route().handler((Handler<RoutingContext>) mf.result()));
            LOGGER.info("EntryMiddlewares created successfully");
        }).onFailure(cfErr -> {
            String errMsg = String.format("Failed to create EntryMiddlewares");
            LOGGER.warn("{}", errMsg);
        });
    }

    private Future<Middleware> createEntryMiddleware(JsonObject middlewareConfig, Router router) {
        Promise<Middleware> promise = Promise.promise();
        createEntryMiddleware(middlewareConfig, router, promise);
        return promise.future();
    }

    private void createEntryMiddleware(JsonObject middlewareConfig, Router router,
                                       Handler<AsyncResult<Middleware>> handler) {
        String middlewareType = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_TYPE);
        JsonObject middlewareOptions = middlewareConfig.getJsonObject(DynamicConfiguration.MIDDLEWARE_OPTIONS);

        MiddlewareFactory middlewareFactory = MiddlewareFactory.Loader.getFactory(middlewareType);
        if (middlewareFactory == null) {
            String errMsg = String.format("Unknown middleware '%s'", middlewareType);
            LOGGER.warn("{}", errMsg);
            handler.handle(Future.failedFuture(errMsg));
            return;
        }

        middlewareFactory.create(this.vertx, router, middlewareOptions).onComplete(handler);
    }


}
