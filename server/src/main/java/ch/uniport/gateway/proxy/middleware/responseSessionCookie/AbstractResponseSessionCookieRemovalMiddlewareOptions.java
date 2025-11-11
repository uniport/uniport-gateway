package ch.uniport.gateway.proxy.middleware.responseSessionCookie;

import ch.uniport.gateway.proxy.middleware.MiddlewareOptionsModel;
import ch.uniport.gateway.proxy.middleware.ModelStyle;
import ch.uniport.gateway.proxy.middleware.session.AbstractSessionMiddlewareOptions;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@ModelStyle
@JsonDeserialize(builder = ResponseSessionCookieRemovalMiddlewareOptions.Builder.class)
public abstract class AbstractResponseSessionCookieRemovalMiddlewareOptions implements MiddlewareOptionsModel {

    public static final String DEFAULT_SESSION_COOKIE_NAME = AbstractSessionMiddlewareOptions.DEFAULT_SESSION_COOKIE_NAME;

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseSessionCookieRemovalMiddlewareOptions.class);

    @Default
    @JsonProperty(ResponseSessionCookieRemovalMiddlewareFactory.SESSION_COOKIE_NAME)
    public String getSessionCookieName() {
        logDefault(LOGGER, ResponseSessionCookieRemovalMiddlewareFactory.SESSION_COOKIE_NAME,
            DEFAULT_SESSION_COOKIE_NAME);
        return DEFAULT_SESSION_COOKIE_NAME;
    }
}
