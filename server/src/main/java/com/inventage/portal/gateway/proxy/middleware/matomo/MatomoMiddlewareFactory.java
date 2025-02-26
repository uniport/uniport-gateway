package com.inventage.portal.gateway.proxy.middleware.matomo;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory.logDefaultIfNotConfigured;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
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
 * Factory for {@link MatomoMiddleware}.
 */
public class MatomoMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String MATOMO = "matomo";
    public static final String MATOMO_JWT_PATH_USERNAME = "pathUsername";
    public static final String MATOMO_JWT_PATH_EMAIL = "pathEmail";
    public static final String MATOMO_JWT_PATH_ROLES = "pathRoles";
    public static final String MATOMO_JWT_PATH_GROUP = "pathGroup";

    // defaults
    private static final String DEFAULT_JWT_PATH_USERNAME = "$.preferred_username";
    private static final String DEFAULT_JWT_PATH_EMAIL = "$.email";
    private static final String DEFAULT_JWT_PATH_ROLES = "$.resource_access.Analytics.roles";
    private static final String DEFAULT_JWT_PATH_GROUP = "$.tenant";

    private static final Logger LOGGER = LoggerFactory.getLogger(MatomoMiddlewareFactory.class);

    @Override
    public String provides() {
        return MATOMO;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .optionalProperty(MATOMO_JWT_PATH_USERNAME, Schemas.stringSchema()
                .with(Keywords.minLength(1))
                .defaultValue(DEFAULT_JWT_PATH_USERNAME))
            .optionalProperty(MATOMO_JWT_PATH_EMAIL, Schemas.stringSchema()
                .with(Keywords.minLength(1))
                .defaultValue(DEFAULT_JWT_PATH_EMAIL))
            .optionalProperty(MATOMO_JWT_PATH_ROLES, Schemas.stringSchema()
                .with(Keywords.minLength(1))
                .defaultValue(DEFAULT_JWT_PATH_ROLES))
            .optionalProperty(MATOMO_JWT_PATH_GROUP, Schemas.stringSchema()
                .with(Keywords.minLength(1))
                .defaultValue(DEFAULT_JWT_PATH_GROUP))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        logDefaultIfNotConfigured(LOGGER, options, MATOMO_JWT_PATH_USERNAME, DEFAULT_JWT_PATH_USERNAME);
        logDefaultIfNotConfigured(LOGGER, options, MATOMO_JWT_PATH_EMAIL, DEFAULT_JWT_PATH_EMAIL);
        logDefaultIfNotConfigured(LOGGER, options, MATOMO_JWT_PATH_ROLES, DEFAULT_JWT_PATH_ROLES);
        logDefaultIfNotConfigured(LOGGER, options, MATOMO_JWT_PATH_GROUP, DEFAULT_JWT_PATH_GROUP);
        return Future.succeededFuture();
    }

    @Override
    public Class<? extends GatewayMiddlewareOptions> modelType() {
        return MatomoMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.info("Created '{}' middleware successfully", MATOMO);

        final String jwtPathUsername = middlewareConfig.getString(MATOMO_JWT_PATH_USERNAME, DEFAULT_JWT_PATH_USERNAME);
        final String jwtPathEmail = middlewareConfig.getString(MATOMO_JWT_PATH_EMAIL, DEFAULT_JWT_PATH_EMAIL);
        final String jwtPathRoles = middlewareConfig.getString(MATOMO_JWT_PATH_ROLES, DEFAULT_JWT_PATH_ROLES);
        final String jwtPathGroup = middlewareConfig.getString(MATOMO_JWT_PATH_GROUP, DEFAULT_JWT_PATH_GROUP);

        return Future.succeededFuture(new MatomoMiddleware(name, jwtPathRoles, jwtPathGroup, jwtPathUsername, jwtPathEmail));
    }
}
