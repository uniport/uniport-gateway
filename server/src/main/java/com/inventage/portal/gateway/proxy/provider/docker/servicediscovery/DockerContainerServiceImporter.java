package com.inventage.portal.gateway.proxy.provider.docker.servicediscovery;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.spi.ServiceImporter;
import io.vertx.servicediscovery.spi.ServicePublisher;

public class DockerContainerServiceImporter implements ServiceImporter {
    private final static Logger LOGGER =
            LoggerFactory.getLogger(DockerContainerServiceImporter.class);

    private long timer;
    private DockerClient client;

    private List<DockerContainerService> services = new ArrayList<>();
    private ServicePublisher publisher;
    private Vertx vertx;
    private String host;

    volatile boolean started;

    /**
     * Starts the bridge.
     *
     * @param vertx         the vert.x instance
     * @param publisher     the service discovery instance
     * @param configuration the bridge configuration if any
     * @param completion    future to assign with completion status
     */
    @Override
    public void start(Vertx vertx, ServicePublisher publisher, JsonObject configuration,
            Promise<Void> completion) {
        this.publisher = publisher;
        this.vertx = vertx;
        DefaultDockerClientConfig.Builder builder =
                DefaultDockerClientConfig.createDefaultConfigBuilder();
        String dockerCertPath = configuration.getString("docker-cert-path");
        String dockerCfgPath = configuration.getString("docker-cfg-path");
        String email = configuration.getString("docker-registry-email");
        String password = configuration.getString("docker-registry-password");
        String username = configuration.getString("docker-registry-username");
        String host = configuration.getString("docker-host");
        boolean tlsVerify = configuration.getBoolean("docker-tls-verify", true);
        String registry =
                configuration.getString("docker-registry-url", "https://index.docker.io/v1/");
        String version = configuration.getString("version");

        if (dockerCertPath != null) {
            builder.withDockerCertPath(dockerCertPath);
        }
        if (dockerCfgPath != null) {
            builder.withDockerConfig(dockerCfgPath);
        }
        if (email != null) {
            builder.withRegistryEmail(email);
        }
        if (password != null) {
            builder.withRegistryPassword(password);
        }
        if (username != null) {
            builder.withRegistryUsername(username);
        }
        if (host != null) {
            builder.withDockerHost(host);
        }
        if (registry != null) {
            builder.withRegistryUrl(registry);
        }
        if (version != null) {
            builder.withApiVersion(version);
        }
        builder.withDockerTlsVerify(tlsVerify);

        DockerClientConfig config = builder.build();
        if (config.getDockerHost().getScheme().equalsIgnoreCase("unix")) {
            try {
                this.host = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                completion.fail(e);
            }
        } else {
            this.host = config.getDockerHost().getHost();
        }
        client = DockerClientBuilder.getInstance(config).build();

        long period = configuration.getLong("scan-period", 3000L);
        if (period > 0) {
            timer = vertx.setPeriodic(period, l -> {
                scan(null);
            });
        }
        scan(completion);
    }

    synchronized void scan(Promise<Void> completion) {
        vertx.<List<Container>>executeBlocking(future -> {
            try {
                future.complete(client.listContainersCmd()
                        .withStatusFilter(Collections.singletonList("running")).exec());
            } catch (Exception e) {
                future.fail(e);
            }
        }, listContainersAr -> {
            if (listContainersAr.failed()) {
                if (completion != null) {
                    completion.fail(listContainersAr.cause());
                } else {
                    LOGGER.warn("scan: fail to import containers from docker",
                            listContainersAr.cause());
                }
                return;
            }
            started = true;
            List<Container> running = listContainersAr.result();
            List<DockerContainerService> toRemove = new ArrayList<>();

            // Detect lost containers
            services.stream().filter(service -> isNotRunning(service.id(), running))
                    .forEach(service -> {
                        unpublish(service);
                        toRemove.add(service);
                    });
            services.removeAll(toRemove);

            if (running != null) {
                // Detect new containers
                running.stream().filter(container -> !isKnown(container)).forEach(container -> {
                    DockerContainerService service = new DockerContainerService(container);
                    services.add(service);
                    publish(service);
                });
            }

            if (completion != null) {
                completion.complete();
            }
        });
    }

    private void publish(DockerContainerService service) {
        Record record = service.record();
        if (record == null) {
            LOGGER.warn("publish: failed to retrieve record for service '{}'",
                    service.serviceName());
            return;
        }
        publisher.publish(record, ar -> {
            if (ar.succeeded()) {
                record.setRegistration(ar.result().getRegistration());
                LOGGER.info("publish: container '{}' on location '{}' was successful",
                        service.serviceName(), record.getLocation());
            } else {
                LOGGER.warn("publish: container '{}' on location '{}' failed",
                        service.serviceName(), record.getLocation(), ar.cause());
            }
        });
    }

    private void unpublish(DockerContainerService service) {
        Record record = service.record();
        if (record == null) {
            LOGGER.warn("unpublish: failed to retrieve record for service '{}'",
                    service.toString());
            return;
        }
        publisher.unpublish(record.getRegistration(), ar -> {
            LOGGER.info(
                    "unpublish: service from container '{}' on location '{}' has been unpublished",
                    service.serviceName(), record.getLocation());
        });
    }

    private boolean isKnown(Container container) {
        for (DockerContainerService service : services) {
            if (service.id().equalsIgnoreCase(container.getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean isNotRunning(String containerId, List<Container> running) {
        if (running == null) {
            // No running container
            return true;
        }

        for (Container container : running) {
            if (container.getId().equalsIgnoreCase(containerId)) {
                // Found in the running list
                return false;
            }
        }

        // Not found in the running list
        return true;
    }

    @Override
    public void close(Handler<Void> completionHandler) {
        vertx.cancelTimer(timer);
        try {
            started = false;
            client.close();
            LOGGER.info("close: successfully closed the service importer " + this);
        } catch (IOException e) {
            LOGGER.warn("close: a failure has been caught while stopping " + this, e);
        }
        if (completionHandler != null) {
            completionHandler.handle(null);
        }
    }
}
