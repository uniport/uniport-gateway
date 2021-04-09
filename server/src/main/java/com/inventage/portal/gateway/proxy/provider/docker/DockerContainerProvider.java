package com.inventage.portal.gateway.proxy.provider.docker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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

/**
 * Generates a complete dynamic configuration from announcements about created/removed docker
 * containers.
 */
public class DockerContainerProvider extends Provider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerContainerProvider.class);

    private static final String ANNOUNCE_ADDRESS = "docker-container-announce";

    private static final String EXTRA_CONFIG_ENABLE = "enable";
    private static final String EXTRA_CONFIG_NETWORK = "network";

    private Vertx vertx;

    private EventBus eb;
    private String configurationAddress;

    private ServiceDiscovery dockerDiscovery;

    private String endpoint;
    private Boolean exposedByDefault;
    private String network;
    private String defaultRule;
    private Boolean watch;
    private Boolean TLS;

    private Map<String, JsonObject> configurations = new HashMap<String, JsonObject>();

    public DockerContainerProvider(Vertx vertx, String configurationAddress, String endpoint,
            Boolean exposedByDefault, String network, String defaultRule, Boolean watch) {
        this.vertx = vertx;
        this.eb = this.vertx.eventBus();
        this.configurationAddress = configurationAddress;

        this.endpoint = endpoint;
        this.exposedByDefault = exposedByDefault;
        this.network = network;
        this.defaultRule = defaultRule;
        this.watch = watch;

        this.TLS = false;
    }

    public void start(Promise<Void> startPromise) {
        provide(startPromise);
    }

    @Override
    public void provide(Promise<Void> startPromise) {
        this.getOrCreateDockerContainerDiscovery();

        MessageConsumer<JsonObject> consumer = this.eb.consumer(ANNOUNCE_ADDRESS);
        consumer.handler(message -> {
            JsonObject config = this.buildConfiguration(message.body());
            if (config == null) {
                return;
            }
            LOGGER.debug("provide: configuration from docker '{}'", config);
            validateAndPublish(config);

            if (!this.watch) {
                consumer.unregister();
            }
        });
        startPromise.complete();
    }

    public String toString() {
        return StaticConfiguration.PROVIDER_DOCKER;
    }

    private ServiceDiscovery getOrCreateDockerContainerDiscovery() {
        if (this.dockerDiscovery == null) {
            this.dockerDiscovery = ServiceDiscovery.create(vertx, new ServiceDiscoveryOptions()
                    .setAnnounceAddress(ANNOUNCE_ADDRESS).setName("docker-discovery"));
            this.dockerDiscovery.registerServiceImporter(new DockerContainerServiceImporter(),
                    new JsonObject().put("docker-tls-verify", this.TLS).put("docker-host",
                            this.endpoint));
        }
        return this.dockerDiscovery;
    }

    private JsonObject buildConfiguration(JsonObject dockerContainer) {
        JsonObject labels = dockerContainer.getJsonObject("metadata");

        String containerId = labels.getString("docker.id");
        String containerName = labels.getString("docker.name");

        String status = dockerContainer.getString("status");
        if (status.equals("DOWN")) {
            if (this.configurations.containsKey(containerId)) {
                this.configurations.remove(containerId);
                return DynamicConfiguration.merge(this.configurations);
            }
            return null;
        } else if (!status.equals("UP")) { // OUT_OF_SERVICE, UNKOWN
            LOGGER.warn("buildConfiguration: unkown status type: '{}'", status);
            return null;
        }

        JsonObject extraConfig = filterExtraConfig(labels);
        if (!keepContainer(extraConfig)) {
            LOGGER.debug("buildConfiguration: ignoring container '{}'", containerName);
            return null;
        }

        LOGGER.debug("buildConfiguration: build configuration for docker container: '{}'",
                containerName);

        List<String> filters = Arrays.asList(String.format("%s.http", Parser.DEFAULT_ROOT_NAME));
        JsonObject confFromLabels =
                Parser.decode(labels.getMap(), Parser.DEFAULT_ROOT_NAME, filters);
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
            this.configurations.put(containerId, confFromLabels);
            return DynamicConfiguration.merge(this.configurations);
        }

        JsonArray ports = labels.getJsonArray("docker.ports");
        String serviceName = containerName;
        int port = getPort(httpConfFromLabels, ports, serviceName, containerName);
        if (port < 0) {
            LOGGER.warn("buildConfiguration: failed to determine port for container '{}'",
                    containerName);
            return null;
        }
        LOGGER.debug("buildConfiguration: using port '{}' of '{}'", port, containerName);

        String host = getHost(labels, extraConfig, containerName);
        LOGGER.debug("buildConfiguration: using host '{}' of '{}'", host, containerName);

        JsonArray serviceConfig =
                this.buildServiceConfiguration(httpConfFromLabels, serviceName, host, port);
        if (serviceConfig == null) {
            LOGGER.warn(
                    "buildConfiguration: failed to build configuration for docker container '{}'",
                    containerName);
            return null;
        }

        Map<String, String> model = new HashMap<String, String>();
        model.put("name", serviceName);
        List<String> filteredKeys = Parser.filterKeys(labels.getMap(), filters);
        for (String filteredKey : filteredKeys) {
            model.put(filteredKey, (String) labels.getMap().get(filteredKey));
        }

        JsonArray routerConfig = this.buildRouterConfiguration(httpConfFromLabels, model);
        if (routerConfig == null) {
            LOGGER.warn("buildConfiguration: failed to build router configuration for service '{}'",
                    containerName);
            return null;
        }

        DynamicConfiguration.validate(vertx, confFromLabels, false).onSuccess(handler -> {
            LOGGER.debug("buildConfiguration: configuration from labels '{}'", confFromLabels);
            this.configurations.put(containerId, confFromLabels);
        }).onFailure(err -> {
            LOGGER.warn(
                    "buildConfiguration: invalid configuration form container labels '{}' (container name: '{}', labels: '{}')",
                    err.getMessage(), containerName, confFromLabels);
        });

        return DynamicConfiguration.merge(this.configurations);
    }

    private JsonObject filterExtraConfig(JsonObject labels) {
        String enableFilter = String.format("%s.%s", Parser.DEFAULT_ROOT_NAME, EXTRA_CONFIG_ENABLE);
        String dockerFilter = String.format("%s.%s", Parser.DEFAULT_ROOT_NAME,
                StaticConfiguration.PROVIDER_DOCKER);
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
            LOGGER.debug("keepContainer: filtering disabled container");
            return false;
        }

        return true;
    }

    // If a container is linked to several networks and no network is specified, then it will
    // randomly pick one (depending on how docker is returning them).
    private String getHost(JsonObject labels, JsonObject extraConfig, String containerName) {
        JsonObject hostPerNetwork = labels.getJsonObject("docker.hostPerNetwork");
        String host = null;
        if (hostPerNetwork.size() < 1) {
            String defaultNetwork = "defaultNetworkMode";
            LOGGER.debug("getHost: use default network mode '{}' of container '{}'", defaultNetwork,
                    containerName);
            host = hostPerNetwork.getString(defaultNetwork);
        } else if (hostPerNetwork.size() > 1) {
            LOGGER.debug("getHost: container '{}' is linked to several networks (total: {})",
                    containerName, hostPerNetwork.size());
            JsonObject dockerExtraConfig =
                    extraConfig.getJsonObject(StaticConfiguration.PROVIDER_DOCKER);
            String network = null;
            if (dockerExtraConfig != null && dockerExtraConfig.containsKey(EXTRA_CONFIG_NETWORK)) {
                network = dockerExtraConfig.getString(EXTRA_CONFIG_NETWORK);
                LOGGER.debug(
                        "getHost: trying network '{}' as specified in the labels of container '{}'",
                        network, containerName);
            } else if (this.network != null && this.network.length() != 0) {
                network = this.network;
                LOGGER.debug(
                        "getHost: trying network '{}' as specified in the provider configuration of container '{}'",
                        network, containerName);
            }

            if (network != null) {
                if (hostPerNetwork.containsKey(network)) {
                    LOGGER.debug("getHost: using network '{}' of container '{}'", network,
                            containerName);
                    host = hostPerNetwork.getString(network);
                } else {
                    LOGGER.info(
                            "getHost: unknown network '{}'. Using random one of container '{}'.",
                            network, containerName);
                    for (Object h : hostPerNetwork.getMap().values()) {
                        host = (String) h;
                        break;
                    }
                }
            } else {
                LOGGER.info("getHost: no network specified. Using random one of container '{}'",
                        containerName);
                for (Object h : hostPerNetwork.getMap().values()) {
                    host = (String) h;
                    break;
                }
            }
        } else {
            for (Object h : hostPerNetwork.getMap().values()) {
                host = (String) h;
            }
        }
        return host;
    }

    // side effect: serviceName might be changed
    private int getPort(JsonObject httpConfFromLabels, JsonArray ports, String serviceName,
            String containerName) {
        int port;
        if (ports.size() < 1) {
            LOGGER.warn("getPort: ignoring container with no exposed ports '{}'", containerName);
            return -1;
        } else if (ports.size() > 1) {
            // check if a port is specified in the labels
            LOGGER.debug("getPort: container exposes more than one port");
            String errMsgFormat =
                    "getPort: Ignoring container '%s' with more than one exposed port and none specified in the labels (invalid %s)";
            JsonArray services = httpConfFromLabels.getJsonArray(DynamicConfiguration.SERVICES);
            if (services == null || services.size() == 0) {
                LOGGER.warn(String.format(errMsgFormat, containerName, "services"));
                return -1;
            }
            if (services.size() > 1) {
                LOGGER.warn("getPort: Invalid services configuration '{}'", services);
                return -1;
            }
            JsonObject service = services.getJsonObject(0);
            serviceName = service.getString(DynamicConfiguration.SERVICE_NAME);

            JsonArray servers = service.getJsonArray(DynamicConfiguration.SERVICE_SERVERS);
            if (servers == null || servers.size() == 0) {
                LOGGER.warn(String.format(errMsgFormat, containerName, "servers"));
                return -1;
            }
            if (servers.size() > 1) {
                LOGGER.warn("getPort: Invalid servers configuration '{}'", servers);
                return -1;
            }

            JsonObject server = servers.getJsonObject(0);
            port = server.getInteger(DynamicConfiguration.SERVICE_SERVER_PORT);
        } else {
            port = ports.getInteger(0);
        }
        return port;
    }

    private JsonArray buildServiceConfiguration(JsonObject httpConf, String containerName,
            String host, int port) {
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
        DynamicConfiguration.validate(this.vertx, config, false).onSuccess(handler -> {
            LOGGER.info("validateAndPublish: configuration published");
            this.eb.publish(this.configurationAddress,
                    new JsonObject()
                            .put(Provider.PROVIDER_NAME, StaticConfiguration.PROVIDER_DOCKER)
                            .put(Provider.PROVIDER_CONFIGURATION, config));
        }).onFailure(err -> {
            LOGGER.warn("validateAndPublish: unable to publish invalid configuration '{}': '{}'",
                    config, err.getMessage());
        });
    }
}
