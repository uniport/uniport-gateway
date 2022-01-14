package com.inventage.portal.gateway.core.session;

import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The value used in the cookie for detecting a replaced session cookie.
 * The value has the following format: <counter>:<last-session-access>
 */
public class DetectionCookieValue {

    private static final Logger LOGGER = LoggerFactory.getLogger(DetectionCookieValue.class);

    protected static final String SPLITTER = ":";
    protected static final int MAX_RETRIES = 5;

    int counter;
    long lastSessionAccess;

    DetectionCookieValue() {
        counter = 0;
        lastSessionAccess = System.currentTimeMillis();
    }

    DetectionCookieValue(String cookieValue) {
        try {
            final String[] parts = cookieValue.split(SPLITTER);
            if (parts.length > 0) {
                counter = Integer.parseInt(parts[0]);
            }
            if (parts.length > 1) {
                lastSessionAccess = Long.parseLong(parts[1]);
            }
        } catch (Throwable t) {
            LOGGER.warn("constructor: the received cookie value '{}' couldn't be parsed", cookieValue);
        }
    }

    @Override
    public String toString() {
        return String.format("%s%s%s", counter, SPLITTER, lastSessionAccess);
    }

    String increment() {
        counter++;
        return toString();
    }

    boolean isWithInLimit() {
        if (counter >= MAX_RETRIES) {
            LOGGER.warn("isWithInLimit: counter value '{}' exceeds limit '{}'", counter, MAX_RETRIES);
            return false;
        }
        if ((System.currentTimeMillis() - lastSessionAccess >= 30 * 60 * 1000)) {
            LOGGER.warn("isWithInLimit: cookie with last session access date '{}' is outdated", lastSessionAccess);
            return false;
        }
        LOGGER.debug("isWithInLimit: value '{}'", toString());
        return true;
    }

}
