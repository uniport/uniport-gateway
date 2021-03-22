package com.inventage.portal.gateway.core.middleware.proxy.request.uri;

import java.util.Optional;
import java.util.ServiceLoader;
import com.inventage.portal.gateway.core.middleware.MiddlewareFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.json.JsonObject;

public interface UriMiddlewareFactory {

    Logger LOGGER = LoggerFactory.getLogger(MiddlewareFactory.class);

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
