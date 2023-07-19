package com.inventage.portal.gateway.proxy.middleware.csp;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Can be used as a CSP violation reporting server to be configured with the 'report-uri' or 'report-to' directive.
 * See: https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP#violation_report_syntax
 */
public class CSPViolationReportingServerMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSPViolationReportingServerMiddleware.class);

    private final String name;

    public CSPViolationReportingServerMiddleware(final String name) {
        this.name = name;
    }

    @Override
    public void handle(final RoutingContext ctx) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        if (!ctx.request().method().equals(HttpMethod.POST)) {
            LOGGER.info("Ignoring non-conformant request to the CSP violation reporting server.");
            ctx.fail(HttpResponseStatus.METHOD_NOT_ALLOWED.code());
            return;
        }

        ctx.request().body().onSuccess(body -> {
            final String errMsg;
            if (body == null) {
                errMsg = String.format("Received CSP violation report, but body is empty");
            } else {
                errMsg = String.format("Received CSP violation report: '%s'", body.toString());
            }
            LOGGER.warn(errMsg);
        }).onFailure(err -> {
            LOGGER.warn("Received CSP violation report, but failed to read body '{}'", err.getMessage());
        });

        ctx.end();
    }
}
