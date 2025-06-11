package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.Value.Default;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ModelStyle
public abstract class AbstractOAuth2MiddlewareOptionsBase implements MiddlewareOptionsModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2MiddlewareOptions.class);

    @JsonProperty(OAuth2MiddlewareFactory.CLIENT_ID)
    public abstract String getClientId();

    @JsonProperty(OAuth2MiddlewareFactory.CLIENT_SECRET)
    public abstract String getClientSecret();

    @JsonProperty(OAuth2MiddlewareFactory.DISCOVERY_URL)
    public abstract String getDiscoveryURL();

    @JsonProperty(OAuth2MiddlewareFactory.SESSION_SCOPE)
    public abstract String getSessionScope();

    @Default
    @JsonProperty(OAuth2MiddlewareFactory.RESPONSE_MODE)
    public String getResponseMode() {
        logDefault(LOGGER, OAuth2MiddlewareFactory.RESPONSE_MODE, OAuth2MiddlewareFactory.DEFAULT_OIDC_RESPONSE_MODE);
        return OAuth2MiddlewareFactory.DEFAULT_OIDC_RESPONSE_MODE;
    }

    @Default
    @JsonProperty(OAuth2MiddlewareFactory.PROXY_AUTHENTICATION_FLOW)
    public boolean proxyAuthenticationFlow() {
        logDefault(LOGGER, OAuth2MiddlewareFactory.PROXY_AUTHENTICATION_FLOW, OAuth2MiddlewareFactory.DEFAULT_OAUTH2_PROXY_AUTHENTICATION_FLOW);
        return OAuth2MiddlewareFactory.DEFAULT_OAUTH2_PROXY_AUTHENTICATION_FLOW;
    }

    @Nullable
    @JsonProperty(OAuth2MiddlewareFactory.PUBLIC_URL)
    public abstract String getPublicURL();

    @JsonProperty(OAuth2MiddlewareFactory.ADDITIONAL_SCOPES)
    public abstract List<String> getAdditionalScopes();

    @JsonProperty(OAuth2MiddlewareFactory.ADDITIONAL_PARAMETERS)
    public abstract Map<String, String> getAdditionalAuthRequestParameters();

    @JsonProperty(OAuth2MiddlewareFactory.PASSTHROUGH_PARAMETERS)
    public abstract List<String> getPassthroughParameters();

    // dynamically added during runtime
    public abstract Map<String, String> env();
}
