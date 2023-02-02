/*
 * Copyright (c) 2011-2016 The original author or authors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Apache License v2.0 which accompanies this
 * distribution.
 *
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package com.inventage.portal.gateway.proxy.provider.docker.servicediscovery;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ContainerPort;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Represent a Docker container.
 */
public class DockerContainerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerContainerService.class.getName());

    private final String name;
    private final String containerId;

    private final List<String> containerNames;

    private final Record record;

    public DockerContainerService(Container container) {
        containerId = container.getId();
        containerNames = Arrays.stream(container.getNames()).collect(Collectors.toList());
        if (!containerNames.isEmpty()) {
            name = containerNames.get(0);
        }
        else {
            name = containerId;
        }

        record = createRecord(container);
    }

    public Record record() {
        return record;
    }

    public String name() {
        return name;
    }

    public String id() {
        return containerId;
    }

    private Record createRecord(Container container) {
        final Record record = new Record().setName(name);

        final Map<String, String> labels = container.getLabels();
        if (labels != null) {
            for (Map.Entry<String, String> entry : labels.entrySet()) {
                record.getMetadata().put(entry.getKey(), entry.getValue());
            }
        }

        final JsonArray names = new JsonArray();
        containerNames.forEach(names::add);
        record.getMetadata().put("docker.names", names);
        record.getMetadata().put("docker.name", name);
        record.getMetadata().put("docker.id", containerId);

        final JsonArray ports = new JsonArray();
        for (ContainerPort port : container.getPorts()) {
            ports.add(port.getPrivatePort());
        }
        record.getMetadata().put("docker.ports", ports);

        final JsonObject hostPerNetwork = new JsonObject();
        hostPerNetwork.put("defaultNetworkMode", container.getHostConfig().getNetworkMode());
        for (Entry<String, ContainerNetwork> entry : container.getNetworkSettings().getNetworks().entrySet()) {
            hostPerNetwork.put(entry.getKey(), entry.getValue().getIpAddress());
        }

        record.getMetadata().put("docker.hostPerNetwork", hostPerNetwork);

        // NOTE: record location is not set
        return record;
    }
}
