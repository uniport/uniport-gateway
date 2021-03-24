package com.inventage.portal.gateway.proxy.middleware.proxy.request.uri;

import java.util.Optional;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.json.JsonObject;

public interface UriMiddlewareFactory {

    final Logger LOGGER = LoggerFactory.getLogger(UriMiddlewareFactory.class);

    String provides();

    UriMiddleware create(JsonObject uriMiddlewareConfig);

    class Loader {
        public static UriMiddlewareFactory getFactory(String middlewareName) {
            LOGGER.debug("get URI middleware factory: for '{}'", middlewareName);
            final Optional<UriMiddlewareFactory> factory = ServiceLoader
                    .load(UriMiddlewareFactory.class).stream().map(ServiceLoader.Provider::get)
                    .filter(instance -> instance.provides().equals(middlewareName)).findFirst();
            if (factory.isPresent()) {
                return factory.get();
            }
            LOGGER.debug("URI middleware factory not found for '{}'", middlewareName);
            return null;
        }
    }

}
