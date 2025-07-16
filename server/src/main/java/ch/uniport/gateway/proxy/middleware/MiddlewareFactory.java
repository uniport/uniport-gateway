package ch.uniport.gateway.proxy.middleware;

import ch.uniport.gateway.proxy.config.model.MiddlewareOptionsModel;
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
 * Service interface for providing middlewares. Implementations must add an
 * entry with the fully
 * qualified class name into
 * META-INF/services/ch.uniport.gateway.proxy.middleware.MiddlewareFactory
 */
public interface MiddlewareFactory {

    Logger LOGGER = LoggerFactory.getLogger(MiddlewareFactory.class);

    String provides();

    ObjectSchemaBuilder optionsSchema();

    Future<Void> validate(JsonObject options);

    Class<? extends MiddlewareOptionsModel> modelType();

    Future<Middleware> create(Vertx vertx, String name, Router router, MiddlewareOptionsModel config);

    @SuppressWarnings("unchecked")
    default <T extends MiddlewareOptionsModel> T castOptions(MiddlewareOptionsModel options, Class<T> clazz) {
        if (options == null) {
            return null;
        }
        if (!modelType().isInstance(options)) {
            throw new IllegalStateException(
                String.format("unexpected middleware options type: '%s'", options.getClass()));
        }
        return (T) options;
    }

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
