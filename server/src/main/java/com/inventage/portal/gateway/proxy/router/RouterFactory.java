package com.inventage.portal.gateway.proxy.router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * Essentially it translates the dynamic configuration to what vertx understands. It creates a new
 * vertx router with routes for each router, middleware and service defined in the dynamic
 * configuration. Special cases are the proxy middleware, URI middleware and the OAuth2 middleware.
 * The proxy middleware is always the final middleware (if no redirect applies) to forward the
 * request to the corresponding server. Since Vertx does not allow to manipulate the URI of a
 * request, this manipulation is done in the proxy middleware. The OAuth2 middleware requires to
 * know the public hostname (like localhost or example.com) and the entrypoint port of this
 * application to route all authenticating requests throught this application as well. To avoid path
 * overlap, routes are sorted, by default, in descending order using rules length. The priority is
 * directly equal to the length of the rule, and so the longest length has the highest priority.
 */
public class RouterFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(RouterFactory.class);

    private Vertx vertx;
    private String publicHostname;
    private String entrypointPort;

    public RouterFactory(Vertx vertx, String publicHostname, String entrypointPort) {
        this.vertx = vertx;
        this.publicHostname = publicHostname;
        this.entrypointPort = entrypointPort;
    }

    public Future<Router> createRouter(JsonObject dynamicConfig) {
        Promise<Router> promise = Promise.promise();
        createRouter(dynamicConfig, promise);
        return promise.future();
    }

    private void createRouter(JsonObject dynamicConfig,
            final Handler<AsyncResult<Router>> handler) {
        Router router = Router.router(this.vertx);

        JsonObject httpConfig = dynamicConfig.getJsonObject(DynamicConfiguration.HTTP);

        JsonArray routers = httpConfig.getJsonArray(DynamicConfiguration.ROUTERS);
        JsonArray middlwares = httpConfig.getJsonArray(DynamicConfiguration.MIDDLEWARES);
        JsonArray services = httpConfig.getJsonArray(DynamicConfiguration.SERVICES);

        sortByRuleLength(routers);
        LOGGER.debug("createRouter: creating router from config");

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
            List<Future> middlewareFutures = new ArrayList();
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

                    // needed to ensure authenticating requests are routed through this application
                    // TODO this does not work when published != exposed port
                    if (middlewareType.equals(DynamicConfiguration.MIDDLEWARE_OAUTH2)) {
                        middlewareOptions.put("publicHostname", this.publicHostname);
                        middlewareOptions.put("entrypointPort", this.entrypointPort);
                    }

                    MiddlewareFactory middlewareFactory =
                            MiddlewareFactory.Loader.getFactory(middlewareType);
                    if (middlewareFactory != null) {
                        Future<Middleware> middlewareFuture =
                                middlewareFactory.create(this.vertx, router, middlewareOptions);
                        middlewareFutures.add(middlewareFuture);
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

                    LOGGER.warn("createRouter: Ignoring unknown middleware '{}'", middlewareType);
                }
            }

            UriMiddleware uriMiddleware = null;
            if (uriMiddlewares.size() > 0) {
                uriMiddleware = uriMiddlewares.get(0);
                if (uriMiddlewares.size() > 1) {
                    LOGGER.warn(
                            "createRouter: Multiple URI middlewares defined. At most one can be used. Chosing the first '{}'",
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

            Future<Middleware> proxyMiddlewareFuture = (new ProxyMiddlewareFactory()).create(vertx,
                    router, serverConfig, uriMiddleware);
            middlewareFutures.add(proxyMiddlewareFuture);

            CompositeFuture.all(middlewareFutures).onComplete(ar -> {
                middlewareFutures.forEach(mf -> {
                    if (mf.succeeded()) {
                        route.handler((Handler<RoutingContext>) mf.result());
                    } else {
                        router.delete(route.getPath());
                        LOGGER.warn(
                                "createRouter: Ignoring path '{}'. Failed to create middleware: '{}'",
                                route.getPath(), mf.cause().getMessage());
                        handler.handle(Future.failedFuture(
                                "Failed to create middleware '" + mf.cause().getMessage() + "'"));
                    }
                });
            });
        }

        // TODO ensure all routes are built
        handler.handle(Future.succeededFuture(router));
    }

    // To avoid path overlap, routes are sorted, by default, in descending order using rules length.
    // The priority is directly equal to the length of the rule, and so the longest length has the
    // highest priority.
    // Additionally, a priority for each router can be defined. This overwrites priority calculates
    // by the length of the rule.
    private JsonArray sortByRuleLength(JsonArray routers) {
        List<JsonObject> routerList = routers.getList();

        Collections.sort(routerList, new Comparator<JsonObject>() {

            @Override
            public int compare(JsonObject a, JsonObject b) {
                String ruleA = a.getString(DynamicConfiguration.ROUTER_RULE);
                String ruleB = b.getString(DynamicConfiguration.ROUTER_RULE);

                int priorityA = ruleA.length();
                int priorityB = ruleB.length();

                if (a.containsKey(DynamicConfiguration.ROUTER_PRIORITY)) {
                    priorityA = a.getInteger(DynamicConfiguration.ROUTER_PRIORITY);
                }

                if (b.containsKey(DynamicConfiguration.ROUTER_PRIORITY)) {
                    priorityB = b.getInteger(DynamicConfiguration.ROUTER_PRIORITY);
                }

                return priorityB - priorityA;
            }

        });

        return routers;
    }

    private RoutingRule path(Vertx vertx, String path) {
        return new RoutingRule() {
            @Override
            public Route apply(Router router) {
                LOGGER.debug("apply: create route with exact path '{}'", path);
                return router.route(path);
            }
        };
    }

    private RoutingRule pathPrefix(Vertx vertx, String pathPrefix) {
        return new RoutingRule() {
            @Override
            public Route apply(Router router) {
                LOGGER.debug("apply: create route with path prefix '{}'", pathPrefix);
                return router.route(pathPrefix);
            }
        };
    }

    private RoutingRule host(Vertx vertx, String host) {
        return new RoutingRule() {
            @Override
            public Route apply(Router router) {
                LOGGER.debug("apply: create route with host '{}'", host);
                return router.route().virtualHost(host);
            }

        };
    }

    // only rules like Path("/blub"), PathPrefix('/abc') and Host('example.com') are supported
    private RoutingRule parseRule(Vertx vertx, String rule) {
        Pattern rulePattern = Pattern
                .compile("^(?<ruleName>(Path|PathPrefix|Host))\\('(?<ruleValue>[0-9a-zA-Z\\/]+)'\\)$");
        Matcher m = rulePattern.matcher(rule);

        if (!m.find()) {
            return null;
        }

        RoutingRule routingRule;
        String ruleValue = m.group("ruleValue");
        switch (m.group("ruleName")) {
            case "Path": {
                routingRule = path(vertx, ruleValue);
                break;
            }
            case "PathPrefix": {
                // append * to do path prefix routing
                String pathPrefix = ruleValue;
                pathPrefix += "*";

                routingRule = pathPrefix(vertx, pathPrefix);
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
