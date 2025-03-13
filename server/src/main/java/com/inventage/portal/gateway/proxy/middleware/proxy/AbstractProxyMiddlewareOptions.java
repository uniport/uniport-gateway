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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = ProxyMiddlewareOptions.Builder.class)
public abstract class AbstractProxyMiddlewareOptions implements GatewayMiddlewareOptions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyMiddlewareOptions.class);

    @Check
    protected void validate() {
        Preconditions.checkState(!getServers().isEmpty(), "'getServers' must have at least one element");
    }

    @JsonProperty(ProxyMiddlewareFactory.NAME)
    public abstract String getName();

    @JsonProperty(ProxyMiddlewareFactory.SERVERS)
    public abstract List<ServerOptions> getServers();

    @Default
    @JsonProperty(ProxyMiddlewareFactory.VERBOSE)
    public boolean isVerbose() {
        logDefault(LOGGER, ProxyMiddlewareFactory.VERBOSE, ProxyMiddlewareFactory.DEFAULT_VERBOSE);
        return ProxyMiddlewareFactory.DEFAULT_VERBOSE;
    }

    @Immutable
    @GatewayStyle
    @JsonDeserialize(builder = ServerOptions.Builder.class)
    public abstract static class AbstractServerOptions implements GatewayMiddlewareOptions {

        @Default
        @JsonProperty(ProxyMiddlewareFactory.SERVER_PROTOCOL)
        public String getProtocol() {
            return ProxyMiddlewareFactory.DEFAULT_SERVER_PROTOCOL;
        }

        @JsonProperty(ProxyMiddlewareFactory.SERVER_HOST)
        public abstract String getHost();

        @JsonProperty(ProxyMiddlewareFactory.SERVER_PORT)
        public abstract int getPort();

        @Default
        @JsonProperty(ProxyMiddlewareFactory.SERVER_HTTPS_OPTIONS)
        public HTTPsOptions getHTTPs() {
            logDefault(LOGGER, ProxyMiddlewareFactory.SERVER_HTTPS_OPTIONS);
            return HTTPsOptions.builder().build();
        }
    }

    @Immutable
    @GatewayStyle
    @JsonDeserialize(builder = HTTPsOptions.Builder.class)
    public abstract static class AbstractHTTPsOptions implements GatewayMiddlewareOptions {

        @Default
        @JsonProperty(ProxyMiddlewareFactory.VERIFY_HOSTNAME)
        public boolean verifyHostname() {
            logDefault(LOGGER, ProxyMiddlewareFactory.VERIFY_HOSTNAME, ProxyMiddlewareFactory.DEFAULT_VERIFY_HOSTNAME);
            return ProxyMiddlewareFactory.DEFAULT_VERIFY_HOSTNAME;
        }

        @Default
        @JsonProperty(ProxyMiddlewareFactory.TRUST_ALL)
        public boolean trustAll() {
            logDefault(LOGGER, ProxyMiddlewareFactory.TRUST_ALL, ProxyMiddlewareFactory.DEFAULT_TRUST_ALL);
            return ProxyMiddlewareFactory.DEFAULT_TRUST_ALL;
        }

        @Default
        @Nullable
        @JsonProperty(ProxyMiddlewareFactory.TRUST_STORE_PATH)
        public String getTrustStorePath() {
            logDefault(LOGGER, ProxyMiddlewareFactory.TRUST_STORE_PATH, ProxyMiddlewareFactory.DEFAULT_TRUST_STORE_PATH);
            return ProxyMiddlewareFactory.DEFAULT_TRUST_STORE_PATH;
        }

        @Default
        @Nullable
        @JsonProperty(ProxyMiddlewareFactory.TRUST_STORE_PASSWORD)
        public String getTrustStorePassword() {
            logDefault(LOGGER, ProxyMiddlewareFactory.TRUST_STORE_PASSWORD, ProxyMiddlewareFactory.DEFAULT_TRUST_STORE_PATH);
            return ProxyMiddlewareFactory.DEFAULT_TRUST_STORE_PATH;
        }
    }
}
