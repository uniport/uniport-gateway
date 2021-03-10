package com.inventage.portal.gateway.core.provider.docker;

import java.util.Arrays;

import com.inventage.portal.gateway.core.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.core.config.label.Parser;
import com.inventage.portal.gateway.core.provider.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
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
        System.out.println(dockerContainer);

        JsonObject config = new JsonObject();

        String status = dockerContainer.getString("status");
        JsonObject metadata = dockerContainer.getJsonObject("metadata");

        String containerId = metadata.getString("portal.docker.id");
        String serviceName = metadata.getString("portal.docker.serviceName");
        String name = metadata.getString("portal.docker.name");

        JsonObject labels = metadata.getJsonObject("portal.docker.labels");

        String ip = metadata.getString("portal.docker.ip");
        String port = metadata.getString("portal.docker.port");

        String health = metadata.getString("portal.docker.health");

        JsonObject confFromLabels = Parser.decode(labels.getMap(), Parser.defaultRootName,
                Arrays.asList("portal.http"));

        // check entrypoints exist
        // parse rule

        return config;
    }
}
