package ch.uniport.gateway.proxy.middleware.replacedSessionCookieDetection;

import static ch.uniport.gateway.proxy.middleware.replacedSessionCookieDetection.DetectionCookieValue.SPLITTER;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DetectionCookieValueTest {

    @Test
    public void isWithInLimitDefault() {
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
    public void isWithInLimitNew() {
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
    public void isWithInLimitMax() {
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
    public void isWithInLimitFalseCounter() {
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
    public void isWithInLimitFalseAccess() {
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
    public void testToString() {
        // given
        final long timestamp = System.currentTimeMillis();
        final DetectionCookieValue cookieValue = new DetectionCookieValue(1 + SPLITTER + timestamp);
        // when
        final String value = cookieValue.toString();
        // then
        Assertions.assertEquals(1 + SPLITTER + timestamp, value);
    }

    @Test
    public void testConstructorDefault() {
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
    public void testConstructorWithOne() {
        // given
        final long validTimestamp = System.currentTimeMillis() + 1 * 60 * 1000; // now+1m
        final DetectionCookieValue cookieValue = new DetectionCookieValue(1 + SPLITTER + validTimestamp);
        // when
        // then
        Assertions.assertEquals(1, cookieValue.retries);
        Assertions.assertEquals(validTimestamp, cookieValue.sessionLifeTime);
    }
}
