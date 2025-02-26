package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuth2MiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(OAuth2MiddlewareFactory.OAUTH2_CLIENT_ID)
    private String clientId;

    @JsonProperty(OAuth2MiddlewareFactory.OAUTH2_CLIENT_SECRET)
    private String clientSecret;

    @JsonProperty(OAuth2MiddlewareFactory.OAUTH2_DISCOVERY_URL)
    private String discoveryURL;

    @JsonProperty(OAuth2MiddlewareFactory.OAUTH2_SESSION_SCOPE)
    private String sessionScope;

    @JsonProperty(OAuth2MiddlewareFactory.OAUTH2_RESPONSE_MODE)
    private String responseMode;

    @JsonProperty(OAuth2MiddlewareFactory.OAUTH2_PROXY_AUTHENTICATION_FLOW)
    private Boolean proxyAuthenticationFlow;

    @JsonProperty(OAuth2MiddlewareFactory.OAUTH2_PUBLIC_URL)
    private String publicURL;

    @JsonProperty(OAuth2MiddlewareFactory.OAUTH2_ADDITIONAL_SCOPES)
    private List<String> additionalScopes;

    @JsonProperty(OAuth2MiddlewareFactory.OAUTH2_ADDITIONAL_PARAMETERS)
    private Map<String, String> additionalParameters;

    @JsonProperty(OAuth2MiddlewareFactory.OAUTH2_PASSTHROUGH_PARAMETERS)
    private List<String> passthroughParameters;

    public OAuth2MiddlewareOptions() {
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getDiscoveryURL() {
        return discoveryURL;
    }

    public String getSessionScope() {
        return sessionScope;
    }

    public String getResponseMode() {
        return responseMode;
    }

    public Boolean proxyAuthenticationFlow() {
        return proxyAuthenticationFlow;
    }

    public String getPublicURL() {
        return publicURL;
    }

    public List<String> getAdditionalScopes() {
        return additionalScopes == null ? null : List.copyOf(additionalScopes);
    }

    public Map<String, String> getAdditionalParameters() {
        return additionalParameters == null ? null : Map.copyOf(additionalParameters);
    }

    public List<String> getPassthroughParameters() {
        return passthroughParameters == null ? null : List.copyOf(passthroughParameters);
    }

    @Override
    public OAuth2MiddlewareOptions clone() {
        try {
            final OAuth2MiddlewareOptions options = (OAuth2MiddlewareOptions) super.clone();
            options.additionalScopes = additionalScopes == null ? null : List.copyOf(additionalScopes);
            options.additionalParameters = additionalParameters == null ? null : Map.copyOf(additionalParameters);
            options.passthroughParameters = passthroughParameters == null ? null : List.copyOf(passthroughParameters);
            return options;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
