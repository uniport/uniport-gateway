package com.inventage.portal.gateway.core.provider.aggregator;

import com.inventage.portal.gateway.core.config.startup.StaticConfiguration;
import com.inventage.portal.gateway.core.provider.Provider;
import com.inventage.portal.gateway.core.provider.ProviderFactory;

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

        for (int i = 0; i < providers.size(); i++) {
            JsonObject providerConfig = providers.getJsonObject(i);

            Provider provider = ProviderFactory.Loader
                    .getFactory(providerConfig.getString(StaticConfiguration.PROVIDER_NAME))
                    .create(this.vertx, this.configurationAddress, providerConfig);

            launchProvider(provider);
        }

        startPromise.complete();
    }

    private void launchProvider(Provider provider) {
        vertx.deployVerticle(provider);
    }

}
