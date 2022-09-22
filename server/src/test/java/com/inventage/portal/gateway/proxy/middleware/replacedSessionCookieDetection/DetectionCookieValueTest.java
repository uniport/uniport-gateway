package com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.DetectionCookieValue.MAX_RETRIES;
import static com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.DetectionCookieValue.SPLITTER;

public class DetectionCookieValueTest {

    @Test
    public void test_isWithInLimit_default() {
        // given
        final DetectionCookieValue cookieValue = new DetectionCookieValue();
        // when
        final boolean withInLimit = cookieValue.isWithInLimit();
        // then
        Assertions.assertTrue(withInLimit);
    }

    @Test
    public void test_isWithInLimit_new() {
        // given
        final DetectionCookieValue cookieValue = new DetectionCookieValue("0:" +System.currentTimeMillis());
        // when
        final boolean withInLimit = cookieValue.isWithInLimit();
        // then
        Assertions.assertTrue(withInLimit);
    }

    @Test
    public void test_isWithInLimit_max() {
        // given
        final DetectionCookieValue cookieValue = new DetectionCookieValue((MAX_RETRIES - 1) + SPLITTER +System.currentTimeMillis());
        // when
        final boolean withInLimit = cookieValue.isWithInLimit();
        // then
        Assertions.assertTrue(withInLimit);
    }

    @Test
    public void test_isWithInLimit_false_counter() {
        // given
        final DetectionCookieValue cookieValue = new DetectionCookieValue(MAX_RETRIES +SPLITTER +System.currentTimeMillis());
        // when
        final boolean withInLimit = cookieValue.isWithInLimit();
        // then
        Assertions.assertFalse(withInLimit);
    }

    @Test
    public void test_isWithInLimit_false_access() {
        // given
        final DetectionCookieValue cookieValue = new DetectionCookieValue((MAX_RETRIES - 1) +SPLITTER +1000);
        // when
        final boolean withInLimit = cookieValue.isWithInLimit();
        // then
        Assertions.assertFalse(withInLimit);
    }

    @Test
    public void test_toString() {
        // given
        final long accessTime = System.currentTimeMillis();
        final DetectionCookieValue cookieValue = new DetectionCookieValue(1 +SPLITTER +accessTime);
        // when
        final String value = cookieValue.toString();
        // then
        Assertions.assertEquals(1 +SPLITTER +accessTime, value);
    }

    @Test
    public void test_contstructor_default() {
        // given
        final DetectionCookieValue cookieValue = new DetectionCookieValue();
        // when
        // then
        Assertions.assertEquals(0, cookieValue.counter);
    }

    @Test
    public void test_contstructor_with_one() {
        // given
        final long accessTime = System.currentTimeMillis();
        final DetectionCookieValue cookieValue = new DetectionCookieValue(1 +SPLITTER +accessTime);
        // when
        // then
        Assertions.assertEquals(1, cookieValue.counter);
        Assertions.assertEquals(accessTime, cookieValue.lastSessionAccess);
    }
}
