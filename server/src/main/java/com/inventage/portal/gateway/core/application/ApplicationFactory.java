package com.inventage.portal.gateway.core.application;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ApplicationFactory {

    /**
     * Used in the portal-gateway.json applications.provider field.
     *
     * @return normally the fully qualified class name
     */
    String provides();

    /**
     * @param vertx
     *            running instance by which an application can get their router
     * @param applicationConfig
     *            extract of the config for this application
     * @param env
     *            environment variables
     * @return new application instance
     */
    Application create(Vertx vertx, JsonObject entrypointConfig, JsonArray providerConfigs, JsonObject env);
}
