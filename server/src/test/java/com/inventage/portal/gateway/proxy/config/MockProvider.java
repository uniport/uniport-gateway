package com.inventage.portal.gateway.proxy.config;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.inventage.portal.gateway.proxy.provider.Provider;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public class MockProvider extends Provider {
    private EventBus eb;
    private String configurationAddress;

    private List<JsonObject> messages;
    private long waitMs;

    public MockProvider(Vertx vertx, String configurationAddress, List<JsonObject> messages) {
        this(vertx, configurationAddress, messages, 0);
    }

    public MockProvider(Vertx vertx, String configurationAddress, List<JsonObject> messages, long waitMs) {
        this.eb = vertx.eventBus();
        this.configurationAddress = configurationAddress;
        this.messages = messages;
        this.waitMs = waitMs;
    }

    public void start(Promise<Void> startPromise) {
        provide(startPromise);
    }

    @Override
    public void provide(Promise<Void> startPromise) {
        for (int i = 0; i < this.messages.size(); i++) {
            JsonObject message = this.messages.get(i);

            this.eb.publish(this.configurationAddress, message);

            long waitMs = this.waitMs;
            if (waitMs == 0) {
                waitMs = 20;
            }

            System.out.printf("Wait %s %s\n", waitMs, System.currentTimeMillis());
            try {
                TimeUnit.MILLISECONDS.sleep(waitMs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        startPromise.complete();
    }

}
