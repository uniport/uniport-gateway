package ch.uniport.gateway.proxy.middleware;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Random;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Learning Test for composing CompletionStages.
 */
@ExtendWith(VertxExtension.class)
public class CompletionStageTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompletionStageTest.class);

    //@Test
    void thenCompose(Vertx vertx, VertxTestContext testContext) {
        asyncToUpperCase("hello", vertx)
            .thenCompose(s -> asyncToLowerCase(s, vertx))
            .thenCompose(s -> asyncToUpperCase(s, vertx))
            .whenComplete((result, error) -> {
                if (error == null) {
                    VertxAssertions.assertEquals(testContext, "HELLO", result);
                } else {
                    VertxAssertions.fail(testContext, error);
                }
                testContext.completeNow();
            });
    }

    private CompletionStage<String> asyncToUpperCase(String input, Vertx vertx) {
        final Promise<String> result = Promise.promise();
        final int i = new Random().nextInt(10 - 1 + 1);
        LOGGER.info("waiting for = '{}' seconds", i);
        vertx.setTimer(i * 1000, timeout -> result.complete(input.toUpperCase()));
        return result.future().toCompletionStage();
    }

    private CompletionStage<String> asyncToLowerCase(String input, Vertx vertx) {
        final Promise<String> result = Promise.promise();
        final int i = new Random().nextInt(10 - 1 + 1);
        LOGGER.info("waiting for = '{}' seconds", i);
        vertx.setTimer(i * 1000, timeout -> result.complete(input.toLowerCase()));
        return result.future().toCompletionStage();
    }

}
