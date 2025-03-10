package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayStyle;
import java.util.List;
import org.immutables.value.Value.Check;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = SessionBagMiddlewareOptions.Builder.class)
public abstract class AbstractSessionBagMiddlewareOptions implements GatewayMiddlewareOptions {

    @Check
    protected void validate() {
        Preconditions.checkState(!getWhitelistedCookieOptions().isEmpty(), "'getWhitelistedCookieOptions' must have at least one element");
    }

    @Default
    @JsonProperty(SessionBagMiddlewareFactory.SESSION_BAG_SESSION_COOKIE_NAME)
    public String getSessionCookieName() {
        return SessionBagMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;
    }

    @JsonProperty(SessionBagMiddlewareFactory.SESSION_BAG_WHITELISTED_COOKIES)
    public abstract List<WhitelistedCookieOption> getWhitelistedCookieOptions();

    @Immutable
    @GatewayStyle
    @JsonDeserialize(builder = WhitelistedCookieOption.Builder.class)
    public abstract static class AbstractWhitelistedCookieOption implements GatewayMiddlewareOptions {

        @JsonProperty(SessionBagMiddlewareFactory.SESSION_BAG_WHITELISTED_COOKIE_NAME)
        public abstract String getName();

        @JsonProperty(SessionBagMiddlewareFactory.SESSION_BAG_WHITELISTED_COOKIE_PATH)
        public abstract String getPath();
    }
}
