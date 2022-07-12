package com.inventage.portal.gateway.core.log;

import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.core.session.SessionAdapter;

import io.opentelemetry.api.trace.Span;
import io.reactiverse.contextual.logging.ContextualData;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;

/**
 * Log every request and/or response and adds the requestId and the sessionId to the contextual data.
 */
public class RequestResponseLogger implements Handler<RoutingContext> {

    public static final String HTTP_HEADER_REQUEST_ID = "X-IPS-Trace-Id";
    public static final String CONTEXTUAL_DATA_REQUEST_ID = "traceId";
    public static final String CONTEXTUAL_DATA_SESSION_ID = "sessionId";
    public static final String CONTEXTUAL_DATA_USER_ID = "userId";
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponseLogger.class);

    public static Handler<RoutingContext> create() {
        return new RequestResponseLogger();
    }

    @Override
    public void handle(RoutingContext routingContext) {
        ContextualData.put(CONTEXTUAL_DATA_USER_ID, getUserId(routingContext.user()));
        final String traceId = Span.current().getSpanContext().getTraceId();
        ContextualData.put(CONTEXTUAL_DATA_REQUEST_ID, traceId);
        ContextualData.put(CONTEXTUAL_DATA_SESSION_ID, SessionAdapter.displaySessionId(routingContext.session()));

        final long start = System.currentTimeMillis();
        LOGGER.debug("Incoming uri '{}'", routingContext.request().uri());
        // add the ips-request-id to the HTTP header, if it is not yet set
        routingContext.addHeadersEndHandler(v -> routingContext.response().putHeader(HTTP_HEADER_REQUEST_ID, traceId));
        routingContext.addBodyEndHandler(v -> {
            // More logging when response is >= 400
            if (routingContext.response().getStatusCode() >= 400) {
                LOGGER.debug("'{}' in '{}' ms",
                        routingContext.request().uri(),
                        routingContext.response().getStatusCode(),
                        routingContext.response().getStatusMessage(),
                        System.currentTimeMillis() - start);
                return;
            }

            LOGGER.debug("Outgoing uri '{}' with status '{}' in '{}' ms",
                    routingContext.request().uri(),
                    routingContext.response().getStatusCode(),
                    System.currentTimeMillis() - start);
        });
        routingContext.next();
    }

    private String getUserId(User u) {
        if (u == null) {
            return "";
        }
        JsonObject prinicipal = u.principal();
        if (prinicipal == null) {
            return "";
        }

        String userId = "";
        if (prinicipal.containsKey("id_token")) {
            JsonObject idToken = decodeJWT(prinicipal.getString("id_token"));
            if (idToken.containsKey("preferred_username")) {
                userId = idToken.getString("preferred_username");
            }
        } else if (prinicipal.containsKey("access_token")) {
            JsonObject accessToken = decodeJWT(prinicipal.getString("access_token"));
            if (accessToken.containsKey("preferred_username")) {
                userId = accessToken.getString("preferred_username");
            }
        }
        return userId;
    }

    private JsonObject decodeJWT(String jwt) {
        String[] chunks = jwt.split("\\.");
        Base64.Decoder decoder = Base64.getDecoder();
        String payload = new String(decoder.decode(chunks[1]));
        return new JsonObject(payload);
    }
}
