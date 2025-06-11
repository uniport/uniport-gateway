package com.inventage.portal.gateway.proxy.provider.file;

import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.core.config.model.FileProviderModel;
import com.inventage.portal.gateway.core.config.model.ProviderModel;
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
    public Provider create(Vertx vertx, String configurationAddress, ProviderModel config, JsonObject env) {
        final FileProviderModel provider = castProvider(config, FileProviderModel.class);
        return new FileConfigProvider(vertx, configurationAddress, provider.getFilename(), provider.getDirectory(), provider.isWatch(), env);
    }

}
