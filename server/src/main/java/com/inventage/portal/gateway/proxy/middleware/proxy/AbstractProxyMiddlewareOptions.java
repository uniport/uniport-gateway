package com.inventage.portal.gateway.proxy.middleware.proxy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayStyle;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value.Check;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = ProxyMiddlewareOptions.Builder.class)
public abstract class AbstractProxyMiddlewareOptions implements GatewayMiddlewareOptions {

    @Check
    protected void validate() {
        Preconditions.checkState(!getServers().isEmpty(), "'getServers' must have at least one element");
    }

    @JsonProperty(ProxyMiddlewareFactory.SERVICE_NAME)
    public abstract String getName();

    @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVERS)
    public abstract List<ServerOptions> getServers();

    @Default
    @JsonProperty(ProxyMiddlewareFactory.SERVICE_VERBOSE)
    public boolean isVerbose() {
        return ProxyMiddlewareFactory.DEFAULT_VERBOSE;
    }

    @Immutable
    @GatewayStyle
    @JsonDeserialize(builder = ServerOptions.Builder.class)
    public abstract static class AbstractServerOptions implements GatewayMiddlewareOptions {

        @Default
        @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVER_PROTOCOL)
        public String getProtocol() {
            return ProxyMiddlewareFactory.DEFAULT_SERVER_PROTOCOL;
        }

        @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVER_HOST)
        public abstract String getHost();

        @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVER_PORT)
        public abstract int getPort();

        @Nullable
        @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVER_HTTPS_OPTIONS)
        public abstract HTTPsOptions getHTTPs();
    }

    @Immutable
    @GatewayStyle
    @JsonDeserialize(builder = HTTPsOptions.Builder.class)
    public abstract static class AbstractHTTPsOptions implements GatewayMiddlewareOptions {

        @Default
        @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVER_HTTPS_OPTIONS_VERIFY_HOSTNAME)
        public boolean verifyHostname() {
            return ProxyMiddlewareFactory.DEFAULT_HTTPS_VERIFY_HOSTNAME;
        }

        @Default
        @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_ALL)
        public boolean trustAll() {
            return ProxyMiddlewareFactory.DEFAULT_HTTPS_TRUST_ALL;
        }

        @Default
        @Nullable
        @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PATH)
        public String getTrustStorePath() {
            return ProxyMiddlewareFactory.DEFAULT_HTTPS_TRUST_STORE_PATH;
        }

        @Default
        @Nullable
        @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PASSWORD)
        public String getTrustStorePassword() {
            return ProxyMiddlewareFactory.DEFAULT_HTTPS_TRUST_STORE_PATH;
        }
    }
}
