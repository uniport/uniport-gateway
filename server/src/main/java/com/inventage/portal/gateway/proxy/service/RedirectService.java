package com.inventage.portal.gateway.proxy.service;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

/**
 * Service for returning redirects responses to the browser.
 * Such a service can easily be used for redirecting to a default URL.
 */
public class RedirectService implements Service {

    private final JsonObject serviceConfig;

    public RedirectService(JsonObject serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    @Override
    public void handle(HttpServerRequest request) {
        request.headers();
        request.response().setStatusCode(HttpResponseStatus.FOUND.code()).putHeader("Location", serviceConfig.getString("destination")).end();
    }
}
