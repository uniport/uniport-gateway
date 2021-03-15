package com.inventage.portal.gateway.proxy.service;

import com.inventage.portal.gateway.core.PortalGatewayVerticle;
import com.inventage.portal.gateway.proxy.request.header.RequestHeaderMiddlewareProvider;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.ServiceLoader;

public interface ServiceProvider {

    Logger LOGGER = LoggerFactory.getLogger(ServiceProvider.class);

    String provides();

    Service create(JsonObject serviceConfig, JsonObject globalConfig, Vertx vertx);

    class Loader {
        public static ServiceProvider getProvider(String providerId) {
            LOGGER.debug("getProvider: for '{}'", providerId);
            final Optional<ServiceProvider> provider = ServiceLoader.load(ServiceProvider.class)
                    .stream().map(ServiceLoader.Provider::get)
                    .filter(instance -> instance.provides().equals(providerId)).findFirst();
            if (provider.isPresent()) {
                return provider.get();
            } else {
                throw new IllegalStateException(
                        String.format("Service provider '%s' doesn't exist!", providerId));
            }
        }
    }
}
