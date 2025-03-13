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
                withMiddleware("foo", ReplacePathRegexMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        ReplacePathRegexMiddlewareFactory.REGEX, "^$",
                        ReplacePathRegexMiddlewareFactory.REPLACEMENT, "foobar")))));

        final JsonObject missingOptions = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ReplacePathRegexMiddlewareFactory.TYPE)));

        final JsonObject unknownProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ReplacePathRegexMiddlewareFactory.TYPE,
                    withMiddlewareOpts(
                        JsonObject.of("bar", "blub")))));

        final JsonObject missingRequiredProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ReplacePathRegexMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        ReplacePathRegexMiddlewareFactory.REGEX, "^$")))));

        return Stream.of(
            Arguments.of("accept simple config", simple, complete, expectedTrue),
            Arguments.of("reject config with no options", missingOptions, complete, expectedFalse),
            Arguments.of("reject config with unknown property", unknownProperty, complete, expectedFalse),
            Arguments.of("reject config with missing required property", missingRequiredProperty, complete, expectedFalse)

        );
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
