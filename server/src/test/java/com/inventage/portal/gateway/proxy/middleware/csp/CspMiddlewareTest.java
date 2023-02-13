package com.inventage.portal.gateway.proxy.middleware.csp;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
class CspMiddlewareTest {

    private static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    private static final String CONTENT_SECURITY_POLICY_REPORT_ONLY = "Content-Security-Policy-Report-Only";

    @Test
    void checkForCspInHTTPHeader(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String default_src = "default-src";
        final String default_src_value = "self";

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
                .withCspMiddleware(JsonArray.of(createDirective(default_src, default_src_value)), false)
                .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final MultiMap headers = httpClientResponse.headers();
            final String csp_values = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(csp_values,
                    String.format("'%s' is NOT contained in http response header!", CONTENT_SECURITY_POLICY));
            assertTrue(csp_values.contains(default_src) && csp_values.contains(default_src_value),
                    " Csp directive and value NOT contained in http response header!");
            testCtx.completeNow();
        }));
    }

    @Test
    void checkReportOnlyInHTTPHeader(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String default_src = "default-src";
        final String default_src_value = "self";

        final String report_to = "report-to";
        final String report_to_value = "www.example.com";

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
                .withCspMiddleware(JsonArray.of(createDirective(default_src, default_src_value),
                        createDirective(report_to, report_to_value)), true)
                .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final MultiMap headers = httpClientResponse.headers();
            final String csp_values_report_only = headers.get(CONTENT_SECURITY_POLICY_REPORT_ONLY);
            final String csp_values = headers.get(CONTENT_SECURITY_POLICY);

            assertNotNull(csp_values_report_only,
                    String.format("'%s' is NOT contained in http response header!", CONTENT_SECURITY_POLICY));
            assertTrue(
                    csp_values_report_only.contains(default_src) && csp_values_report_only.contains(default_src_value)
                            && csp_values_report_only.contains(report_to_value),
                    String.format("Csp report only directives should be contained in the header entry: %s!",
                            CONTENT_SECURITY_POLICY_REPORT_ONLY));
            assertNull(csp_values, " CSP header should not be set");

            testCtx.completeNow();
        }));
    }

    private JsonObject createDirective(String directive, String... values) {
        return JsonObject.of(DynamicConfiguration.MIDDLEWARE_CSP_DIRECTIVE_NAME, directive,
                DynamicConfiguration.MIDDLEWARE_CSP_DIRECTIVE_VALUES, JsonArray.of(values));
    }
}
