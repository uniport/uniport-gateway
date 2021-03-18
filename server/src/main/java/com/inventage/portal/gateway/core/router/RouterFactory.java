package com.inventage.portal.gateway.core.router;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.inventage.portal.gateway.core.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.core.middleware.Middleware;
import com.inventage.portal.gateway.core.middleware.MiddlewareFactory;
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
    Vertx vertx;

    public RouterFactory(Vertx vertx) {
        this.vertx = vertx;
    }

    public void createRouter(JsonObject dynamicConfig, final Handler<AsyncResult<Router>> handler) {
        Router router = Router.router(this.vertx);

        JsonObject httpConfig = dynamicConfig.getJsonObject(DynamicConfiguration.HTTP);
        System.out.println(httpConfig);

        JsonArray routers = httpConfig.getJsonArray(DynamicConfiguration.ROUTERS);
        JsonArray middlwares = httpConfig.getJsonArray(DynamicConfiguration.MIDDLEWARES);
        JsonArray services = httpConfig.getJsonArray(DynamicConfiguration.SERVICES);

        for (int i = 0; i < routers.size(); i++) {
            JsonObject routerConfig = routers.getJsonObject(i);

            String routerName = routerConfig.getString(DynamicConfiguration.ROUTER_NAME);

            String rule = routerConfig.getString(DynamicConfiguration.ROUTER_RULE);
            RoutingRule routingRule = parseRule(this.vertx, rule);
            if (routingRule == null) {
                handler.handle(Future.failedFuture("Failed to parse rule"));
                return;
            }
            Route route = routingRule.apply(router);

            JsonArray middlewareNames = routerConfig.getJsonArray(DynamicConfiguration.MIDDLEWARES);
            if (middlewareNames != null) {
                for (int j = 0; j < middlewareNames.size(); j++) {
                    String middlewareName = middlewareNames.getString(j);
                    JsonObject middlewareConfig = DynamicConfiguration.getObjByKeyWithValue(
                            middlwares, DynamicConfiguration.MIDDLEWARE_NAME, middlewareName);

                    // TODO configure middleware
                    Middleware middlware = MiddlewareFactory.Loader.getFactory(middlewareName)
                            .create(this.vertx, middlewareConfig);
                }
            }

            // TODO experimental
            route.handler(MiddlewareFactory.Loader.getFactory("headers").create(vertx, null));

            String serviceName = routerConfig.getString(DynamicConfiguration.ROUTER_SERVICE);
            JsonObject serviceConfig = DynamicConfiguration.getObjByKeyWithValue(services,
                    DynamicConfiguration.SERVICE_NAME, serviceName);
            JsonArray servers = serviceConfig.getJsonArray(DynamicConfiguration.SERVICE_SERVERS);
            // TODO support multipe servers
            route.handler(MiddlewareFactory.Loader.getFactory("proxy").create(vertx,
                    servers.getJsonObject(0)));

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
