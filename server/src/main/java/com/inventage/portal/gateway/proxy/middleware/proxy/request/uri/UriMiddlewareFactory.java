package com.inventage.portal.gateway.proxy.middleware.proxy.request.uri;

import java.util.Optional;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.json.JsonObject;

/**
 * Service interface for providing URI middlewares. Implementations must add an entry with the fully
 * qualified class name into
 * META-INF/services/com.inventage.portal.gateway.proxy.middleware.proxy.request.uri.UriMiddlewareFactory
 */
public interface UriMiddlewareFactory {

    final Logger LOGGER = LoggerFactory.getLogger(UriMiddlewareFactory.class);

    String provides();

    UriMiddleware create(JsonObject uriMiddlewareConfig);

    class Loader {
        public static UriMiddlewareFactory getFactory(String middlewareName) {
            LOGGER.debug("getFactory: URI middleware factory '{}'", middlewareName);
            final Optional<UriMiddlewareFactory> factory = ServiceLoader
                    .load(UriMiddlewareFactory.class).stream().map(ServiceLoader.Provider::get)
                    .filter(instance -> instance.provides().equals(middlewareName)).findFirst();
            if (factory.isPresent()) {
                return factory.get();
            }
            return null;
        }
    }

}
