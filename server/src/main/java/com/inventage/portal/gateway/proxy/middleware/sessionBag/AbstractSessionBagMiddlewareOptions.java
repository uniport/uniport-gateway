package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.inventage.portal.gateway.core.model.GatewayStyle;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import java.util.List;
import org.immutables.value.Value.Check;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = SessionBagMiddlewareOptions.Builder.class)
public abstract class AbstractSessionBagMiddlewareOptions implements GatewayMiddlewareOptions {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionBagMiddlewareOptions.class);

    @Check
    protected void validate() {
        Preconditions.checkState(!getWhitelistedCookieOptions().isEmpty(), "'getWhitelistedCookieOptions' must have at least one element");
    }

    @Default
    @JsonProperty(SessionBagMiddlewareFactory.SESSION_COOKIE_NAME)
    public String getSessionCookieName() {
        logDefault(LOGGER, SessionBagMiddlewareFactory.SESSION_COOKIE_NAME, SessionBagMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME);
        return SessionBagMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;
    }

    @JsonProperty(SessionBagMiddlewareFactory.WHITELISTED_COOKIES)
    public abstract List<WhitelistedCookieOptions> getWhitelistedCookieOptions();

    @Immutable
    @GatewayStyle
    @JsonDeserialize(builder = WhitelistedCookieOptions.Builder.class)
    public abstract static class AbstractWhitelistedCookieOptions implements GatewayMiddlewareOptions {

        @JsonProperty(SessionBagMiddlewareFactory.WHITELISTED_COOKIE_NAME)
        public abstract String getName();

        @JsonProperty(SessionBagMiddlewareFactory.WHITELISTED_COOKIE_PATH)
        public abstract String getPath();
    }
}
