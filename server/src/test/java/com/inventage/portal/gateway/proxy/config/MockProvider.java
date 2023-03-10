package com.inventage.portal.gateway.proxy.config;

import com.inventage.portal.gateway.proxy.provider.Provider;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MockProvider extends Provider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockProvider.class);

    private final Vertx vertx;
    private final EventBus eb;
    private final String configurationAddress;

    private final List<JsonObject> messages;
    private final long waitMs;

    private long timerId;

    public MockProvider(Vertx vertx, String configurationAddress, List<JsonObject> messages) {
        this(vertx, configurationAddress, messages, 0);
    }

    public MockProvider(Vertx vertx, String configurationAddress, List<JsonObject> messages, long waitMs) {
        this.vertx = vertx;
        this.eb = vertx.eventBus();
        this.configurationAddress = configurationAddress;
        this.messages = messages;

        if (waitMs == 0) {
            this.waitMs = 20;
        }
        else {
            this.waitMs = waitMs;
        }
    }

    @Override
    public void start(Promise<Void> startPromise) {
        provide(startPromise);
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        vertx.cancelTimer(this.timerId);
        stopPromise.complete();
    }

    @Override
    public void provide(Promise<Void> startPromise) {
        if (this.messages.isEmpty()) {
            startPromise.complete();
            return;
        }

        AtomicInteger count = new AtomicInteger(0);
        this.timerId = this.vertx.setPeriodic(this.waitMs, tId -> {
            JsonObject message = this.messages.get(count.get());
            this.eb.publish(this.configurationAddress, message);

            if (count.incrementAndGet() == this.messages.size()) {
                this.vertx.cancelTimer(tId);
            }
            LOGGER.debug("Wait before sending next message");
        });
        startPromise.complete();
    }

}
