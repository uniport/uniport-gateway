package com.inventage.portal.gateway.proxy.response.header;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Every ResponseHeaderMiddleware must have an implementation of this interface, which provides the instances.
 */
public interface ResponseHeaderMiddlewareProvider {

    String provides();

    ResponseHeaderMiddleware<RoutingContext, MultiMap> create(JsonObject headerMiddlewareConfig);


    class Loader {
        public static ResponseHeaderMiddlewareProvider getProvider(String providerId) {
            final Optional<ResponseHeaderMiddlewareProvider> provider = ServiceLoader.load(ResponseHeaderMiddlewareProvider.class).stream()
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
