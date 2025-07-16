package ch.uniport.gateway.proxy.middleware.replacedSessionCookieDetection;

import ch.uniport.gateway.proxy.config.model.MiddlewareOptionsModel;
import ch.uniport.gateway.proxy.middleware.Middleware;
import ch.uniport.gateway.proxy.middleware.MiddlewareFactory;
import ch.uniport.gateway.proxy.middleware.sessionBag.CookieUtil;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.Keywords;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link ReplacedSessionCookieDetectionMiddleware}.
 */
public class ReplacedSessionCookieDetectionMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String TYPE = "replacedSessionCookieDetection";
    public static final String DETECTION_COOKIE_NAME = "name";
    public static final String SESSION_COOKIE_NAME = "sessionCookieName";
    public static final String WAIT_BEFORE_RETRY_MS = "waitTimeInMillisecond";
    public static final String MAX_REDIRECT_RETRIES = "maxRedirectRetries";

    private static final Logger LOGGER = LoggerFactory.getLogger(ReplacedSessionCookieDetectionMiddlewareFactory.class);

    @Override
    public String provides() {
        return TYPE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .optionalProperty(DETECTION_COOKIE_NAME, Schemas.stringSchema()
                .with(Keywords.minLength(1))
                .defaultValue(
                    AbstractReplacedSessionCookieDetectionMiddlewareOptions.DEFAULT_DETECTION_COOKIE_NAME))
            .optionalProperty(SESSION_COOKIE_NAME, Schemas.stringSchema()
                .with(Keywords.minLength(1))
                .defaultValue(
                    AbstractReplacedSessionCookieDetectionMiddlewareOptions.DEFAULT_SESSION_COOKIE_NAME))
            .optionalProperty(WAIT_BEFORE_RETRY_MS, Schemas.intSchema()
                .with(io.vertx.json.schema.draft7.dsl.Keywords.minimum(0))
                .defaultValue(
                    AbstractReplacedSessionCookieDetectionMiddlewareOptions.DEFAULT_WAIT_BEFORE_RETRY_MS))
            .optionalProperty(MAX_REDIRECT_RETRIES, Schemas.intSchema()
                .with(io.vertx.json.schema.draft7.dsl.Keywords.minimum(0))
                .defaultValue(
                    AbstractReplacedSessionCookieDetectionMiddlewareOptions.DEFAULT_MAX_REDIRECT_RETRIES))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        if (options.containsKey(DETECTION_COOKIE_NAME)
            && !CookieUtil.isValidCookieName(LOGGER, options.getString(DETECTION_COOKIE_NAME))) {
            return Future.failedFuture("cookie name is invalid");
        }
        return Future.succeededFuture();
    }

    @Override
    public Class<ReplacedSessionCookieDetectionMiddlewareOptions> modelType() {
        return ReplacedSessionCookieDetectionMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, MiddlewareOptionsModel config) {
        final ReplacedSessionCookieDetectionMiddlewareOptions options = castOptions(config, modelType());
        LOGGER.debug("Created '{}#{}' middleware successfully", TYPE, name);
        return Future.succeededFuture(
            new ReplacedSessionCookieDetectionMiddleware(
                name,
                options.getCookieName(),
                options.getSessionCookieName(),
                options.getWaitBeforeRetryMs(),
                options.getMaxRetries()));
    }
}
