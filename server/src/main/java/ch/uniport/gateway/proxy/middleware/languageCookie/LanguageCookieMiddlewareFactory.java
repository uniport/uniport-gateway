package ch.uniport.gateway.proxy.middleware.languageCookie;

import ch.uniport.gateway.proxy.middleware.Middleware;
import ch.uniport.gateway.proxy.middleware.MiddlewareFactory;
import ch.uniport.gateway.proxy.middleware.MiddlewareOptionsModel;
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
 * Factory for {@link LanguageCookieMiddleware}.
 */
public class LanguageCookieMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String TYPE = "languageCookie";
    public static final String LANGUAGE_COOKIE_NAME = "name";

    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageCookieMiddlewareFactory.class);

    @Override
    public String provides() {
        return TYPE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .optionalProperty(LANGUAGE_COOKIE_NAME, Schemas.stringSchema()
                .with(Keywords.minLength(1))
                .defaultValue(AbstractLanguageCookieMiddlewareOptions.DEFAULT_LANGUAGE_COOKIE_NAME))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        if (options.containsKey(LANGUAGE_COOKIE_NAME)
            && !CookieUtil.isValidCookieName(LOGGER, options.getString(LANGUAGE_COOKIE_NAME))) {
            return Future.failedFuture("cookie name is invalid");
        }
        return Future.succeededFuture();
    }

    @Override
    public Class<LanguageCookieMiddlewareOptions> modelType() {
        return LanguageCookieMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, MiddlewareOptionsModel config) {
        final LanguageCookieMiddlewareOptions options = castOptions(config, modelType());
        LOGGER.debug("Created '{}#{}' middleware successfully", TYPE, name);
        return Future.succeededFuture(
            new LanguageCookieMiddleware(name, options.getCookieName()));
    }
}
