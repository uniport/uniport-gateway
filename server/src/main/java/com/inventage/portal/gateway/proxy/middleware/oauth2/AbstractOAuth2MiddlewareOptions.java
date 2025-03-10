package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareStyle;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayMiddlewareStyle
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = OAuth2MiddlewareOptions.Builder.class)
public abstract class AbstractOAuth2MiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(OAuth2MiddlewareFactory.OAUTH2_CLIENT_ID)
    public abstract String getClientId();

    @JsonProperty(OAuth2MiddlewareFactory.OAUTH2_CLIENT_SECRET)
    public abstract String getClientSecret();

    @JsonProperty(OAuth2MiddlewareFactory.OAUTH2_DISCOVERY_URL)
    public abstract String getDiscoveryURL();

    @JsonProperty(OAuth2MiddlewareFactory.OAUTH2_SESSION_SCOPE)
    public abstract String getSessionScope();

    @Default
    @JsonProperty(OAuth2MiddlewareFactory.OAUTH2_RESPONSE_MODE)
    public String getResponseMode() {
        return OAuth2MiddlewareFactory.DEFAULT_OIDC_RESPONSE_MODE;
    }

    @Default
    @JsonProperty(OAuth2MiddlewareFactory.OAUTH2_PROXY_AUTHENTICATION_FLOW)
    public boolean proxyAuthenticationFlow() {
        return OAuth2MiddlewareFactory.DEFAULT_OAUTH2_PROXY_AUTHENTICATION_FLOW;
    }

    @Nullable
    @JsonProperty(OAuth2MiddlewareFactory.OAUTH2_PUBLIC_URL)
    public abstract String getPublicURL();

    @JsonProperty(OAuth2MiddlewareFactory.OAUTH2_ADDITIONAL_SCOPES)
    public abstract List<String> getAdditionalScopes();

    @JsonProperty(OAuth2MiddlewareFactory.OAUTH2_ADDITIONAL_PARAMETERS)
    public abstract Map<String, String> getAdditionalParameters();

    @JsonProperty(OAuth2MiddlewareFactory.OAUTH2_PASSTHROUGH_PARAMETERS)
    public abstract List<String> getPassthroughParameters();
}
