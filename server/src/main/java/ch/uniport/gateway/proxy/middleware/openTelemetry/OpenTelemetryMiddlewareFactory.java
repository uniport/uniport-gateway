package ch.uniport.gateway.proxy.middleware.openTelemetry;

import ch.uniport.gateway.proxy.middleware.Middleware;
import ch.uniport.gateway.proxy.middleware.MiddlewareFactory;
import ch.uniport.gateway.proxy.middleware.MiddlewareOptionsModel;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link OpenTelemetryMiddleware}.
 */
public class OpenTelemetryMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String TYPE = "openTelemetry";

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenTelemetryMiddlewareFactory.class);

    @Override
    public String provides() {
        return TYPE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.succeededFuture();
    }

    @Override
    public Class<OpenTelemetryMiddlewareOptions> modelType() {
        return OpenTelemetryMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, MiddlewareOptionsModel config) {
        LOGGER.debug("Created '{}#{}' middleware successfully", TYPE, name);
        return Future.succeededFuture(
            new OpenTelemetryMiddleware(name));
    }
}
