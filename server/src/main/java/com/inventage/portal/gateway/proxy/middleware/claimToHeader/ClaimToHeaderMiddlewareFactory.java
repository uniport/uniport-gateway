package com.inventage.portal.gateway.proxy.middleware.claimToHeader;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link ClaimToHeaderMiddleware}.
 */
public class ClaimToHeaderMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String MIDDLEWARE_CLAIM_TO_HEADER = "claimToHeader";
    public static final String MIDDLEWARE_CLAIM_TO_HEADER_PATH = "claimPath";
    public static final String MIDDLEWARE_CLAIM_TO_HEADER_NAME = "headerName";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClaimToHeaderMiddlewareFactory.class);

    @Override
    public String provides() {
        return MIDDLEWARE_CLAIM_TO_HEADER;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .property(MIDDLEWARE_CLAIM_TO_HEADER_PATH, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
            .property(MIDDLEWARE_CLAIM_TO_HEADER_NAME, Schemas.stringSchema()
                .withKeyword(KEYWORD_STRING_MIN_LENGTH, NON_EMPTY_STRING_MIN_LENGTH))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        final String path = options.getString(MIDDLEWARE_CLAIM_TO_HEADER_PATH);
        if (path == null || path.length() == 0) {
            return Future.failedFuture("Claim path not defined");
        }
        final String name = options.getString(MIDDLEWARE_CLAIM_TO_HEADER_NAME);
        if (name == null || name.length() == 0) {
            return Future.failedFuture("Header name not defined");
        }

        return Future.succeededFuture();
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        LOGGER.debug("Created '{}' middleware successfully", MIDDLEWARE_CLAIM_TO_HEADER);
        return Future.succeededFuture(new ClaimToHeaderMiddleware(name,
            middlewareConfig.getString(MIDDLEWARE_CLAIM_TO_HEADER_PATH),
            middlewareConfig.getString(MIDDLEWARE_CLAIM_TO_HEADER_NAME)));
    }
}
