package com.inventage.portal.gateway.proxy.provider.docker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.config.label.Parser;
import com.inventage.portal.gateway.proxy.provider.Provider;
import com.inventage.portal.gateway.proxy.provider.docker.servicediscovery.DockerContainerServiceImporter;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;

public class DockerContainerProvider extends Provider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerContainerProvider.class);

    private static final String announceAddress = "docker-container-announce";
    private static final String defaultTempateRule = "Host('${name}')";

    private Vertx vertx;

    private EventBus eb;
    private String configurationAddress;

    private ServiceDiscovery dockerContainerDiscovery;

    private Boolean watch;
    private String endpoint;
    private String defaultRule;
    private Boolean TLS;

    private Map<String, JsonObject> configurations = new HashMap<String, JsonObject>();

    public DockerContainerProvider(Vertx vertx, String configurationAddress) {
        this.vertx = vertx;
        this.eb = this.vertx.eventBus();
        this.configurationAddress = configurationAddress;

        this.watch = true;
        this.endpoint = "unix:///var/run/docker.sock";
        this.defaultRule = defaultTempateRule;
        this.TLS = false;
    }

    public void start(Promise<Void> startPromise) {
        provide(startPromise);
    }

    @Override
    public void provide(Promise<Void> startPromise) {
        this.getOrCreateDockerContainerDiscovery();

        MessageConsumer<JsonObject> consumer = this.eb.consumer(announceAddress);
        consumer.handler(message -> {
            JsonObject config = this.buildConfiguration(message.body());
            validateAndPublish(config);

            if (!this.watch) {
                consumer.unregister();
            }
        });
        startPromise.complete();
    }

    private ServiceDiscovery getOrCreateDockerContainerDiscovery() {
        if (this.dockerContainerDiscovery == null) {
            this.dockerContainerDiscovery =
                    ServiceDiscovery.create(vertx, new ServiceDiscoveryOptions()
                            .setAnnounceAddress(announceAddress).setName("docker-discovery"));
            this.dockerContainerDiscovery
                    .registerServiceImporter(new DockerContainerServiceImporter(), new JsonObject()
                            .put("docker-tls-verify", this.TLS).put("docker-host", this.endpoint));
        }
        return this.dockerContainerDiscovery;
    }

    private JsonObject buildConfiguration(JsonObject dockerContainer) {
        JsonObject metadata = dockerContainer.getJsonObject("metadata");

        String serviceName = metadata.getString("portal.docker.serviceName");
        String containerId = metadata.getString("portal.docker.id");
        String containerName = serviceName + "-" + containerId;

        String status = dockerContainer.getString("status");
        if (status.equals("DOWN")) {
            this.configurations.remove(containerName);
            return DynamicConfiguration.merge(this.configurations);
        } else if (!status.equals("UP")) { // OUT_OF_SERVICE, UNKOWN
            LOGGER.warn("buildConfiguration: unkown status type: " + status);
            return null;
        }

        JsonObject labels = metadata.getJsonObject("portal.docker.labels");

        String host = metadata.getString("portal.docker.ip");
        int port = metadata.getInteger("portal.docker.port");

        JsonObject confFromLabels = Parser.decode(labels.getMap(), Parser.DEFAULT_ROOT_NAME,
                Arrays.asList("portal.http"));
        if (confFromLabels == null) {
            LOGGER.warn("buildConfiguration: failed to decode labels to json for service '{}'",
                    serviceName);
            return null;
        }

        JsonObject httpConfFromLabels = confFromLabels.getJsonObject(DynamicConfiguration.HTTP);
        if (httpConfFromLabels.getJsonArray(DynamicConfiguration.ROUTERS).size() == 0
                && httpConfFromLabels.getJsonArray(DynamicConfiguration.MIDDLEWARES).size() == 0
                && httpConfFromLabels.getJsonArray(DynamicConfiguration.SERVICES).size() == 0) {
            this.configurations.put(containerName, confFromLabels);
            return DynamicConfiguration.merge(this.configurations);
        }

        JsonArray serviceConfig =
                this.buildServiceConfiguration(httpConfFromLabels, serviceName, host, port);
        if (serviceConfig == null) {
            LOGGER.warn("buildConfiguration: failed to build configuration for service '{}'",
                    serviceName);
            return null;
        }

        Map<String, String> model = new HashMap<String, String>();
        model.put("name", serviceName);

        JsonArray routerConfig =
                this.buildRouterConfiguration(httpConfFromLabels, serviceName, model);
        if (routerConfig == null) {
            LOGGER.warn("buildConfiguration: failed to build router configuration for service '{}'",
                    serviceName);
            return null;
        }

        DynamicConfiguration.validate(vertx, confFromLabels).onComplete(ar -> {
            if (ar.succeeded()) {
                LOGGER.debug("buildConfiguration: configuration from labels '{}'", confFromLabels);
                this.configurations.put(containerName, confFromLabels);
            } else {
                LOGGER.warn(
                        "buildConfiguration: invalid configuration form container labels '{}': '{}'",
                        serviceName, confFromLabels);
            }
        });

        return DynamicConfiguration.merge(this.configurations);
    }

    private JsonArray buildServiceConfiguration(JsonObject httpConf, String serviceName,
            String host, int port) {
        JsonArray services = httpConf.getJsonArray(DynamicConfiguration.SERVICES);
        if (services.size() == 0) {
            JsonObject fallbackService =
                    new JsonObject().put(DynamicConfiguration.SERVICE_NAME, serviceName)
                            .put(DynamicConfiguration.SERVICE_SERVERS, new JsonArray());
            services.add(fallbackService);
        }

        for (int i = 0; i < services.size(); i++) {
            JsonObject service = services.getJsonObject(i);
            JsonObject server = this.addServer(service, host, port);
            if (server == null) {
                LOGGER.warn(
                        "buildServiceConfiguration: failed to add server to service '{}', host '{}', port '{}'",
                        service, host, port);
                return null;
            }
        }
        return services;
    }

    // there is at most one docker container per service
    // since docker does not provide a out of the book load balancer
    // newer containers overwrite the old one
    private JsonObject addServer(JsonObject service, String host, int port) {
        if (service == null) {
            LOGGER.warn("addServer: service is not defined");
            return null;
        }

        JsonArray servers = service.getJsonArray(DynamicConfiguration.SERVICE_SERVERS);

        if (servers == null) {
            servers = new JsonArray();
        }
        if (servers.size() == 0) {
            servers.add(new JsonObject());
        }

        JsonObject server = servers.getJsonObject(0);
        server.put(DynamicConfiguration.SERVICE_SERVER_HOST, host);
        server.put(DynamicConfiguration.SERVICE_SERVER_PORT, port);
        return server;
    }

    private JsonArray buildRouterConfiguration(JsonObject httpConf, String serviceName,
            Map<String, String> model) {
        JsonArray routers = httpConf.getJsonArray(DynamicConfiguration.ROUTERS);
        JsonArray middlewares = httpConf.getJsonArray(DynamicConfiguration.MIDDLEWARES);
        JsonArray services = httpConf.getJsonArray(DynamicConfiguration.SERVICES);
        if (routers.size() == 0) {
            if (services.size() > 1) {
                LOGGER.warn(
                        "buildRouterConfiguration: could not create a router for the container: too many services '{}'",
                        services.toString());
                return null;
            } else {
                routers.add(new JsonObject());
            }
        }

        for (int i = 0; i < routers.size(); i++) {
            JsonObject router = routers.getJsonObject(i);
            if (!router.containsKey("rule")) {
                StringSubstitutor sub = new StringSubstitutor(model);
                String resolvedRule = sub.replace(this.defaultRule);
                if (resolvedRule.length() == 0) {
                    LOGGER.warn("buildRouterConfiguration: undefined rule: '{}'", resolvedRule);
                    return null;
                }
                router.put(DynamicConfiguration.ROUTER_RULE, resolvedRule);
            }


            JsonArray middlewareNames =
                    router.getJsonArray(DynamicConfiguration.ROUTER_MIDDLEWARES);
            if (middlewareNames == null) {
                middlewareNames = new JsonArray();
            }
            for (int j = 0; j < middlewares.size(); j++) {
                String middlewareName = middlewares.getJsonObject(j)
                        .getString(DynamicConfiguration.MIDDLEWARE_NAME);
                if (!middlewareNames.contains(middlewareName)) {
                    middlewareNames.add(middlewareName);
                }
            }
            if (middlewareNames.size() > 0) {
                router.put(DynamicConfiguration.ROUTER_MIDDLEWARES, middlewareNames);
            }

            if (!router.containsKey(DynamicConfiguration.ROUTER_SERVICE)) {
                if (services.size() > 1) {
                    LOGGER.warn(
                            "buildRouterConfiguration: could not define the service name for the router: too many services '{}'",
                            services.toString());
                }
                for (int j = 0; j < services.size(); j++) {
                    router.put(DynamicConfiguration.ROUTER_SERVICE,
                            services.getJsonObject(j).getString(DynamicConfiguration.SERVICE_NAME));
                }
            }
        }
        return routers;
    }

    private void validateAndPublish(JsonObject config) {
        DynamicConfiguration.validate(this.vertx, config).onComplete(ar -> {
            if (ar.succeeded()) {
                LOGGER.info("validateAndPublish: configuration published");
                this.eb.publish(this.configurationAddress,
                        new JsonObject()
                                .put(Provider.PROVIDER_NAME, StaticConfiguration.PROVIDER_DOCKER)
                                .put(Provider.PROVIDER_CONFIGURATION, config));
            } else {
                LOGGER.warn("validateAndPublish: unable to publish invalid configuration: '{}'",
                        config);
            }
        });
    }
}
