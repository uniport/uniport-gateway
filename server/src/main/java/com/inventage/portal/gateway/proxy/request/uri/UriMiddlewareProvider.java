package com.inventage.portal.gateway.proxy.request.uri;

import io.vertx.core.json.JsonObject;

import java.util.Optional;
import java.util.ServiceLoader;

public interface UriMiddlewareProvider {

    String provides();

    UriMiddleware create(JsonObject uriMiddlewareConfig);

    class Loader {
        public static UriMiddlewareProvider getProvider(String providerId) {
            final Optional<UriMiddlewareProvider> provider = ServiceLoader
                    .load(UriMiddlewareProvider.class).stream().map(ServiceLoader.Provider::get)
                    .filter(instance -> instance.provides().equals(providerId)).findFirst();
            if (provider.isPresent()) {
                return provider.get();
            } else {
                throw new IllegalStateException(
                        String.format("Uri middleware provider '%s' doesn't exist!", providerId));
            }
        }
    }

}
