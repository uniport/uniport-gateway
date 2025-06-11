package com.inventage.portal.gateway.proxy.provider.aggregator;

import com.inventage.portal.gateway.core.config.model.ProviderModel;
import com.inventage.portal.gateway.proxy.provider.Provider;
import com.inventage.portal.gateway.proxy.provider.ProviderFactory;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates and launches all specified providers.
 */
public class ProviderAggregator extends Provider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderAggregator.class);

    private static final String NAME = "providerAggregator";

    private final Vertx vertx;

    private final String configurationAddress;
    private final List<ProviderModel> providerConfigs;
    private final JsonObject env;

    public ProviderAggregator(Vertx vertx, String configurationAddress, List<ProviderModel> providerConfigs, JsonObject env) {
        this.vertx = vertx;
        this.configurationAddress = configurationAddress;
        this.providerConfigs = providerConfigs;
        this.env = new JsonObject(env.getMap());
    }

    @Override
    public void start(Promise<Void> startPromise) {
        provide(startPromise);
    }

    @Override
    public void provide(Promise<Void> startPromise) {
        final List<Future<String>> futures = new ArrayList<>();
        for (ProviderModel providerConfig : providerConfigs) {
            final String providerName = providerConfig.getName();
            final ProviderFactory providerFactory = ProviderFactory.Loader.getFactory(providerName);

            if (providerFactory == null) {
                LOGGER.warn("Ignoring unknown provider '{}'", providerName);
                continue;
            }

            final Provider provider = providerFactory.create(this.vertx, this.configurationAddress, providerConfig, this.env);

            futures.add(launchProvider(provider));
        }

        Future.join(futures).onSuccess(cf -> {
            LOGGER.info("Launched {}/{} providers successfully", futures.size(), this.providerConfigs.size());
            startPromise.complete();
        }).onFailure(err -> {
            startPromise.fail(err.getMessage());
        });
    }

    @Override
    public String toString() {
        return NAME;
    }

    private Future<String> launchProvider(Provider provider) {
        LOGGER.debug("Launching provider '{}'", provider);
        return this.vertx.deployVerticle(provider);
    }

}
