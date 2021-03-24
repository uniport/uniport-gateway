package com.inventage.portal.gateway.proxy.middleware.proxy.request.uri.replacePathRegex;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ReplacePathRegexMiddlewareTest {

    @Test
    public void test_starting_with() {
        // given
        final ReplacePathRegexMiddleware r = new ReplacePathRegexMiddleware("^/organisation/", "/");
        // when
        final String uri = r.apply("/organisation/v1/graphql");
        // then
        Assertions.assertEquals("/v1/graphql", uri);
    }

    @Test
    public void test_exact() {
        // given
        final ReplacePathRegexMiddleware r =
                new ReplacePathRegexMiddleware("^/status*.*", "/status/200");
        // when
        final String uri = r.apply("/status/404");
        // then
        Assertions.assertEquals("/status/200", uri);
    }

    @Test
    public void test_exact2() {
        // given
        final ReplacePathRegexMiddleware r = new ReplacePathRegexMiddleware("^/organisation/", "/");
        // when
        final String uri = r.apply("/organisation/headers");
        // then
        Assertions.assertEquals("/headers", uri);
    }
}
