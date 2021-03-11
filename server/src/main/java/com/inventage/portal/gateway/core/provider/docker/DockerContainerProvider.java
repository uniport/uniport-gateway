package com.inventage.portal.gateway.core.provider.docker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.inventage.portal.gateway.core.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.core.config.label.Parser;
import com.inventage.portal.gateway.core.provider.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;

public class DockerContainerProvider implements Provider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerContainerProvider.class);
    private static final String announceAddress = "docker-container-announce";
    private static final String defaultTempateRule = "Host(`{}`)";

    private Vertx vertx;
    private EventBus eb;

    private ServiceDiscovery dockerContainerDiscovery;

    private Boolean watch;
    private String endpoint;
    private String defaultRule;
    private Boolean TLS;

    private Map<String, JsonObject> configurations = new HashMap<String, JsonObject>();

    public DockerContainerProvider(Vertx vertx) {
        this.vertx = vertx;
        this.eb = vertx.eventBus();

        this.watch = true;
        this.endpoint = "unix:///var/run/docker.sock";
        this.defaultRule = defaultTempateRule;
        this.TLS = false;
    }

    @Override
    public void provide(String configurationAddress) {
        this.getOrCreateDockerContainerDiscovery();

        MessageConsumer<JsonObject> consumer = this.eb.consumer(announceAddress);
        consumer.handler(message -> {
            JsonObject dockerContainer = message.body();

            JsonObject config = this.buildConfiguration(dockerContainer);

            DynamicConfiguration.validate(vertx, config).onComplete(validateAr -> {
                if (validateAr.succeeded()) {
                    LOGGER.info("docker container provider: configuration published");
                    eb.publish(configurationAddress, config);
                } else {
                    LOGGER.error("docker container provider: invalid configuration");
                }

                if (!this.watch) {
                    consumer.unregister();
                }
            });
        });
    }

    private ServiceDiscovery getOrCreateDockerContainerDiscovery() {
        if (this.dockerContainerDiscovery == null) {
            this.dockerContainerDiscovery = ServiceDiscovery.create(vertx,
                    new ServiceDiscoveryOptions().setAnnounceAddress(announceAddress).setName("docker-discovery"));
            this.dockerContainerDiscovery.registerServiceImporter(new DockerContainerServiceImporter(),
                    new JsonObject().put("docker-tls-verify", this.TLS).put("docker-host", this.endpoint));
        }
        return this.dockerContainerDiscovery;
    }

    private JsonObject buildConfiguration(JsonObject dockerContainer) {
        JsonObject config = new JsonObject();

        // TODO I am not really convinced with using the Record type to un/publish
        String status = dockerContainer.getString("status");
        JsonObject metadata = dockerContainer.getJsonObject("metadata");

        String serviceName = metadata.getString("portal.docker.serviceName");
        String containerId = metadata.getString("portal.docker.id");
        String containerName = serviceName + "-" + containerId;

        JsonObject labels = metadata.getJsonObject("portal.docker.labels");

        String ip = metadata.getString("portal.docker.ip");
        String port = metadata.getString("portal.docker.port");

        JsonObject confFromLabels = Parser.decode(labels.getMap(), Parser.DEFAULT_ROOT_NAME,
                Arrays.asList("portal.http"));

        JsonObject httpConf = confFromLabels.getJsonObject(DynamicConfiguration.HTTP);
        if (httpConf.getJsonArray(DynamicConfiguration.ROUTERS).size() == 0
                && httpConf.getJsonArray(DynamicConfiguration.MIDDLEWARES).size() == 0
                && httpConf.getJsonArray(DynamicConfiguration.SERVICES).size() == 0) {
            this.configurations.put(serviceName, confFromLabels);
            // TODO return merged config
        }

        this.buildServiceConfiguration(httpConf, serviceName, ip, port);

        this.buildRouterConfiguration(httpConf, serviceName);

        System.out.println(confFromLabels);

        DynamicConfiguration.validate(vertx, confFromLabels).onComplete(ar -> {
            if (ar.succeeded()) {
                LOGGER.debug("docker container provider: configuration from labels '{}'", confFromLabels);
            } else {
                LOGGER.error("docker container provider: invalid configuration form container labels '{}': '{}'",
                        serviceName, ar.cause());
            }
        });

        return config;
    }

    private void buildServiceConfiguration(JsonObject httpConf, String serviceName, String ip, String port) {
        JsonArray services = httpConf.getJsonArray(DynamicConfiguration.SERVICES);
        if (services.size() == 0) {
            JsonObject fallbackService = new JsonObject().put(DynamicConfiguration.SERVICE_NAME, serviceName)
                    .put(DynamicConfiguration.SERVICE_SERVERS, new JsonArray());
            services.add(fallbackService);
        }

        List<JsonObject> servicesAsList = services.getList();
        for (int i = 0; i < servicesAsList.size(); i++) {
            JsonObject service = servicesAsList.get(i);
            this.addServer(service, ip, port);
        }
    }

    // there is at most one docker container per service
    // since docker does not provide a out of the book load balancer
    // newer containers overwrite the old one
    private void addServer(JsonObject service, String ip, String port) {
        if (service == null) {
            throw new IllegalArgumentException("service is not defined");
        }

        JsonArray servers = service.getJsonArray(DynamicConfiguration.SERVICE_SERVERS);

        if (servers == null) {
            servers = new JsonArray();
        }
        if (servers.size() == 0) {
            servers.add(new JsonObject());
        }

        String url = ip + ":" + port;
        servers.getJsonObject(0).put(DynamicConfiguration.SERVICE_SERVER_URL, url);
    }

    private void buildRouterConfiguration(JsonObject httpConf, String serviceName) {
        JsonArray routers = httpConf.getJsonArray(DynamicConfiguration.ROUTERS);
        JsonArray services = httpConf.getJsonArray(DynamicConfiguration.SERVICES);
        if (routers.size() == 0) {
            if (services.size() > 1) {
                throw new IllegalArgumentException("Could not create a router for the container: too many services");
            } else {
                routers.add(new JsonObject());
            }
        }

        // TODO finish building router config

    }
}
