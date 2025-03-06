package com.inventage.portal.gateway.proxy.middleware.proxy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = ProxyMiddlewareOptions.Builder.class)
public final class ProxyMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVERS)
    private List<ServerOptions> servers;

    @JsonProperty(ProxyMiddlewareFactory.SERVICE_VERBOSE)
    private Boolean verbose;

    public static Builder builder() {
        return new Builder();
    }

    private ProxyMiddlewareOptions(Builder builder) {
        this.servers = builder.servers;
        this.verbose = builder.verbose;
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

    @JsonDeserialize(builder = ServerOptions.Builder.class)
    public static final class ServerOptions implements GatewayMiddlewareOptions {

        @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVER_PROTOCOL)
        private String protocol;

        @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVER_HOST)
        private String host;

        @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVER_PORT)
        private Integer port;

        @JsonProperty(ProxyMiddlewareFactory.SERVICE_SERVER_HTTPS_OPTIONS)
        private HTTPsOptions https;

        private ServerOptions(Builder builder) {
            this.protocol = builder.protocol;
            this.host = builder.host;
            this.port = builder.port;
            this.https = builder.https;
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

        @JsonPOJOBuilder
        public static final class Builder {

            private String protocol;
            private String host;
            private Integer port;
            private HTTPsOptions https;

            public Builder withProtocol(String protocol) {
                this.protocol = protocol;
                return this;
            }

            public Builder withHost(String host) {
                this.host = host;
                return this;
            }

            public Builder withPort(Integer port) {
                this.port = port;
                return this;
            }

            public Builder withHttpsOptions(HTTPsOptions httpsOptions) {
                this.https = httpsOptions;
                return this;
            }

            public ServerOptions build() {
                return new ServerOptions(this);
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

        private HTTPsOptions(Builder builder) {
            this.verifyHostname = builder.verifyHostname;
            this.trustAll = builder.trustAll;
            this.trustStorePath = builder.trustStorePath;
            this.trustStorePassword = builder.trustStorePassword;
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

        @JsonPOJOBuilder
        public static final class Builder {

            private Boolean verifyHostname;
            private Boolean trustAll;
            private String trustStorePath;
            private String trustStorePassword;

            public Builder withVerifyHostname(Boolean verifyHostname) {
                this.verifyHostname = verifyHostname;
                return this;
            }

            public Builder withTrustAll(Boolean trustAll) {
                this.trustAll = trustAll;
                return this;
            }

            public Builder withTrustStorePath(String trustStorePath) {
                this.trustStorePath = trustStorePath;
                return this;
            }

            public Builder withTrustStorePassword(String trustStorePassword) {
                this.trustStorePassword = trustStorePassword;
                return this;
            }

            public HTTPsOptions build() {
                return new HTTPsOptions(this);
            }
        }
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private List<ServerOptions> servers;
        private Boolean verbose;

        public Builder withServers(List<ServerOptions> servers) {
            this.servers = servers;
            return this;
        }

        public Builder withVerbose(Boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public ProxyMiddlewareOptions build() {
            return new ProxyMiddlewareOptions(this);
        }
    }
}
