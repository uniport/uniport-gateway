package com.inventage.portal.gateway.proxy;

import com.inventage.portal.gateway.core.PortalGatewayVerticle;
import com.inventage.portal.gateway.core.application.Application;
import com.inventage.portal.gateway.core.config.ConfigAdapter;
import com.inventage.portal.gateway.core.entrypoint.Entrypoint;
import com.inventage.portal.gateway.core.provider.docker.DockerContainerServiceImporter;
import com.inventage.portal.gateway.proxy.oauth2.OAuth2Configuration;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;

import java.util.*;

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

    private Set<String> proxyNames = new HashSet<>();

    private Map<String, String> deployedServices = new HashMap<>();

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
        String endpoint = "unix:///var/run/docker.sock";
        ServiceDiscovery dockerDiscovery = ServiceDiscovery.create(vertx,
                new ServiceDiscoveryOptions().setAnnounceAddress(announceAddress).setName("docker-discovery"));
        dockerDiscovery.registerServiceImporter(new DockerContainerServiceImporter(),
                new JsonObject().put("docker-tls-verify", false).put("docker-host", endpoint));

        EventBus eb = vertx.eventBus();
        MessageConsumer<JsonObject> announceConsumer = eb.consumer(announceAddress);
        announceConsumer.handler(service -> {
            // TODO send object instead
            // https://vertx.io/docs/vertx-core/java/#_message_codecs
            JsonObject record = service.body();

            String ID = record.getString("docker.id");
            String name = record.getString("docker.name");

            String pathPrefix = "/"; // TODO read from labels

            String status = record.getString("status");
            switch (status) {
            case "UP":
                JsonObject location = record.getJsonObject("location");
                String host = location.getString("host");
                int port = location.getInteger("port");

                // TODO not happy with this
                String provider = "com.inventage.portal.gateway.proxy.service.ServiceJsonFileProvider";

                // TODO everthing that comes from routerConfig should be removed
                String publicHostname = globalConfig.getString(PortalGatewayVerticle.PORTAL_GATEWAY_PUBLIC_HOSTNAME,
                        PortalGatewayVerticle.PORTAL_GATEWAY_PUBLIC_HOSTNAME_DEFAULT);
                JsonObject routerConfig = new JsonObject().put("name", name).put("pathPrefix", pathPrefix)
                        .put("service", ID);
                JsonObject serviceConfig = new JsonObject().put("name", ID).put("provider", provider)
                        .put("serverHost", host).put("serverPort", port);
                Router proxyRouter = Router.router(vertx);
                Optional<OAuth2Configuration> oAuth2Configuration = oAuth2Configuration(routerConfig);
                Optional<JsonObject> middlewareConfiguration = middlewareConfig(routerConfig);

                ProxyVerticle proxyVerticle = new ProxyVerticle(routerConfig, publicHostname,
                        Entrypoint.entrypointConfigByName(entrypoint, globalConfig).getInteger(Entrypoint.PORT),
                        middlewareConfiguration, serviceConfig, proxyRouter, oAuth2Configuration);

                router.mountSubRouter(pathPrefix, proxyRouter);

                vertx.deployVerticle(proxyVerticle, asyncResult -> {
                    if (asyncResult.succeeded()) {
                        String deploymentID = asyncResult.result();
                        System.out.println("Deployed " + deploymentID);
                        deployedServices.put(ID, deploymentID);
                    } else {
                        System.out.println("Deployment failed");
                    }
                });
                break;
            case "DOWN":
                String deploymentID = deployedServices.get(ID);
                unMountSubRouter(pathPrefix, router);
                vertx.undeploy(deploymentID, asyncResult -> {
                    if (asyncResult.succeeded()) {
                        System.out.println("Undeployed " + deploymentID);
                    } else {
                        System.out.println("Undeploment failed " + deploymentID);
                    }
                });
                deployedServices.remove(ID);
                break;
            default:
                System.out.println("Unknown Status received " + status);
                break;
            }
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

    public void unMountSubRouter(String mountPoint, Router router) {
        router.getRoutes().stream().filter(route -> route.getPath() != null && route.getPath().equals(mountPoint))
                .forEach(route -> route.remove());
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

    private JsonObject serviceConfig(String name) {
        final JsonArray configs = globalConfig.getJsonArray(SERVICES);
        return configs.stream().map(object -> new JsonObject(Json.encode(object)))
                .filter(service -> service.getString(SERVICE_NAME).equals(name)).findFirst().orElseThrow(() -> {
                    throw new IllegalStateException(String.format("Service '%s' not found!", name));
                });
    }

    private boolean checkUniqueness(JsonObject json) {
        if (proxyNames.contains(json.getString(ROUTER_NAME))) {
            throw new IllegalStateException(String.format("Name of proxy already used, but it must be unique '%s'",
                    json.getString(ROUTER_NAME)));
        }
        proxyNames.add(json.getString(ROUTER_NAME));
        return true;
    }
}
