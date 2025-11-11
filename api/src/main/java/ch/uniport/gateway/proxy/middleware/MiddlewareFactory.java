package ch.uniport.gateway.proxy.middleware;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
}
