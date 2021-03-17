package com.inventage.portal.gateway.proxy;

import com.inventage.portal.gateway.core.middleware.headers.HeaderHandler;
import com.inventage.portal.gateway.core.middleware.proxy.ProxyHandler;
import com.inventage.portal.gateway.proxy.request.header.RequestHeaderMiddleware;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Function;

/**
 *
 */
public class RouterVerticle extends AbstractVerticle {

    public static final String SERVICE_PROVIDER = "provider";
    public static final String MIDDLEWARE_HEADERS = "headers";
    public static final String MIDDLEWARE_URIS = "uris";
    public static final String MIDDLEWARE_PROVIDER = "provider";

    private static Logger LOGGER = LoggerFactory.getLogger(RouterVerticle.class);

    private JsonObject routerConfig;
    private String publicHostname;
    private int entrypointPort;
    private Optional<JsonObject> middlewareConfig;
    private JsonObject serviceConfig;
    private Router router;
    private RequestHeaderMiddleware headerMiddleware = RequestHeaderMiddleware.IDENTITY;
    private Function<String, String> uriMiddleware = Function.identity();

    public RouterVerticle(JsonObject routerConfig, String publicHostname, int entrypointPort,
            Optional<JsonObject> middlewareConfig, JsonObject serviceConfig, Router router) {
        this.routerConfig = routerConfig;
        this.publicHostname = publicHostname;
        this.entrypointPort = entrypointPort;
        this.middlewareConfig = middlewareConfig;
        this.serviceConfig = serviceConfig;
        this.router = router;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        LOGGER.debug("start: router'{}'",
                this.routerConfig.getString(ProxyApplication.ROUTER_NAME));
        init(startPromise);
    }

    private void init(Promise<Void> startPromise) {
        LOGGER.debug("init: router'{}'", this.routerConfig.getString(ProxyApplication.ROUTER_NAME));

        Route route = router.route();
        route.handler(HeaderHandler.create());
        route.handler(ProxyHandler.create(vertx, serviceConfig));

        startPromise.complete();
    }
}
