package ch.uniport.gateway.proxy.middleware;

import ch.uniport.gateway.proxy.config.DynamicConfiguration;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import java.util.stream.Stream;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(Lifecycle.PER_CLASS)
public abstract class MiddlewareTestBase {

    protected abstract Stream<Arguments> provideConfigValidationTestData();

    // the sole purpose of these following variable are to improve readability
    protected final boolean expectedTrue = true;
    protected final boolean expectedFalse = false;

    // the default used in the following should be "complete" since it is more restrictive
    protected final boolean complete = true;
    protected final boolean incomplete = false;

    @ParameterizedTest
    @MethodSource("provideConfigValidationTestData")
    void validateTest(
        String name, JsonObject json, Boolean complete, Boolean expected,
        Vertx vertx, VertxTestContext testCtx
    ) {
        DynamicConfiguration.validate(vertx, json, complete)
            .onComplete(ar -> {
                if (ar.succeeded() && expected || ar.failed() && !expected) {
                    testCtx.completeNow();
                } else {
                    testCtx.failNow(String.format(
                        "validate configuration: '%s' was expected to have '%s':\nError: '%s'\nInput: '%s'", name,
                        expected ? "succeeded" : "failed",
                        ar.cause(),
                        json != null ? json.encodePrettily() : null));
                }
            });
    }
}
