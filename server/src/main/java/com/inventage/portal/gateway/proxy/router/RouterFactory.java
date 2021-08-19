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
import com.inventage.portal.gateway.proxy.middleware.sessionBag.SessionBagMiddlewareFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpResponseStatus;
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

    public static final String PUBLIC_URL = "publicUrl";
    private static final Logger LOGGER = LoggerFactory.getLogger(RouterFactory.class);

    private Vertx vertx;
    private String publicUrl;

    public RouterFactory(Vertx vertx, String publicUrl) {
        this.vertx = vertx;
        this.publicUrl = publicUrl;
    }

    public Future<Router> createRouter(JsonObject dynamicConfig) {
        Promise<Router> promise = Promise.promise();
        createRouter(dynamicConfig, promise);
        return promise.future();
    }

    private void createRouter(JsonObject dynamicConfig, Handler<AsyncResult<Router>> handler) {
        Router router = Router.router(this.vertx);

        JsonObject httpConfig = dynamicConfig.getJsonObject(DynamicConfiguration.HTTP);

        JsonArray routers = httpConfig.getJsonArray(DynamicConfiguration.ROUTERS);
        JsonArray middlewares = httpConfig.getJsonArray(DynamicConfiguration.MIDDLEWARES);
        JsonArray services = httpConfig.getJsonArray(DynamicConfiguration.SERVICES);

        JsonObject sessionBagOptions = retrieveSessionBagOptions(middlewares);

        sortByRuleLength(routers);

        LOGGER.debug("createRouter: creating router from config");
        List<Future> subRouterFutures = new ArrayList<Future>();
        for (int i = 0; i < routers.size(); i++) {
            JsonObject routerConfig = routers.getJsonObject(i);
            subRouterFutures.add(createSubRouter(routerConfig, middlewares, services, sessionBagOptions));
        }

        // Handlers will get called if and only if
        // - all futures are completed
        CompositeFuture.join(subRouterFutures).onComplete(ar -> {
            subRouterFutures.forEach(srf -> {
                if (srf.succeeded()) {
                    router.mountSubRouter("/", (Router) srf.result());
                } else {
                    String errMsg = String.format("Ignoring route '%s'", srf.cause().getMessage());
                    LOGGER.warn("createRouter: {}", errMsg);
                }
            });

            addHealthRoute(router);
            handler.handle(Future.succeededFuture(router));
        });
    }

    private Future<Router> createSubRouter(JsonObject routerConfig, JsonArray middlewares, JsonArray services,
            JsonObject sessionBagOptions) {
        Promise<Router> promise = Promise.promise();
        createSubRouter(routerConfig, middlewares, services, sessionBagOptions, promise);
        return promise.future();
    }

    private void createSubRouter(JsonObject routerConfig, JsonArray middlewares, JsonArray services,
            JsonObject sessionBagOptions, Handler<AsyncResult<Router>> handler) {
        Router router = Router.router(this.vertx);
        String routerName = routerConfig.getString(DynamicConfiguration.ROUTER_NAME);

        String rule = routerConfig.getString(DynamicConfiguration.ROUTER_RULE);
        RoutingRule routingRule = parseRule(this.vertx, rule);
        if (routingRule == null) {
            String errMsg = String.format("Failed to parse rule of router '%s'", routerName);
            LOGGER.warn("createSubRouter: {}", errMsg);
            handler.handle(Future.failedFuture(errMsg));
            return;
        }
        Route route = routingRule.apply(router);

        List<Future> middlewareFutures = new ArrayList<Future>();

        // TODO maybe move out of the for loop by introducing middlewares per entrypoint
        // required to be the first middleware to guarantee every request is processed
        Future<Middleware> sessionBagMiddlewareFuture = (new SessionBagMiddlewareFactory()).create(vertx,
                sessionBagOptions);
        middlewareFutures.add(sessionBagMiddlewareFuture);

        JsonArray middlewareNames = routerConfig.getJsonArray(DynamicConfiguration.MIDDLEWARES, new JsonArray());
        for (int j = 0; j < middlewareNames.size(); j++) {
            String middlewareName = middlewareNames.getString(j);
            JsonObject middlewareConfig = DynamicConfiguration.getObjByKeyWithValue(middlewares,
                    DynamicConfiguration.MIDDLEWARE_NAME, middlewareName);
            middlewareFutures.add(createMiddlware(middlewareConfig, router));
        }

        String serviceName = routerConfig.getString(DynamicConfiguration.ROUTER_SERVICE);
        JsonObject serviceConfig = DynamicConfiguration.getObjByKeyWithValue(services,
                DynamicConfiguration.SERVICE_NAME, serviceName);
        JsonArray serverConfigs = serviceConfig.getJsonArray(DynamicConfiguration.SERVICE_SERVERS);
        // TODO support multipe servers
        JsonObject serverConfig = serverConfigs.getJsonObject(0);

        // required to be the last middleware
        Future<Middleware> proxyMiddlewareFuture = (new ProxyMiddlewareFactory()).create(vertx, router, serverConfig);
        middlewareFutures.add(proxyMiddlewareFuture);

        // Handlers will get called if and only if
        // - all futures are succeeded and completed
        // - any future is failed.
        CompositeFuture.all(middlewareFutures).onSuccess(cf -> {
            middlewareFutures.forEach(mf -> {
                route.handler((Handler<RoutingContext>) mf.result());
            });
            LOGGER.debug("createSubRouter: Middlewares of router '{}' created successfully", routerName);
            handler.handle(Future.succeededFuture(router));
        }).onFailure(cfErr -> {
            String errMsg = String.format("Failed to create middlewares of router '%s'", routerName);
            LOGGER.warn("createSubRouter: {}", errMsg);
            handler.handle(Future.failedFuture(errMsg));
        });
    }

    private Future<Middleware> createMiddlware(JsonObject middlewareConfig, Router router) {
        Promise<Middleware> promise = Promise.promise();
        createMiddleware(middlewareConfig, router, promise);
        return promise.future();
    }

    private void createMiddleware(JsonObject middlewareConfig, Router router,
            Handler<AsyncResult<Middleware>> handler) {
        String middlewareType = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_TYPE);
        JsonObject middlewareOptions = middlewareConfig.getJsonObject(DynamicConfiguration.MIDDLEWARE_OPTIONS);

        // needed to ensure authenticating requests are routed through this application
        if (middlewareType.equals(DynamicConfiguration.MIDDLEWARE_OAUTH2)) {
            middlewareOptions.put(PUBLIC_URL, this.publicUrl.toString());
        }

        MiddlewareFactory middlewareFactory = MiddlewareFactory.Loader.getFactory(middlewareType);
        if (middlewareFactory == null) {
            String errMsg = String.format("Unknown middleware '%s'", middlewareType);
            LOGGER.warn("createMiddleware: {}", errMsg);
            handler.handle(Future.failedFuture(errMsg));
            return;
        }

        middlewareFactory.create(this.vertx, router, middlewareOptions).onComplete(ar -> {
            handler.handle(ar);
        });
    }

    /**
     * Adds a healthcheck route to the given router. The healtcheck return '200
     * OK' for a successful healthcheack and '500 Internal Server Error' for a
     * failed healthcheck.
     * No routes configured (apart from the healthcheck) results in an unhealthy
     * state.
     *
     * @param router used as the proxy router
     */
    private void addHealthRoute(Router router) {
        boolean isHealthy = true;
        if (router.getRoutes().size() == 0) {
            LOGGER.info("addHealthRoute: no routes configured yet");
            isHealthy = false;
        }

        final int statusCode;
        if (isHealthy) {
            statusCode = HttpResponseStatus.OK.code();
        } else {
            statusCode = HttpResponseStatus.INTERNAL_SERVER_ERROR.code();
        }
        router.route("/health").handler(ctx -> {
            ctx.response().setStatusCode(statusCode).end();
        });
    }

    /**
     * Retrieves the session bag options from the configured middlewares.
     * If there are more than one configuration, only the first one is considered.
     * As a side effect of this methods all session bag configurations are removed
     * from the configured middlewares.
     *
     * @param middlewares all configured middlewares
     * @return session bag options
     */
    private JsonObject retrieveSessionBagOptions(JsonArray middlewares) {
        if (middlewares == null) {
            return new JsonObject().put(DynamicConfiguration.MIDDLEWARE_SESSION_BAG_WHITHELISTED_COOKIES,
                    new JsonArray());
        }
        List<JsonObject> sessionBagMiddlewares = new ArrayList<JsonObject>();
        for (int i = 0; i < middlewares.size(); i++) {
            JsonObject middleware = middlewares.getJsonObject(i);
            if (middleware.getString(DynamicConfiguration.MIDDLEWARE_TYPE)
                    .equals(DynamicConfiguration.MIDDLEWARE_SESSION_BAG)) {
                sessionBagMiddlewares.add(middleware);
            }
        }
        for (JsonObject sessionBagMiddleware : sessionBagMiddlewares) {
            middlewares.remove(sessionBagMiddleware);
        }
        if (sessionBagMiddlewares.isEmpty()) {
            return new JsonObject().put(DynamicConfiguration.MIDDLEWARE_SESSION_BAG_WHITHELISTED_COOKIES,
                    new JsonArray());
        }
        if (sessionBagMiddlewares.size() > 1) {
            LOGGER.warn("retrieveSessionBagOptions: more than one session bag configurations found. Using first one.");
        }
        return sessionBagMiddlewares.get(0).getJsonObject(DynamicConfiguration.MIDDLEWARE_OPTIONS);
    }

    /**
    * To avoid path overlap, routes are sorted, by default, in descending order using rules length.
    * The priority is directly equal to the length of the rule, and so the longest length has the
    * highest priority.
    * Additionally, a priority for each router can be defined. This overwrites priority calculates
    * by the length of the rule.
    */
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
