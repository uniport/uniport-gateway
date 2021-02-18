package com.inventage.portal.gateway.proxy.request.header;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Every RequestHeaderMiddleware must have an implementation of this interface, which provides the instances.
 */
public interface RequestHeaderMiddlewareProvider {

    String provides();

    RequestHeaderMiddleware<RoutingContext, MultiMap> create(JsonObject headerMiddlewareConfig);


    class Loader {
        public static RequestHeaderMiddlewareProvider getProvider(String providerId) {
            final Optional<RequestHeaderMiddlewareProvider> provider = ServiceLoader.load(RequestHeaderMiddlewareProvider.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .filter(instance -> instance.provides().equals(providerId))
                    .findFirst();
            if (provider.isPresent()) {
                return provider.get();
            }
            else {
                throw new IllegalStateException(String.format("Header middleware provider '%s' doesn't exist!", providerId));
            }
        }
    }

}
