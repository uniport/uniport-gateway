package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionBagMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(SessionBagMiddlewareFactory.SESSION_BAG_SESSION_COOKIE_NAME)
    private String sessionCookieName;

    @JsonProperty(SessionBagMiddlewareFactory.SESSION_BAG_WHITELISTED_COOKIES)
    private List<WhitelistedCookieOption> whitelistedCookies;

    public SessionBagMiddlewareOptions() {
    }

    public String getSessionCookieName() {
        return sessionCookieName;
    }

    public List<WhitelistedCookieOption> getWhitelistedCookieOptions() {
        return whitelistedCookies.stream().map(WhitelistedCookieOption::clone).toList();
    }

    @Override
    public SessionBagMiddlewareOptions clone() {
        try {
            final SessionBagMiddlewareOptions options = (SessionBagMiddlewareOptions) super.clone();
            options.whitelistedCookies = whitelistedCookies == null ? null : whitelistedCookies.stream().map(WhitelistedCookieOption::clone).toList();
            return options;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public static final class WhitelistedCookieOption implements GatewayMiddlewareOptions {

        @JsonProperty(SessionBagMiddlewareFactory.SESSION_BAG_WHITELISTED_COOKIE_NAME)
        private String name;

        @JsonProperty(SessionBagMiddlewareFactory.SESSION_BAG_WHITELISTED_COOKIE_PATH)
        private String path;

        private WhitelistedCookieOption() {
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        @Override
        public WhitelistedCookieOption clone() {
            try {
                final WhitelistedCookieOption options = (WhitelistedCookieOption) super.clone();
                return options;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
