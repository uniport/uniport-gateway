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

    public static final String HTTP_HEADER_REQUEST_ID = "ips-request-id";
    public static final String CONTEXTUAL_DATA_REQUEST_ID = "requestId";
    public static final String CONTEXTUAL_DATA_SESSION_ID = "sessionId";
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponseLogger.class);

    public static Handler<RoutingContext> create() {
        return new RequestResponseLogger();
    }

    @Override
    public void handle(RoutingContext routingContext) {
        final String request_id = String.valueOf(routingContext.request().hashCode());
        // add the ips-request-id to the HTTP header, if it is not yet set
        if (!routingContext.request().headers().contains(HTTP_HEADER_REQUEST_ID)) {
            routingContext.request().headers().add(HTTP_HEADER_REQUEST_ID, request_id);
        }
        ContextualData.put(CONTEXTUAL_DATA_REQUEST_ID, request_id);
        ContextualData.put(CONTEXTUAL_DATA_SESSION_ID, SessionAdapter.displaySessionId(routingContext.session()));
        final long start = System.currentTimeMillis();
        LOGGER.debug("handle: incoming uri '{}'", routingContext.request().uri());
        routingContext.addHeadersEndHandler(v ->
                routingContext.response().putHeader(HTTP_HEADER_REQUEST_ID, request_id));
        routingContext.addBodyEndHandler(v ->
                LOGGER.debug("handle: outgoing uri '{}' with status '{}' in '{}' ms",
                    routingContext.request().uri(),
                    routingContext.response().getStatusCode(),
                    System.currentTimeMillis() - start)
        );
        routingContext.next();
    }

}
