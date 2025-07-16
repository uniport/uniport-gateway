package ch.uniport.gateway.proxy.middleware.replacedSessionCookieDetection;

import ch.uniport.gateway.core.config.model.ModelStyle;
import ch.uniport.gateway.proxy.config.model.MiddlewareOptionsModel;
import ch.uniport.gateway.proxy.middleware.session.AbstractSessionMiddlewareOptions;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@ModelStyle
@JsonDeserialize(builder = ReplacedSessionCookieDetectionMiddlewareOptions.Builder.class)
public abstract class AbstractReplacedSessionCookieDetectionMiddlewareOptions implements MiddlewareOptionsModel {

    public static final String DEFAULT_DETECTION_COOKIE_NAME = "uniport.state";
    public static final String DEFAULT_SESSION_COOKIE_NAME = AbstractSessionMiddlewareOptions.DEFAULT_SESSION_COOKIE_NAME;
    public static final int DEFAULT_WAIT_BEFORE_RETRY_MS = 50;
    public static final int DEFAULT_MAX_REDIRECT_RETRIES = 5;

    private static final Logger LOGGER = LoggerFactory.getLogger(ReplacedSessionCookieDetectionMiddlewareOptions.class);

    @Default
    @JsonProperty(ReplacedSessionCookieDetectionMiddlewareFactory.DETECTION_COOKIE_NAME)
    public String getCookieName() {
        logDefault(LOGGER, ReplacedSessionCookieDetectionMiddlewareFactory.DETECTION_COOKIE_NAME,
            DEFAULT_DETECTION_COOKIE_NAME);
        return DEFAULT_DETECTION_COOKIE_NAME;
    }

    @Default
    @JsonProperty(ReplacedSessionCookieDetectionMiddlewareFactory.SESSION_COOKIE_NAME)
    public String getSessionCookieName() {
        logDefault(LOGGER, ReplacedSessionCookieDetectionMiddlewareFactory.SESSION_COOKIE_NAME,
            DEFAULT_SESSION_COOKIE_NAME);
        return DEFAULT_SESSION_COOKIE_NAME;
    }

    @Default
    @JsonProperty(ReplacedSessionCookieDetectionMiddlewareFactory.WAIT_BEFORE_RETRY_MS)
    public int getWaitBeforeRetryMs() {
        logDefault(LOGGER, ReplacedSessionCookieDetectionMiddlewareFactory.WAIT_BEFORE_RETRY_MS,
            DEFAULT_WAIT_BEFORE_RETRY_MS);
        return DEFAULT_WAIT_BEFORE_RETRY_MS;
    }

    @Default
    @JsonProperty(ReplacedSessionCookieDetectionMiddlewareFactory.MAX_REDIRECT_RETRIES)
    public int getMaxRetries() {
        logDefault(LOGGER, ReplacedSessionCookieDetectionMiddlewareFactory.MAX_REDIRECT_RETRIES,
            DEFAULT_MAX_REDIRECT_RETRIES);
        return DEFAULT_MAX_REDIRECT_RETRIES;
    }
}
