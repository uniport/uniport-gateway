package com.inventage.portal.gateway.proxy.middleware.log;

import static com.inventage.portal.gateway.TestUtils.buildConfiguration;
import static com.inventage.portal.gateway.TestUtils.withMiddleware;
import static com.inventage.portal.gateway.TestUtils.withMiddlewareOpts;
import static com.inventage.portal.gateway.TestUtils.withMiddlewares;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static io.vertx.core.http.HttpMethod.GET;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareTestBase;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.slf4j.LoggerFactory;

@ExtendWith(VertxExtension.class)
public class RequestResponseLoggerMiddlewareTest extends MiddlewareTestBase {

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {
        final JsonObject simple = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", RequestResponseLoggerMiddlewareFactory.REQUEST_RESPONSE_LOGGER,
                    withMiddlewareOpts(JsonObject.of(
                        RequestResponseLoggerMiddlewareFactory.REQUEST_RESPONSE_LOGGER_FILTER_REGEX, ".*/health.*|.*/ready.*")))));

        final JsonObject minimal = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", RequestResponseLoggerMiddlewareFactory.REQUEST_RESPONSE_LOGGER)));

        final JsonObject missingOptions = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", RequestResponseLoggerMiddlewareFactory.REQUEST_RESPONSE_LOGGER)));

        final JsonObject unknownProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", RequestResponseLoggerMiddlewareFactory.REQUEST_RESPONSE_LOGGER,
                    withMiddlewareOpts(
                        JsonObject.of("bar", "blub")))));

        return Stream.of(
            Arguments.of("accept simple config", simple, complete, expectedTrue),
            Arguments.of("accept minimal config", minimal, complete, expectedTrue),
            Arguments.of("accept config with no options", missingOptions, complete, expectedTrue),
            Arguments.of("reject config with unknown property", unknownProperty, complete, expectedFalse)

        );
    }

    @Test
    public void decodeJWT() {
        // given
        final String jwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJtb2NrLlRlc3RKd3RQcm92aWRlciIsImF1ZCI6Ik9yZ2FuaXNhdGlvbiIsInN1YiI6InVzZXIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJlODI2NjUxMy02NWZkLTQ4ZTAtYmU0OS1lZDUxMTVlYTUwYzQiLCJuYW1lIjoiVMOpc3Qgw5tzZXIiLCJnaXZlbl9uYW1lIjoiTm_DqSIsInRlbmFudCI6InBvcnRhbCIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJwb3J0YWx1c2VyIl19LCJvcmdhbmlzYXRpb24iOiJpbnZlbnRhZ2UuY29tIiwicmVzb3VyY2VfYWNjZXNzIjp7Ik9yZ2FuaXNhdGlvbiI6eyJyb2xlcyI6WyJVU0VSIl19fSwiaHR0cHM6Ly9oYXN1cmEuaW8vand0L2NsYWltcyI6eyJ4LWhhc3VyYS11c2VyLWlkIjoidXNlciIsIngtaGFzdXJhLW9yZ2FuaXNhdGlvbi1pZCI6ImludmVudGFnZS5jb20iLCJ4LWhhc3VyYS10ZW5hbnQtaWQiOiJwb3J0YWwiLCJ4LWhhc3VyYS1kZWZhdWx0LXJvbGUiOiJVU0VSIiwieC1oYXN1cmEtYWxsb3dlZC1yb2xlcyI6WyJVU0VSIl19LCJpYXQiOjE2OTg0MDQ1MDgsImV4cCI6MTcyOTk0MDUwOCwianRpIjoiZWJiNDM5ZGItOTNkMy00YjBhLWIyNmUtZjM2ZGJhNmVhMWE0In0.SGQXR5552uv34XIglvUosQE1AWu0Lt9ZODMbfW88k1BK-zCfo_V_iW74pORUYmrO-Mw7S83fqBEJ9gUZPHDT8aLMpEIwDamwYnxNB4EzvKKaHcDuBk5WALmrHULo6w6MQkbtplUpmI8zK9CY7yhF2YDJ4a0hRJtdnkd3AitB8hasVbOclY_PZb5DYvR_iukoLcltrkLTBjC0YNa8aKPRUHW9TLVaRydHzBTHPeyG2tbg7QFaSRA61byrIZoL0VJiYTJQnahzr-BZaOuH3YHdrG6msnifGmHCdrf8A3SdEEksLFbI3zqHVds-jNwCY8hONO41T0GzkkbhU4xbPIUkvA";
        RequestResponseLoggerMiddleware requestResponseLoggerMiddleware = new RequestResponseLoggerMiddleware("AKKP-977", null, null, true, true);
        // when
        JsonObject payload = requestResponseLoggerMiddleware.decodeJWT(jwt);
        // then
        Assertions.assertNotNull(payload);
    }

    @Test
    public void requestNotLogged(Vertx vertx, VertxTestContext testCtx) {
        // given
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        Logger logger = setupLogger(listAppender);
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();

        portalGateway(vertx, "localhost", testCtx)
            .withRoutingContextHolder(routingContext)
            .withRequestResponseLoggerMiddleware("/health.*|/ready.*")
            .build().start()
            // when
            .incomingRequest(GET, "/health/test", new RequestOptions().setHeaders(headers), (incomingResponse) -> {
                // then
                assertTrue(listAppender.list.stream()
                    .noneMatch(event -> event.getFormattedMessage().matches(".*/health.*|/ready.*")));
                testCtx.completeNow();
                logger.detachAppender(listAppender);
            });
    }

    @Test
    public void requestLogged(Vertx vertx, VertxTestContext testCtx) {
        // given
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        Logger logger = setupLogger(listAppender);
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();

        portalGateway(vertx, "localhost", testCtx)
            .withRoutingContextHolder(routingContext)
            .withRequestResponseLoggerMiddleware("/health.*")
            .build().start()
            // when
            .incomingRequest(GET, "/test", new RequestOptions().setHeaders(headers), (incomingResponse) -> {
                // then
                assertTrue(listAppender.list.stream()
                    .anyMatch(event -> event.getFormattedMessage().matches(".*/test.*")));
                testCtx.completeNow();
                logger.detachAppender(listAppender);
            });
    }

    @Test
    public void noUriPatternForIgnoringRequestsSet(Vertx vertx, VertxTestContext testCtx) {
        // given
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        Logger logger = setupLogger(listAppender);
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();

        portalGateway(vertx, "localhost", testCtx)
            .withRoutingContextHolder(routingContext)
            .withRequestResponseLoggerMiddleware(null)
            .build().start()
            // when
            .incomingRequest(GET, "/test", new RequestOptions().setHeaders(headers), (incomingResponse) -> {
                // then
                assertTrue(listAppender.list.stream()
                    .anyMatch(event -> event.getFormattedMessage().matches(".*/test.*")));
                testCtx.completeNow();
                logger.detachAppender(listAppender);
            });
    }

    private Logger setupLogger(ListAppender<ILoggingEvent> listAppender) {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestResponseLoggerMiddleware.class);
        logger.addAppender(listAppender);
        listAppender.start();
        return logger;
    }

}
