package com.inventage.portal.gateway.proxy.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value.Check;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@ModelStyle
@JsonDeserialize(builder = ServiceModel.Builder.class)
public abstract class AbstractServiceModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceModel.class);

    // defaults
    public static final String DEFAULT_SERVICE_SERVER_PROTOCOL = "http";
    public static final boolean DEFAULT_SERVICE_SERVER_HTTPS_OPTIONS_VERIFY_HOSTNAME = false;
    public static final boolean DEFAULT_SERVICE_SERVER_HTTPS_OPTIONS_TRUST_ALL = true;
    public static final String DEFAULT_SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PATH = null;
    public static final String DEFAULT_SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PASSWORD = null;
    public static final boolean DEFAULT_SERVICE_VERBOSE = false;

    @Check
    protected void validate() {
        Preconditions.checkState(!getServers().isEmpty(), "'getServers' must have at least one element");
    }

    @JsonProperty(DynamicConfiguration.SERVICE_NAME)
    public abstract String getName();

    @JsonProperty(DynamicConfiguration.SERVICE_SERVERS)
    public abstract List<ServerOptions> getServers();

    @Default
    @JsonProperty(DynamicConfiguration.SERVICE_VERBOSE)
    public boolean isVerbose() {
        logDefault(LOGGER, DynamicConfiguration.SERVICE_VERBOSE, DEFAULT_SERVICE_VERBOSE);
        return DEFAULT_SERVICE_VERBOSE;
    }

    @Immutable
    @ModelStyle
    @JsonDeserialize(builder = ServerOptions.Builder.class)
    public abstract static class AbstractServerOptions implements MiddlewareOptionsModel {

        @Default
        @JsonProperty(DynamicConfiguration.SERVICE_SERVER_PROTOCOL)
        public String getProtocol() {
            return DEFAULT_SERVICE_SERVER_PROTOCOL;
        }

        @JsonProperty(DynamicConfiguration.SERVICE_SERVER_HOST)
        public abstract String getHost();

        @JsonProperty(DynamicConfiguration.SERVICE_SERVER_PORT)
        public abstract int getPort();

        @Default
        @JsonProperty(DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS)
        public HTTPsOptions getHTTPs() {
            logDefault(LOGGER, DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS);
            return HTTPsOptions.builder().build();
        }
    }

    @Immutable
    @ModelStyle
    @JsonDeserialize(builder = HTTPsOptions.Builder.class)
    public abstract static class AbstractHTTPsOptions implements MiddlewareOptionsModel {

        @Default
        @JsonProperty(DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS_VERIFY_HOSTNAME)
        public boolean verifyHostname() {
            logDefault(LOGGER, DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS_VERIFY_HOSTNAME,
                DEFAULT_SERVICE_SERVER_HTTPS_OPTIONS_VERIFY_HOSTNAME);
            return DEFAULT_SERVICE_SERVER_HTTPS_OPTIONS_VERIFY_HOSTNAME;
        }

        @Default
        @JsonProperty(DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_ALL)
        public boolean trustAll() {
            logDefault(LOGGER, DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_ALL,
                DEFAULT_SERVICE_SERVER_HTTPS_OPTIONS_TRUST_ALL);
            return DEFAULT_SERVICE_SERVER_HTTPS_OPTIONS_TRUST_ALL;
        }

        @Default
        @Nullable
        @JsonProperty(DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PATH)
        public String getTrustStorePath() {
            logDefault(LOGGER, DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PATH,
                DEFAULT_SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PATH);
            return DEFAULT_SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PATH;
        }

        @Default
        @Nullable
        @JsonProperty(DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PASSWORD)
        public String getTrustStorePassword() {
            logDefault(LOGGER, DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PASSWORD,
                DEFAULT_SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PASSWORD);
            return DEFAULT_SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PASSWORD;
        }
    }

    private void logDefault(Logger logger, String key, Object defaultValue) {
        logger.debug("'{}' not configured. Using default value: '{}'", key, defaultValue);
    }
}