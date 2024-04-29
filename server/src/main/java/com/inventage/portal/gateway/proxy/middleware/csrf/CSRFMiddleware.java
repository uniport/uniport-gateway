package com.inventage.portal.gateway.proxy.middleware.csrf;

import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CSRFHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class CSRFMiddleware extends TraceMiddleware {
    private static final Logger LOGGER = LoggerFactory.getLogger(CSRFMiddleware.class);
    private static final int MILLISECONDS_IN_1_MINUTE = 60000;

    private final String name;
    private final CSRFHandler csrfHandler;

    /**
    */
    public CSRFMiddleware(
        Vertx vertx, String name, String secret, String cookieName, String cookiePath,
        boolean cookieSecure,
        String headerName, long timeoutInMinute, String origin, boolean nagHttps
    ) {
        this.name = name;
        this.csrfHandler = CSRFHandler.create(vertx, secret)
            .setCookieName(cookieName)
            .setCookiePath(cookiePath)
            .setCookieSecure(cookieSecure)
            .setHeaderName(headerName)
            .setTimeout(timeoutInMinute * MILLISECONDS_IN_1_MINUTE)
            .setNagHttps(nagHttps);
        if (origin != null) {
            this.csrfHandler.setOrigin(origin);
        }
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());
        csrfHandler.handle(ctx);
    }
}
