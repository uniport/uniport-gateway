package com.inventage.portal.gateway.proxy.middleware;

import io.vertx.core.buffer.Buffer;
import org.assertj.core.api.AbstractAssert;
import org.junit.jupiter.api.Assertions;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class BufferAssert extends AbstractAssert<BufferAssert, Buffer> {

    final Map<String, String> body;

    protected BufferAssert(Buffer actual) {
        super(actual, Buffer.class);
        body = extractParametersFromBody(actual.toString());
    }

    public static BufferAssert assertThat(Buffer actual) {
        return new BufferAssert(actual);
    }

    public BufferAssert hasKeyValue(String key, String expectedValue) {
        Assertions.assertEquals(expectedValue, body.get(key));
        return this;
    }

    private Map<String, String> extractParametersFromBody(String body) {
        return Arrays.stream(body.split("&"))
                .collect(Collectors.toMap(entry -> entry.split("=")[0], entry -> entry.split("=")[1]));
    }
}
