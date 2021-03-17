package com.inventage.portal.gateway.proxy;

import com.inventage.portal.gateway.core.PortalGatewayVerticle;
import com.inventage.portal.gateway.core.application.Application;
import com.inventage.portal.gateway.core.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.core.entrypoint.Entrypoint;
import com.inventage.portal.gateway.core.provider.aggregator.ProviderAggregator;
import com.inventage.portal.gateway.core.router.RouterFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Application for the proxy feature of the portal gateway. The routers will be read from the
 * "routers" configuration key.
 */
public class ProxyApplication implements Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyApplication.class);

    public static final String ROUTERS = "routers";
    public static final String ROUTER_NAME = "name";
    public static final String PATH_PREFIX = "pathPrefix";
    public static final String SERVICES = "services";
    public static final String SERVICE = "service";
    public static final String SERVICE_NAME = "name";
    public static final String MIDDLEWARE = "middleware";

    /**
     * the name of this instance
     */
    private final String name;

    /**
     * the name of the entrypoint this application should be mounted on
     */
    private final String entrypoint;

    /** the global configuration */
    private final JsonObject staticConfig;

    /**
     * the selection criteria for delegating incoming requests to this application
     */
    private String rootPath = "/";

    /**
     * the router on which the routes for this application will be added
     */
    private Router router;

    private final int providersThrottleDuration = 2;

    public ProxyApplication(String name, String entrypoint, JsonObject staticConfig, Vertx vertx) {
        this.name = name;
        this.entrypoint = entrypoint;
        this.staticConfig = staticConfig;
        this.router = Router.router(vertx);
    }

    public String toString() {
        return String.format("%s:%s", getClass().getSimpleName(), name);
    }

    @Override
    public String rootPath() {
        return rootPath;
    }

    @Override
    public Optional<Router> router() {
        return Optional.of(router);
    }

    @Override
    public String entrypoint() {
        return entrypoint;
    }

    @Override
    public Future<?> deployOn(Vertx vertx) {
        // TODO move this definition elsewhere
        String configurationAddress = "configuration-announce-address";

        ProviderAggregator aggregator =
                new ProviderAggregator(vertx, configurationAddress, staticConfig);

        ConfigurationWatcher watcher =
                new ConfigurationWatcher(vertx, aggregator, configurationAddress,
                        this.providersThrottleDuration, Arrays.asList(this.entrypoint));

        RouterFactory routerFactory = new RouterFactory(vertx);

        watcher.addListener(switchRouter(vertx, routerFactory));

        return watcher.start();
    }

    private Listener switchRouter(Vertx vertx, RouterFactory routerFactory) {
        return new Listener() {
            @Override
            public void listen(JsonObject config) {
                updateRoutes(vertx, config);
                // Router router = routerFactory.createRouter(config);
                // setRouter(router);
                // TODO update entrypoint router
            }
        };
    }

    private RoutingRule pathPrefix(Vertx vertx, String pathPrefix) {
        return new RoutingRule() {
            @Override
            public Route apply(Router router) {
                Router subRouter = Router.router(vertx);
                return router.mountSubRouter(pathPrefix, subRouter);
            }
        };
    }

    private RoutingRule host(Vertx vertx, String host) {
        return new RoutingRule() {
            @Override
            public Route apply(Router router) {
                Router subRouter = Router.router(vertx);
                return subRouter.route().virtualHost(host);
            }

        };
    }

    // only rules like PathPrefix('/abc') and Host('example.com') are supported
    private RoutingRule parseRule(Vertx vertx, String rule) {
        Pattern rulePattern =
                Pattern.compile("^(?<ruleName>(Path|Host))\\('(?<ruleValue>[a-zA-Z]+)'\\)$");
        Matcher m = rulePattern.matcher(rule);

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
                LOGGER.error("Failed to parse rule: Unknown rule '{}'", rule);
                routingRule = null;
                break;
            }
        }
        return routingRule;
    }

    private void setRouter(Router router) {
        this.router = router;
    }

    private void updateRoutes(Vertx vertx, JsonObject config) {
        JsonObject httpConfig = config.getJsonObject(DynamicConfiguration.HTTP);
        System.out.println(httpConfig);

        JsonArray newRouterConfigs = new JsonArray();

        String provider = "com.inventage.portal.gateway.proxy.service.ServiceJsonFileProvider";
        String publicHostname =
                this.staticConfig.getString(PortalGatewayVerticle.PORTAL_GATEWAY_PUBLIC_HOSTNAME,
                        PortalGatewayVerticle.PORTAL_GATEWAY_PUBLIC_HOSTNAME_DEFAULT);

        List<RouterVerticle> proxyVerticles = new ArrayList<>();

        JsonArray routers = httpConfig.getJsonArray(DynamicConfiguration.ROUTERS);
        JsonArray services = httpConfig.getJsonArray(DynamicConfiguration.SERVICES);

        for (int i = 0; i < routers.size(); i++) {
            JsonObject router = routers.getJsonObject(i);
            String serviceName = router.getString(DynamicConfiguration.ROUTER_SERVICE);
            JsonObject service = DynamicConfiguration.getObjByKeyWithValue(services,
                    DynamicConfiguration.SERVICE_NAME, serviceName);

            // TODO substitute with parsed rule
            String rule = router.getString(DynamicConfiguration.ROUTER_RULE);
            String pathPrefix =
                    rule.substring(rule.indexOf("(") + 1, rule.indexOf(")")).replace("'", "");

            // TODO support multipe servers
            JsonArray servers = service.getJsonArray(DynamicConfiguration.SERVICE_SERVERS);
            String host =
                    servers.getJsonObject(0).getString(DynamicConfiguration.SERVICE_SERVER_HOST);
            String port =
                    servers.getJsonObject(0).getString(DynamicConfiguration.SERVICE_SERVER_PORT);

            JsonObject routerConfig = new JsonObject().put(ROUTER_NAME,
                    router.getString(DynamicConfiguration.ROUTER_NAME));

            JsonObject serviceConfig =
                    new JsonObject().put(SERVICE_NAME, serviceName).put(PROVIDER, provider)
                            .put("serverHost", host).put("serverPort", Integer.parseInt(port));

            // TODO read middlewares
            Optional<JsonObject> middlewareConfiguration = null;

            final Router proxyRouter = Router.router(vertx);

            RouterVerticle proxyVerticle =
                    new RouterVerticle(routerConfig, publicHostname,
                            Entrypoint.entrypointConfigByName(entrypoint, this.staticConfig)
                                    .getInteger(Entrypoint.PORT),
                            middlewareConfiguration, serviceConfig, proxyRouter);
            proxyVerticles.add(proxyVerticle);

            newRouterConfigs
                    .add(new JsonObject().put("path", pathPrefix).put("router", proxyRouter));
        }

        CompositeFuture.join(
                proxyVerticles.stream().map(proxyVerticle -> vertx.deployVerticle(proxyVerticle))
                        .collect(Collectors.toList()))
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        this.router.clear();
                        for (int i = 0; i < newRouterConfigs.size(); i++) {
                            JsonObject newRouterConfig = newRouterConfigs.getJsonObject(i);
                            String path = newRouterConfig.getString("path");
                            Router r = ((Router) newRouterConfig.getValue("router"));
                            this.router.mountSubRouter(path, r);
                        }
                        // TODO undeploy the old proxyVerticles
                        LOGGER.info("Router updated");
                    } else {
                        LOGGER.error("Router update failed '{}'", ar.cause());
                    }
                });
    }

    private Optional<JsonObject> middlewareConfig(JsonObject proxy) {
        final JsonObject middleware = proxy.getJsonObject(MIDDLEWARE);
        if (middleware != null) {
            return Optional.of(middleware);
        } else {
            return Optional.empty();
        }
    }
}
