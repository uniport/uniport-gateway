package com.inventage.portal.gateway.core.log;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log every request and/or response.
 */
public class RequestResponseLogger implements Handler<RoutingContext> {

    private static Logger LOGGER = LoggerFactory.getLogger(RequestResponseLogger.class);

    public static Handler<RoutingContext> create() {
        return new RequestResponseLogger();
    }

    @Override
    public void handle(RoutingContext routingContext) {
        final long start = System.currentTimeMillis();
        LOGGER.debug("handle: uri '{}' for request '{}'", routingContext.request().uri(),
                routingContext.request().hashCode());
        routingContext.next();
        LOGGER.debug("handle: uri '{}' for request '{}' finished in '{}' ms",
                routingContext.request().uri(), routingContext.request().hashCode(),
                System.currentTimeMillis() - start);
    }
}
