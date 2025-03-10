package com.inventage.portal.gateway.proxy.middleware.csp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP.CSPMergeStrategy;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class CSPMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final String directiveName = "aName";
        final String directiveValue = "aValue";
        final Boolean reportOnly = true;
        final CSPMergeStrategy mergeStrategy = CSPMergeStrategy.INTERNAL;

        final JsonObject json = JsonObject.of(
            CSPMiddlewareFactory.CSP_DIRECTIVES, List.of(
                Map.of(
                    CSPMiddlewareFactory.CSP_DIRECTIVE_NAME, directiveName,
                    CSPMiddlewareFactory.CSP_DIRECTIVE_VALUES, List.of(directiveValue))),
            CSPMiddlewareFactory.CSP_REPORT_ONLY, reportOnly,
            CSPMiddlewareFactory.CSP_MERGE_STRATEGY, mergeStrategy);

        // when
        final ThrowingSupplier<CSPMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), CSPMiddlewareOptions.class);

        // then
        final CSPMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);

        assertNotNull(options.getDirectives());
        assertEquals(directiveName, options.getDirectives().get(0).getName());

        assertNotNull(options.getDirectives().get(0).getValues());
        assertEquals(directiveValue, options.getDirectives().get(0).getValues().get(0));

        assertEquals(reportOnly, options.isReportOnly());
        assertEquals(mergeStrategy, options.getMergeStrategy());
    }
}
