package com.inventage.portal.gateway.proxy.middleware.log;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static io.vertx.core.http.HttpMethod.GET;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

@ExtendWith(VertxExtension.class)
public class RequestResponseLoggerMiddlewareTest {

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
            .incomingRequest(GET, "/health/test", new RequestOptions().setHeaders(headers), testCtx, (incomingResponse) -> {
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
            .incomingRequest(GET, "/test", new RequestOptions().setHeaders(headers), testCtx, (incomingResponse) -> {
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
            .incomingRequest(GET, "/test", new RequestOptions().setHeaders(headers), testCtx, (incomingResponse) -> {
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
