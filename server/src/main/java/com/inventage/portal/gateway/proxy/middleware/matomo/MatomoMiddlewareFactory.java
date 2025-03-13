package com.inventage.portal.gateway.proxy.middleware.matomo;

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
    public static final String TYPE = "matomo";
    public static final String JWT_PATH_USERNAME = "pathUsername";
    public static final String JWT_PATH_EMAIL = "pathEmail";
    public static final String JWT_PATH_ROLES = "pathRoles";
    public static final String JWT_PATH_GROUP = "pathGroup";

    // defaults
    public static final String DEFAULT_JWT_PATH_USERNAME = "$.preferred_username";
    public static final String DEFAULT_JWT_PATH_EMAIL = "$.email";
    public static final String DEFAULT_JWT_PATH_ROLES = "$.resource_access.Analytics.roles";
    public static final String DEFAULT_JWT_PATH_GROUP = "$.tenant";

    private static final Logger LOGGER = LoggerFactory.getLogger(MatomoMiddlewareFactory.class);

    @Override
    public String provides() {
        return TYPE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .optionalProperty(JWT_PATH_USERNAME, Schemas.stringSchema()
                .with(Keywords.minLength(1))
                .defaultValue(DEFAULT_JWT_PATH_USERNAME))
            .optionalProperty(JWT_PATH_EMAIL, Schemas.stringSchema()
                .with(Keywords.minLength(1))
                .defaultValue(DEFAULT_JWT_PATH_EMAIL))
            .optionalProperty(JWT_PATH_ROLES, Schemas.stringSchema()
                .with(Keywords.minLength(1))
                .defaultValue(DEFAULT_JWT_PATH_ROLES))
            .optionalProperty(JWT_PATH_GROUP, Schemas.stringSchema()
                .with(Keywords.minLength(1))
                .defaultValue(DEFAULT_JWT_PATH_GROUP))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.succeededFuture();
    }

    @Override
    public Class<MatomoMiddlewareOptions> modelType() {
        return MatomoMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, GatewayMiddlewareOptions config) {
        final MatomoMiddlewareOptions options = castOptions(config, modelType());
        LOGGER.info("Created '{}' middleware successfully", TYPE);
        return Future.succeededFuture(
            new MatomoMiddleware(name, options.getJWTPathRoles(), options.getJWTPathGroup(), options.getJWTPathUsername(), options.getJWTPathEMail()));
    }
}
