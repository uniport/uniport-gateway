package com.inventage.portal.gateway.proxy.provider.docker;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.spi.ServiceImporter;
import io.vertx.servicediscovery.spi.ServicePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MockServiceImporter implements ServiceImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerContainerProvider.class);

    private Vertx vertx;
    private ServicePublisher publisher;

    private final long scanPeriodMs;
    private long timerId;

    private final List<JsonObject> containers;
    private final AtomicInteger count;

    public MockServiceImporter(List<JsonObject> containers, long scanPeriodMs) {
        if (containers == null) {
            LOGGER.warn("Initializing undefined publishedServices");
            containers = new ArrayList<JsonObject>();
        }
        this.containers = containers;
        this.count = new AtomicInteger();
        this.scanPeriodMs = scanPeriodMs;
    }

    /**
     * Starts the mock.
     *
     * @param vertx         the vert.x instance
     * @param publisher     the service discovery instance
     * @param configuration the mock configuration if any
     * @param completion    future to assign with completion status
     */
    @Override
    public void start(Vertx vertx, ServicePublisher publisher, JsonObject configuration, Promise<Void> completion) {
        this.vertx = vertx;
        this.publisher = publisher;

        if (this.scanPeriodMs > 0) {
            this.timerId = vertx.setPeriodic(this.scanPeriodMs, tId -> {
                scan(null);
            });
        }
        scan(completion);
    }

    private void scan(Promise<Void> completion) {
        if (count.get() >= this.containers.size()) {
            if (completion != null) {
                completion.complete();
            }
            return;
        }

        publish(new Record(this.containers.get(count.getAndIncrement())));

        if (completion != null) {
            completion.complete();
        }
    }

    private void publish(Record service) {
        publisher.publish(service).onSuccess(r -> {
            service.setRegistration(r.getRegistration());
            LOGGER.info("Service from container '{}' has been published", service.getName());
        }).onFailure(err -> {
            LOGGER.warn("Service from container '{}' could not have been published", service.getName());
        });
    }

    private void unpublish(Record service) {
        publisher.unpublish(service.getRegistration(), ar -> {
            LOGGER.info("Service from container '{}' has been unpublished", service.getName());
        });
    }

    @Override
    public void close(Handler<Void> completionHandler) {
        vertx.cancelTimer(timerId);
        if (completionHandler != null) {
            completionHandler.handle(null);
        }
    }

}
