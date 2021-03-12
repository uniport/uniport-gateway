package com.inventage.portal.gateway.proxy;

import com.inventage.portal.gateway.core.PortalGatewayVerticle;
import com.inventage.portal.gateway.core.application.Application;
import com.inventage.portal.gateway.core.config.ConfigAdapter;
import com.inventage.portal.gateway.core.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.core.entrypoint.Entrypoint;
import com.inventage.portal.gateway.core.provider.docker.DockerContainerProvider;
import com.inventage.portal.gateway.proxy.oauth2.OAuth2Configuration;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Application for the proxy feature of the portal gateway. The routers will be
 * read from the "routers" configuration key.
 */
public class ProxyApplication implements Application {

    public static final String ROUTERS = "routers";
    public static final String ROUTER_NAME = "name";
    public static final String PATH_PREFIX = "pathPrefix";
    public static final String OAUTH2 = "oauth2";
    public static final String OAUTH2_CLIENTID = "clientId";
    public static final String OAUTH2_CLIENTSECRET = "clientSecret";
    public static final String OAUTH2_DISCOVERYURL = "discoveryUrl";
    public static final String OAUTH2_CALLBACK_PREFIX = "/callback/";
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
    private final JsonObject globalConfig;

    /**
     * the selection criteria for delegating incoming requests to this application
     */
    private String rootPath = "/";

    /**
     * the router on which the routes for this application will be added
     */
    private final Router router;

    public ProxyApplication(String name, String entrypoint, JsonObject globalConfig, Vertx vertx) {
        this.name = name;
        this.entrypoint = entrypoint;
        this.globalConfig = globalConfig;
        this.router = Router.router(vertx);
    }

    public String toString() {
        return String.format("%s:%s", getClass().getSimpleName(), name);
    }

    @Override
    public Future<?> deployOn(Vertx vertx) {
        String announceAddress = "service-announce";

        // TODO configure this according to the static configuration
        // TODO merge configs of all providers
        DockerContainerProvider dockerprovider = new DockerContainerProvider(vertx);
        dockerprovider.provide(announceAddress);

        EventBus eb = vertx.eventBus();
        MessageConsumer<JsonObject> announceConsumer = eb.consumer(announceAddress);
        announceConsumer.handler(service -> {
            JsonObject config = service.body();
            updateRoutes(vertx, config);
        });

        // TODO: not really happy with this but how should it be solved?
        // maybe ping
        return Future.succeededFuture();
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

    private void updateRoutes(Vertx vertx, JsonObject config) {
        Router newRouter = Router.router(vertx);

        System.out.println(config);
        JsonObject httpConfig = config.getJsonObject(DynamicConfiguration.HTTP);

        JsonArray newRouterConfigs = new JsonArray();

        // TODO why do we need different providers?
        // maybe also change the name to something else like RequestHandlers, Connectors
        String provider = "com.inventage.portal.gateway.proxy.service.ServiceJsonFileProvider";
        String publicHostname = globalConfig.getString(PortalGatewayVerticle.PORTAL_GATEWAY_PUBLIC_HOSTNAME,
                PortalGatewayVerticle.PORTAL_GATEWAY_PUBLIC_HOSTNAME_DEFAULT);

        List<ProxyVerticle> proxyVerticles = new ArrayList<>();

        JsonArray routers = httpConfig.getJsonArray(DynamicConfiguration.ROUTERS);
        JsonArray services = httpConfig.getJsonArray(DynamicConfiguration.SERVICES);

        for (int i = 0; i < routers.size(); i++) {
            JsonObject router = routers.getJsonObject(i);
            String serviceName = router.getString(DynamicConfiguration.ROUTER_SERVICE);
            JsonObject service = DynamicConfiguration.getObjByKeyWithValue(services, DynamicConfiguration.SERVICE_NAME,
                    serviceName);

            // TODO parse rule (https://github.com/Sallatik/predicate-parser)
            String rule = router.getString(DynamicConfiguration.ROUTER_RULE);
            String pathPrefix = rule.substring(rule.indexOf("(") + 1, rule.indexOf(")")).replace("'", "");

            // TODO support multipe servers and solve this ugly host-port splitting
            JsonArray servers = service.getJsonArray(DynamicConfiguration.SERVICE_SERVERS);
            String url = servers.getJsonObject(0).getString(DynamicConfiguration.SERVICE_SERVER_URL);
            String[] hostPort = url.split(":");

            JsonObject routerConfig = new JsonObject().put("name", router.getString(DynamicConfiguration.ROUTER_NAME))
                    .put(PATH_PREFIX, pathPrefix).put(SERVICE, serviceName);

            JsonObject serviceConfig = new JsonObject().put(SERVICE_NAME, serviceName).put(PROVIDER, provider)
                    .put("serverHost", hostPort[0]).put("serverPort", Integer.parseInt(hostPort[1]));
            System.out.println(serviceConfig);

            Optional<OAuth2Configuration> oAuth2Configuration = oAuth2Configuration(routerConfig);
            Optional<JsonObject> middlewareConfiguration = middlewareConfig(routerConfig);

            final Router proxyRouter = Router.router(vertx);

            ProxyVerticle proxyVerticle = new ProxyVerticle(routerConfig, publicHostname,
                    Entrypoint.entrypointConfigByName(entrypoint, globalConfig).getInteger(Entrypoint.PORT),
                    middlewareConfiguration, serviceConfig, proxyRouter, oAuth2Configuration);
            proxyVerticles.add(proxyVerticle);

            newRouter.mountSubRouter(pathPrefix, proxyRouter);
            newRouterConfigs.add(new JsonObject().put("path", pathPrefix).put("router", proxyRouter));
        }

        CompositeFuture.join(proxyVerticles.stream().map(proxyVerticle -> vertx.deployVerticle(proxyVerticle))
                .collect(Collectors.toList())).onComplete(ar -> {
                    if (ar.succeeded()) {
                        this.router.clear();
                        for (int i = 0; i < newRouterConfigs.size(); i++) {
                            JsonObject newRouterConfig = newRouterConfigs.getJsonObject(i);
                            String path = newRouterConfig.getString("path");
                            Router r = ((Router) newRouterConfig.getValue("router"));
                            this.router.mountSubRouter(path, r);
                        }
                        System.out.println("Router updated");
                        System.out.println(this.router.getRoutes().toString());
                        // TODO undeploy the old proxyVerticles
                    } else {
                        System.out.println("Router update failed");
                        System.out.println(ar.cause());
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

    private Optional<OAuth2Configuration> oAuth2Configuration(JsonObject proxy) {
        final JsonObject oauth2 = proxy.getJsonObject(OAUTH2);
        if (oauth2 != null) {
            return Optional.of(
                    new OAuth2Configuration(oauth2.getString(OAUTH2_CLIENTID), oauth2.getString(OAUTH2_CLIENTSECRET),
                            ConfigAdapter.replaceEnvVariables(globalConfig, oauth2.getString(OAUTH2_DISCOVERYURL)),
                            router.get(OAUTH2_CALLBACK_PREFIX + proxy.getString(ROUTER_NAME).toLowerCase())));
        } else {
            return Optional.empty();
        }
    }
}
