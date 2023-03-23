package com.inventage.portal.gateway.proxy.router;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.proxy.ProxyMiddlewareFactory;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Essentially it translates the dynamic configuration to what vertx
 * understands. It creates a new vertx router with routes for each router,
 * middleware and service defined in the dynamic configuration. Special cases
 * are the proxy middleware, URI middleware and the OAuth2 middleware.
 * The proxy middleware is always the final middleware (if no redirect applies)
 * to forward the request to the corresponding server. Since Vertx does not
 * allow to manipulate the URI of a request, this manipulation is done in the
 * proxy middleware. The OAuth2 middleware requires to know the public hostname
 * (like localhost or example.com) and the entrypoint port of this
 * application to route all authenticating requests through this application as
 * well. To avoid path overlap, routes are sorted, by default, in descending
 * order using rules length. The priority is directly equal to the length of the
 * rule, and so the longest length has the highest priority.
 */
public class RouterFactory {

    public static final String PUBLIC_PROTOCOL_KEY = "publicProtocol";
    public static final String PUBLIC_HOSTNAME_KEY = "publicHostname";
    public static final String PUBLIC_PORT_KEY = "publicPort";

    private static final Logger LOGGER = LoggerFactory.getLogger(RouterFactory.class);

    private final Vertx vertx;
    private final String publicProtocol;
    private final String publicHostname;
    private final String publicPort;

    public RouterFactory(Vertx vertx, String publicProtocol, String publicHostname, String publicPort) {
        this.vertx = vertx;
        this.publicProtocol = publicProtocol;
        this.publicHostname = publicHostname;
        this.publicPort = publicPort;
    }

    public RouterFactory(RouterFactory other) {
        this.vertx = other.vertx;
        this.publicProtocol = other.publicProtocol;
        this.publicHostname = other.publicHostname;
        this.publicPort = other.publicPort;
    }

    public Future<Router> createRouter(JsonObject dynamicConfig) {
        final Promise<Router> promise = Promise.promise();
        createRouter(dynamicConfig, promise);
        return promise.future();
    }

    private void createRouter(JsonObject dynamicConfig, Handler<AsyncResult<Router>> handler) {
        final JsonObject httpConfig = dynamicConfig.getJsonObject(DynamicConfiguration.HTTP);

        final JsonArray routers = httpConfig.getJsonArray(DynamicConfiguration.ROUTERS);
        final JsonArray middlewares = httpConfig.getJsonArray(DynamicConfiguration.MIDDLEWARES);
        final JsonArray services = httpConfig.getJsonArray(DynamicConfiguration.SERVICES);

        sortByRuleLength(routers);

        LOGGER.debug("Creating router from config");
        final List<Future> subRouterFutures = new ArrayList<>();
        for (int i = 0; i < routers.size(); i++) {
            final JsonObject routerConfig = routers.getJsonObject(i);
            subRouterFutures.add(createSubRouter(routerConfig, middlewares, services));
        }

        final Router router = Router.router(this.vertx);

        // has to be first route, so no other paths are shadowing it
        addHealthRoute(router);

        // Handlers will get called if and only if
        // - all futures are completed
        CompositeFuture.join(subRouterFutures).onComplete(ar -> {
            subRouterFutures.forEach(srf -> {
                if (srf.succeeded()) {
                    router.route("/*").setName("router").subRouter((Router) srf.result());
                } else {
                    handler.handle(Future.failedFuture(String.format("Route failed '{}'", srf.cause().getMessage())));
                    LOGGER.warn("Ignoring route '{}'", srf.cause().getMessage());
                }
            });

            handler.handle(Future.succeededFuture(router));
        });
    }

    private Future<Router> createSubRouter(JsonObject routerConfig, JsonArray middlewares, JsonArray services) {
        final Promise<Router> promise = Promise.promise();
        createSubRouter(routerConfig, middlewares, services, promise);
        return promise.future();
    }

    private void createSubRouter(JsonObject routerConfig, JsonArray middlewares, JsonArray services,
        Handler<AsyncResult<Router>> handler) {
        final Router router = Router.router(this.vertx);
        final String routerName = routerConfig.getString(DynamicConfiguration.ROUTER_NAME);

        final String rule = routerConfig.getString(DynamicConfiguration.ROUTER_RULE);
        final RoutingRule routingRule = parseRule(rule);
        if (routingRule == null) {
            final String errMsg = String.format("Failed to parse rule of router '%s'", routerName);
            LOGGER.warn("{}", errMsg);
            handler.handle(Future.failedFuture(errMsg));
            return;
        }
        final Route route = routingRule.apply(router);

        final List<Future> middlewareFutures = new ArrayList<>();

        final JsonArray middlewareNames = routerConfig.getJsonArray(DynamicConfiguration.MIDDLEWARES, new JsonArray());
        for (int j = 0; j < middlewareNames.size(); j++) {
            final String middlewareName = middlewareNames.getString(j);
            final JsonObject middlewareConfig = DynamicConfiguration.getObjByKeyWithValue(middlewares,
                DynamicConfiguration.MIDDLEWARE_NAME, middlewareName);
            middlewareFutures.add(createMiddleware(middlewareConfig, router));
        }

        final String serviceName = routerConfig.getString(DynamicConfiguration.ROUTER_SERVICE);
        final JsonObject serviceConfig = DynamicConfiguration.getObjByKeyWithValue(services,
            DynamicConfiguration.SERVICE_NAME, serviceName);
        final JsonArray serverConfigs = serviceConfig.getJsonArray(DynamicConfiguration.SERVICE_SERVERS);
        // TODO support multiple servers
        final JsonObject serverConfig = serverConfigs.getJsonObject(0);
        serverConfig.put(DynamicConfiguration.SERVICE_NAME, serviceName);

        // required to be the last middleware
        final Future<Middleware> proxyMiddlewareFuture = (new ProxyMiddlewareFactory()).create(vertx, "proxy", router,
            serverConfig);
        middlewareFutures.add(proxyMiddlewareFuture);

        // Handlers will get called if and only if
        // - all futures are succeeded and completed
        // - any future is failed.
        CompositeFuture.all(middlewareFutures).onSuccess(cf -> {
            middlewareFutures.forEach(mf -> route.handler((Handler<RoutingContext>) mf.result()));
            LOGGER.debug("Middlewares of router '{}' created successfully", routerName);
            handler.handle(Future.succeededFuture(router));
        }).onFailure(cfErr -> {
            final String errMsg = String.format("Failed to create middlewares of router '%s'", routerName);
            LOGGER.warn("{}", errMsg);
            handler.handle(Future.failedFuture(errMsg));
        });
    }

