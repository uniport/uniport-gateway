package com.inventage.portal.gateway.proxy.middleware.claimToHeader;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClaimToHeaderMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClaimToHeaderMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_CLAIM_TO_HEADER;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("Created '{}' middleware successfully",
            DynamicConfiguration.MIDDLEWARE_CLAIM_TO_HEADER);
        return Future.succeededFuture(new ClaimToHeaderMiddleware(name,
            middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_CLAIM_TO_HEADER_PATH),
            middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_CLAIM_TO_HEADER_NAME)));
    }
}
