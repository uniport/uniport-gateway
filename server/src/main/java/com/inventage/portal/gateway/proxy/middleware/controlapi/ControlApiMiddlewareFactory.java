package com.inventage.portal.gateway.proxy.middleware.controlapi;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link ControlApiMiddleware}.
 */
public class ControlApiMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String MIDDLEWARE_CONTROL_API = "controlApi";
    public static final String MIDDLEWARE_CONTROL_API_SESSION_RESET_URL = "iamSessionResetUrl";
    public static final String MIDDLEWARE_CONTROL_API_ACTION = "action";
    public static final String MIDDLEWARE_CONTROL_API_ACTION_SESSION_TERMINATE = "SESSION_TERMINATE";
    public static final String MIDDLEWARE_CONTROL_API_ACTION_SESSION_RESET = "SESSION_RESET";

    public static final List<String> MIDDLEWARE_CONTROL_API_ACTIONS = List.of(
        MIDDLEWARE_CONTROL_API_ACTION_SESSION_TERMINATE,
        MIDDLEWARE_CONTROL_API_ACTION_SESSION_RESET);

    private static final Logger LOGGER = LoggerFactory.getLogger(ControlApiMiddlewareFactory.class);

    // reusable instance
    private WebClient webClient;

    @Override
    public String provides() {
        return MIDDLEWARE_CONTROL_API;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .property(MIDDLEWARE_CONTROL_API_ACTION, Schemas.stringSchema()
                .withKeyword(KEYWORD_ENUM, JsonArray.of(MIDDLEWARE_CONTROL_API_ACTIONS.toArray())))
            .optionalProperty(MIDDLEWARE_CONTROL_API_SESSION_RESET_URL, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        final String action = options.getString(MIDDLEWARE_CONTROL_API_ACTION);
        if (action == null) {
            return Future.failedFuture("No control api action defined");
        }

        if (!MIDDLEWARE_CONTROL_API_ACTIONS.contains(action)) {
            return Future.failedFuture("Not supported control api action defined.");
        }

        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("Created '{}' middleware successfully", MIDDLEWARE_CONTROL_API);

        if (webClient == null) {
            webClient = WebClient.create(vertx);
        }

        final String action = middlewareConfig.getString(MIDDLEWARE_CONTROL_API_ACTION);
        final String iamSessionResetURI = middlewareConfig.getString(MIDDLEWARE_CONTROL_API_SESSION_RESET_URL);

        return Future.succeededFuture(new ControlApiMiddleware(vertx, name, ControlApiAction.valueOf(action), iamSessionResetURI, webClient));
    }

}
