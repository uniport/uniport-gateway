package com.inventage.portal.gateway.proxy.provider.file.JsonDirectoryConfigStore;

import io.vertx.config.spi.ConfigStore;
import io.vertx.config.spi.ConfigStoreFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class JsonDirectoryConfigStoreFactory implements ConfigStoreFactory {

    @Override
    public String name() {
        return "jsonDirectory";
    }

    @Override
    public ConfigStore create(Vertx vertx, JsonObject configuration) {
        return new JsonDirectoryConfigStore(vertx, configuration);
    }

}
