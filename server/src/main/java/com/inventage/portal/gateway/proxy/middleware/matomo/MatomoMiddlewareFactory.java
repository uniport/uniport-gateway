package com.inventage.portal.gateway.proxy.middleware.matomo;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatomoMiddlewareFactory implements MiddlewareFactory {
    private static final String DEFAULT_JWT_PATH_ROLES = "$.resource_access.Analytics.roles";
    private static final String DEFAULT_JWT_PATH_GROUP = "$.tenant";
    private static final String DEFAULT_JWT_PATH_USERNAME = "$.preferred_username";
    private static final String DEFAULT_JWT_PATH_EMAIL = "$.email";

    private static final Logger LOGGER = LoggerFactory.getLogger(MatomoMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_MATOMO;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.info("Created '{}' middleware successfully", DynamicConfiguration.MIDDLEWARE_MATOMO);

        final String jwtPathRoles = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_MATOMO_JWT_PATH_ROLES, DEFAULT_JWT_PATH_ROLES);
        final String jwtPathGroup = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_MATOMO_JWT_PATH_GROUP, DEFAULT_JWT_PATH_GROUP);
        final String jwtPathUsername = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_MATOMO_JWT_PATH_USERNAME, DEFAULT_JWT_PATH_USERNAME);
        final String jwtPathEmail = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_MATOMO_JWT_PATH_EMAIL, DEFAULT_JWT_PATH_EMAIL);

        return Future.succeededFuture(new MatomoMiddleware(name, jwtPathRoles, jwtPathGroup, jwtPathUsername, jwtPathEmail));
    }
}
