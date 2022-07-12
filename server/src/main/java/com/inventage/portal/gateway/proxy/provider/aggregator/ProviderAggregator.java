package com.inventage.portal.gateway.proxy.provider.aggregator;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.proxy.provider.Provider;
import com.inventage.portal.gateway.proxy.provider.ProviderFactory;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Creates and launches all specified providers.
 */
public class ProviderAggregator extends Provider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderAggregator.class);

    private static final String NAME = "providerAggregator";

    private Vertx vertx;

    private String configurationAddress;
    private JsonArray providers;
    private JsonObject env;

    public ProviderAggregator(Vertx vertx, String configurationAddress, JsonArray providers, JsonObject env) {
        this.vertx = vertx;
        this.configurationAddress = configurationAddress;
        this.providers = providers;
        this.env = env;
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
                LOGGER.warn("Ignoring unknown provider '{}'", providerName);
                continue;
            }

            Provider provider = providerFactory.create(this.vertx, this.configurationAddress, providerConfig, this.env);

            futures.add(launchProvider(provider));
        }

        CompositeFuture.join(futures).onSuccess(cf -> {
            LOGGER.info("launched {}/{} providers successfully", futures.size(), this.providers.size());
            startPromise.complete();
        }).onFailure(err -> {
            startPromise.fail(err.getMessage());
        });
    }

    public String toString() {
        return NAME;
    }

    private Future<String> launchProvider(Provider provider) {
        LOGGER.debug("provider '{}'", provider);
        return this.vertx.deployVerticle(provider);
    }

}
