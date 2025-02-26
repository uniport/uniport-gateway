package com.inventage.portal.gateway.proxy.middleware.proxy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProxyMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVERS)
    private List<ServerOptions> servers;

    @JsonProperty(ProxyMiddlewareFactory.SERVICE_VERBOSE)
    private Boolean verbose;

    public ProxyMiddlewareOptions() {
    }

    public List<ServerOptions> getServers() {
        return servers == null ? null : servers.stream().map(ServerOptions::clone).toList();
    }

    public Boolean isVerbose() {
        return verbose;
    }

    @Override
    public ProxyMiddlewareOptions clone() {
        try {
            final ProxyMiddlewareOptions options = (ProxyMiddlewareOptions) super.clone();
            options.servers = servers == null ? null : servers.stream().map(ServerOptions::clone).toList();
            return options;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public static final class ServerOptions implements GatewayMiddlewareOptions {

        @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVER_PROTOCOL)
        private String protocol;

        @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVER_HOST)
        private String host;

        @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVER_PORT)
        private Integer port;

        @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVER_HTTPS_OPTIONS)
        private HTTPsOptions https;

        private ServerOptions() {
        }

        public String getProtocol() {
            return protocol;
        }

        public String getHost() {
            return host;
        }

        public Integer getPort() {
            return port;
        }

        public HTTPsOptions getHTTPs() {
            return https.clone();
        }

        public ServerOptions clone() {
            try {
                final ServerOptions options = (ServerOptions) super.clone();
                options.https = https == null ? null : https.clone();
                return options;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static final class HTTPsOptions implements GatewayMiddlewareOptions {

        @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVER_HTTPS_OPTIONS_VERIFY_HOSTNAME)
        private Boolean verifyHostname;

        @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_ALL)
        private Boolean trustAll;

        @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PATH)
        private String trustStorePath;

        @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PASSWORD)
        private String trustStorePassword;

        private HTTPsOptions() {
        }

        public Boolean verifyHostname() {
            return verifyHostname;
        }

        public Boolean trustAll() {
            return trustAll;
        }

        public String getTrustStorePath() {
            return trustStorePath;
        }

        public String getTrustStorePassword() {
            return trustStorePassword;
        }

        public HTTPsOptions clone() {
            try {
                return (HTTPsOptions) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
