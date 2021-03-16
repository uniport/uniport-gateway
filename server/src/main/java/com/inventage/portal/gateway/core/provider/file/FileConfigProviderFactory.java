package com.inventage.portal.gateway.core.provider.file;

import com.inventage.portal.gateway.core.provider.Provider;
import com.inventage.portal.gateway.core.provider.ProviderFactory;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class FileConfigProviderFactory implements ProviderFactory {
    public static final String PROVIDER_NAME = "file";
    public static final String PROVIDER_FILENAME = "filename";
    public static final String PROVIDER_DIRECTORY = "directory";
    public static final String PROVIDER_WATCH = "watch";

    @Override
    public String provides() {
        return PROVIDER_NAME;
    }

    @Override
    public Provider create(Vertx vertx, String configurationAddress, JsonObject providerConfig) {
        String filename = providerConfig.getString(PROVIDER_FILENAME, "");
        String directory = providerConfig.getString(PROVIDER_DIRECTORY, "");
        Boolean watch = providerConfig.getBoolean(PROVIDER_WATCH, false);
        return new FileConfigProvider(vertx, configurationAddress, filename, directory, watch);
    }

}
