package com.inventage.portal.gateway.core.middleware;

import java.util.Optional;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public interface MiddlewareFactory {

    Logger LOGGER = LoggerFactory.getLogger(MiddlewareFactory.class);

    String provides();

    Middleware create(Vertx vertx, JsonObject middlewareConfig);

    class Loader {
        public static MiddlewareFactory getFactory(String middlewareName) {
            LOGGER.debug("get middleware factory: for '{}'", middlewareName);
            final Optional<MiddlewareFactory> middleware = ServiceLoader
                    .load(MiddlewareFactory.class).stream().map(ServiceLoader.Provider::get)
                    .filter(instance -> instance.provides().equals(middlewareName)).findFirst();
            if (middleware.isPresent()) {
                return middleware.get();
            } else {
                throw new IllegalStateException(
                        String.format("Middleware factory '%s' doesn't exist!", middlewareName));
            }
        }
    }

}
