package com.inventage.portal.gateway.proxy.middleware.controlapi;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlApiMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControlApiMiddlewareFactory.class);

    // reusable instance
    private WebClient webClient;

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_CONTROL_API;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("Created '{}' middleware successfully", DynamicConfiguration.MIDDLEWARE_CONTROL_API);

        if (webClient == null) {
            webClient = WebClient.create(vertx);
        }

        final String action = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_CONTROL_API_ACTION);
        return Future.succeededFuture(new ControlApiMiddleware(name, action, webClient));
    }

}
