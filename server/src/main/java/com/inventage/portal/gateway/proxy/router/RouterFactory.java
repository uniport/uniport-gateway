package com.inventage.portal.gateway.proxy.router;

import com.google.common.collect.ImmutableList;
import com.inventage.portal.gateway.GatewayRouterInternal;
import com.inventage.portal.gateway.Runtime;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareOptions;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2RegistrationMiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.proxy.ProxyMiddlewareFactory;
import com.inventage.portal.gateway.proxy.model.Gateway;
import com.inventage.portal.gateway.proxy.model.GatewayMiddleware;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayRouter;
import com.inventage.portal.gateway.proxy.model.GatewayService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.spi.cluster.hazelcast.ClusterHealthCheck;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final String entrypointName;

    private static final String PATH_RULE_NAME = "Path";
    private static final String PATH_REGEX_RULE_NAME = "PathRegex";
    private static final String PATH_PREFIX_RULE_NAME = "PathPrefix";
    private static final String PATH_PREFIX_REGEX_RULE_NAME = "PathPrefixRegex";
    private static final String HOST_RULE_NAME = "Host";
    private static final String HOST_REGEX_RULE_NAME = "HostRegex";
    private static final Pattern RULE_PATTERN = Pattern.compile(String.format("^(?<ruleName>(%s))\\('(?<ruleValue>.*)'\\)$",
        String.join("|", PATH_RULE_NAME, PATH_REGEX_RULE_NAME, PATH_PREFIX_RULE_NAME, PATH_PREFIX_REGEX_RULE_NAME, HOST_RULE_NAME, HOST_REGEX_RULE_NAME)));
    /**
     * Allowed rule values according to RFC 3986 - Uniform Resource Identifier (URI): Generic Syntax
     * See https://www.rfc-editor.org/rfc/rfc3986.html#appendix-A
     * 
     * URI = scheme ":" hier-part [ "?" query ] [ "#" fragment ]
     * 
     * hier-part = "//" authority (path-abempty | path-absolute | path-rootless | path-empty)
     * 
     * authority = [ userinfo "@" ] host [ ":" port ]
     * host = IP-literal | IPv4address | reg-name
     * port = *DIGIT
     * 
     * IP-literal = "[" ( IPv6address | IPvFuture ) "]"
     * IPvFuture = "v" 1*HEXDIG "." 1*( unreserved | sub-delims | ":" )
     * reg-name = *( unreserved | pct-encoded | sub-delims )
     * 
     * path-abempty = *( "/" segment )
     * path-absolute = "/" [ segment-nz *( "/" segment ) ]
     * path-rootless = segment-nz *( "/" segment )
     * path-empty = 0<pchar>
     * 
     * segment = *pchar
     * segment-nz = 1*pchar
     * 
     * pchar = unreserved | pct-encoded | sub-delims | ":" | "@"
     * unreserved = ALPHA | DIGIT | "-" | "." | "_" | "~"
     * pct-encoded = "%" HEXDIG HEXDIG
     * sub-delims = "!" | "$" | "&" | "'" | "(" | ")" | "*" | "+" | "," | ";" | "="
     * 
     * alpha, digit, hex, IPv4address, IPv6address are omitted
     * 
     * This yields the following regexes:
     */
    private static final String ALPHA = "A-Za-z";
    private static final String DIGIT = "0-9";
    private static final String HEX_DIGIT = DIGIT + "A-Fa-f";
    private static final String UNRESERVED = ALPHA + DIGIT + "\\-" + "\\." + "_" + "~";
    private static final String PCT_ENCODED = "%" + HEX_DIGIT;
    private static final String SUB_DELIMS = "!" + "\\$" + "&" + "'" + "\\(" + "\\)" + "\\*" + "\\+" + "," + ";" + "=";

    private static final String IP_LITERAL = "\\[" + "\\]" + ":";
    private static final String IPV4 = DIGIT + "\\.";
    private static final String REG_NAME = UNRESERVED + PCT_ENCODED + SUB_DELIMS;

    private static final String PCHAR = UNRESERVED + PCT_ENCODED + SUB_DELIMS + ":" + "@";

    /*
     * We dont enforce the complete structure, but at least only allow possible characters. 
     * Invalid combinations of those are still possible. Could still be extended though.
     */
    private static final Pattern HOST_PATTERN = Pattern.compile("^[" + IP_LITERAL + IPV4 + REG_NAME + "]+$");
    private static final Pattern PATH_PATTERN = Pattern.compile("^\\/[" + PCHAR + "\\/]*$");

    /**
     * Validates a router config i.e. with a valid rule.
     * 
     * @param config
     *            of a router
     * @throws IllegalArgumentException
     *             if the router is invalid
     */
    public static void validateRouter(JsonObject config) {
        final String rule = config.getString(DynamicConfiguration.ROUTER_RULE);
        if (rule == null) {
            throw new IllegalArgumentException("no rule");
        }

        final Matcher m = RULE_PATTERN.matcher(rule);
        if (!m.matches()) {
            throw new IllegalArgumentException("illegal rule format");
        }

        final String ruleName = m.group("ruleName");
        final String ruleValue = m.group("ruleValue");
        switch (ruleName) {
            case PATH_RULE_NAME: {
                if (!PATH_PATTERN.matcher(ruleValue).matches()) {
                    throw new IllegalArgumentException("illegal path value");
                }
                return;
            }
            case PATH_PREFIX_RULE_NAME: {
                if (!PATH_PATTERN.matcher(ruleValue).matches()) {
                    throw new IllegalArgumentException("illegal path prefix value");
                }
                return;
            }
            case HOST_RULE_NAME: {
                if (!HOST_PATTERN.matcher(ruleValue).matches()) {
                    throw new IllegalArgumentException("illegal host value");
                }
                return;
            }
            case PATH_REGEX_RULE_NAME:
            case PATH_PREFIX_REGEX_RULE_NAME:
            case HOST_REGEX_RULE_NAME: {
                return; // regex value is not validated further
            }
            default: {
                throw new IllegalArgumentException("unkown rule");
            }
        }
    }

    public RouterFactory(Vertx vertx, String publicProtocol, String publicHostname, String publicPort, String entrypointName) {
        this.vertx = vertx;
        this.publicProtocol = publicProtocol;
        this.publicHostname = publicHostname;
        this.publicPort = publicPort;
        this.entrypointName = entrypointName;
    }

    public RouterFactory(RouterFactory other) {
        this.vertx = other.vertx;
        this.publicProtocol = other.publicProtocol;
        this.publicHostname = other.publicHostname;
        this.publicPort = other.publicPort;
        this.entrypointName = other.entrypointName;
    }

    public Future<Router> createRouter(Gateway model) {
        final Promise<Router> promise = Promise.promise();
        createRouter(model, promise);
        return promise.future();
    }

    private void createRouter(Gateway model, Handler<AsyncResult<Router>> handler) {
        final List<GatewayRouter> routers = new LinkedList<GatewayRouter>(model.getRouters());
        Collections.sort(routers);
        LOGGER.debug("Routing requests in the following order:");
        for (GatewayRouter r : routers) {
            LOGGER.debug("Router '{}': rule '{}', priority '{}'", r.getName(), r.getRule(), r.getPriority() > 0 ? r.getPriority() : r.getRule().length());
        }

        final ImmutableList<GatewayMiddleware> middlewares = model.getMiddlewares();
        final ImmutableList<GatewayService> services = model.getServices();

        LOGGER.debug("Creating router from config");
        final List<Future<Router>> subRouterFutures = new ArrayList<>();
        for (GatewayRouter r : routers) {

            final ImmutableList<String> entrypoints = r.getEntrypoints();
            if (entrypoints.isEmpty()) {
                final String errMsg = "Router has no entrypoints";
                handler.handle(Future.failedFuture(errMsg));

                // Fast-failing
                Runtime.fatal(vertx, errMsg);
                return;
            }

            if (!entrypoints.contains(entrypointName)) {
                continue;
            }

            subRouterFutures.add(createSubRouter(r, middlewares, services));
        }

        final GatewayRouterInternal router = GatewayRouterInternal.router(this.vertx, "root");

        // has to be first route, so no other paths are shadowing it
        addHealthRoute(router);

        // Handlers will get called if and only if
        // - all futures are completed
        Future.join(subRouterFutures).onComplete(ar -> {
            subRouterFutures.forEach(srf -> {
                if (srf.succeeded()) {
                    final Router subRouter = srf.result();
                    router.mountSubRouter("/", subRouter);
                } else {
                    handler.handle(Future.failedFuture(String.format("Route failed '{}'", srf.cause().getMessage())));
                    LOGGER.warn("Ignoring route '{}'", srf.cause().getMessage());

                    // Fast-failing
                    Runtime.fatal(vertx, srf.cause().getMessage());
                }
            });
            LOGGER.debug("Router '{}' created successfully", router.getName());
            handler.handle(Future.succeededFuture(router));
        });
    }

    private Future<Router> createSubRouter(GatewayRouter routerConfig, ImmutableList<GatewayMiddleware> middlewares, ImmutableList<GatewayService> services) {
        final Promise<Router> promise = Promise.promise();
        createSubRouter(routerConfig, middlewares, services, promise);
        return promise.future();
    }

    private void createSubRouter(GatewayRouter routerConfig, ImmutableList<GatewayMiddleware> middlewares, ImmutableList<GatewayService> services, Handler<AsyncResult<Router>> handler) {
        final String routerName = routerConfig.getName();
        final Router router = GatewayRouterInternal.router(this.vertx, String.format("rule matcher %s", routerName));

        final String rule = routerConfig.getRule();
        final RoutingRule routingRule = parseRule(rule);
        if (routingRule == null) {
            final String errMsg = String.format("Failed to parse rule of router '%s'", routerName);
            LOGGER.warn("{}", errMsg);
            handler.handle(Future.failedFuture(errMsg));
            return;
        }
        final Route route = routingRule.apply(router).last();

        final List<Future<Middleware>> middlewareFutures = new ArrayList<>();

        final ImmutableList<String> middlewareNames = routerConfig.getMiddlewares();
        for (String middlewareName : middlewareNames) {
            final Optional<GatewayMiddleware> middlewareConfig = middlewares.stream()
                .filter(m -> m.getName().equals(middlewareName))
                .findFirst();

            if (middlewareConfig.isEmpty()) {
                final String errMsg = String.format("Failed to find middleware '%s' in router '%s'", middlewareName, routerName);
                LOGGER.warn("{}", errMsg);
                handler.handle(Future.failedFuture(errMsg));
                return;
            }

            middlewareFutures.add(createMiddleware(middlewareConfig.get(), router));
        }

        final String serviceName = routerConfig.getService();
        final Optional<GatewayService> serviceConfig = services.stream()
            .filter(s -> s.getName().equals(serviceName))
            .findFirst();

        if (serviceConfig.isEmpty()) {
            final String errMsg = String.format("Failed to find service '%s' in router '%s'", serviceConfig, routerName);
            LOGGER.warn("{}", errMsg);
            handler.handle(Future.failedFuture(errMsg));
            return;
        }

        // required to be the last middleware
        final Future<Middleware> proxyMiddlewareFuture = new ProxyMiddlewareFactory().create(vertx, serviceName, router, serviceConfig.get());
        middlewareFutures.add(proxyMiddlewareFuture);

        // Handlers will get called if and only if
        // - all futures are succeeded and completed
        // - any future is failed.
        Future.all(middlewareFutures)
            .onSuccess(cf -> {
                middlewareFutures.forEach(mf -> route.handler((Handler<RoutingContext>) mf.result()));
                LOGGER.debug("Middlewares of router '{}' created successfully", routerName);
                handler.handle(Future.succeededFuture(router));
            }).onFailure(cfErr -> {
                final String errMsg = String.format("Failed to create middlewares of router '%s'", routerName);
                LOGGER.warn("{}", errMsg);
                handler.handle(Future.failedFuture(errMsg));
            });
    }

    private Future<Middleware> createMiddleware(GatewayMiddleware middlewareConfig, Router router) {
        final Promise<Middleware> promise = Promise.promise();
        createMiddleware(middlewareConfig, router, promise);
        return promise.future();
    }

    private void createMiddleware(GatewayMiddleware middlewareConfig, Router router, Handler<AsyncResult<Middleware>> handler) {
        final String middlewareType = middlewareConfig.getType();
        final GatewayMiddlewareOptions middlewareOptions = injectPublicProtocolHostPort(middlewareType, middlewareConfig.getOptions());

        final Optional<MiddlewareFactory> middlewareFactory = MiddlewareFactory.Loader.getFactory(middlewareType);
        if (middlewareFactory.isEmpty()) {
            final String errMsg = String.format("Unknown middleware '%s'", middlewareType);
            LOGGER.warn("{}", errMsg);
            handler.handle(Future.failedFuture(errMsg));
            return;
        }

        final String middlewareName = middlewareConfig.getName();
        middlewareFactory.get()
            .create(this.vertx, middlewareName, router, middlewareOptions)
            .onComplete(handler);
    }

    private GatewayMiddlewareOptions injectPublicProtocolHostPort(String type, GatewayMiddlewareOptions options) {
        if (!(type.equals(OAuth2MiddlewareFactory.TYPE) || type.equals(OAuth2RegistrationMiddlewareFactory.TYPE))) {
            return options;
        }

        if (!(options instanceof OAuth2MiddlewareOptions)) {
            throw new IllegalStateException(String.format("unexpected middleware options type: '%s'", options.getClass()));
        }
        final OAuth2MiddlewareOptions oauth2options = (OAuth2MiddlewareOptions) options;

        // needed to ensure authenticating requests are routed through this application
        return oauth2options.withEnv(
            Map.of(
                PUBLIC_PROTOCOL_KEY, publicProtocol,
                PUBLIC_HOSTNAME_KEY, publicHostname,
                PUBLIC_PORT_KEY, publicPort));
    }

    private void addHealthRoute(Router router) {
        boolean anyHealthCheckRegistered = false;

        final HealthChecks checks = HealthChecks.create(vertx);
        if (vertx.isClustered()) {
            final Handler<Promise<Status>> clusterHealthProcedure = ClusterHealthCheck.createProcedure(vertx);
            checks.register("cluster-health", clusterHealthProcedure);
            anyHealthCheckRegistered = true;
        }

        if (!anyHealthCheckRegistered) {
            // at least one health check is needed, provide one if no other is given
            checks.register("default-health", promise -> promise.complete(Status.OK()));
        }

        final Handler<RoutingContext> healthChecksHandler = HealthCheckHandler.createWithHealthChecks(checks);
        router.get("/readiness").setName("readiness").handler(healthChecksHandler);
        router.route("/health").setName("health").handler(healthChecksHandler);
    }

    /**
     * Rules like Path("/foo"), PathPrefix('/bar'), PathRegex('/language/(de|en)/.*'), Host('*.example.com') and HostRegex('(de|en)\.example\.com') and are
     * supported.
     */
    private RoutingRule parseRule(String rule) {
        if (rule == null) {
            return null;
        }

        final Matcher m = RULE_PATTERN.matcher(rule);
        if (!m.matches()) {
            return null;
        }

        final String ruleName = m.group("ruleName");
        final String ruleValue = m.group("ruleValue");
        switch (ruleName) {
            case PATH_RULE_NAME: {
                return path(ruleValue);
            }
            case PATH_REGEX_RULE_NAME: {
                return pathRegex(ruleValue);
            }
            case PATH_PREFIX_RULE_NAME: {
                return pathPrefix(ruleValue);
            }
            case PATH_PREFIX_REGEX_RULE_NAME: {
                return pathPrefixRegex(ruleValue);
            }
            case HOST_RULE_NAME: {
                return host(ruleValue);
            }
            case HOST_REGEX_RULE_NAME: {
                return hostRegex(ruleValue);
            }
            default: {
                return null;
            }
        }
    }

    private RoutingRule path(String path) {
        final Matcher m = PATH_PATTERN.matcher(path);
        if (!m.matches()) {
            return null;
        }

        final String name = String.format("path matcher: %s", path);
        return router -> {
            LOGGER.debug("Create route with exact path '{}'", path);
            return router.route(path).setName(name);
        };
    }

    private RoutingRule pathPrefix(String path) {
        final Matcher m = PATH_PATTERN.matcher(path);
        if (!m.matches()) {
            return null;
        }

        // append * to do path prefix routing as implemented by vertx Router.route()
        final String pathPrefix = path + "*";
        final String name = String.format("path prefix matcher: %s", pathPrefix);
        return router -> {
            LOGGER.debug("Create route with path prefix '{}'", pathPrefix);
            return router.route(pathPrefix).setName(name);
        };
    }

    private RoutingRule host(String host) {
        final Matcher m = HOST_PATTERN.matcher(host);
        if (!m.matches()) {
            return null;
        }

        final String name = String.format("host matcher: %s", host);
        return router -> {
            LOGGER.debug("Create route with host '{}'", host);
            return router.route().virtualHost(host).setName(name);
        };
    }

    // https://vertx.tk/docs/vertx-web/java/#_routing_with_regular_expressions
    private RoutingRule pathRegex(String pathRegex) {
        final String name = String.format("path regex matcher: %s", pathRegex);
        return router -> {
            return router.route().pathRegex(pathRegex).setName(name);
        };
    }

    private RoutingRule pathPrefixRegex(String pathRegex) {
        // append non-capturing atomic group that matches anything to do regex-based path prefix routing
        final String pathPrefixRegex = pathRegex + "(?>.*)";
        final String name = String.format("path regex matcher: %s", pathRegex);
        return router -> {
            return router.route().pathRegex(pathPrefixRegex).setName(name);
        };
    }

    private RoutingRule hostRegex(String hostRegex) {
        final String name = String.format("host regex matcher: %s", hostRegex);
        return router -> {
            // technically the same as this.host, but we dont check the rule value
            return router.route().virtualHost(hostRegex).setName(name);
        };
    }
}
