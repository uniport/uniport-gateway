package com.inventage.portal.gateway.proxy.middleware.checkRoute;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.HttpResponder;
import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.opentelemetry.api.trace.Span;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware for checking a specific route.
 * If "_check-route_" is found within in the URI of the incoming request, the request is
 * ended immediately and returns a status code 202. Otherwise, the request is passed on.
 */
public class CheckRouteMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckRouteMiddleware.class);
    private static final HttpResponseStatus RESPONSE_STATUS_CODE = HttpResponseStatus.ACCEPTED; // 202

    private final String name;

    /**
    */
    public CheckRouteMiddleware(String name) {
        this.name = name;
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        if (isCheckRoute(ctx)) {
            LOGGER.info("done for URL '{}' with status code '{}'", ctx.request().absoluteURI(),
                RESPONSE_STATUS_CODE.code());
            HttpResponder.respondWithStatusCode(RESPONSE_STATUS_CODE, ctx);
        } else {
            ctx.next();
        }
    }

    private boolean isCheckRoute(RoutingContext ctx) {
        return ctx.request().uri().contains(DynamicConfiguration.MIDDLEWARE_CHECK_ROUTE_PATH);
    }

}
