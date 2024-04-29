package com.inventage.portal.gateway.proxy.middleware.log;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class RequestResponseLoggerMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponseLoggerMiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_REQUEST_RESPONSE_LOGGER;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        final String requestFilterPattern = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_REQUEST_RESPONSE_LOGGER_FILTER_REGEX);
        List<String> contentTypesToLog = null;
        if (middlewareConfig.getJsonArray(DynamicConfiguration.MIDDLEWARE_REQUEST_RESPONSE_LOGGER_CONTENT_TYPES) != null) {
            contentTypesToLog = middlewareConfig.getJsonArray(DynamicConfiguration.MIDDLEWARE_REQUEST_RESPONSE_LOGGER_CONTENT_TYPES).getList();
        }
        final Boolean loggingRequestEnabled = middlewareConfig.getBoolean(DynamicConfiguration.MIDDLEWARE_REQUEST_RESPONSE_LOGGER_LOGGING_REQUEST_ENABLED);
        final Boolean loggingResponseEnabled = middlewareConfig.getBoolean(DynamicConfiguration.MIDDLEWARE_REQUEST_RESPONSE_LOGGER_LOGGING_RESPONSE_ENABLED);

        LOGGER.debug("Created '{}' middleware successfully", DynamicConfiguration.MIDDLEWARE_REQUEST_RESPONSE_LOGGER);
        return Future.succeededFuture(new RequestResponseLoggerMiddleware(name, requestFilterPattern, contentTypesToLog, loggingRequestEnabled, loggingResponseEnabled));
    }

}
