package com.inventage.portal.gateway.core.provider.kubernetes;

import com.inventage.portal.gateway.core.config.startup.StaticConfiguration;
import com.inventage.portal.gateway.core.provider.Provider;
import com.inventage.portal.gateway.core.provider.ProviderFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class KubernetesServiceProviderFactory implements ProviderFactory {

    @Override
    public String provides() {
        return StaticConfiguration.PROVIDER_KUBERNETES;
    }

    @Override
    public Provider create(Vertx vertx, String configurationAddress, JsonObject providerConfig) {
        return new KubernetesServiceProvider(vertx, configurationAddress);
    }

}
