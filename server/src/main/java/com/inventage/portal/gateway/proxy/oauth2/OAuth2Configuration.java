package com.inventage.portal.gateway.proxy.oauth2;

import io.vertx.ext.web.Route;

public class OAuth2Configuration {

    private final String clientId;
    private final String clientSecret;
    private final String discoveryUrl;
    private Route callback;

    public OAuth2Configuration(String clientId, String clientSecret, String discoveryUrl,
            Route callback) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.discoveryUrl = discoveryUrl;
        this.callback = callback;
    }

    public String clientId() {
        return clientId;
    }

    public String clientSecret() {
        return clientSecret;
    }

    public String discoveryUrl() {
        return discoveryUrl;
    }

    public Route callback() {
        return callback;
    }
}
