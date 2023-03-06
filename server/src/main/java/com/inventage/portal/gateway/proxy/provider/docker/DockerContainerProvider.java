package com.inventage.portal.gateway.proxy.provider.docker;

import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.config.label.Parser;
import com.inventage.portal.gateway.proxy.provider.Provider;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.spi.ServiceImporter;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates a complete dynamic configuration from announcements about created/removed docker
 * containers.
 */
public class DockerContainerProvider extends Provider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerContainerProvider.class);

    private static final String ANNOUNCE_ADDRESS = "docker-container-announce";

    private static final String EXTRA_CONFIG_ENABLE = "enable";
    private static final String EXTRA_CONFIG_NETWORK = "network";

    private final Vertx vertx;

    private final EventBus eb;
    private final String configurationAddress;

    private final ServiceImporter serviceImporter;
    private ServiceDiscovery dockerDiscovery;
    private final JsonObject serviceImporterConfiguration;

    private final Boolean exposedByDefault;
    private final String network;
    private final String defaultRule;
    private final Boolean watch;

    private final Map<String, JsonObject> configurations = new HashMap<>();

    public DockerContainerProvider(Vertx vertx, String configurationAddress, ServiceImporter serviceImporter,
                                   JsonObject serviceImporterConfiguration, Boolean exposedByDefault, String network, String defaultRule,
                                   Boolean watch) {
        this.vertx = vertx;
        this.eb = this.vertx.eventBus();
        this.configurationAddress = configurationAddress;
        this.serviceImporter = serviceImporter;
        if (serviceImporterConfiguration == null) {
            serviceImporterConfiguration = new JsonObject();
        }
        this.serviceImporterConfiguration = serviceImporterConfiguration;

        this.exposedByDefault = exposedByDefault;
        this.network = network;
        this.defaultRule = defaultRule;
        this.watch = watch;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        provide(startPromise);
    }

    @Override
    public void provide(Promise<Void> startPromise) {
        final MessageConsumer<JsonObject> consumer = this.eb.consumer(ANNOUNCE_ADDRESS);
        consumer.handler(message -> {
            final JsonObject config = this.buildConfiguration(message.body());
            if (config == null) {
                return;
            }
            LOGGER.debug("Configuration from docker '{}'", config);
            validateAndPublish(config);

            if (!this.watch) {
                LOGGER.debug("Stop listening for new configurations");
                consumer.unregister();
            }
        });

        this.getOrCreateDockerContainerDiscovery();

        startPromise.complete();
    }

    public String toString() {
        return StaticConfiguration.PROVIDER_DOCKER;
    }

    private ServiceDiscovery getOrCreateDockerContainerDiscovery() {
        if (this.dockerDiscovery == null) {
            this.dockerDiscovery = ServiceDiscovery.create(this.vertx,
                    new ServiceDiscoveryOptions().setAnnounceAddress(ANNOUNCE_ADDRESS).setName("docker-discovery"));
            this.dockerDiscovery.registerServiceImporter(this.serviceImporter, this.serviceImporterConfiguration);
        }
        return this.dockerDiscovery;
    }

    private JsonObject buildConfiguration(JsonObject dockerContainer) {
        final JsonObject labels = dockerContainer.getJsonObject("metadata");

        final String containerId = labels.getString("docker.id");
        final String containerName = labels.getString("docker.name");

        final String status = dockerContainer.getString("status");
        if (status.equals("DOWN")) {
            LOGGER.debug("Received announcement of removed docker container '{}'", containerName);
            if (this.configurations.containsKey(containerId)) {
                this.configurations.remove(containerId);
                return DynamicConfiguration.merge(this.configurations);
            }
            return null;
        }
        else if (!status.equals("UP")) { // OUT_OF_SERVICE, UNKNOWN
            LOGGER.warn("Ignoring unknown status '{}'", status);
            return null;
        }
        LOGGER.debug("Received announcement of new docker container '{}'", containerName);

        final JsonObject extraConfig = filterExtraConfig(labels);
        if (!keepContainer(extraConfig)) {
            LOGGER.debug("Ignoring docker container '{}'", containerName);
            return null;
        }

        LOGGER.debug("Build configuration for docker container: '{}'", containerName);

        final List<String> filters = Collections.singletonList(String.format("%s.http", Parser.DEFAULT_ROOT_NAME));
        final JsonObject confFromLabels = Parser.decode(labels.getMap(), Parser.DEFAULT_ROOT_NAME, filters);
        if (confFromLabels == null) {
            LOGGER.warn("Failed to decode labels to json for docker container '{}'", containerName);
            return null;
        }

        final JsonObject httpConfFromLabels = confFromLabels.getJsonObject(DynamicConfiguration.HTTP);
        final JsonArray ports = labels.getJsonArray("docker.ports");
        final int port = getPort(httpConfFromLabels, ports, containerName);
        if (port < 0) {
            LOGGER.warn("Failed to determine port for container '{}'", containerName);
            return null;
        }
        LOGGER.debug("Using port '{}' of '{}'", port, containerName);

        final String host = getHost(labels, extraConfig, containerName);
        LOGGER.debug("Using host '{}' of '{}'", host, containerName);

        final JsonArray serviceConfig = this.buildServiceConfiguration(httpConfFromLabels, containerName, host, port);
        if (serviceConfig == null) {
            LOGGER.warn("Failed to build configuration for docker container '{}'", containerName);
            return null;
        }

        final Map<String, String> model = new HashMap<>();
        model.put("name", containerName);
        final List<String> filteredKeys = Parser.filterKeys(labels.getMap(), List.of(Parser.DEFAULT_ROOT_NAME));
        for (String filteredKey : filteredKeys) {
            model.put(filteredKey, (String) labels.getMap().get(filteredKey));
        }

        final JsonArray routerConfig = this.buildRouterConfiguration(httpConfFromLabels, model);
        if (routerConfig == null) {
            LOGGER.warn("Failed to build router configuration for service '{}'", containerName);
            return null;
        }

        DynamicConfiguration.validate(vertx, confFromLabels, false).onSuccess(handler -> {
            LOGGER.debug("Configuration from labels '{}'", confFromLabels);
            this.configurations.put(containerId, confFromLabels);
        }).onFailure(err -> LOGGER.warn(
                "Invalid configuration form container labels '{}' (container name: '{}', labels: '{}')",
                err.getMessage(), containerName, confFromLabels));

        return DynamicConfiguration.merge(this.configurations);
    }

    private JsonObject filterExtraConfig(JsonObject labels) {
        final String enableFilter = String.format("%s.%s", Parser.DEFAULT_ROOT_NAME, EXTRA_CONFIG_ENABLE);
        final String dockerFilter = String.format("%s.%s", Parser.DEFAULT_ROOT_NAME, StaticConfiguration.PROVIDER_DOCKER);
        JsonObject extraConfig = Parser.decode(labels.getMap(), Parser.DEFAULT_ROOT_NAME,
                Arrays.asList(dockerFilter, enableFilter));

        if (extraConfig == null) {
            extraConfig = new JsonObject();
        }
        if (!extraConfig.containsKey(EXTRA_CONFIG_ENABLE)) {
            extraConfig.put(EXTRA_CONFIG_ENABLE, this.exposedByDefault);
        }
        return extraConfig;
    }

    private Boolean keepContainer(JsonObject extraConfig) {
        if (!extraConfig.getBoolean(EXTRA_CONFIG_ENABLE, true)) {
            LOGGER.debug("Filtering disabled container");
            return false;
        }

        return true;
    }

    // If a container is linked to several networks and no network is specified, then it will
    // randomly pick one (depending on how docker is returning them).
    private String getHost(JsonObject labels, JsonObject extraConfig, String containerName) {
        final JsonObject hostPerNetwork = labels.getJsonObject("docker.hostPerNetwork");
        String host = null;
        if (hostPerNetwork.size() < 1) {
            final String defaultNetwork = "defaultNetworkMode";
            LOGGER.debug("Use default network mode '{}' of container '{}'", defaultNetwork, containerName);
            host = hostPerNetwork.getString(defaultNetwork);
        }
        else if (hostPerNetwork.size() > 1) {
            LOGGER.debug("Container '{}' is linked to several networks (total: {})", containerName,
                    hostPerNetwork.size());
            final JsonObject dockerExtraConfig = extraConfig.getJsonObject(StaticConfiguration.PROVIDER_DOCKER);
            String network = null;
            if (dockerExtraConfig != null && dockerExtraConfig.containsKey(EXTRA_CONFIG_NETWORK)) {
                network = dockerExtraConfig.getString(EXTRA_CONFIG_NETWORK);
                LOGGER.debug("Trying network '{}' as specified in the labels of container '{}'", network,
                        containerName);
            }
            else if (this.network != null && this.network.length() != 0) {
                network = this.network;
                LOGGER.debug(
                        "Trying network '{}' as specified in the provider configuration of container '{}'",
                        network, containerName);
            }

            if (network != null) {
                if (hostPerNetwork.containsKey(network)) {
                    LOGGER.debug("Using network '{}' of container '{}'", network, containerName);
                    host = hostPerNetwork.getString(network);
                }
                else {
                    LOGGER.info("Unknown network '{}'. Using random one of container '{}'.", network,
                            containerName);
                    for (Object h : hostPerNetwork.getMap().values()) {
                        host = (String) h;
                        break;
                    }
                }
            }
            else {
                LOGGER.info("No network specified. Using random one of container '{}'", containerName);
                for (Object h : hostPerNetwork.getMap().values()) {
                    host = (String) h;
                    break;
                }
            }
        }
        else {
            for (Object h : hostPerNetwork.getMap().values()) {
                host = (String) h;
            }
        }
        return host;
    }

    private int getPort(JsonObject httpConfFromLabels, JsonArray ports, String containerName) {
        final int port;
        if (ports.size() < 1) {
            LOGGER.warn("Ignoring container with no exposed ports '{}'", containerName);
            return -1;
        }
        else if (ports.size() > 1) {
            // check if a port is specified in the labels
            LOGGER.debug("Container exposes more than one port");
            final String errMsgFormat = "getPort: Ignoring container '%s' with more than one exposed port and none specified in the labels (invalid %s)";
            final JsonArray services = httpConfFromLabels.getJsonArray(DynamicConfiguration.SERVICES);
            if (services == null || services.size() == 0) {
                LOGGER.warn(String.format(errMsgFormat, containerName, "services"));
                return -1;
            }
            if (services.size() > 1) {
                LOGGER.warn("Invalid services configuration '{}'", services);
                return -1;
            }
            final JsonObject service = services.getJsonObject(0);

            final JsonArray servers = service.getJsonArray(DynamicConfiguration.SERVICE_SERVERS);
            if (servers == null || servers.size() == 0) {
                LOGGER.warn(String.format(errMsgFormat, containerName, "servers"));
                return -1;
            }
            if (servers.size() > 1) {
                LOGGER.warn("Invalid servers configuration '{}'", servers);
                return -1;
            }

            final JsonObject server = servers.getJsonObject(0);
            port = server.getInteger(DynamicConfiguration.SERVICE_SERVER_PORT);
        }
        else {
            port = ports.getInteger(0);
        }
        return port;
    }

    private JsonArray buildServiceConfiguration(JsonObject httpConf, String containerName, String host, int port) {
        final JsonArray services = httpConf.getJsonArray(DynamicConfiguration.SERVICES);
        if (services.size() == 0) {
            final JsonObject fallbackService = new JsonObject().put(DynamicConfiguration.SERVICE_NAME, containerName)
                    .put(DynamicConfiguration.SERVICE_SERVERS, new JsonArray());
            services.add(fallbackService);
        }

        for (int i = 0; i < services.size(); i++) {
            final JsonObject service = services.getJsonObject(i);
            final JsonObject server = this.addServer(service, host, port);
            if (server == null) {
                LOGGER.warn("Failed to add server to service '{}', host '{}', port '{}'",
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
            LOGGER.warn("Service is not defined");
            return null;
        }

        JsonArray servers = service.getJsonArray(DynamicConfiguration.SERVICE_SERVERS);

        if (servers == null) {
            servers = new JsonArray();
        }
        if (servers.size() == 0) {
            servers.add(new JsonObject());
        }

        final JsonObject server = servers.getJsonObject(0);
        server.put(DynamicConfiguration.SERVICE_SERVER_HOST, host);
        server.put(DynamicConfiguration.SERVICE_SERVER_PORT, port);
        return server;
    }

    private JsonArray buildRouterConfiguration(JsonObject httpConf, Map<String, String> model) {
        final JsonArray routers = httpConf.getJsonArray(DynamicConfiguration.ROUTERS);
        final JsonArray middlewares = httpConf.getJsonArray(DynamicConfiguration.MIDDLEWARES);
        final JsonArray services = httpConf.getJsonArray(DynamicConfiguration.SERVICES);
        if (routers.size() == 0) {
            if (services.size() > 1) {
                LOGGER.warn("Could not create a router for the container: too many services '{}'", services);
                return null;
            }
            else {
                routers.add(new JsonObject());
            }
        }

        for (int i = 0; i < routers.size(); i++) {
            final JsonObject router = routers.getJsonObject(i);
            if (!router.containsKey("rule")) {
                final StringSubstitutor sub = new StringSubstitutor(model);
                final String resolvedRule = sub.replace(this.defaultRule);
                if (resolvedRule.length() == 0) {
                    LOGGER.warn("Undefined rule: '{}'", resolvedRule);
                    return null;
                }
                router.put(DynamicConfiguration.ROUTER_RULE, resolvedRule);
            }

            JsonArray middlewareNames = router.getJsonArray(DynamicConfiguration.ROUTER_MIDDLEWARES);
            if (middlewareNames == null) {
                middlewareNames = new JsonArray();
            }
            for (int j = 0; j < middlewares.size(); j++) {
                final String middlewareName = middlewares.getJsonObject(j).getString(DynamicConfiguration.MIDDLEWARE_NAME);
                if (!middlewareNames.contains(middlewareName)) {
                    middlewareNames.add(middlewareName);
                }
            }
            if (middlewareNames.size() > 0) {
                router.put(DynamicConfiguration.ROUTER_MIDDLEWARES, middlewareNames);
            }

            if (!router.containsKey(DynamicConfiguration.ROUTER_SERVICE)) {
                if (services.size() > 1) {
                    LOGGER.warn("Could not define the service name for the router: too many services '{}'", services);
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
        DynamicConfiguration.validate(this.vertx, config, false).onSuccess(handler -> {
            LOGGER.info("Configuration published");
            this.eb.publish(this.configurationAddress,
                    new JsonObject().put(Provider.PROVIDER_NAME, StaticConfiguration.PROVIDER_DOCKER)
                            .put(Provider.PROVIDER_CONFIGURATION, config));
        }).onFailure(err -> LOGGER.warn("Unable to publish invalid configuration '{}': '{}'", config,
                err.getMessage()));
    }
}
