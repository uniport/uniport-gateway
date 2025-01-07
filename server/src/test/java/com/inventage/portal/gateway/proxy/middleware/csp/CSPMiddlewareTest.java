package com.inventage.portal.gateway.proxy.middleware.csp;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static com.inventage.portal.gateway.proxy.middleware.VertxAssertions.assertFalse;
import static com.inventage.portal.gateway.proxy.middleware.VertxAssertions.assertNotNull;
import static com.inventage.portal.gateway.proxy.middleware.VertxAssertions.assertNull;
import static com.inventage.portal.gateway.proxy.middleware.VertxAssertions.assertTrue;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP.CSPMergeStrategy;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class CSPMiddlewareTest {

    private static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    private static final String CONTENT_SECURITY_POLICY_REPORT_ONLY = "Content-Security-Policy-Report-Only";

    @Test
    void checkForCspInHTTPHeader(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String defaultSrc = "default-src";
        final String defaultSrcValue = "self";

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(defaultSrc, defaultSrcValue)), false)
            .build().start();

        // when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            // then
            final MultiMap headers = httpClientResponse.headers();
            final String cspValues = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(testCtx, cspValues, String.format("should have '%s' header!", CONTENT_SECURITY_POLICY));
            assertTrue(testCtx, cspValues.contains(defaultSrc) && cspValues.contains(defaultSrcValue),
                String.format("should have CSP directive and value '%s %s' in '%s'", defaultSrc, defaultSrcValue, cspValues));
            testCtx.completeNow();
        }));
    }

    @Test
    void checkReportOnlyInHTTPHeader(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String defaultSrc = "default-src";
        final String defaultSrcValue = "self";

        final String reportTo = "report-to";
        final String reportToValue = "www.example.com";

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(
                createDirective(defaultSrc, defaultSrcValue),
                createDirective(reportTo, reportToValue)), true)
            .build().start();

        // when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            // then
            final MultiMap headers = httpClientResponse.headers();
            final String cspValuesReportOnly = headers.get(CONTENT_SECURITY_POLICY_REPORT_ONLY);
            final String cspValues = headers.get(CONTENT_SECURITY_POLICY);

            assertNull(testCtx, cspValues, String.format("should not have '%s' header", CONTENT_SECURITY_POLICY));
            assertNotNull(testCtx, cspValuesReportOnly, String.format("should '%s' header!", CONTENT_SECURITY_POLICY));
            assertTrue(testCtx,
                cspValuesReportOnly.contains(defaultSrc)
                    && cspValuesReportOnly.contains(defaultSrcValue)
                    && cspValuesReportOnly.contains(reportToValue),
                String.format("should have CSP directive and value '%s %s %s' in '%s'", defaultSrc, defaultSrcValue, reportToValue, cspValues));

            testCtx.completeNow();
        }));
    }

    @Test
    void chainingTwoCSPMiddlewaresDifferentDirectives(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String styleDirective = "style-src";
        final String styleValue = "self";

        final String mediaDirective = "media-src";
        final String mediaValue = "self";

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(mediaDirective, mediaValue)), false)
            .withCspMiddleware(JsonArray.of(createDirective(styleDirective, styleValue)), false)
            .build().start();

        // when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            // then
            final MultiMap headers = httpClientResponse.headers();
            final String cspValues = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(testCtx, cspValues, String.format("should have '%s' header", CONTENT_SECURITY_POLICY));
            assertTrue(testCtx, cspValues.contains(styleDirective) && cspValues.contains(styleValue),
                String.format("should have CSP directive and value '%s %s' in '%s'", styleDirective, styleValue, cspValues));
            assertTrue(testCtx, cspValues.contains(mediaDirective) && cspValues.contains(mediaValue),
                String.format("should have CSP directive and value '%s %s' in '%s'", mediaDirective, mediaValue, cspValues));
            testCtx.completeNow();
        }));
    }

    @Test
    void chainingTwoCSPMiddlewaresWithSameDirective(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String styleDirective = "style-src";
        final String styleValue = "self";
        final String differentStyleValue = "https://fonts.googleapis.com https://fonts.gstatic.com https://cdn.jsdelivr.net";

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(styleDirective, differentStyleValue)), false)
            .withCspMiddleware(JsonArray.of(createDirective(styleDirective, styleValue)), false)
            .build().start();

        // when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            // then
            final MultiMap headers = httpClientResponse.headers();
            final String cspValues = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(testCtx, cspValues, String.format("should not have '%s' header", CONTENT_SECURITY_POLICY));
            assertTrue(testCtx, cspValues.contains(styleDirective) && cspValues.contains(styleValue),
                String.format("should have CSP directive and value '%s %s' in '%s'", styleDirective, styleValue, cspValues));

            for (String expectedValue : differentStyleValue.split(" ")) {
                assertTrue(testCtx, cspValues.contains(expectedValue),
                    String.format("should have CSP value '%s' in '%s'", expectedValue, cspValues));
            }

            testCtx.completeNow();
        }));
    }

    @Test
    void emptyExternalCSPPoliciesFromBackend(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //given
        final String middlewareStyleDirective = "style-src";
        final String middlewareStyleValue = "self";

        final int backendPort = TestUtils.findFreePort();
        final Handler<RoutingContext> cspInsertionHandler = ctx -> {
            ctx.response()
                .putHeader(CONTENT_SECURITY_POLICY, "")
                .end();
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(middlewareStyleDirective, middlewareStyleValue)), false, CSPMergeStrategy.UNION)
            .withProxyMiddleware(backendPort)
            .withProxyMiddleware(backendPort)
            .withBackend(vertx, backendPort, cspInsertionHandler)
            .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final MultiMap headers = httpClientResponse.headers();
            final String cspValues = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(testCtx, cspValues, String.format("should have '%s' header", CONTENT_SECURITY_POLICY));
            assertTrue(testCtx, cspValues.contains(middlewareStyleDirective) && cspValues.contains(middlewareStyleValue),
                String.format("should have CSP directive and value '%s %s' in '%s'", middlewareStyleDirective, middlewareStyleValue, cspValues));
            testCtx.completeNow();
        }));
    }

    @Test
    void chainingTwoCSPMiddlewareAndMergingDisjunctiveExternalCSPPoliciesFromBackend(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //given
        final String middlewareStyleDirective = "style-src";
        final String middlewareStyleValue = "self";
        final String differentStyleValue = "https://fonts.googleapis.com";

        final String backendMediaDirective = "media-src";
        final String backendMediaValue = "'self'";

        final int backendPort = TestUtils.findFreePort();
        final Handler<RoutingContext> cspInsertionHandler = ctx -> {
            ctx.response()
                .putHeader(CONTENT_SECURITY_POLICY, backendMediaDirective + " " + backendMediaValue)
                .end();
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(middlewareStyleDirective, middlewareStyleValue)), false, CSPMergeStrategy.UNION)
            .withCspMiddleware(JsonArray.of(createDirective(middlewareStyleDirective, differentStyleValue)), false, CSPMergeStrategy.UNION)
            .withProxyMiddleware(backendPort)
            .withBackend(vertx, backendPort, cspInsertionHandler)
            .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final MultiMap headers = httpClientResponse.headers();
            final String cspValues = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(testCtx, cspValues, String.format("should have '%s' header!", CONTENT_SECURITY_POLICY));
            assertTrue(testCtx, cspValues.contains(middlewareStyleDirective) && cspValues.contains(middlewareStyleValue),
                String.format("should have CSP directive and value '%s %s' in '%s'", middlewareStyleDirective, middlewareStyleValue, cspValues));
            assertTrue(testCtx, cspValues.contains(backendMediaDirective) && cspValues.contains(backendMediaValue),
                String.format("should have CSP directive and value '%s %s' in '%s'", backendMediaDirective, backendMediaValue, cspValues));
            assertTrue(testCtx, cspValues.contains(differentStyleValue),
                String.format("should have CSP directive and value '%ss' in '%s'", differentStyleValue, cspValues));
            testCtx.completeNow();
        }));
    }

    @Test
    void chainingTwoCSPMiddlewareAndIgnoringExternalCSPPoliciesFromBackend(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String middlewareStyleDirective = "style-src";
        final String middlewareStyleValue = "self";
        final String differentStyleValue = "https://fonts.googleapis.com";

        final String backendMediaDirective = "media-src";
        final String backendMediaValue = "'self'";

        final int backendPort = TestUtils.findFreePort();
        final Handler<RoutingContext> cspInsertionHandler = ctx -> {
            ctx.response()
                .putHeader(CONTENT_SECURITY_POLICY, backendMediaDirective + " " + backendMediaValue)
                .end();
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(middlewareStyleDirective, middlewareStyleValue)), false, CSPMergeStrategy.UNION)
            .withCspMiddleware(JsonArray.of(createDirective(middlewareStyleDirective, differentStyleValue)), false, CSPMergeStrategy.INTERNAL)
            .withProxyMiddleware(backendPort)
            .withBackend(vertx, backendPort, cspInsertionHandler)
            .build().start();

        // when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final MultiMap headers = httpClientResponse.headers();
            final String cspValues = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(testCtx, cspValues, String.format("should have '%s' header!", CONTENT_SECURITY_POLICY));
            assertTrue(testCtx, cspValues.contains(middlewareStyleDirective) && cspValues.contains(middlewareStyleValue),
                String.format("should have CSP directive and value '%s %s' in '%s'", middlewareStyleDirective, middlewareStyleValue, cspValues));
            assertFalse(testCtx, cspValues.contains(backendMediaDirective) && cspValues.contains(backendMediaValue),
                String.format("should not have CSP directive and value '%s %s' in '%s'", backendMediaDirective, backendMediaValue, cspValues));
            assertTrue(testCtx, cspValues.contains(differentStyleValue),
                String.format("should have CSP directive and value '%s' in '%s'", differentStyleValue, cspValues));
            testCtx.completeNow();
        }));
    }

    @Test
    void chainingTwoCSPMiddlewareAndUsingExternalCSPPoliciesFromBackend(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //given
        final String middlewareStyleDirective = "style-src";
        final String middlewareStyleValue = "self";
        final String differentStyleValue = "https://fonts.googleapis.com";

        final String backendMediaDirective = "media-src";
        final String backendMediaValue = "'self'";

        final int backendPort = TestUtils.findFreePort();
        final Handler<RoutingContext> cspInsertionHandler = ctx -> {
            ctx.response()
                .putHeader(CONTENT_SECURITY_POLICY, backendMediaDirective + " " + backendMediaValue)
                .end();
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(middlewareStyleDirective, middlewareStyleValue)), false, CSPMergeStrategy.EXTERNAL)
            .withCspMiddleware(JsonArray.of(createDirective(middlewareStyleDirective, differentStyleValue)), false, CSPMergeStrategy.EXTERNAL)
            .withProxyMiddleware(backendPort)
            .withBackend(vertx, backendPort, cspInsertionHandler)
            .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final MultiMap headers = httpClientResponse.headers();
            final String cspValues = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(testCtx, cspValues, String.format("should have '%s' header!", CONTENT_SECURITY_POLICY));
            assertFalse(testCtx, cspValues.contains(middlewareStyleDirective) && cspValues.contains(middlewareStyleValue),
                String.format("should not have CSP directive and value '%s %s' in '%s'", middlewareStyleDirective, middlewareStyleValue, cspValues));
            assertTrue(testCtx, cspValues.contains(backendMediaDirective) && cspValues.contains(backendMediaValue),
                String.format("should have CSP directive and value '%s %s' in '%s'", backendMediaDirective, backendMediaValue, cspValues));
            assertFalse(testCtx, cspValues.contains(differentStyleValue),
                String.format("should not have CSP directive and value '%s' in '%s'", differentStyleValue, cspValues));
            testCtx.completeNow();
        }));
    }

    @Test
    void mergingDisjunctiveExternalCSPPoliciesFromBackend(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //given
        final String middlewareStyleDirective = "style-src";
        final String middlewareStyleValue = "self";

        final String backendMediaDirective = "media-src";
        final String backendMediaValue = "'self'";

        final int backendPort = TestUtils.findFreePort();
        final Handler<RoutingContext> cspInsertionHandler = ctx -> {
            ctx.response()
                .putHeader(CONTENT_SECURITY_POLICY, backendMediaDirective + " " + backendMediaValue)
                .end();
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(middlewareStyleDirective, middlewareStyleValue)), false, CSPMergeStrategy.UNION)
            .withProxyMiddleware(backendPort)
            .withBackend(vertx, backendPort, cspInsertionHandler)
            .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final MultiMap headers = httpClientResponse.headers();
            final String cspValues = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(testCtx, cspValues, String.format("should have '%s' header!", CONTENT_SECURITY_POLICY));
            assertTrue(testCtx, cspValues.contains(middlewareStyleDirective) && cspValues.contains(middlewareStyleValue),
                String.format("should have CSP directive and value '%s %s' in '%s'", middlewareStyleDirective, middlewareStyleValue, cspValues));
            assertTrue(testCtx, cspValues.contains(backendMediaDirective) && cspValues.contains(backendMediaValue),
                String.format("should have CSP directive and value '%s %s' in '%s'", backendMediaDirective, backendMediaValue, cspValues));
            testCtx.completeNow();
        }));
    }

    @Test
    void mergingSameDirectiveDifferentValuesExternalCSPPoliciesFromBackend(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //given
        final String middlewareStyleDirective = "style-src";
        final String middlewareStyleValue = "self";

        final String backendMediaDirective = "style-src";
        final String backendMediaValue = "http://test.com";

        final int backendPort = TestUtils.findFreePort();
        final Handler<RoutingContext> cspInsertionHandler = ctx -> {
            ctx.response()
                .putHeader(CONTENT_SECURITY_POLICY, backendMediaDirective + " " + backendMediaValue)
                .end();
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(middlewareStyleDirective, middlewareStyleValue)), false, CSPMergeStrategy.UNION)
            .withProxyMiddleware(backendPort)
            .withBackend(vertx, backendPort, cspInsertionHandler)
            .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final MultiMap headers = httpClientResponse.headers();
            final String cspValues = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(testCtx, cspValues, String.format("should have '%s' header!", CONTENT_SECURITY_POLICY));
            assertTrue(testCtx, cspValues.contains(middlewareStyleDirective) && cspValues.contains(middlewareStyleValue),
                String.format("should have CSP directive and value '%s %s' in '%s'", middlewareStyleDirective, middlewareStyleValue, cspValues));
            assertTrue(testCtx, cspValues.contains(backendMediaDirective) && cspValues.contains(backendMediaValue),
                String.format("should have CSP directive and value '%s %s' in '%s'", backendMediaDirective, backendMediaValue, cspValues));
            testCtx.completeNow();
        }));
    }

    @Test
    void keepExternalPolicies(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String middlewareStyleDirective = "style-src";
        final String middlewareStyleValue = "self";

        final String backendMediaDirective = "style-src";
        final String backendMediaValue = "http://test.com";

        final int backendPort = TestUtils.findFreePort();
        final Handler<RoutingContext> cspInsertionHandler = ctx -> {
            ctx.response()
                .putHeader(CONTENT_SECURITY_POLICY, backendMediaDirective + " " + backendMediaValue)
                .end();
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(middlewareStyleDirective, middlewareStyleValue)), false, CSPMergeStrategy.EXTERNAL)
            .withProxyMiddleware(backendPort)
            .withBackend(vertx, backendPort, cspInsertionHandler)
            .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final MultiMap headers = httpClientResponse.headers();
            final String cspValues = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(testCtx, cspValues, String.format("should have '%s' header!", CONTENT_SECURITY_POLICY));
            assertFalse(testCtx, cspValues.contains(middlewareStyleDirective) && cspValues.contains(middlewareStyleValue),
                String.format("should not have CSP directive and value '%s %s' in '%s'", middlewareStyleDirective, middlewareStyleValue, cspValues));
            assertTrue(testCtx, cspValues.contains(backendMediaDirective) && cspValues.contains(backendMediaValue),
                String.format("should have CSP directive and value '%s %s' in '%s'", backendMediaDirective, backendMediaValue, cspValues));
            testCtx.completeNow();
        }));
    }

    @Test
    void keepInternalPolicies(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //given
        final String middlewareStyleDirective = "style-src";
        final String middlewareStyleValue = "self";

        final String backendMediaDirective = "style-src";
        final String backendMediaValue = "http://test.com";

        final int backendPort = TestUtils.findFreePort();
        final Handler<RoutingContext> cspInsertionHandler = ctx -> {
            ctx.response().putHeader(CONTENT_SECURITY_POLICY, backendMediaDirective + " " + backendMediaValue);
            ctx.response().end();
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(middlewareStyleDirective, middlewareStyleValue)), false, CSPMergeStrategy.EXTERNAL)
            .withProxyMiddleware(backendPort)
            .withBackend(vertx, backendPort, cspInsertionHandler)
            .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final MultiMap headers = httpClientResponse.headers();
            final String cspValues = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(testCtx, cspValues, String.format("should have '%s' header!", CONTENT_SECURITY_POLICY));
            assertFalse(testCtx, cspValues.contains(middlewareStyleDirective) && cspValues.contains(middlewareStyleValue),
                String.format("should not have CSP directive and value '%s %s' in '%s'", middlewareStyleDirective, middlewareStyleValue, cspValues));
            assertTrue(testCtx, cspValues.contains(backendMediaDirective) && cspValues.contains(backendMediaValue),
                String.format("should have CSP directive and value '%s %s' in '%s'", backendMediaDirective, backendMediaValue, cspValues));
            testCtx.completeNow();
        }));
    }

    private JsonObject createDirective(String directive, String... values) {
        return JsonObject.of(DynamicConfiguration.MIDDLEWARE_CSP_DIRECTIVE_NAME, directive,
            DynamicConfiguration.MIDDLEWARE_CSP_DIRECTIVE_VALUES, JsonArray.of((Object[]) values));
    }
}
