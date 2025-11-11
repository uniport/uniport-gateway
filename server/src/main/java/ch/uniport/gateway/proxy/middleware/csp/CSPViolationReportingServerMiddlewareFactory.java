package ch.uniport.gateway.proxy.middleware.csp;

import ch.uniport.gateway.proxy.middleware.Middleware;
import ch.uniport.gateway.proxy.middleware.MiddlewareFactory;
import ch.uniport.gateway.proxy.middleware.MiddlewareOptionsModel;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * Factory for {@link CSPViolationReportingServerMiddleware}.
 */
public class CSPViolationReportingServerMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String TYPE = "cspViolationReportingServer";
    public static final String LOG_LEVEL = "logLevel";
    public static final List<String> LOG_LEVELS = List.of(
        Level.TRACE.toString(),
        Level.DEBUG.toString(),
        Level.INFO.toString(),
        Level.WARN.toString(),
        Level.ERROR.toString());

    private static final Logger LOGGER = LoggerFactory.getLogger(CSPViolationReportingServerMiddlewareFactory.class);

    @Override
    public String provides() {
        return TYPE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .requiredProperty(LOG_LEVEL, Schemas.enumSchema(LOG_LEVELS.toArray()))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.succeededFuture();
    }

    @Override
    public Class<CSPViolationReportingServerMiddlewareOptions> modelType() {
        return CSPViolationReportingServerMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(
        final Vertx vertx, final String name, final Router router,
        MiddlewareOptionsModel config
    ) {
        final CSPViolationReportingServerMiddlewareOptions options = castOptions(config, modelType());
        LOGGER.debug("Created '{}#{}' middleware successfully", TYPE, name);
        return Future.succeededFuture(
            new CSPViolationReportingServerMiddleware(name, options.getLogLevel()));
    }
}
