package com.inventage.portal.gateway.core.provider.aggregator;

import java.util.ArrayList;
import java.util.List;

import com.inventage.portal.gateway.core.config.startup.StaticConfiguration;
import com.inventage.portal.gateway.core.provider.Provider;
import com.inventage.portal.gateway.core.provider.ProviderFactory;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ProviderAggregator extends Provider {

    private Vertx vertx;

    private String configurationAddress;
    private JsonObject staticConfig;

    public ProviderAggregator(Vertx vertx, String configurationAddress, JsonObject staticConfig) {
        this.vertx = vertx;
        this.configurationAddress = configurationAddress;
        this.staticConfig = staticConfig;
    }

    public void start(Promise<Void> startPromise) {
        provide(startPromise);
    }

    @Override
    public void provide(Promise<Void> startPromise) {
        JsonArray providers = this.staticConfig.getJsonArray(StaticConfiguration.PROVIDERS);

        List<Future> futures = new ArrayList<>();
        for (int i = 0; i < providers.size(); i++) {
            JsonObject providerConfig = providers.getJsonObject(i);

            Provider provider = ProviderFactory.Loader
                    .getFactory(providerConfig.getString(StaticConfiguration.PROVIDER_NAME))
                    .create(this.vertx, this.configurationAddress, providerConfig);

            futures.add(launchProvider(provider));
        }

        CompositeFuture.join(futures).onComplete(ar -> {
            if (ar.succeeded()) {
                startPromise.complete();
            } else {
                startPromise.fail(ar.cause());
            }
        });
    }

    private Future<String> launchProvider(Provider provider) {
        return this.vertx.deployVerticle(provider);
    }

}
