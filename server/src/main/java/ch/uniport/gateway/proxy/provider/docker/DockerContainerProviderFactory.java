package ch.uniport.gateway.proxy.provider.docker;

import ch.uniport.gateway.core.config.StaticConfiguration;
import ch.uniport.gateway.core.config.model.ProviderModel;
import ch.uniport.gateway.proxy.provider.Provider;
import ch.uniport.gateway.proxy.provider.ProviderFactory;
import ch.uniport.gateway.proxy.provider.docker.servicediscovery.DockerContainerServiceImporter;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class DockerContainerProviderFactory implements ProviderFactory {

    @Override
    public String provides() {
        return StaticConfiguration.PROVIDER_DOCKER;
    }

    @Override
    public Provider create(Vertx vertx, String configurationAddress, ProviderModel config, JsonObject env) {
        final DockerProviderModel provider = castProvider(config, DockerProviderModel.class);

        final JsonObject serviceImporterConfiguration = new JsonObject()
            .put("docker-tls-verify", AbstractDockerProviderModel.DEFAULT_USE_TLS) // not configurable at the moment
            .put("docker-host", provider.getEndpoint());

        return new DockerContainerProvider(vertx,
            configurationAddress,
            new DockerContainerServiceImporter(),
            serviceImporterConfiguration,
            provider.isExposedByDefault(),
            provider.getNetwork(),
            provider.getDefaultRule(),
            provider.isWatch());
    }

    @Override
    public Class<DockerProviderModel> modelType() {
        return DockerProviderModel.class;
    }
}
