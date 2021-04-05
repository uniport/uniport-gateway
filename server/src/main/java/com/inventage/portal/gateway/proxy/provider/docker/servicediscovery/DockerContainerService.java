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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ContainerNetworkSettings;
import com.github.dockerjava.api.model.ContainerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.servicediscovery.Record;

/**
 * Represent a Docker container.
 */
public class DockerContainerService {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(DockerContainerService.class.getName());

  private final String name;
  private String containerId;

  private List<String> containerNames;

  private Record record;

  public DockerContainerService(Container container) {
    LOGGER.trace("constructor");

    containerId = container.getId();
    containerNames = Arrays.stream(container.getNames()).collect(Collectors.toList());
    if (!containerNames.isEmpty()) {
      name = containerNames.get(0);
    } else {
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
    LOGGER.trace("createRecord");
    Record record = new Record().setName(name);

    Map<String, String> labels = container.getLabels();
    if (labels != null) {
      for (Map.Entry<String, String> entry : labels.entrySet()) {
        record.getMetadata().put(entry.getKey(), entry.getValue());
      }
    }

    JsonArray names = new JsonArray();
    containerNames.forEach(names::add);
    record.getMetadata().put("docker.names", names);
    record.getMetadata().put("docker.name", name);
    record.getMetadata().put("docker.id", containerId);

    record.getMetadata().put("docker.ip", getIP(container));
    JsonArray ports = new JsonArray();
    for (ContainerPort port : container.getPorts()) {
      ports.add(port.getPrivatePort());
    }
    record.getMetadata().put("docker.ports", ports);

    // NOTE: record location is not set
    return record;
  }

  private static String getIP(Container container) {
    String networkMode = container.getHostConfig().getNetworkMode();
    if (networkMode != "") {
      ContainerNetworkSettings settings = container.getNetworkSettings();
      if (settings != null) {
        ContainerNetwork network = settings.getNetworks().get(networkMode);
        if (network != null) {
          return network.getIpAddress();
        }
        LOGGER.warn("getIPAddress: could not find network named " + networkMode + " for container "
            + container.getId()
            + "! Maybe you're missing the project's prefix in the label? Defaulting to first available network.");
      }
    }

    for (ContainerNetwork network : container.getNetworkSettings().getNetworks().values()) {
      return network.getIpAddress();
    }

    LOGGER.warn("getIPAddress: unable to find the IP address");
    return "";
  }
}

