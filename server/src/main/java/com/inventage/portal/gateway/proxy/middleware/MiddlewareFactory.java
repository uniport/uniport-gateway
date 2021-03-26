package com.inventage.portal.gateway.proxy.middleware;

import java.util.Optional;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

/**
 * Service interface for providing middlewares. Implementations must add an entry with the fully
 * qualified class name into
 * META-INF/services/com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory
 */
public interface MiddlewareFactory {

    final Logger LOGGER = LoggerFactory.getLogger(MiddlewareFactory.class);

    String provides();

    Future<Middleware> create(Vertx vertx, Router router, JsonObject middlewareConfig);

    class Loader {
        public static MiddlewareFactory getFactory(String middlewareName) {
            LOGGER.trace("getFactory");
            LOGGER.debug("getFactory: middleware for '{}'", middlewareName);
            final Optional<MiddlewareFactory> middleware = ServiceLoader
                    .load(MiddlewareFactory.class).stream().map(ServiceLoader.Provider::get)
                    .filter(instance -> instance.provides().equals(middlewareName)).findFirst();
            if (middleware.isPresent()) {
                return middleware.get();
            }
            LOGGER.debug("getFactory: middleware factory not found for '{}'", middlewareName);
            return null;
        }
    }

}
