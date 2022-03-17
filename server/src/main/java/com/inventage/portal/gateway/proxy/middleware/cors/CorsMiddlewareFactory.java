package com.inventage.portal.gateway.proxy.middleware.cors;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.sessionBag.SessionBagMiddleware;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorsMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CorsMiddlewareFactory.class);

    @Override
    public String provides() {
        return "cors"; //DynamicConfiguration.MIDDLEWARE_CORS;;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, Router router, JsonObject middlewareConfig) {
        //LOGGER.debug("create: '{}' middleware", DynamicConfiguration.MIDDLEWARE_CORS);
        return Future.succeededFuture(new CorsMiddleware(router, "http://localhost"));    }
}