    private Future<Middleware> createMiddleware(JsonObject middlewareConfig, Router router) {
        final Promise<Middleware> promise = Promise.promise();
        createMiddleware(middlewareConfig, router, promise);
        return promise.future();
    }

    private void createMiddleware(JsonObject middlewareConfig, Router router,
        Handler<AsyncResult<Middleware>> handler) {
        final String middlewareType = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_TYPE);
        final JsonObject middlewareOptions = middlewareConfig.getJsonObject(DynamicConfiguration.MIDDLEWARE_OPTIONS,
            new JsonObject());

        // needed to ensure authenticating requests are routed through this application
        if (middlewareType.equals(DynamicConfiguration.MIDDLEWARE_OAUTH2)
            || middlewareType.equals(DynamicConfiguration.MIDDLEWARE_OAUTH2_REGISTRATION)) {
            middlewareOptions.put(PUBLIC_PROTOCOL_KEY, this.publicProtocol);
            middlewareOptions.put(PUBLIC_HOSTNAME_KEY, this.publicHostname);
            middlewareOptions.put(PUBLIC_PORT_KEY, this.publicPort);
        }

        final MiddlewareFactory middlewareFactory = MiddlewareFactory.Loader.getFactory(middlewareType);
        if (middlewareFactory == null) {
            final String errMsg = String.format("Unknown middleware '%s'", middlewareType);
            LOGGER.warn("{}", errMsg);
            handler.handle(Future.failedFuture(errMsg));
            return;
        }

        final String middlewareName = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_NAME);
        middlewareFactory.create(this.vertx, middlewareName, router, middlewareOptions).onComplete(handler);
    }

    private void addHealthRoute(Router router) {
        router.route("/health").setName("health").handler(ctx -> {
            ctx.response().setStatusCode(HttpResponseStatus.OK.code()).end();
        });
    }

    /**
     * To avoid path overlap, routes are sorted, by default, in descending order
     * using rules length.
     * The priority is directly equal to the length of the rule, and so the longest
     * length has the
     * highest priority.
     * Additionally, a priority for each router can be defined. This overwrites
     * priority calculates
     * by the length of the rule.
     */
    private void sortByRuleLength(JsonArray routers) {
        final List<JsonObject> routerList = routers.getList();

        Collections.sort(routerList, (a, b) -> {
            final String ruleA = a.getString(DynamicConfiguration.ROUTER_RULE);
            final String ruleB = b.getString(DynamicConfiguration.ROUTER_RULE);

            int priorityA = ruleA.length();
            int priorityB = ruleB.length();

            if (a.containsKey(DynamicConfiguration.ROUTER_PRIORITY)) {
                priorityA = a.getInteger(DynamicConfiguration.ROUTER_PRIORITY);
            }

            if (b.containsKey(DynamicConfiguration.ROUTER_PRIORITY)) {
                priorityB = b.getInteger(DynamicConfiguration.ROUTER_PRIORITY);
            }

            return priorityB - priorityA;
        });

    }

    private RoutingRule path(String path) {
        return router -> {
            LOGGER.debug("Create route with exact path '{}'", path);
            return router.route(path).setName(String.format("path matcher: %s", path));
        };
    }

    private RoutingRule pathPrefix(String pathPrefix) {
        return router -> {
            LOGGER.debug("Create route with path prefix '{}'", pathPrefix);
            return router.route(pathPrefix).setName(String.format("path prefix matcher: %s", pathPrefix));
        };
    }

    private RoutingRule host(String host) {
        return router -> {
            LOGGER.debug("Create route with host '{}'", host);
            return router.route().virtualHost(host).setName(String.format("host matcher: %s", host));
        };
    }

    // only rules like Path("/foo"), PathPrefix('/bar') and Host('example.com') are
    // supported
    protected RoutingRule parseRule(String rule) {
        final Pattern rulePattern = Pattern
            .compile("^(?<ruleName>(Path|PathPrefix|Host))\\('(?<ruleValue>[\\da-zA-Z/\\-.]+)'\\)$");
        final Matcher m = rulePattern.matcher(rule);

        if (!m.find()) {
            return null;
        }

        final RoutingRule routingRule;
        final String ruleValue = m.group("ruleValue");
        switch (m.group("ruleName")) {
            case "Path": {
                routingRule = path(ruleValue);
                break;
            }
            case "PathPrefix": {
                // append * to do path prefix routing
                String pathPrefix = ruleValue;
                pathPrefix += "*";

                routingRule = pathPrefix(pathPrefix);
                break;
            }
            case "Host": {
                routingRule = host(ruleValue);
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
