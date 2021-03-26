package com.inventage.portal.gateway.proxy.provider.aggregator;

import java.util.ArrayList;
import java.util.List;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.proxy.provider.Provider;
import com.inventage.portal.gateway.proxy.provider.ProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ProviderAggregator extends Provider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderAggregator.class);

    private Vertx vertx;

    private String configurationAddress;
    private JsonArray providers;

    public ProviderAggregator(Vertx vertx, String configurationAddress, JsonArray providers) {
        this.vertx = vertx;
        this.configurationAddress = configurationAddress;
        this.providers = providers;
    }

    public void start(Promise<Void> startPromise) {
        provide(startPromise);
    }

    @Override
    public void provide(Promise<Void> startPromise) {
        List<Future> futures = new ArrayList<>();
        for (int i = 0; i < this.providers.size(); i++) {
            JsonObject providerConfig = this.providers.getJsonObject(i);

            String providerName = providerConfig.getString(StaticConfiguration.PROVIDER_NAME);
            ProviderFactory providerFactory = ProviderFactory.Loader.getFactory(providerName);

            if (providerFactory == null) {
                LOGGER.error("Ignoring unknown provider '{}'", providerName);
                continue;
            }

            Provider provider =
                    providerFactory.create(this.vertx, this.configurationAddress, providerConfig);

            futures.add(launchProvider(provider));
        }

        CompositeFuture.join(futures).onComplete(ar -> {
            if (ar.succeeded()) {
                LOGGER.info("provide: launched {}/{} providers successfully", futures.size(),
                        this.providers.size());
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
