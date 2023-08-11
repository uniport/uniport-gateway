package com.inventage.portal.gateway.proxy.middleware.session;

/**
 * Calculates the session lifetime based on the given session idle timeout.
 * The session lifetime is the point in time (Unix Epoch in seconds) until the session is valid (== not expired).
 */
public class SessionLifetimeValue {

    private final long sessionLifeTime;

    public SessionLifetimeValue(Long lastAccessed, Long sessionIdleTimeoutInMilliSeconds) {
        sessionLifeTime = (lastAccessed + sessionIdleTimeoutInMilliSeconds) / 1000;
    }

    @Override
    public String toString() {
        return String.format("%s", sessionLifeTime);
    }

}
