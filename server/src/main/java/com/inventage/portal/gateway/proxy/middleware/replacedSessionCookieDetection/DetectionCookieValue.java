package com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The value used in the cookie for detecting a replaced session cookie.
 * The value has the following format: <counter>:<session-lifetime>
 * The session lifetime is the point in time (Unix Epoch in seconds) until the session is valid (== not expired).
 */
public class DetectionCookieValue {

    private static final Logger LOGGER = LoggerFactory.getLogger(DetectionCookieValue.class);

    protected static final String SPLITTER = ":";

    protected int retries;
    protected long sessionLifeTime;

    DetectionCookieValue(long lastAccessed, long sessionIdleTimeoutInMilliSeconds) {
        retries = 0;
        sessionLifeTime = (lastAccessed + sessionIdleTimeoutInMilliSeconds) / 1000;
    }

    DetectionCookieValue(String cookieValue) {
        try {
            final String[] parts = cookieValue.split(SPLITTER);
            if (parts.length > 0) {
                retries = Integer.parseInt(parts[0]);
            }
            if (parts.length > 1) {
                sessionLifeTime = Long.parseLong(parts[1]);
            }
        } catch (Throwable t) {
            LOGGER.warn("Failed to parse cookie value '{}'", cookieValue);
        }
    }

    @Override
    public String toString() {
        return String.format("%s%s%s", retries, SPLITTER, sessionLifeTime);
    }

    void increment() {
        retries++;
    }

    boolean isWithInLimit(int maxRedirectRetries) {
        if (retries >= maxRedirectRetries) {
            LOGGER.warn("Counter value '{}' exceeds limit '{}'", retries, maxRedirectRetries);
            return false;
        }
        if (isExpired()) {
            return false;
        }
        LOGGER.debug("Counter value '{}'", this);
        return true;
    }

    boolean isExpired() {
        if (sessionLifeTime < System.currentTimeMillis() / 1000) {
            LOGGER.warn("Cookie with session life time until '{}' is expired", sessionLifeTime);
            return true;
        }
        return false;
    }

}
