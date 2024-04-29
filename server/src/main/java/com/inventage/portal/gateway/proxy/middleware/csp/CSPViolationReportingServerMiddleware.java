package com.inventage.portal.gateway.proxy.middleware.csp;

import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * Can be used as a CSP violation reporting server to be configured with the 'report-uri' or 'report-to' directive.
 * See: https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP#violation_report_syntax
 */
public class CSPViolationReportingServerMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSPViolationReportingServerMiddleware.class);

    private final String name;
    private final Level level;

    /**
    */
    public CSPViolationReportingServerMiddleware(final String name, final Level level) {
        this.name = name;
        this.level = level;
    }

    @Override
    public void handleWithTraceSpan(final RoutingContext ctx, Span span) {
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
            log(errMsg);
        }).onFailure(err -> {
            LOGGER.warn("Received CSP violation report, but failed to read body '{}'", err.getMessage());
        });

        ctx.end();
    }

    private void log(String msg) {
        switch (this.level) {
            case TRACE:
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(msg);
                }
                break;
            case DEBUG:
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(msg);
                }
                break;
            case INFO:
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(msg);
                }
                break;
            case WARN:
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(msg);
                }
                break;
            case ERROR:
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(msg);
                }
                break;
            default:
                throw new RuntimeException("unreachable switch case");
        }
    }
}
