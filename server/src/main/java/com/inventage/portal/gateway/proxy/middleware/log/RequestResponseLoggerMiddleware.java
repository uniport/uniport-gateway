package com.inventage.portal.gateway.proxy.middleware.log;

import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.proxy.middleware.Middleware;

import io.opentelemetry.api.trace.Span;
import io.reactiverse.contextual.logging.ContextualData;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;

/**
 * Log every request and/or response and adds the requestId and the sessionId to the contextual data.
 */
public class RequestResponseLoggerMiddleware implements Middleware {

    public static final String HTTP_HEADER_REQUEST_ID = "X-IPS-Trace-Id";
    public static final String CONTEXTUAL_DATA_REQUEST_ID = "traceId";
    public static final String CONTEXTUAL_DATA_SESSION_ID = "sessionId";
    public static final String CONTEXTUAL_DATA_USER_ID = "userId";

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponseLoggerMiddleware.class);

    private final String name;

    public RequestResponseLoggerMiddleware(String name) {
        this.name = name;
    }

    @Override
    public void handle(RoutingContext ctx) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        ContextualData.put(CONTEXTUAL_DATA_USER_ID, getUserId(ctx.user()));
        final String traceId = Span.current().getSpanContext().getTraceId();
        ContextualData.put(CONTEXTUAL_DATA_REQUEST_ID, traceId);
        ContextualData.put(CONTEXTUAL_DATA_SESSION_ID, SessionAdapter.displaySessionId(ctx.session()));

        final long start = System.currentTimeMillis();
        logRequest(ctx);
        // add the ips-request-id to the HTTP header, if it is not yet set
        ctx.addHeadersEndHandler(v -> ctx.response().putHeader(HTTP_HEADER_REQUEST_ID, traceId));
        ctx.addBodyEndHandler(v -> {
            logResponse(ctx, start);
        });
        ctx.next();
    }

    private void logRequest(RoutingContext routingContext) {
        LOGGER.debug("Incoming URI '{}'", routingContext.request().uri());
    }

    private void logResponse(RoutingContext routingContext, long start) {
        if (routingContext.response().getStatusCode() >= 400) {
            // More logging when response is >= 400
            LOGGER.debug("'Outgoing URI '{}' with status '{}' and message '{}' in '{}' ms",
                    routingContext.request().uri(),
                    routingContext.response().getStatusCode(),
                    routingContext.response().getStatusMessage(),
                    System.currentTimeMillis() - start);
        } else {
            LOGGER.debug("Outgoing URI '{}' with status '{}' in '{}' ms",
                    routingContext.request().uri(),
                    routingContext.response().getStatusCode(),
                    System.currentTimeMillis() - start);
        }
    }

    private String getUserId(User u) {
        if (u == null) {
            return "";
        }
        final JsonObject principal = u.principal();
        if (principal == null) {
            return "";
        }

        String userId = "";
        if (principal.containsKey("id_token")) {
            final JsonObject idToken = decodeJWT(principal.getString("id_token"));
            if (idToken.containsKey("preferred_username")) {
                userId = idToken.getString("preferred_username");
            }
        } else if (principal.containsKey("access_token")) {
            final JsonObject accessToken = decodeJWT(principal.getString("access_token"));
            if (accessToken.containsKey("preferred_username")) {
                userId = accessToken.getString("preferred_username");
            }
        }
        return userId;
    }

    private JsonObject decodeJWT(String jwt) {
        final String[] chunks = jwt.split("\\.");
        final Base64.Decoder decoder = Base64.getDecoder();
        final String payload = new String(decoder.decode(chunks[1]));
        return new JsonObject(payload);
    }
}
