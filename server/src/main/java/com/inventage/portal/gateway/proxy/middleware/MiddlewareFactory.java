package com.inventage.portal.gateway.proxy.middleware;

import java.util.Optional;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public interface MiddlewareFactory {

    final Logger LOGGER = LoggerFactory.getLogger(MiddlewareFactory.class);

    String provides();

    Future<Middleware> create(Vertx vertx, Router router, JsonObject middlewareConfig);

    class Loader {
        public static MiddlewareFactory getFactory(String middlewareName) {
            LOGGER.debug("get middleware factory: for '{}'", middlewareName);
            final Optional<MiddlewareFactory> middleware = ServiceLoader
                    .load(MiddlewareFactory.class).stream().map(ServiceLoader.Provider::get)
                    .filter(instance -> instance.provides().equals(middlewareName)).findFirst();
            if (middleware.isPresent()) {
                return middleware.get();
            }
            LOGGER.debug("middleware factory not found for '{}'", middlewareName);
            return null;
        }
    }

}
