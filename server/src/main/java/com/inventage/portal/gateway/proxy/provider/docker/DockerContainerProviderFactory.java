package com.inventage.portal.gateway.proxy.provider.docker;

import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.proxy.provider.Provider;
import com.inventage.portal.gateway.proxy.provider.ProviderFactory;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class DockerContainerProviderFactory implements ProviderFactory {

  private static final String defaultTemplateRule = "Host('${name}')";

  @Override
  public String provides() {
    return StaticConfiguration.PROVIDER_DOCKER;
  }

  @Override
  public Provider create(Vertx vertx, String configurationAddress, JsonObject providerConfig, JsonObject env) {
    String endpoint = providerConfig.getString(StaticConfiguration.PROVIDER_DOCKER_ENDPOINT,
        "unix:///var/run/docker.sock");
    Boolean exposedByDefault = providerConfig.getBoolean(StaticConfiguration.PROVIDER_DOCKER_EXPOSED_BY_DEFAULT, true);
    String network = providerConfig.getString(StaticConfiguration.PROVIDER_DOCKER_NETWORK, "");
    String defaultRule = providerConfig.getString(StaticConfiguration.PROVIDER_DOCKER_DEFAULT_RULE,
        defaultTemplateRule);
    Boolean watch = providerConfig.getBoolean(StaticConfiguration.PROVIDER_FILE_WATCH, true);
    return new DockerContainerProvider(vertx, configurationAddress, endpoint, exposedByDefault, network, defaultRule,
        watch);
  }

}
