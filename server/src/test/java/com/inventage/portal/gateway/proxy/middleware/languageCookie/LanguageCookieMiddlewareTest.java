package com.inventage.portal.gateway.proxy.middleware.languageCookie;

import static com.inventage.portal.gateway.TestUtils.buildConfiguration;
import static com.inventage.portal.gateway.TestUtils.withMiddleware;
import static com.inventage.portal.gateway.TestUtils.withMiddlewareOpts;
import static com.inventage.portal.gateway.TestUtils.withMiddlewares;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static com.inventage.portal.gateway.proxy.middleware.languageCookie.LanguageCookieMiddlewareFactory.DEFAULT_LANGUAGE_COOKIE_NAME;
import static io.vertx.core.http.HttpMethod.GET;

import com.inventage.portal.gateway.proxy.middleware.MiddlewareTestBase;
import com.inventage.portal.gateway.proxy.middleware.VertxAssertions;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(VertxExtension.class)
public class LanguageCookieMiddlewareTest extends MiddlewareTestBase {

    private static final String host = "localhost";

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {
        final JsonObject simple = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", LanguageCookieMiddlewareFactory.TYPE,
                    withMiddlewareOpts(
                        JsonObject.of(LanguageCookieMiddlewareFactory.LANGUAGE_COOKIE_NAME, "blub")))));

        final JsonObject minimal = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", LanguageCookieMiddlewareFactory.TYPE)));

        final JsonObject unknownProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", LanguageCookieMiddlewareFactory.TYPE,
                    withMiddlewareOpts(
                        JsonObject.of("bar", "blub")))));

        return Stream.of(
            Arguments.of("valid config", simple, complete, expectedTrue),
            Arguments.of("minimal config", minimal, complete, expectedTrue),
            Arguments.of("reject config with unknown property", unknownProperty, complete, expectedFalse)

        );
    }

    @Test
    public void removeCookieInRequestsTest(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add(HttpHeaders.COOKIE, DEFAULT_LANGUAGE_COOKIE_NAME + "=de");
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();

        portalGateway(vertx, host, testCtx)
            .withRoutingContextHolder(routingContext)
            .withLanguageCookieMiddleware()
            .build().start()
            // when
            .incomingRequest(GET, "/", new RequestOptions().setHeaders(headers), (incomingResponse) -> {
                // then
                VertxAssertions.assertTrue(testCtx, routingContext.get().request().headers().contains(HttpHeaders.ACCEPT_LANGUAGE),
                    "request should contain accept language");
                VertxAssertions.assertEquals(testCtx, "de", routingContext.get().request().getHeader(HttpHeaders.ACCEPT_LANGUAGE),
                    "accept-language header should be set to 'de'");
                VertxAssertions.assertNotNull(testCtx, routingContext.get().request().getCookie(DEFAULT_LANGUAGE_COOKIE_NAME),
                    "request should contain IPS language cookie.");
                testCtx.completeNow();
            });
    }

    @Test
    public void noCookieAvailableTest(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();

        portalGateway(vertx, host, testCtx)
            .withRoutingContextHolder(routingContext)
            .withLanguageCookieMiddleware()
            .build().start()
            // when
            .incomingRequest(GET, "/", (incomingResponse) -> {
                // then
                VertxAssertions.assertFalse(testCtx, routingContext.get().request().headers().contains(DEFAULT_LANGUAGE_COOKIE_NAME),
                    "response should not contain IPS language cookie.");
                VertxAssertions.assertFalse(testCtx, routingContext.get().request().headers().contains(HttpHeaders.ACCEPT_LANGUAGE),
                    "response should not contain accept language");
                testCtx.completeNow();
            });
    }
}
