package ch.uniport.gateway.proxy.middleware;

import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;

public final class VertxAssertions {

    private VertxAssertions() {
    }

    public static void assertTrue(VertxTestContext testCtx, boolean condition) {
        testCtx.verify(() -> {
            Assertions.assertTrue(condition);
        });
    }

    public static void assertTrue(VertxTestContext testCtx, boolean condition, String message) {
        testCtx.verify(() -> {
            Assertions.assertTrue(condition, message);
        });
    }

    public static void assertFalse(VertxTestContext testCtx, boolean condition) {
        testCtx.verify(() -> {
            Assertions.assertFalse(condition);
        });
    }

    public static void assertFalse(VertxTestContext testCtx, boolean condition, String message) {
        testCtx.verify(() -> {
            Assertions.assertFalse(condition, message);
        });
    }

    public static void assertEquals(VertxTestContext testCtx, Object expected, Object actual) {
        testCtx.verify(() -> {
            Assertions.assertEquals(expected, actual);
        });
    }

    public static void assertEquals(VertxTestContext testCtx, Object expected, Object actual, String message) {
        testCtx.verify(() -> {
            Assertions.assertEquals(expected, actual, message);
        });
    }

    public static void assertNotEquals(VertxTestContext testCtx, Object expected, Object actual) {
        testCtx.verify(() -> {
            Assertions.assertNotEquals(expected, actual);
        });
    }

    public static void assertNull(VertxTestContext testCtx, Object actual) {
        testCtx.verify(() -> {
            Assertions.assertNull(actual);
        });
    }

    public static void assertNull(VertxTestContext testCtx, Object actual, String message) {
        testCtx.verify(() -> {
            Assertions.assertNull(actual, message);
        });
    }

    public static void assertNotNull(VertxTestContext testCtx, Object actual) {
        testCtx.verify(() -> {
            Assertions.assertNotNull(actual);
        });
    }

    public static void assertNotNull(VertxTestContext testCtx, Object actual, String message) {
        testCtx.verify(() -> {
            Assertions.assertNotNull(actual, message);
        });
    }

    public static void fail(VertxTestContext testCtx, Throwable cause) {
        testCtx.verify(() -> {
            Assertions.fail(cause);
        });
    }

    public static void assertDoesNotThrow(VertxTestContext testCtx, Executable executable, String message) {
        testCtx.verify(() -> {
            Assertions.assertDoesNotThrow(executable, message);
        });
    }

    public static <T extends Throwable> void assertThrows(VertxTestContext testCtx, Class<T> expectedType, Executable executable, String message) {
        testCtx.verify(() -> {
            Assertions.assertThrows(expectedType, executable, message);
        });
    }
}
