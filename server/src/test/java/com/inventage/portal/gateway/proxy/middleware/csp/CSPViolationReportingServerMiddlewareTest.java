package com.inventage.portal.gateway.proxy.middleware.csp;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

@ExtendWith(VertxExtension.class)
class CSPViolationReportingServerMiddlewareTest {

    @Test
    void shouldAcceptAndLogConformantCspViolationReports(Vertx vertx, VertxTestContext testCtx) {
        //given
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspViolationReportingServerMiddleware()
            .build().start();

        final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        final Logger logger = setupLogger(listAppender);

        // taken from: https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP#sample_violation_report
        final JsonObject violationReport = JsonObject.of(
            "blocked-uri", "http://example.com/css/style.css",
            "disposition", "report",
            "document-uri", "http://example.com/signup.html",
            "effective-directive", "style-src-elem",
            "original-policy", "default-src 'none'; style-src cdn.example.com; report-to /_/csp-reports",
            "referrer", "",
            "status-code", 200,
            "violated-directive", "style-src-elem");

        //when
        gateway.incomingRequest(HttpMethod.POST, "/", new RequestOptions(), violationReport.toString(), (httpClientResponse -> {
            //then
            final int statusCode = httpClientResponse.statusCode();

            assertTrue(statusCode == 200,
                "Csp violation reporting server should only accept post requests");
            assertTrue(listAppender.list
                .stream()
                .anyMatch(entry -> entry
                    .getFormattedMessage()
                    .matches(".*\"blocked-uri\":\"http://example.com/css/style.css\".*")),
                "Csp violation reporting server should log report");
            testCtx.completeNow();
            logger.detachAppender(listAppender);
        }));
    }

    @Test
    void shouldIgnoreNonConformantCspViolationReports(Vertx vertx, VertxTestContext testCtx) {
        //given
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspViolationReportingServerMiddleware()
            .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final int statusCode = httpClientResponse.statusCode();

            assertTrue(statusCode == 405, " Csp violation reporting server should only accept post requests");
            testCtx.completeNow();
        }));
    }

    private Logger setupLogger(ListAppender<ILoggingEvent> listAppender) {
        final Logger logger = (Logger) LoggerFactory.getLogger(CSPViolationReportingServerMiddleware.class);
        logger.addAppender(listAppender);
        listAppender.start();
        return logger;
    }
}
