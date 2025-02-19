package com.inventage.portal.gateway.proxy.middleware.log;

import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log every request and/or response and adds the requestId and the sessionId to
 * the contextual data.
 */
public class RequestResponseLoggerMiddleware extends TraceMiddleware {

    public static final String CONTEXTUAL_DATA_USER_ID = "userId";

    public static final String EMPTY_USER_ID = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponseLoggerMiddleware.class);

    private final String name;

    private final Pattern uriPatternForIgnoringRequests;

    private final List<String> contentTypesToLog;

    private final boolean isLoggingRequestEnabled;

    private final boolean isLoggingResponseEnabled;

    public RequestResponseLoggerMiddleware(
        String name,
        String uriPatternForIgnoringRequests,
        List<String> contentTypesToLog,
        boolean isLoggingRequestEnabled,
        boolean isLoggingResponseEnabled
    ) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(contentTypesToLog, "contentTypesToLog must not be null");
        // uriPatternForIgnoringRequests is allowed to be null

        this.name = name;
        this.uriPatternForIgnoringRequests = uriPatternForIgnoringRequests == null ? null : Pattern.compile(uriPatternForIgnoringRequests);
        this.contentTypesToLog = contentTypesToLog;
        this.isLoggingRequestEnabled = isLoggingRequestEnabled;
        this.isLoggingResponseEnabled = isLoggingResponseEnabled;
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        if (isRequestIgnoredForLogging(ctx.request().uri())) {
            ctx.next();
            return;
        }

        final long start = System.currentTimeMillis();
        ContextualDataAdapter.put(CONTEXTUAL_DATA_USER_ID, getUserId(ctx.user()));

        LOGGER.debug("{} for '{}'", name, ctx.request().absoluteURI());

        if (isLoggingRequestEnabled) {
            if (LOGGER.isTraceEnabled()) {
                logRequestTrace(ctx);
            } else if (LOGGER.isDebugEnabled()) {
                logRequestDebug(ctx);
            } else if (LOGGER.isInfoEnabled()) {
                logRequestInfo(ctx);
            }
        }

        if (isLoggingResponseEnabled) {
            ctx.addBodyEndHandler(v -> {
                if (LOGGER.isTraceEnabled()) {
                    logResponseTrace(ctx, start);
                } else if (LOGGER.isDebugEnabled()) {
                    logResponseDebug(ctx, start);
                } else if (LOGGER.isInfoEnabled()) {
                    logResponseInfo(ctx, start);
                }
            });
        }
        ctx.next();
    }

    private boolean isRequestIgnoredForLogging(String requestUri) {
        if (this.uriPatternForIgnoringRequests == null) {
            return false;
        }
        return uriPatternForIgnoringRequests.matcher(requestUri).matches();
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
        if (contentType != null
            && this.contentTypesToLog.stream().anyMatch(ctl -> contentType.startsWith(ctl))) {
            request.body().onSuccess(body -> {
                if (body != null) {
                    final String bodyContent = body.toString();
                    LOGGER.trace("{} incoming Request '{}'\nHeaders '{}'\nBody '{}'", name, infoLog, headersLog,
                        bodyContent);
                }
            }).onFailure(error -> {
                LOGGER.trace("{} incoming Request '{}'\nHeaders '{}'\nBody '{}'", name, infoLog, headersLog,
                    error.getMessage());
            });
        } else {
            LOGGER.trace("{} incoming Request '{}'\n Headers '{}'\n Content-type '{}'", name, infoLog, headersLog,
                contentType);
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
        final String contentLengthStr = request.headers().get("content-length");
        if (contentLengthStr != null) {
            try {
                contentLength = Long.parseLong(contentLengthStr);
            } catch (NumberFormatException e) {
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

        // as per RFC1945 the header is referer but it is not mandatory some
        // implementations use referrer
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
            return EMPTY_USER_ID;
        }
        final JsonObject principal = u.principal();
        if (principal == null) {
            return EMPTY_USER_ID;
        }

        String userId = EMPTY_USER_ID;
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

    protected JsonObject decodeJWT(String jwt) {
        try {
            final String[] chunks = jwt.split("\\.");
            // JWT: https://datatracker.ietf.org/doc/html/rfc7519#section-3
            final Base64.Decoder urlDecoder = Base64.getUrlDecoder();
            final String payload = new String(urlDecoder.decode(chunks[1]));
            return new JsonObject(payload);
        } catch (IllegalArgumentException e) {
            LOGGER.error("failed for JWT '{}' because of '{}'", jwt, e.getMessage());
            throw e;
        }
    }
}
