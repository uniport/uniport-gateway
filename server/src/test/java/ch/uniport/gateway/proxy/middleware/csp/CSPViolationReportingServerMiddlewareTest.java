package ch.uniport.gateway.proxy.middleware.csp;

import static ch.uniport.gateway.TestUtils.buildConfiguration;
import static ch.uniport.gateway.TestUtils.withMiddleware;
import static ch.uniport.gateway.TestUtils.withMiddlewareOpts;
import static ch.uniport.gateway.TestUtils.withMiddlewares;
import static ch.uniport.gateway.proxy.middleware.MiddlewareServerBuilder.uniportGateway;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import ch.uniport.gateway.proxy.middleware.MiddlewareServer;
import ch.uniport.gateway.proxy.middleware.MiddlewareTestBase;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

@ExtendWith(VertxExtension.class)
class CSPViolationReportingServerMiddlewareTest extends MiddlewareTestBase {

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {
        final JsonObject logLevelTrace = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CSPViolationReportingServerMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        CSPViolationReportingServerMiddlewareFactory.LOG_LEVEL, "TRACE")))));

        final JsonObject logLevelWithWeirdCaps = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CSPViolationReportingServerMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        CSPViolationReportingServerMiddlewareFactory.LOG_LEVEL, "eRroR")))));

        final JsonObject invalidValues = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CSPViolationReportingServerMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        CSPViolationReportingServerMiddlewareFactory.LOG_LEVEL, "blub")))));

        final JsonObject missingOptions = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CSPViolationReportingServerMiddlewareFactory.TYPE)));

        final JsonObject unknownProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CSPViolationReportingServerMiddlewareFactory.TYPE,
                    withMiddlewareOpts(
                        JsonObject.of("bar", "blub")))));

        return Stream.of(
            Arguments.of("accept config with log level", logLevelTrace, complete, expectedTrue),
            Arguments.of("reject config with log level with weird caps", logLevelWithWeirdCaps, complete, expectedFalse),
            Arguments.of("reject config with invalid log level", invalidValues, complete, expectedFalse),
            Arguments.of("reject config with no options", missingOptions, complete, expectedFalse),
            Arguments.of("reject config with unknown property", unknownProperty, complete, expectedFalse)

        );
    }

    @ParameterizedTest
    @ValueSource(strings = { "TRACE", "DEBUG", "INFO", "WARN", "ERROR" })
    void shouldAcceptAndLogConformantCspViolationReports(String logLevel, Vertx vertx, VertxTestContext testCtx) {
        //given
        final MiddlewareServer gateway = uniportGateway(vertx, testCtx)
            .withCspViolationReportingServerMiddleware(logLevel)
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

        final Predicate<ILoggingEvent> isCorrectLogLevel = e -> e.getLevel().toString().matches(logLevel);
        final Predicate<ILoggingEvent> hasViolationReport = e -> e.getFormattedMessage().matches(".*\"blocked-uri\":\"http://example.com/css/style.css\".*");

        //when
        gateway.incomingRequest(HttpMethod.POST, "/", new RequestOptions(), violationReport.toString(), (httpClientResponse -> {
            //then
            final int statusCode = httpClientResponse.statusCode();
            assertTrue(statusCode == 200,
                "Csp violation reporting server should only accept post requests");
            assertTrue(listAppender.list
                .stream()
                .anyMatch(isCorrectLogLevel.and(hasViolationReport)),
                "Csp violation reporting server should log report on correct log level");
            testCtx.completeNow();
            logger.detachAppender(listAppender);
        }));
    }

    @Test
    void shouldIgnoreNonConformantCspViolationReports(Vertx vertx, VertxTestContext testCtx) {
        //given
        final MiddlewareServer gateway = uniportGateway(vertx, testCtx)
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
        // because we also test on trace
        logger.setLevel(Level.TRACE);
        listAppender.start();
        return logger;
    }
}
