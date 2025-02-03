package com.inventage.portal.gateway.proxy.middleware.openTelemetry;

import static com.inventage.portal.gateway.TestUtils.buildConfiguration;
import static com.inventage.portal.gateway.TestUtils.withMiddleware;
import static com.inventage.portal.gateway.TestUtils.withMiddlewares;

import com.inventage.portal.gateway.proxy.middleware.MiddlewareTestBase;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(VertxExtension.class)
public class OpenTelemetryMiddlewareTest extends MiddlewareTestBase {

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {
        final JsonObject simple = buildConfiguration(
            withMiddlewares(
                withMiddleware("openTelemetry", OpenTelemetryMiddlewareFactory.OPEN_TELEMETRY)));

        return Stream.of(
            Arguments.of("accept openTelemetry middleware", simple, complete, expectedTrue)

        );
    }
}
