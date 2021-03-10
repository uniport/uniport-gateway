package com.inventage.portal.gateway.core.provider.docker;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ContainerNetworkSettings;
import com.github.dockerjava.api.model.ContainerPort;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.spi.ServiceType;
import io.vertx.servicediscovery.types.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DockerContainerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerContainerService.class.getName());

    private String ID;
    private String name;
    private List<String> containerNames;
    private Map<String, String> labels;
    // private Object networkSettings;
    private String health;
    // private Object node;
    // private Object ExtraConf;

    private Record record;

    public DockerContainerService(Container container) {
        ID = container.getId();
        containerNames = Arrays.stream(container.getNames()).collect(Collectors.toList());
        if (!containerNames.isEmpty()) {
            name = containerNames.get(0);
        } else {
            name = ID;
        }
        labels = container.getLabels();
        health = container.getState();

        record = createRecord(container);
    }

    public String name() {
        return name;
    }

    public List<String> names() {
        return containerNames;
    }

    public String id() {
        return ID;
    }

    public Record record() {
        return record;
    }

    private Record createRecord(Container container) {
        Record record = new Record().setName(name);

        if (labels != null) {
            for (Map.Entry<String, String> entry : labels.entrySet()) {
                record.getMetadata().put(entry.getKey(), entry.getValue());
            }
        }

        JsonArray names = new JsonArray();
        containerNames.forEach(names::add);
        record.getMetadata().put("docker.names", names);
        record.getMetadata().put("docker.name", name);
        record.getMetadata().put("docker.id", ID);
        record.getMetadata().put("docker.health", health);

        String type = record.getMetadata().getString("service.type");

        if (type == null) {
            type = discoverType(record, container);
        }

        switch (type) {
        case HttpEndpoint.TYPE:
            manageHttpService(record, container);
            break;
        default:
            manageUnknownService(record, container, type);
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
            LOGGER.warn("More than one ports has been found for " + record.getName() + " - taking the "
                    + "first one to build the record location");
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

    private static void manageUnknownService(Record record, Container container, String type) {
        ContainerPort[] ports = container.getPorts();
        if (ports != null && ports.length != 0) {
            if (ports.length > 1) {
                LOGGER.warn("More than one ports has been found for " + record.getName() + " - taking the "
                        + "first one to build the record location");
            }
            ContainerPort port = ports[0];

            JsonObject location = new JsonObject();
            location.put("type", port.getType());
            location.put("ip", getIPAddress(container));
            location.put("port", port.getPrivatePort());

            record.setLocation(location).setType(type);
        } else {
            throw new IllegalStateException("Cannot extract the location from the container" + record + " - no port");
        }
    }

    private static void manageHttpService(Record record, Container container) {
        ContainerPort[] ports = container.getPorts();
        if (ports != null && ports.length != 0) {
            if (ports.length > 1) {
                LOGGER.warn("More than one ports has been found for " + record.getName() + " - taking the "
                        + "first one to build the record location");
            }
            ContainerPort port = ports[0];
            int p = port.getPrivatePort();

            record.setType(HttpEndpoint.TYPE);
            HttpLocation location = new HttpLocation();
            location.setHost(getIPAddress(container));
            location.setPort(p);
            if (isTrue(container.getLabels(), "ssl") || p == 443) {
                location.setSsl(true);
            }

            record.setLocation(location.toJson());
        } else {
            throw new IllegalStateException("Cannot extract the HTTP URL from the container " + record + " - no port");
        }
    }

    private static boolean isTrue(Map<String, String> labels, String key) {
        return labels != null && "true".equalsIgnoreCase(labels.get(key));
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
                LOGGER.warn("Could not find network named " + networkMode + " for container " + container.getId()
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
}
