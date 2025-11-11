package ch.uniport.gateway.proxy.middleware.sessionBag;

import ch.uniport.gateway.proxy.middleware.MiddlewareOptionsModel;
import ch.uniport.gateway.proxy.middleware.ModelStyle;
import ch.uniport.gateway.proxy.middleware.session.AbstractSessionMiddlewareOptions;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import java.util.List;
import org.immutables.value.Value.Check;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@ModelStyle
@JsonDeserialize(builder = SessionBagMiddlewareOptions.Builder.class)
public abstract class AbstractSessionBagMiddlewareOptions implements MiddlewareOptionsModel {

    public static final String DEFAULT_SESSION_COOKIE_NAME = AbstractSessionMiddlewareOptions.DEFAULT_SESSION_COOKIE_NAME;

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionBagMiddlewareOptions.class);

    @Check
    protected void validate() {
        Preconditions.checkState(!getWhitelistedCookieOptions().isEmpty(),
            "'getWhitelistedCookieOptions' must have at least one element");
    }

    @Default
    @JsonProperty(SessionBagMiddlewareFactory.SESSION_COOKIE_NAME)
    public String getSessionCookieName() {
        logDefault(LOGGER, SessionBagMiddlewareFactory.SESSION_COOKIE_NAME, DEFAULT_SESSION_COOKIE_NAME);
        return DEFAULT_SESSION_COOKIE_NAME;
    }

    @JsonProperty(SessionBagMiddlewareFactory.WHITELISTED_COOKIES)
    public abstract List<WhitelistedCookieOptions> getWhitelistedCookieOptions();

    @Immutable
    @ModelStyle
    @JsonDeserialize(builder = WhitelistedCookieOptions.Builder.class)
    public abstract static class AbstractWhitelistedCookieOptions implements MiddlewareOptionsModel {

        @JsonProperty(SessionBagMiddlewareFactory.WHITELISTED_COOKIE_NAME)
        public abstract String getName();

        @JsonProperty(SessionBagMiddlewareFactory.WHITELISTED_COOKIE_PATH)
        public abstract String getPath();
    }
}
