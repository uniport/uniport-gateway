package ch.uniport.gateway.proxy.provider.file;

import ch.uniport.gateway.core.config.StaticConfiguration;
import ch.uniport.gateway.core.config.model.ProviderModel;
import ch.uniport.gateway.proxy.provider.Provider;
import ch.uniport.gateway.proxy.provider.ProviderFactory;
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
        return new FileConfigProvider(vertx, configurationAddress, provider.getFilename(), provider.getDirectory(),
            provider.isWatch(), env);
    }

    @Override
    public Class<? extends ProviderModel> modelType() {
        return FileProviderModel.class;
    }

}
