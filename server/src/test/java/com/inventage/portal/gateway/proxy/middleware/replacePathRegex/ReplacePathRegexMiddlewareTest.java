package com.inventage.portal.gateway.proxy.middleware.replacePathRegex;

import static com.inventage.portal.gateway.TestUtils.buildConfiguration;
import static com.inventage.portal.gateway.TestUtils.withMiddleware;
import static com.inventage.portal.gateway.TestUtils.withMiddlewareOpts;
import static com.inventage.portal.gateway.TestUtils.withMiddlewares;

import com.inventage.portal.gateway.proxy.middleware.MiddlewareTestBase;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(VertxExtension.class)
public class ReplacePathRegexMiddlewareTest extends MiddlewareTestBase {

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {
        final JsonObject simple = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ReplacePathRegexMiddlewareFactory.REPLACE_PATH_REGEX,
                    withMiddlewareOpts(JsonObject.of(
                        ReplacePathRegexMiddlewareFactory.REPLACE_PATH_REGEX_REGEX, "^$",
                        ReplacePathRegexMiddlewareFactory.REPLACE_PATH_REGEX_REPLACEMENT, "foobar")))));

        final JsonObject missingOptions = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ReplacePathRegexMiddlewareFactory.REPLACE_PATH_REGEX)));

        return Stream.of(
            Arguments.of("accept replace path middleware", simple, complete, expectedTrue),
            Arguments.of("reject replace path middleware with missing options", missingOptions, complete, expectedFalse));
    }

    @Test
    public void test_starting_with() {
        // given
        final ReplacePathRegexMiddleware r = new ReplacePathRegexMiddleware("replacePath", "^/organisation/(.*)",
            "/$1");
        // when
        final String uri = r.apply("/organisation/v1/graphql");
        // then
        Assertions.assertEquals("/v1/graphql", uri);
    }

    @Test
    public void test_exact() {
        // given
        final ReplacePathRegexMiddleware r = new ReplacePathRegexMiddleware("replacePath", "^/status*.*",
            "/status/200");
        // when
        final String uri = r.apply("/status/404");
        // then
        Assertions.assertEquals("/status/200", uri);
    }

    @Test
    public void test_exact2() {
        // given
        final ReplacePathRegexMiddleware r = new ReplacePathRegexMiddleware("replacePath", "^/organisation/(.*)",
            "/$1");
        // when
        final String uri = r.apply("/organisation/headers");
        // then
        Assertions.assertEquals("/headers", uri);
    }
}
