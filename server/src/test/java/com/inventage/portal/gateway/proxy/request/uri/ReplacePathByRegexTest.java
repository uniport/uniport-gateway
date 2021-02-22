package com.inventage.portal.gateway.proxy.request.uri;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ReplacePathByRegexTest {

    @Test
    public void test_starting_with() {
        // given
        final ReplacePathByRegex r = new ReplacePathByRegex("^/organisation/", "/");
        // when
        final String uri = r.apply("/organisation/v1/graphql");
        // then
        Assertions.assertEquals("/v1/graphql", uri);
    }

    @Test
    public void test_exact() {
        // given
        final ReplacePathByRegex r = new ReplacePathByRegex("^/status*.*", "/status/200");
        // when
        final String uri = r.apply("/status/404");
        // then
        Assertions.assertEquals("/status/200", uri);
    }

    @Test
    public void test_exact2() {
        // given
        final ReplacePathByRegex r = new ReplacePathByRegex("^/organisation/", "/");
        // when
        final String uri = r.apply("/organisation/headers");
        // then
        Assertions.assertEquals("/headers", uri);
    }
}
