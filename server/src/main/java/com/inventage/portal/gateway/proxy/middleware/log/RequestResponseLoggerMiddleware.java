package com.inventage.portal.gateway.proxy.middleware.log;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import io.opentelemetry.api.trace.Span;
import io.reactiverse.contextual.logging.ContextualData;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

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
        final long start = System.currentTimeMillis();
        ContextualData.put(CONTEXTUAL_DATA_USER_ID, getUserId(ctx.user()));
        final String traceId = Span.current().getSpanContext().getTraceId();
        ContextualData.put(CONTEXTUAL_DATA_REQUEST_ID, traceId);
        ContextualData.put(CONTEXTUAL_DATA_SESSION_ID, SessionAdapter.displaySessionId(ctx.session()));

        LOGGER.debug("{} for '{}'", name, ctx.request().absoluteURI());

        if (LOGGER.isTraceEnabled()) {
            logRequestTrace(ctx);
        }
        else if (LOGGER.isDebugEnabled()) {
            logRequestDebug(ctx);
        }
        else if (LOGGER.isInfoEnabled()) {
            logRequestInfo(ctx);
        }

        ctx.addHeadersEndHandler(
                v -> ctx.response().putHeader(HTTP_HEADER_REQUEST_ID, traceId));
        ctx.addBodyEndHandler(v -> {
            if (LOGGER.isTraceEnabled()) {
                logResponseTrace(ctx, start);
            }
            else if (LOGGER.isDebugEnabled()) {
                logResponseDebug(ctx, start);
            }
            else if (LOGGER.isInfoEnabled()) {
                logResponseInfo(ctx, start);
            }
        });
        ctx.next();
    }

    private String getClientAddress(SocketAddress inetSocketAddress) {
        if (inetSocketAddress == null) {
            return null;
        }
        return inetSocketAddress.host();
    }

    private void logRequestInfo(RoutingContext context) {
        final String infoLog = generateHttpRequestLogMessage(context);
        LOGGER.info("{} incoming Request '{}'", name, infoLog);
    }

    private void logRequestDebug(RoutingContext context) {
        final String infoLog = generateHttpRequestLogMessage(context);
        final String headersLog = generateHttpHeaderLogMessage(context.request().headers());
        LOGGER.debug("{} incoming Request '{}'\nHeaders '{}'", name, infoLog, headersLog);
    }

    private void logRequestTrace(RoutingContext context) {
        final String infoLog = generateHttpRequestLogMessage(context);
        final String headersLog = generateHttpHeaderLogMessage(context.request().headers());
        final var request = context.request();
        final String contentType = request.getHeader("Content-Type");
        if (contentType != null && (contentType.startsWith("text/plain") || contentType.startsWith("application/json"))) {
            request.body().onSuccess(body -> {
                if (body != null) {
                    final String bodyContent = body.toString();
                    LOGGER.trace("{} incoming Request '{}'\nHeaders '{}'\nBody '{}'", name, infoLog, headersLog, bodyContent);
                }
            }).onFailure(error -> {
                LOGGER.trace("{} incoming Request '{}'\nHeaders '{}'\nBody '{}'", name, infoLog, headersLog, error.getMessage());
            });
        }
        else {
            LOGGER.trace("{} incoming Request '{}'\n Headers '{}'\n Content-type '{}'", name, infoLog, headersLog, contentType);
        }
    }

    private void logResponseDebug(RoutingContext context, long start) {
        final String infoLog = generateHttpResponseLogMessage(context, start);
        final String headerLog = generateHttpHeaderLogMessage(context.response().headers());
        LOGGER.debug("{} outgoing Response '{}' \nHeaders '{}'", name, infoLog, headerLog);
    }

    private void logResponseInfo(RoutingContext context, long start) {
        final String infoLog = generateHttpResponseLogMessage(context, start);
        LOGGER.info("{} outgoing Response '{}'", name, infoLog);
    }

    private void logResponseTrace(RoutingContext context, long start) {
        final String infoLog = generateHttpResponseLogMessage(context, start);
        final String headerLog = generateHttpHeaderLogMessage(context.response().headers());
        LOGGER.trace("{} outgoing Response '{}' \nHeaders '{}'", name, infoLog, headerLog);
    }

    private String generateHttpResponseLogMessage(RoutingContext routingContext, long start) {
        final long timestamp = System.currentTimeMillis();
        final HttpServerResponse response = routingContext.response();
        final int statusCode = response.getStatusCode();
        final String statusMessage = response.getStatusMessage();
        final String uri = routingContext.request().uri();
        final HttpMethod method = routingContext.request().method();

        return String.format("\"%s %s \" %d %s %d \" in %d ms \"",
                method,
                uri,
                statusCode,
                statusMessage,
                System.currentTimeMillis() - start,
                timestamp - start);
    }

    private String generateHttpRequestLogMessage(RoutingContext routingContext) {
        final long timestamp = System.currentTimeMillis();
        final String remoteClient = getClientAddress(routingContext.request().remoteAddress());
        final HttpMethod method = routingContext.request().method();
        final String uri = routingContext.request().uri();
        final HttpVersion version = routingContext.request().version();

        final HttpServerRequest request = routingContext.request();
        long contentLength = 0;
        final Object obj = request.headers().get("content-length");
        if (obj != null) {
            try {
                contentLength = Long.parseLong(obj.toString());
            }
            catch (NumberFormatException e) {
                // ignore it and continue
            }
        }

        String versionFormatted = "-";
        switch (version) {
            case HTTP_1_0:
                versionFormatted = "HTTP/1.0";
                break;
            case HTTP_1_1:
                versionFormatted = "HTTP/1.1";
                break;
            case HTTP_2:
                versionFormatted = "HTTP/2.0";
                break;
            default:
                versionFormatted = "UNKNOWN_HTTP_VERSION";
                break;
        }

        final MultiMap headers = request.headers();

        // as per RFC1945 the header is referer but it is not mandatory some implementations use referrer
        String referrer = headers.contains("referrer") ? headers.get("referrer") : headers.get("referer");
        String userAgent = request.headers().get("user-agent");
        referrer = referrer == null ? "-" : referrer;
        userAgent = userAgent == null ? "-" : userAgent;

        return String.format("\"%s %s %s\" %d \"%s\" \"%s\" - %dms %s",
                method,
                uri,
                versionFormatted,
                contentLength,
                referrer,
                userAgent,
                (System.currentTimeMillis() - timestamp),
                remoteClient);
    }

    private String generateHttpHeaderLogMessage(MultiMap headers) {
        final StringBuilder headerBuilder = new StringBuilder();
        for (String headerName : headers.names()) {
            headerBuilder.append(headerName).append(": ").append(headers.get(headerName)).append(" - ");
        }
        return headerBuilder.toString();
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
        }
        else if (principal.containsKey("access_token")) {
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
