package com.inventage.portal.gateway.proxy.middleware.replacePathRegex;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class ReplacePathRegexMiddlewareFactory implements MiddlewareFactory {

  @Override
  public String provides() {
    return DynamicConfiguration.MIDDLEWARE_REPLACE_PATH_REGEX;
  }

  @Override
  public Future<Middleware> create(Vertx vertx, Router router, JsonObject middlewareConfig) {
    LOGGER.debug("create: Created '{}' middleware successfully", DynamicConfiguration.MIDDLEWARE_REPLACE_PATH_REGEX);
    return Future.succeededFuture(new ReplacePathRegexMiddleware(
        middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_REPLACE_PATH_REGEX_REGEX),
        middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_REPLACE_PATH_REGEX_REPLACEMENT)));
  }
}
