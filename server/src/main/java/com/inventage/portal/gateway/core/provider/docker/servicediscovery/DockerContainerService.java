package com.inventage.portal.gateway.core.provider.docker.servicediscovery;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ContainerNetworkSettings;
import com.github.dockerjava.api.model.ContainerPort;

import io.vertx.core.json.JsonObject;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.spi.ServiceType;
import io.vertx.servicediscovery.types.*;

import java.util.Map;

public class DockerContainerService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DockerContainerService.class.getName());

    private static final String labelDockerComposeProject = "com.docker.compose.project";
    private static final String labelDockerComposeService = "com.docker.compose.service";

    private String id;
    private String serviceName;
    private String name;
    private Map<String, String> labels;

    private String ip;
    private int port;

    private String health;

    private Record record;

    public DockerContainerService(Container container) {
        this.id = container.getId();

        String name;
        if (container.getNames().length > 0) {
            name = container.getNames()[0];
        } else {
            name = this.id;
        }
        this.name = name;
        this.serviceName = this.getServiceName(container, name);

        this.labels = container.getLabels();
        this.ip = getIPAddress(container);
        this.port = getPort(container);
        this.health = container.getState();

        this.record = createRecord(container);
    }

    public String id() {
        return this.id;
    }

    public Record record() {
        return this.record;
    }

    private String getServiceName(Container container, String defaultServiceName) {
        String serviceName = defaultServiceName;

        Map<String, String> labels = container.getLabels();
        if (labels.containsKey(labelDockerComposeProject)
                && labels.containsKey(labelDockerComposeService)) {
            this.serviceName = labels.get(labelDockerComposeProject) + "_"
                    + labels.get(labelDockerComposeService);
        }
        return serviceName;
    }

    private Record createRecord(Container container) {
        Record record = new Record().setName(this.name);

        record.getMetadata().put("portal.docker.id", this.id);
        record.getMetadata().put("portal.docker.serviceName", this.serviceName);
        record.getMetadata().put("portal.docker.name", this.name);

        JsonObject labels = new JsonObject();
        if (this.labels != null) {
            for (Map.Entry<String, String> entry : this.labels.entrySet()) {
                labels.put(entry.getKey(), entry.getValue());
            }
        }
        record.getMetadata().put("portal.docker.labels", labels);

        record.getMetadata().put("portal.docker.ip", this.ip);
        record.getMetadata().put("portal.docker.port", this.port);

        record.getMetadata().put("portal.docker.health", this.health);

        String type = record.getMetadata().getString("service.type");
        if (type == null) {
            type = discoverType(record, container);
        }

        switch (type) {
            case HttpEndpoint.TYPE:
                HttpLocation httpLocation = new HttpLocation();
                httpLocation.setHost(this.ip);
                httpLocation.setPort(this.port);
                httpLocation.setSsl(this.port == 443);
                record.setLocation(httpLocation.toJson()).setType(type);
                break;
            default:
                JsonObject location = new JsonObject();
                location.put("ip", this.ip);
                location.put("port", this.port);
                record.setLocation(location).setType(type);
                break;
        }

        return record;
    }

    static String discoverType(Record record, Container container) {
        ContainerPort[] ports = container.getPorts();
        if (ports == null || ports.length == 0) {
            return ServiceType.UNKNOWN;
        }

        if (ports.length > 1) {
            LOGGER.warn("More than one ports has been found for " + record.getName()
                    + " - taking the " + "first one to build the record location");
        }

        ContainerPort port = ports[0];
        int p = port.getPrivatePort();

        // Http
        if (p == 80 || p == 443 || p >= 8080 && p <= 9000) {
            return HttpEndpoint.TYPE;
        }

        // PostGreSQL
        if (p == 5432 || p == 5433) {
            return JDBCDataSource.TYPE;
        }

        // MySQL
        if (p == 3306 || p == 13306) {
            return JDBCDataSource.TYPE;
        }

        // Redis
        if (p == 6379) {
            return RedisDataSource.TYPE;
        }

        // Mongo
        if (p == 27017 || p == 27018 || p == 27019) {
            return MongoDataSource.TYPE;
        }

        return ServiceType.UNKNOWN;
    }

    private static String getIPAddress(Container container) {
        String networkMode = container.getHostConfig().getNetworkMode();
        if (networkMode != "") {
            ContainerNetworkSettings settings = container.getNetworkSettings();
            if (settings != null) {
                ContainerNetwork network = settings.getNetworks().get(networkMode);
                if (network != null) {
                    return network.getIpAddress();
                }
                LOGGER.warn("Could not find network named " + networkMode + " for container "
                        + container.getId()
                        + "! Maybe you're missing the project's prefix in the label? Defaulting to first available network.");
            }
        }

        // TODO maybe check for host and container network mode
        // traefik/pkg/docker/config.go:325

        for (ContainerNetwork network : container.getNetworkSettings().getNetworks().values()) {
            return network.getIpAddress();
        }

        LOGGER.warn("Unable to find the IP address");
        return "";
    }

    private static int getPort(Container container) {
        ContainerPort[] ports = container.getPorts();
        if (ports != null && ports.length != 0) {
            if (ports.length > 1) {
                LOGGER.warn("More than one ports has been found for " + container.getId()
                        + " - taking the " + "first one to build the record location");
            }
            ContainerPort port = ports[0];
            return port.getPrivatePort();
        } else {
            throw new IllegalStateException("Cannot extract the HTTP URL from the container "
                    + container.getNames() + " - no port");
        }
    }
}
