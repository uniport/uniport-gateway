package com.inventage.portal.gateway.proxy.router;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.proxy.ProxyMiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.proxy.request.uri.UriMiddleware;
import com.inventage.portal.gateway.proxy.middleware.proxy.request.uri.UriMiddlewareFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

public class RouterFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(RouterFactory.class);

    private Vertx vertx;

    public RouterFactory(Vertx vertx) {
        this.vertx = vertx;
    }

    public void createRouter(JsonObject dynamicConfig, final Handler<AsyncResult<Router>> handler) {
        Router router = Router.router(this.vertx);

        JsonObject httpConfig = dynamicConfig.getJsonObject(DynamicConfiguration.HTTP);

        JsonArray routers = httpConfig.getJsonArray(DynamicConfiguration.ROUTERS);
        JsonArray middlwares = httpConfig.getJsonArray(DynamicConfiguration.MIDDLEWARES);
        JsonArray services = httpConfig.getJsonArray(DynamicConfiguration.SERVICES);

        for (int i = 0; i < routers.size(); i++) {
            JsonObject routerConfig = routers.getJsonObject(i);

            String routerName = routerConfig.getString(DynamicConfiguration.ROUTER_NAME);

            String rule = routerConfig.getString(DynamicConfiguration.ROUTER_RULE);
            RoutingRule routingRule = parseRule(this.vertx, rule);
            if (routingRule == null) {
                handler.handle(Future.failedFuture("Failed to parse rule of router " + routerName));
                return;
            }
            Route route = routingRule.apply(router);

            List<UriMiddleware> uriMiddlewares = new ArrayList<>();
            JsonArray middlewareNames = routerConfig.getJsonArray(DynamicConfiguration.MIDDLEWARES);
            if (middlewareNames != null) {
                for (int j = 0; j < middlewareNames.size(); j++) {
                    String middlewareName = middlewareNames.getString(j);
                    JsonObject middlewareConfig = DynamicConfiguration.getObjByKeyWithValue(
                            middlwares, DynamicConfiguration.MIDDLEWARE_NAME, middlewareName);

                    String middlewareType =
                            middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_TYPE);
                    JsonObject middlewareOptions =
                            middlewareConfig.getJsonObject(DynamicConfiguration.MIDDLEWARE_OPTIONS);

                    MiddlewareFactory middlewareFactory =
                            MiddlewareFactory.Loader.getFactory(middlewareType);
                    if (middlewareFactory != null) {
                        // TODO wait for futures to complete and set handlers on completion
                        Middleware middleware =
                                middlewareFactory.create(this.vertx, middlewareOptions);
                        route.handler(middleware);
                        continue;
                    }

                    UriMiddlewareFactory uriMiddlewareFactory =
                            UriMiddlewareFactory.Loader.getFactory(middlewareType);
                    if (uriMiddlewareFactory != null) {
                        UriMiddleware uriMiddleware =
                                uriMiddlewareFactory.create(middlewareOptions);
                        uriMiddlewares.add(uriMiddleware);
                        continue;
                    }

                    LOGGER.error("Ignoring unknown middleware '{}'", middlewareType);
                }
            }

            UriMiddleware uriMiddleware = null;
            if (uriMiddlewares.size() > 0) {
                uriMiddleware = uriMiddlewares.get(0);
                if (uriMiddlewares.size() > 1) {
                    LOGGER.warn(
                            "Multiple URI middlewares defined. At most one can be used. Chosing the first '{}'",
                            uriMiddleware.toString());
                }
            }

            String serviceName = routerConfig.getString(DynamicConfiguration.ROUTER_SERVICE);
            JsonObject serviceConfig = DynamicConfiguration.getObjByKeyWithValue(services,
                    DynamicConfiguration.SERVICE_NAME, serviceName);
            JsonArray serverConfigs =
                    serviceConfig.getJsonArray(DynamicConfiguration.SERVICE_SERVERS);
            // TODO support multipe servers
            JsonObject serverConfig = serverConfigs.getJsonObject(0);

            Middleware proxyMiddleware =
                    (new ProxyMiddlewareFactory()).create(vertx, serverConfig, uriMiddleware);
            route.handler(proxyMiddleware);
        }

        handler.handle(Future.succeededFuture(router));
    }

    public Future<Router> createRouter(JsonObject dynamicConfig) {
        Promise<Router> promise = Promise.promise();
        createRouter(dynamicConfig, promise);
        return promise.future();
    }

    private RoutingRule pathPrefix(Vertx vertx, String pathPrefix) {
        return new RoutingRule() {
            @Override
            public Route apply(Router router) {
                return router.route(pathPrefix);
            }
        };
    }

    private RoutingRule host(Vertx vertx, String host) {
        return new RoutingRule() {
            @Override
            public Route apply(Router router) {
                return router.route().virtualHost(host);
            }

        };
    }

    // only rules like PathPrefix('/abc') and Host('example.com') are supported
    private RoutingRule parseRule(Vertx vertx, String rule) {
        Pattern rulePattern = Pattern
                .compile("^(?<ruleName>(PathPrefix|Host))\\('(?<ruleValue>[a-zA-Z\\/]+)'\\)$");
        Matcher m = rulePattern.matcher(rule);

        if (!m.find()) {
            return null;
        }

        RoutingRule routingRule;
        String ruleValue = m.group("ruleValue");
        switch (m.group("ruleName")) {
            case "PathPrefix": {
                routingRule = pathPrefix(vertx, ruleValue);
                break;
            }
            case "Host": {
                routingRule = host(vertx, ruleValue);
                break;
            }
            default: {
                routingRule = null;
                break;
            }
        }
        return routingRule;
    }
}
