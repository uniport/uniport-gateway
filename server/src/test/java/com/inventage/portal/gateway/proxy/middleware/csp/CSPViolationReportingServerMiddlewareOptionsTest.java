package com.inventage.portal.gateway.proxy.middleware.csp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class CSPViolationReportingServerMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final String logLevel = "aLogLevel";
        final JsonObject json = JsonObject.of(
            CSPViolationReportingServerMiddlewareFactory.CSP_VIOLATION_REPORTING_SERVER_LOG_LEVEL, logLevel);

        // when
        final ThrowingSupplier<CSPViolationReportingServerMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), CSPViolationReportingServerMiddlewareOptions.class);

        // then
        final CSPViolationReportingServerMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
        assertEquals(logLevel, options.getLogLevel());
    }
}
