package ch.uniport.gateway.proxy.middleware.controlapi;

import ch.uniport.gateway.proxy.config.model.MiddlewareOptionsModel;
import ch.uniport.gateway.proxy.middleware.Middleware;
import ch.uniport.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.json.schema.common.dsl.Keywords;
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
    public static final String TYPE = "controlApi";
    public static final String SESSION_RESET_URL = "iamSessionResetUrl";
    public static final String ACTION = "action";
    public static final String ACTION_SESSION_TERMINATE = "SESSION_TERMINATE";
    public static final String ACTION_SESSION_RESET = "SESSION_RESET";

    public static final List<String> ACTIONS = List.of(
        ACTION_SESSION_TERMINATE,
        ACTION_SESSION_RESET);

    private static final Logger LOGGER = LoggerFactory.getLogger(ControlApiMiddlewareFactory.class);

    // reusable instance
    private WebClient webClient;

    @Override
    public String provides() {
        return TYPE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .requiredProperty(ACTION, Schemas.enumSchema(ACTIONS.toArray()))
            .optionalProperty(SESSION_RESET_URL, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.succeededFuture();
    }

    @Override
    public Class<ControlApiMiddlewareOptions> modelType() {
        return ControlApiMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, MiddlewareOptionsModel config) {
        final ControlApiMiddlewareOptions options = castOptions(config, modelType());

        if (webClient == null) {
            webClient = WebClient.create(vertx);
        }

        LOGGER.debug("Created '{}#{}' middleware successfully", TYPE, name);
        return Future.succeededFuture(
            new ControlApiMiddleware(vertx, name, options.getAction(), options.getSessionResetURL(), webClient));
    }

}
