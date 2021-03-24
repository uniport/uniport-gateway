package com.inventage.portal.gateway.proxy.provider.file;

import com.inventage.portal.gateway.core.config.StaticConfiguration;
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
    public Provider create(Vertx vertx, String configurationAddress, JsonObject providerConfig) {
        String filename = providerConfig.getString(StaticConfiguration.PROVIDER_FILE_FILENAME, "");
        String directory =
                providerConfig.getString(StaticConfiguration.PROVIDER_FILE_DIRECTORY, "");
        Boolean watch = providerConfig.getBoolean(StaticConfiguration.PROVIDER_FILE_WATCH, false);
        return new FileConfigProvider(vertx, configurationAddress, filename, directory, watch);
    }

}
