package com.inventage.portal.gateway.proxy.middleware.controlapi;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory.logDefaultIfNotConfigured;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.json.schema.common.dsl.Keywords;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link ControlApiMiddleware}.
 */
public class ControlApiMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String CONTROL_API = "controlApi";
    public static final String CONTROL_API_SESSION_RESET_URL = "iamSessionResetUrl";
    public static final String CONTROL_API_ACTION = "action";
    public static final String CONTROL_API_ACTION_SESSION_TERMINATE = "SESSION_TERMINATE";
    public static final String CONTROL_API_ACTION_SESSION_RESET = "SESSION_RESET";

    public static final String[] MIDDLEWARE_CONTROL_API_ACTIONS = new String[] {
        CONTROL_API_ACTION_SESSION_TERMINATE,
        CONTROL_API_ACTION_SESSION_RESET
    };

    // defaults
    public static final String DEFAULT_RESET_URL = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(ControlApiMiddlewareFactory.class);

    // reusable instance
    private WebClient webClient;

    @Override
    public String provides() {
        return CONTROL_API;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .requiredProperty(CONTROL_API_ACTION, Schemas.enumSchema((Object[]) MIDDLEWARE_CONTROL_API_ACTIONS))
            .optionalProperty(CONTROL_API_SESSION_RESET_URL, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        logDefaultIfNotConfigured(LOGGER, options, CONTROL_API_SESSION_RESET_URL, null);

        return Future.succeededFuture();
    }

    @Override
    public Class<? extends GatewayMiddlewareOptions> modelType() {
        return ControlApiMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        if (webClient == null) {
            webClient = WebClient.create(vertx);
        }

        final String action = middlewareConfig.getString(CONTROL_API_ACTION);
        final String iamSessionResetURI = middlewareConfig.getString(CONTROL_API_SESSION_RESET_URL, DEFAULT_RESET_URL);

        LOGGER.debug("Created '{}' middleware successfully", CONTROL_API);
        return Future.succeededFuture(new ControlApiMiddleware(vertx, name, ControlApiAction.valueOf(action), iamSessionResetURI, webClient));
    }

}
