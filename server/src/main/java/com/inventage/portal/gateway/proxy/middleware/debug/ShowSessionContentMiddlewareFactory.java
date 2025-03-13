package com.inventage.portal.gateway.proxy.middleware.debug;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link ShowSessionContentMiddleware}.
 */
public class ShowSessionContentMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShowSessionContentMiddlewareFactory.class);

    // schema
    public static final String TYPE = "_session_";

    // defaults
    private static final String INSTANCE_NAME_PROPERTY = "PORTAL_GATEWAY_INSTANCE_NAME";
    private static final String DEFAULT_INSTANCE_NAME = "unknown";

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
    public Class<ShowSessionContentMiddlewareOptions> modelType() {
        return ShowSessionContentMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, GatewayMiddlewareOptions config) {
        final String instanceName = System.getenv().getOrDefault(INSTANCE_NAME_PROPERTY, DEFAULT_INSTANCE_NAME); // TODO move into Runtime
        LOGGER.debug("Created '{}' middleware successfully", TYPE);
        return Future.succeededFuture(
            new ShowSessionContentMiddleware(name, instanceName));
    }

}
