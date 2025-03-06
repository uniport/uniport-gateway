package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = OAuth2MiddlewareOptions.Builder.class)
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

    public static BaseBuilder<?> builder() {
        return new Builder();
    }

    protected OAuth2MiddlewareOptions(BaseBuilder<?> builder) {
        if (builder.clientId == null) {
            throw new IllegalArgumentException("client id is required");
        }
        if (builder.clientSecret == null) {
            throw new IllegalArgumentException("client secret is required");
        }
        if (builder.discoveryURL == null) {
            throw new IllegalArgumentException("discovery URL is required");
        }
        if (builder.sessionScope == null) {
            throw new IllegalArgumentException("session scope is required");
        }

        this.clientId = builder.clientId;
        this.clientSecret = builder.clientSecret;
        this.discoveryURL = builder.discoveryURL;
        this.sessionScope = builder.sessionScope;
        this.responseMode = builder.responseMode;
        this.proxyAuthenticationFlow = builder.proxyAuthenticationFlow;
        this.publicURL = builder.publicURL;
        this.additionalScopes = builder.additionalScopes;
        this.additionalParameters = builder.additionalParameters;
        this.passthroughParameters = builder.passthroughParameters;
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

    protected abstract static class BaseBuilder<T extends BaseBuilder<T>> {
        protected String clientId;
        protected String clientSecret;
        protected String discoveryURL;
        protected String sessionScope;
        protected String responseMode;
        protected Boolean proxyAuthenticationFlow;
        protected String publicURL;
        protected List<String> additionalScopes;
        protected Map<String, String> additionalParameters;
        protected List<String> passthroughParameters;

        public static BaseBuilder<?> builder() {
            return new Builder();
        }

        protected abstract T self();

        public T withClientId(String clientId) {
            this.clientId = clientId;
            return self();
        }

        public T withClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return self();
        }

        public T withDiscoveryURL(String discoveryURL) {
            this.discoveryURL = discoveryURL;
            return self();
        }

        public T withSessionScope(String sessionScope) {
            this.sessionScope = sessionScope;
            return self();
        }

        public T withResponseMode(String responseMode) {
            this.responseMode = responseMode;
            return self();
        }

        public T withProxyAuthenticationFlow(Boolean proxyAuthenticationFlow) {
            this.proxyAuthenticationFlow = proxyAuthenticationFlow;
            return self();
        }

        public T withPublicURL(String publicURL) {
            this.publicURL = publicURL;
            return self();
        }

        public T withAdditionalScopes(List<String> additionalScopes) {
            this.additionalScopes = additionalScopes;
            return self();
        }

        public T withAdditionalParameters(Map<String, String> additionalParameters) {
            this.additionalParameters = additionalParameters;
            return self();
        }

        public T withPassthroughParameters(List<String> passthroughParameters) {
            this.passthroughParameters = passthroughParameters;
            return self();
        }

        public OAuth2MiddlewareOptions build() {
            return new OAuth2MiddlewareOptions(this);
        }
    }

    @JsonPOJOBuilder
    public static class Builder extends BaseBuilder<Builder> {
        @Override
        protected Builder self() {
            return this;
        }
    }
}
