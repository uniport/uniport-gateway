package com.inventage.portal.gateway.proxy.middleware.csp;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP.CSPMergeStrategy;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class CSPMiddlewareTest {

    private static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    private static final String CONTENT_SECURITY_POLICY_REPORT_ONLY = "Content-Security-Policy-Report-Only";

    @Test
    void checkForCspInHTTPHeader(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String defaultSrc = "default-src";
        final String defaultSrcValue = "self";

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(defaultSrc, defaultSrcValue)), false)
            .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final MultiMap headers = httpClientResponse.headers();
            final String cspValues = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(cspValues,
                String.format("'%s' is NOT contained in http response header!",
                    CONTENT_SECURITY_POLICY));
            assertTrue(cspValues.contains(defaultSrc) && cspValues.contains(defaultSrcValue),
                " Csp directive and value NOT contained in http response header!");
            testCtx.completeNow();
        }));
    }

    @Test
    void checkReportOnlyInHTTPHeader(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String defaultSrc = "default-src";
        final String defaultSrcValue = "self";

        final String reportTo = "report-to";
        final String reportToValue = "www.example.com";

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(defaultSrc, defaultSrcValue),
                createDirective(reportTo, reportToValue)), true)
            .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final MultiMap headers = httpClientResponse.headers();
            final String cspValuesReportOnly = headers.get(CONTENT_SECURITY_POLICY_REPORT_ONLY);
            final String cpsValues = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(cspValuesReportOnly,
                String.format("'%s' is NOT contained in http response header!",
                    CONTENT_SECURITY_POLICY));
            assertTrue(
                cspValuesReportOnly.contains(defaultSrc)
                    && cspValuesReportOnly.contains(defaultSrcValue)
                    && cspValuesReportOnly.contains(reportToValue),
                String.format("Csp report only directives should be contained in the header entry: %s!",
                    CONTENT_SECURITY_POLICY_REPORT_ONLY));
            assertNull(cpsValues, " CSP header should not be set");

            testCtx.completeNow();
        }));
    }

    @Test
    void chainingTwoCSPMiddlewaresDifferentDirectives(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String styleDirective = "style-src";
        final String styleValue = "self";

        final String mediaDirective = "media-src";
        final String mediaValue = "self";

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(mediaDirective, mediaValue)), false)
            .withCspMiddleware(JsonArray.of(createDirective(styleDirective, styleValue)), false)
            .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final MultiMap headers = httpClientResponse.headers();
            final String cspValues = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(cspValues,
                String.format("'%s' is NOT contained in http response header!",
                    CONTENT_SECURITY_POLICY));
            assertTrue(cspValues.contains(styleDirective) && cspValues.contains(styleValue),
                " Csp directive and value NOT contained in http response header!");
            assertTrue(cspValues.contains(mediaDirective) && cspValues.contains(mediaValue),
                " Csp directive and value NOT contained in http response header!");
            testCtx.completeNow();
        }));
    }

    @Test
    void chainingTwoCSPMiddlewaresWithSameDirective(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String styleDirective = "style-src";
        final String styleValue = "self";
        final String differentStyleValue = "https://fonts.googleapis.com https://fonts.gstatic.com https://cdn.jsdelivr.net";

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(styleDirective, differentStyleValue)), false)
            .withCspMiddleware(JsonArray.of(createDirective(styleDirective, styleValue)), false)
            .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final MultiMap headers = httpClientResponse.headers();
            final String cspValues = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(cspValues,
                String.format("'%s' is NOT contained in http response header!",
                    CONTENT_SECURITY_POLICY));
            assertTrue(cspValues.contains(styleDirective) && cspValues.contains(styleValue),
                " Csp directive and value NOT contained in http response header!");

            for (String expectedValue : differentStyleValue.split(" ")) {
                assertTrue(cspValues.contains(expectedValue),
                    " Expected " + expectedValue + " in csp header");
            }

            testCtx.completeNow();
        }));
    }

    @Test
    void emptyExternalCSPPoliciesFromBackend(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String middleware_style_directive = "style-src";
        final String middleware_style_value = "self";

        final int backendPort = TestUtils.findFreePort();
        final String backendHost = "localhost";

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(middleware_style_directive, middleware_style_value)), false, CSPMergeStrategy.UNION.toString())
            .withProxyMiddleware(backendHost, backendPort)
            .build().start();

        vertx.createHttpServer().requestHandler(handler -> {
            handler.response().putHeader(CONTENT_SECURITY_POLICY, "").end();
        }).listen(backendPort, backendHost);

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final MultiMap headers = httpClientResponse.headers();
            final String csp_values = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(csp_values,
                String.format("'%s' is NOT contained in http response header!",
                    CONTENT_SECURITY_POLICY));
            assertTrue(csp_values.contains(middleware_style_directive) && csp_values.contains(middleware_style_value),
                " Csp directive and value NOT contained in http response header!");
            testCtx.completeNow();
        }));
    }

    @Test
    void chainingTwoCSPMiddlewareAndMergingDisjunctiveExternalCSPPoliciesFromBackend(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String middleware_style_directive = "style-src";
        final String middleware_style_value = "self";
        final String differentStyleValue = "https://fonts.googleapis.com";

        final String backend_media_directive = "media-src";
        final String backend_media_value = "'self'";

        final int backendPort = TestUtils.findFreePort();
        final String backendHost = "localhost";

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(middleware_style_directive, middleware_style_value)), false, CSPMergeStrategy.UNION.toString())
            .withCspMiddleware(JsonArray.of(createDirective(middleware_style_directive, differentStyleValue)), false, CSPMergeStrategy.UNION.toString())
            .withProxyMiddleware(backendHost, backendPort)
            .build().start();

        vertx.createHttpServer().requestHandler(handler -> {
            handler.response().putHeader(CONTENT_SECURITY_POLICY, backend_media_directive + " " + backend_media_value).end();
        }).listen(backendPort, backendHost);

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final MultiMap headers = httpClientResponse.headers();
            final String csp_values = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(csp_values,
                String.format("'%s' is NOT contained in http response header!",
                    CONTENT_SECURITY_POLICY));
            assertTrue(csp_values.contains(middleware_style_directive) && csp_values.contains(middleware_style_value),
                " Csp directive and value NOT contained in http response header!");
            assertTrue(csp_values.contains(backend_media_directive) && csp_values.contains(backend_media_value),
                " Csp directive and value from backend NOT contained in http response header!");
            assertTrue(csp_values.contains(differentStyleValue),
                " Csp directive and value from second CSP Middleware is NOT contained in http response header!");
            testCtx.completeNow();
        }));
    }

    @Test
    void chainingTwoCSPMiddlewareAndIgnoringExternalCSPPoliciesFromBackend(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String middleware_style_directive = "style-src";
        final String middleware_style_value = "self";
        final String differentStyleValue = "https://fonts.googleapis.com";

        final String backend_media_directive = "media-src";
        final String backend_media_value = "'self'";

        final int backendPort = TestUtils.findFreePort();
        final String backendHost = "localhost";

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(middleware_style_directive, middleware_style_value)), false, CSPMergeStrategy.INTERNAL.toString())
            .withCspMiddleware(JsonArray.of(createDirective(middleware_style_directive, differentStyleValue)), false)
            .withProxyMiddleware(backendHost, backendPort)
            .build().start();

        vertx.createHttpServer().requestHandler(handler -> {
            handler.response().putHeader(CONTENT_SECURITY_POLICY, backend_media_directive + " " + backend_media_value).end();
        }).listen(backendPort, backendHost);

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final MultiMap headers = httpClientResponse.headers();
            final String csp_values = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(csp_values,
                String.format("'%s' is NOT contained in http response header!",
                    CONTENT_SECURITY_POLICY));
            assertTrue(csp_values.contains(middleware_style_directive) && csp_values.contains(middleware_style_value),
                " Csp directive and value NOT contained in http response header!");
            assertFalse(csp_values.contains(backend_media_directive) && csp_values.contains(backend_media_value),
                " Csp directive and value from backend ARE contained in http response header!");
            assertTrue(csp_values.contains(differentStyleValue),
                " Csp directive and value from second CSP Middleware is NOT contained in http response header!");
            testCtx.completeNow();
        }));
    }

    @Test
    void chainingTwoCSPMiddlewareAndUsingExternalCSPPoliciesFromBackend(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String middleware_style_directive = "style-src";
        final String middleware_style_value = "self";
        final String differentStyleValue = "https://fonts.googleapis.com";

        final String backend_media_directive = "media-src";
        final String backend_media_value = "'self'";

        final int backendPort = TestUtils.findFreePort();
        final String backendHost = "localhost";

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(middleware_style_directive, middleware_style_value)), false, CSPMergeStrategy.EXTERNAL.toString())
            .withCspMiddleware(JsonArray.of(createDirective(middleware_style_directive, differentStyleValue)), false, CSPMergeStrategy.EXTERNAL.toString())
            .withProxyMiddleware(backendHost, backendPort)
            .build().start();

        vertx.createHttpServer().requestHandler(handler -> {
            handler.response().putHeader(CONTENT_SECURITY_POLICY, backend_media_directive + " " + backend_media_value).end();
        }).listen(backendPort, backendHost);

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final MultiMap headers = httpClientResponse.headers();
            final String csp_values = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(csp_values,
                String.format("'%s' is NOT contained in http response header!",
                    CONTENT_SECURITY_POLICY));
            assertFalse(csp_values.contains(middleware_style_directive) && csp_values.contains(middleware_style_value),
                " Csp directive and value NOT contained in http response header!");
            assertTrue(csp_values.contains(backend_media_directive) && csp_values.contains(backend_media_value),
                " Csp directive and value from backend ARE contained in http response header!");
            assertFalse(csp_values.contains(differentStyleValue),
                " Csp directive and value from second CSP Middleware is NOT contained in http response header!");
            testCtx.completeNow();
        }));
    }

    @Test
    void mergingDisjunctiveExternalCSPPoliciesFromBackend(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String middleware_style_directive = "style-src";
        final String middleware_style_value = "self";

        final String backend_media_directive = "media-src";
        final String backend_media_value = "'self'";

        final int backendPort = TestUtils.findFreePort();
        final String backendHost = "localhost";

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(middleware_style_directive, middleware_style_value)), false, CSPMergeStrategy.UNION.toString())
            .withProxyMiddleware(backendHost, backendPort)
            .build().start();

        vertx.createHttpServer().requestHandler(handler -> {
            handler.response().putHeader(CONTENT_SECURITY_POLICY, backend_media_directive + " " + backend_media_value).end();
        }).listen(backendPort, backendHost);

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final MultiMap headers = httpClientResponse.headers();
            final String csp_values = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(csp_values,
                String.format("'%s' is NOT contained in http response header!",
                    CONTENT_SECURITY_POLICY));
            assertTrue(csp_values.contains(middleware_style_directive) && csp_values.contains(middleware_style_value),
                " Csp directive and value NOT contained in http response header!");
            assertTrue(csp_values.contains(backend_media_directive) && csp_values.contains(backend_media_value),
                " Csp directive and value from backend NOT contained in http response header!");
            testCtx.completeNow();
        }));
    }

    @Test
    void mergingSameDirectiveDifferentValuesExternalCSPPoliciesFromBackend(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String middleware_style_directive = "style-src";
        final String middleware_style_value = "self";

        final String backend_media_directive = "style-src";
        final String backend_media_value = "http://test.com";

        final int backendPort = TestUtils.findFreePort();
        final String backendHost = "localhost";

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(middleware_style_directive, middleware_style_value)), false, CSPMergeStrategy.UNION.toString())
            .withProxyMiddleware(backendHost, backendPort)
            .build().start();

        vertx.createHttpServer().requestHandler(handler -> {
            handler.response().putHeader(CONTENT_SECURITY_POLICY, backend_media_directive + " " + backend_media_value).end();
        }).listen(backendPort, backendHost);

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final MultiMap headers = httpClientResponse.headers();
            final String csp_values = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(csp_values,
                String.format("'%s' is NOT contained in http response header!",
                    CONTENT_SECURITY_POLICY));
            assertTrue(csp_values.contains(middleware_style_directive) && csp_values.contains(middleware_style_value),
                " Csp directive and value NOT contained in http response header!");
            assertTrue(csp_values.contains(backend_media_directive) && csp_values.contains(backend_media_value),
                " Csp directive and value from backend NOT contained in http response header!");
            testCtx.completeNow();
        }));
    }

    @Test
    void keepExternalPolicies(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String middleware_style_directive = "style-src";
        final String middleware_style_value = "self";

        final String backend_media_directive = "style-src";
        final String backend_media_value = "http://test.com";

        final int backendPort = TestUtils.findFreePort();
        final String backendHost = "localhost";

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(middleware_style_directive, middleware_style_value)), false, CSPMergeStrategy.EXTERNAL.toString())
            .withProxyMiddleware(backendHost, backendPort)
            .build().start();

        vertx.createHttpServer().requestHandler(handler -> {
            handler.response().putHeader(CONTENT_SECURITY_POLICY, backend_media_directive + " " + backend_media_value).end();
        }).listen(backendPort, backendHost);

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final MultiMap headers = httpClientResponse.headers();
            final String csp_values = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(csp_values,
                String.format("'%s' is NOT contained in http response header!",
                    CONTENT_SECURITY_POLICY));
            assertFalse(csp_values.contains(middleware_style_directive) && csp_values.contains(middleware_style_value),
                " Csp directive and value NOT contained in http response header!");
            assertTrue(csp_values.contains(backend_media_directive) && csp_values.contains(backend_media_value),
                " Csp directive and value from backend NOT contained in http response header!");
            testCtx.completeNow();
        }));
    }

    @Test
    void keepInternalPolicies(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String middleware_style_directive = "style-src";
        final String middleware_style_value = "self";

        final String backend_media_directive = "style-src";
        final String backend_media_value = "http://test.com";

        final int backendPort = TestUtils.findFreePort();
        final String backendHost = "localhost";

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withCspMiddleware(JsonArray.of(createDirective(middleware_style_directive, middleware_style_value)), false, CSPMergeStrategy.EXTERNAL.toString())
            .withProxyMiddleware(backendHost, backendPort)
            .build().start();

        vertx.createHttpServer().requestHandler(handler -> {
            handler.response().putHeader(CONTENT_SECURITY_POLICY, backend_media_directive + " " + backend_media_value).end();
        }).listen(backendPort, backendHost);

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final MultiMap headers = httpClientResponse.headers();
            final String csp_values = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(csp_values,
                String.format("'%s' is NOT contained in http response header!",
                    CONTENT_SECURITY_POLICY));
            assertFalse(csp_values.contains(middleware_style_directive) && csp_values.contains(middleware_style_value),
                " Csp directive and value NOT contained in http response header!");
            assertTrue(csp_values.contains(backend_media_directive) && csp_values.contains(backend_media_value),
                " Csp directive and value from backend NOT contained in http response header!");
            testCtx.completeNow();
        }));
    }

    private JsonObject createDirective(String directive, String... values) {
        return JsonObject.of(DynamicConfiguration.MIDDLEWARE_CSP_DIRECTIVE_NAME, directive,
            DynamicConfiguration.MIDDLEWARE_CSP_DIRECTIVE_VALUES, JsonArray.of((Object[]) values));
    }
}
