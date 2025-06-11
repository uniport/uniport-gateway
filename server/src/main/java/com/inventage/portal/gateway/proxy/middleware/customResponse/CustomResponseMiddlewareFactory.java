package com.inventage.portal.gateway.proxy.middleware.customResponse;

import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.Keywords;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link CustomResponseMiddleware}.
 */
public class CustomResponseMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String TYPE = "customResponse";
    public static final String CONTENT = "content";
    public static final String STATUS_CODE = "statusCode";
    public static final String HEADERS = "headers";

    private static final int HTTP_STATUS_CODE_MIN = 100;
    private static final int HTTP_STATUS_CODE_MAX = 599;

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomResponseMiddlewareFactory.class);

    @Override
    public String provides() {
        return TYPE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .requiredProperty(STATUS_CODE, Schemas.intSchema()
                .with(io.vertx.json.schema.draft7.dsl.Keywords.minimum(HTTP_STATUS_CODE_MIN))
                .with(io.vertx.json.schema.draft7.dsl.Keywords.maximum(HTTP_STATUS_CODE_MAX)))
            .requiredProperty(CONTENT, Schemas.stringSchema())
            .optionalProperty(HEADERS, Schemas.objectSchema()
                .additionalProperties(Schemas.stringSchema() // TODO technically this should be an array to allow multi header values
                    .with(Keywords.minLength(1))))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.succeededFuture();
    }

    @Override
    public Class<CustomResponseMiddlewareOptions> modelType() {
        return CustomResponseMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, MiddlewareOptionsModel config) {
        final CustomResponseMiddlewareOptions options = castOptions(config, modelType());
        final MultiMap headers = HeadersMultiMap.httpHeaders().addAll(options.getHeaders());
        LOGGER.debug("Created '{}#{}' middleware successfully", TYPE, name);
        return Future.succeededFuture(
            new CustomResponseMiddleware(name, options.getContent(), options.getStatusCode(), headers));
    }
}
