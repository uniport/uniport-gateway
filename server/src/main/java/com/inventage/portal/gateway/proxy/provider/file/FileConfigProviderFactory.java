package com.inventage.portal.gateway.proxy.provider.file;

import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.core.model.GatewayFileProvider;
import com.inventage.portal.gateway.core.model.GatewayProvider;
import com.inventage.portal.gateway.proxy.provider.Provider;
import com.inventage.portal.gateway.proxy.provider.ProviderFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class FileConfigProviderFactory implements ProviderFactory {

    @Override
    public String provides() {
        return StaticConfiguration.PROVIDER_FILE;
    }

    @Override
    public Provider create(Vertx vertx, String configurationAddress, GatewayProvider config, JsonObject env) {
        final GatewayFileProvider provider = castProvider(config, GatewayFileProvider.class);
        return new FileConfigProvider(vertx, configurationAddress, provider.getFilename(), provider.getDirectory(), provider.isWatch(), env);
    }

}
