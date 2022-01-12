package com.inventage.portal.gateway.core.log;

import com.inventage.portal.gateway.core.session.SessionAdapter;
import io.reactiverse.contextual.logging.ContextualData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Log every request and/or response and adds the requestId and the sessionId to the contextual data.
 */
public class RequestResponseLogger implements Handler<RoutingContext> {

    public static final String CONTEXTUAL_DATA_REQUEST_ID = "requestId";
    public static final String CONTEXTUAL_DATA_SESSION_ID = "sessionId";
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponseLogger.class);

    public static Handler<RoutingContext> create() {
        return new RequestResponseLogger();
    }

    @Override
    public void handle(RoutingContext routingContext) {
        ContextualData.put(CONTEXTUAL_DATA_REQUEST_ID, String.valueOf(routingContext.request().hashCode()));
        ContextualData.put(CONTEXTUAL_DATA_SESSION_ID, SessionAdapter.displaySessionId(routingContext.session()));
        final long start = System.currentTimeMillis();
        LOGGER.debug("handle: incoming uri '{}'", routingContext.request().uri());
        routingContext.addBodyEndHandler(v ->
                LOGGER.debug("handle: outgoing uri '{}' with status '{}' in '{}' ms",
                        routingContext.request().uri(),
                        routingContext.response().getStatusCode(),
                        System.currentTimeMillis() - start));
        routingContext.next();
    }

}
