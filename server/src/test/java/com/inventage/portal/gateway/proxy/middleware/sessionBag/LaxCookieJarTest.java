package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LaxCookieJarTest {

    @Test
    public void testCookieValueWithSpaces() {
        // given
        final String cookieName = "TEST_COOKIE";
        final String cookieValue = "this is a test";
        final String cookieHeader = String.format("%s=%s", cookieName, cookieValue);
        // when
        final LaxCookieJar cookies = new LaxCookieJar(cookieHeader);
        // then
        Assertions.assertEquals(cookieValue, cookies.get(cookieName).getValue());
    }

    @Test
    public void testMultipleCookieValueWithSpaces() {
        // given
        final String cookie1Name = "TEST_COOKIE";
        final String cookie1Value = "this is a test";

        final String cookie2Name = "ANOTHER_TEST_COOKIE";
        final String cookie2Value = "today is a good day";

        final String cookie1Header = String.format("%s=%s", cookie1Name, cookie1Value);
        final String cookie2Header = String.format("%s=%s", cookie2Name, cookie2Value);

        final String cookieHeader = String.join("; ", List.of(cookie1Header, cookie2Header));
        // when
        final LaxCookieJar cookies = new LaxCookieJar(cookieHeader);
        // then
        Assertions.assertEquals(cookie1Value, cookies.get(cookie1Name).getValue());
        Assertions.assertEquals(cookie2Value, cookies.get(cookie2Name).getValue());
    }
}
