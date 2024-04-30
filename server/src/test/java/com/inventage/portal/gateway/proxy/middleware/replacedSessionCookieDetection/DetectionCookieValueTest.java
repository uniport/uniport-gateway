package com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection;

import static com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.DetectionCookieValue.SPLITTER;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DetectionCookieValueTest {

    @Test
    public void test_isWithInLimit_default() {
        // given
        final int lifetime = 1 * 60 * 1000; // 1m
        final long now = System.currentTimeMillis();
        final DetectionCookieValue cookieValue = new DetectionCookieValue(now, lifetime);
        // when
        final int maxRetries = 5;
        final boolean withInLimit = cookieValue.isWithInLimit(maxRetries);
        // then
        Assertions.assertTrue(withInLimit);
    }

    @Test
    public void test_isWithInLimit_new() {
        // given
        final long validTimestamp = System.currentTimeMillis() + 1 * 60 * 1000; // now+1m
        final DetectionCookieValue cookieValue = new DetectionCookieValue(0 + SPLITTER + validTimestamp);
        // when
        final int maxRetries = 5;
        final boolean withInLimit = cookieValue.isWithInLimit(maxRetries);
        // then
        Assertions.assertTrue(withInLimit);
    }

    @Test
    public void test_isWithInLimit_max() {
        // given
        final int maxRetries = 5;
        final long validTimestamp = System.currentTimeMillis() + 1 * 60 * 1000; // now+1m
        final DetectionCookieValue cookieValue = new DetectionCookieValue((maxRetries - 1) + SPLITTER + validTimestamp);
        // when
        final boolean withInLimit = cookieValue.isWithInLimit(maxRetries);
        // then
        Assertions.assertTrue(withInLimit);
    }

    @Test
    public void test_isWithInLimit_false_counter() {
        // given
        final int maxRetries = 5;
        final long validTimestamp = System.currentTimeMillis() + 1 * 60 * 1000; // now+1m
        final DetectionCookieValue cookieValue = new DetectionCookieValue(maxRetries + SPLITTER + validTimestamp);
        // when
        final boolean withInLimit = cookieValue.isWithInLimit(maxRetries);
        // then
        Assertions.assertFalse(withInLimit);
    }

    @Test
    public void test_isWithInLimit_false_access() {
        // given
        final int maxRetries = 5;
        final long expiredTimestamp = 1000;
        final DetectionCookieValue cookieValue = new DetectionCookieValue((maxRetries - 1) + SPLITTER + expiredTimestamp);
        // when
        final boolean withInLimit = cookieValue.isWithInLimit(maxRetries);
        // then
        Assertions.assertFalse(withInLimit);
    }

    @Test
    public void test_toString() {
        // given
        final long timestamp = System.currentTimeMillis();
        final DetectionCookieValue cookieValue = new DetectionCookieValue(1 + SPLITTER + timestamp);
        // when
        final String value = cookieValue.toString();
        // then
        Assertions.assertEquals(1 + SPLITTER + timestamp, value);
    }

    @Test
    public void test_contstructor_default() {
        // given
        final int lifetime = 1 * 60 * 1000; // 1m
        final long now = System.currentTimeMillis();
        final DetectionCookieValue cookieValue = new DetectionCookieValue(now, lifetime);
        // when
        // then
        Assertions.assertEquals(0, cookieValue.retries);
        Assertions.assertEquals((now + lifetime) / 1000, cookieValue.sessionLifeTime);
    }

    @Test
    public void test_contstructor_with_one() {
        // given
        final long validTimestamp = System.currentTimeMillis() + 1 * 60 * 1000; // now+1m
        final DetectionCookieValue cookieValue = new DetectionCookieValue(1 + SPLITTER + validTimestamp);
        // when
        // then
        Assertions.assertEquals(1, cookieValue.retries);
        Assertions.assertEquals(validTimestamp, cookieValue.sessionLifeTime);
    }
}
