package com.inventage.portal.gateway.proxy.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GatewayServiceServerHttpOptions {

    @JsonProperty(DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS_VERIFY_HOSTNAME)
    private Boolean verifyHostname;

    @JsonProperty(DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_ALL)
    private Boolean trustAll;

    private String trustStorePath;
    private String trustStorePassword;

    public GatewayServiceServerHttpOptions() {
    }

    public GatewayServiceServerHttpOptions(GatewayServiceServerHttpOptions other) {
        verifyHostname = other.verifyHostname;
        trustAll = other.trustAll;
        trustStorePath = other.trustStorePath;
        trustStorePassword = other.trustStorePassword;
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

}
