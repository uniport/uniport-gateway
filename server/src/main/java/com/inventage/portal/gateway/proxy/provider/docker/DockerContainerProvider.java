package com.inventage.portal.gateway.proxy.provider.docker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.config.label.Parser;
import com.inventage.portal.gateway.proxy.provider.Provider;
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
import io.vertx.servicediscovery.docker.DockerServiceImporter;

/**
 * Generates a complete dynamic configuration from announcements about created/removed docker
 * containers.
 */
public class DockerContainerProvider extends Provider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerContainerProvider.class);

    private static final String announceAddress = "docker-container-announce";
    private static final String defaultTempateRule = "Host('${name}')";

    private Vertx vertx;

    private EventBus eb;
    private String configurationAddress;

    private ServiceDiscovery dockerDiscovery;

    private Boolean watch;
    private String endpoint;
    private String defaultRule;
    private Boolean TLS;

    private Map<String, JsonObject> configurations = new HashMap<String, JsonObject>();

    public DockerContainerProvider(Vertx vertx, String configurationAddress) {
        LOGGER.trace("construcutor");
        this.vertx = vertx;
        this.eb = this.vertx.eventBus();
        this.configurationAddress = configurationAddress;

        // TODO allow to configure this
        this.watch = true;
        this.endpoint = "unix:///var/run/docker.sock";
        this.defaultRule = defaultTempateRule;
        this.TLS = false;
    }

    public void start(Promise<Void> startPromise) {
        LOGGER.trace("start");
        provide(startPromise);
    }

    @Override
    public void provide(Promise<Void> startPromise) {
        LOGGER.trace("provide");
        this.getOrCreateDockerContainerDiscovery();

        MessageConsumer<JsonObject> consumer = this.eb.consumer(announceAddress);
        consumer.handler(message -> {
            JsonObject config = this.buildConfiguration(message.body());
            if (config == null) {
                return;
            }
            validateAndPublish(config);

            if (!this.watch) {
                consumer.unregister();
            }
        });
        startPromise.complete();
    }

    private ServiceDiscovery getOrCreateDockerContainerDiscovery() {
        LOGGER.trace("getOrCreateDockerContainerDiscovery");
        if (this.dockerDiscovery == null) {
            this.dockerDiscovery = ServiceDiscovery.create(vertx, new ServiceDiscoveryOptions()
                    .setAnnounceAddress(announceAddress).setName("docker-discovery"));
            this.dockerDiscovery.registerServiceImporter(new DockerServiceImporter(),
                    new JsonObject().put("docker-tls-verify", this.TLS).put("docker-host",
                            this.endpoint));
        }
        return this.dockerDiscovery;
    }

    private JsonObject buildConfiguration(JsonObject dockerContainer) {
        LOGGER.trace("buildConfiguration");
        JsonObject metadata = dockerContainer.getJsonObject("metadata");

        String containerId = metadata.getString("docker.id");
        String containerName = metadata.getString("docker.name");
        // TODO handle containers with multipe exposed ports
        // ignore if multipe ports are exposed and no port is set in the labels
        String serviceName = String.format("%s-%s", containerId, containerName);

        String status = dockerContainer.getString("status");
        if (status.equals("DOWN")) {
            this.configurations.remove(serviceName);
            return DynamicConfiguration.merge(this.configurations);
        } else if (!status.equals("UP")) { // OUT_OF_SERVICE, UNKOWN
            LOGGER.warn("buildConfiguration: unkown status type: " + status);
            return null;
        }

        JsonObject location = dockerContainer.getJsonObject("location");
        String host = location.getString("ip");
        int port = location.getInteger("port");

        LOGGER.debug("buildConfiguration: build configuration for docker container: '{}'",
                containerName);

        List<String> filters = Arrays.asList("portal.http");
        JsonObject confFromLabels =
                Parser.decode(metadata.getMap(), Parser.DEFAULT_ROOT_NAME, filters);
        if (confFromLabels == null) {
            LOGGER.warn(
                    "buildConfiguration: failed to decode labels to json for docker container '{}'",
                    containerName);
            return null;
        }

        JsonObject httpConfFromLabels = confFromLabels.getJsonObject(DynamicConfiguration.HTTP);
        if (httpConfFromLabels.getJsonArray(DynamicConfiguration.ROUTERS).size() == 0
                && httpConfFromLabels.getJsonArray(DynamicConfiguration.MIDDLEWARES).size() == 0
                && httpConfFromLabels.getJsonArray(DynamicConfiguration.SERVICES).size() == 0) {
            this.configurations.put(serviceName, confFromLabels);
            return DynamicConfiguration.merge(this.configurations);
        }

        JsonArray serviceConfig =
                this.buildServiceConfiguration(httpConfFromLabels, containerName, host, port);
        if (serviceConfig == null) {
            LOGGER.warn(
                    "buildConfiguration: failed to build configuration for docker container '{}'",
                    containerName);
            return null;
        }

        Map<String, String> model = new HashMap<String, String>();
        model.put("name", containerName);
        List<String> filteredKeys = Parser.filterKeys(metadata.getMap(), filters);
        for (String filteredKey : filteredKeys) {
            model.put(filteredKey, (String) metadata.getMap().get(filteredKey));
        }

        JsonArray routerConfig = this.buildRouterConfiguration(httpConfFromLabels, model);
        if (routerConfig == null) {
            LOGGER.warn("buildConfiguration: failed to build router configuration for service '{}'",
                    containerName);
            return null;
        }

        DynamicConfiguration.validate(vertx, confFromLabels).onComplete(ar -> {
            if (ar.succeeded()) {
                LOGGER.debug("buildConfiguration: configuration from labels '{}'", confFromLabels);
                this.configurations.put(serviceName, confFromLabels);
            } else {
                LOGGER.warn(
                        "buildConfiguration: invalid configuration form container labels '{}': '{}'",
                        containerName, confFromLabels);
            }
        });

        return DynamicConfiguration.merge(this.configurations);
    }

    private JsonArray buildServiceConfiguration(JsonObject httpConf, String containerName,
            String host, int port) {
        LOGGER.trace("buildServiceConfiguration");
        JsonArray services = httpConf.getJsonArray(DynamicConfiguration.SERVICES);
        if (services.size() == 0) {
            JsonObject fallbackService =
                    new JsonObject().put(DynamicConfiguration.SERVICE_NAME, containerName)
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
        LOGGER.trace("addServer");
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

    private JsonArray buildRouterConfiguration(JsonObject httpConf, Map<String, String> model) {
        LOGGER.trace("buildRouterConfiguration");
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
        LOGGER.trace("validateAndPublish");
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
