package com.inventage.portal.gateway.proxy.middleware;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service interface for providing middlewares. Implementations must add an entry with the fully
 * qualified class name into
 * META-INF/services/com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory
 */
public interface MiddlewareFactory {

    Logger LOGGER = LoggerFactory.getLogger(MiddlewareFactory.class);

    // schema keywords
    String KEYWORD_ENUM = "enum";
    String KEYWORD_STRING_MIN_LENGTH = "minLength";
    String KEYWORD_INT_MIN = "minimum";
    String KEYWORD_INT_MAX = "maximum";
    int NON_EMPTY_STRING_MIN_LENGTH = 1;
    int INT_MIN = 0;
    int HTTP_STATUS_CODE_MIN = 100;
    int HTTP_STATUS_CODE_MAX = 599;
    String KEYWORD_TYPE = "type";
    String KEYWORD_PATTERN = "pattern";
    String INT_TYPE = "integer";
    String STRING_TYPE = "string";
    String ENV_VARIABLE_PATTERN = "^\\$\\{.*\\}$";

    String provides();

    ObjectSchemaBuilder optionsSchema();

    Future<Void> validate(JsonObject options);

    Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig);

    class Loader {
        public static List<MiddlewareFactory> listFactories() {
            return ServiceLoader.load(MiddlewareFactory.class).stream()
                .map(ServiceLoader.Provider::get)
                .toList();
        }

        public static Optional<MiddlewareFactory> getFactory(String middlewareName) {
            LOGGER.debug("Middleware factory for '{}'", middlewareName);
            return listFactories().stream()
                .filter(instance -> instance.provides().equals(middlewareName))
                .findFirst();
        }
    }

}
