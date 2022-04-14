package com.inventage.portal.gateway.core.log;

import com.inventage.portal.gateway.core.session.SessionAdapter;
import io.opentelemetry.api.trace.Span;
import io.reactiverse.contextual.logging.ContextualData;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log every request and/or response and adds the requestId and the sessionId to the contextual data.
 */
public class RequestResponseLogger implements Handler<RoutingContext> {

    public static final String HTTP_HEADER_REQUEST_ID = "X-IPS-Trace-Id";
    public static final String CONTEXTUAL_DATA_REQUEST_ID = "traceId";
    public static final String CONTEXTUAL_DATA_SESSION_ID = "sessionId";
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponseLogger.class);

    public static Handler<RoutingContext> create() {
        return new RequestResponseLogger();
    }

    @Override
    public void handle(RoutingContext routingContext) {
        final String traceId = Span.current().getSpanContext().getTraceId();
        // add the ips-request-id to the HTTP header, if it is not yet set
        ContextualData.put(CONTEXTUAL_DATA_REQUEST_ID, traceId);
        ContextualData.put(CONTEXTUAL_DATA_SESSION_ID, SessionAdapter.displaySessionId(routingContext.session()));
        final long start = System.currentTimeMillis();
        LOGGER.debug("handle: incoming uri '{}'", routingContext.request().uri());
        routingContext.addHeadersEndHandler(v ->
                routingContext.response().putHeader(HTTP_HEADER_REQUEST_ID, traceId));
        routingContext.addBodyEndHandler(v -> {
            // More logging when response is >= 400
            if (routingContext.response().getStatusCode() >= 400) {
                LOGGER.debug("handle: outgoing uri '{}' with status '{}' and message: '{}' in '{}' ms",
                        routingContext.request().uri(),
                        routingContext.response().getStatusCode(),
                        routingContext.response().getStatusMessage(),
                        System.currentTimeMillis() - start);
                return;
            }

            LOGGER.debug("handle: outgoing uri '{}' with status '{}' in '{}' ms",
                    routingContext.request().uri(),
                    routingContext.response().getStatusCode(),
                    System.currentTimeMillis() - start);
        });
        routingContext.next();
    }
}
