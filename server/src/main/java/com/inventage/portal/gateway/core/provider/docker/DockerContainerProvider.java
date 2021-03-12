package com.inventage.portal.gateway.core.provider.docker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.inventage.portal.gateway.core.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.core.config.label.Parser;
import com.inventage.portal.gateway.core.provider.AbstractProvider;

import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;

public class DockerContainerProvider extends AbstractProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerContainerProvider.class);
    private static final String announceAddress = "docker-container-announce";
    private static final String defaultTempateRule = "Host('${name}')";

    private EventBus eb;
    private String configurationAddress;

    private ServiceDiscovery dockerContainerDiscovery;

    private Boolean watch;
    private String endpoint;
    private String defaultRule;
    private Boolean TLS;

    private Map<String, JsonObject> configurations = new HashMap<String, JsonObject>();

    public DockerContainerProvider(String configurationAddress) {
        this.eb = vertx.eventBus();
        this.configurationAddress = configurationAddress;

        this.watch = true;
        this.endpoint = "unix:///var/run/docker.sock";
        this.defaultRule = defaultTempateRule;
        this.TLS = false;
    }

    @Override
    public void provide(Promise<Void> startPromise) {
        this.getOrCreateDockerContainerDiscovery();

        MessageConsumer<JsonObject> consumer = this.eb.consumer(announceAddress);
        consumer.handler(message -> {
            JsonObject dockerContainer = message.body();

            JsonObject config = this.buildConfiguration(dockerContainer);

            DynamicConfiguration.validate(vertx, config).onComplete(validateAr -> {
                if (validateAr.succeeded()) {
                    LOGGER.info("configuration published");
                    eb.publish(this.configurationAddress, config);
                } else {
                    LOGGER.error("invalid configuration");
                }

                if (!this.watch) {
                    consumer.unregister();
                }
            });
        });
        startPromise.complete();
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
        // TODO I am not really convinced with using the Record type to un/publish
        JsonObject metadata = dockerContainer.getJsonObject("metadata");

        String serviceName = metadata.getString("portal.docker.serviceName");
        String containerId = metadata.getString("portal.docker.id");
        String containerName = serviceName + "-" + containerId;

        String status = dockerContainer.getString("status");
        if (status.equals("DOWN")) {
            this.configurations.remove(containerName);
            return DynamicConfiguration.merge(this.configurations);
        } else if (!status.equals("UP")) { // OUT_OF_SERVICE, UNKOWN
            throw new IllegalArgumentException("unkown status type: " + status);
        }

        JsonObject labels = metadata.getJsonObject("portal.docker.labels");

        String ip = metadata.getString("portal.docker.ip");
        String port = metadata.getString("portal.docker.port");

        JsonObject confFromLabels = Parser.decode(labels.getMap(), Parser.DEFAULT_ROOT_NAME,
                Arrays.asList("portal.http"));

        JsonObject httpConf = confFromLabels.getJsonObject(DynamicConfiguration.HTTP);
        if (httpConf.getJsonArray(DynamicConfiguration.ROUTERS).size() == 0
                && httpConf.getJsonArray(DynamicConfiguration.MIDDLEWARES).size() == 0
                && httpConf.getJsonArray(DynamicConfiguration.SERVICES).size() == 0) {
            this.configurations.put(containerName, confFromLabels);
            return confFromLabels;
        }

        this.buildServiceConfiguration(httpConf, serviceName, ip, port);

        Map<String, String> model = new HashMap<String, String>();
        model.put("name", serviceName);

        this.buildRouterConfiguration(httpConf, serviceName, model);

        this.configurations.put(containerName, confFromLabels);

        DynamicConfiguration.validate(vertx, confFromLabels).onComplete(ar -> {
            if (ar.succeeded()) {
                LOGGER.debug("configuration from labels '{}'", confFromLabels);
            } else {
                LOGGER.error("invalid configuration form container labels '{}': '{}'", serviceName, ar.cause());
            }
        });

        return DynamicConfiguration.merge(this.configurations);
    }

    private void buildServiceConfiguration(JsonObject httpConf, String serviceName, String ip, String port) {
        JsonArray services = httpConf.getJsonArray(DynamicConfiguration.SERVICES);
        if (services.size() == 0) {
            JsonObject fallbackService = new JsonObject().put(DynamicConfiguration.SERVICE_NAME, serviceName)
                    .put(DynamicConfiguration.SERVICE_SERVERS, new JsonArray());
            services.add(fallbackService);
        }

        for (int i = 0; i < services.size(); i++) {
            JsonObject service = services.getJsonObject(i);
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

    private void buildRouterConfiguration(JsonObject httpConf, String serviceName, Map<String, String> model) {
        JsonArray routers = httpConf.getJsonArray(DynamicConfiguration.ROUTERS);
        JsonArray services = httpConf.getJsonArray(DynamicConfiguration.SERVICES);
        if (routers.size() == 0) {
            if (services.size() > 1) {
                throw new IllegalArgumentException("Could not create a router for the container: too many services");
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
                    throw new IllegalArgumentException("Undefined rule");
                }
                router.put(DynamicConfiguration.ROUTER_RULE, resolvedRule);
            }

            if (!router.containsKey(DynamicConfiguration.ROUTER_SERVICE)) {
                if (services.size() > 1) {
                    throw new IllegalArgumentException(
                            "Could not define the service name for the router: too many services");
                }
                for (int j = 0; j < services.size(); j++) {
                    router.put(DynamicConfiguration.ROUTER_SERVICE,
                            services.getJsonObject(j).getString(DynamicConfiguration.SERVICE_NAME));
                }
            }
        }
    }
}
