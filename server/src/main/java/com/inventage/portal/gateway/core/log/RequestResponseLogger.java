package com.inventage.portal.gateway.core.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Log every request and/or response.
 */
public class RequestResponseLogger implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponseLogger.class);

    public static Handler<RoutingContext> create() {
        return new RequestResponseLogger();
    }

    @Override
    public void handle(RoutingContext routingContext) {
        final long start = System.currentTimeMillis();
        LOGGER.debug("handle: incoming uri '{}' for request '{}'", routingContext.request().uri(),
                routingContext.request().hashCode());
        routingContext.next();
        LOGGER.debug("handle: outgoing uri '{}' for request '{}' with status '{}' in '{}' ms", routingContext.request().uri(),
                routingContext.request().hashCode(), routingContext.response().getStatusCode(), System.currentTimeMillis() - start);
    }
}
