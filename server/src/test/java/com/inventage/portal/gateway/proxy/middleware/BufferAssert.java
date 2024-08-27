package com.inventage.portal.gateway.proxy.middleware;

import io.vertx.core.buffer.Buffer;
import io.vertx.junit5.VertxTestContext;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.assertj.core.api.AbstractAssert;

public class BufferAssert extends AbstractAssert<BufferAssert, Buffer> {

    final VertxTestContext testContext;
    final Map<String, String> body;

    protected BufferAssert(VertxTestContext testContext, Buffer actual) {
        super(actual, BufferAssert.class);
        this.testContext = testContext;
        body = extractParametersFromBody(actual.toString());
    }

    public static BufferAssert assertThat(VertxTestContext testContext, Buffer actual) {
        return new BufferAssert(testContext, actual);
    }

    public BufferAssert hasKeyValue(String key, String expectedValue) {
        VertxAssertions.assertEquals(testContext, expectedValue, body.get(key));
        return this;
    }

    private Map<String, String> extractParametersFromBody(String body) {
        return Arrays.stream(body.split("&"))
            .collect(Collectors.toMap(entry -> entry.split("=")[0], entry -> entry.split("=")[1]));
    }
}
