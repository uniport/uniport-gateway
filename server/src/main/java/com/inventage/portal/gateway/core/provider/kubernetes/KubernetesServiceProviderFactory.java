package com.inventage.portal.gateway.core.provider.kubernetes;

import com.inventage.portal.gateway.core.provider.Provider;
import com.inventage.portal.gateway.core.provider.ProviderFactory;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class KubernetesServiceProviderFactory implements ProviderFactory {
    public static final String PROVIDER_NAME = "kubernetesIngress";

    @Override
    public String provides() {
        return PROVIDER_NAME;
    }

    @Override
    public Provider create(Vertx vertx, String configurationAddress, JsonObject providerConfig) {
        return new KubernetesServiceProvider(vertx, configurationAddress);
    }

}
