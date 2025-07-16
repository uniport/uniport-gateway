package ch.uniport.gateway.proxy.provider.docker;

import ch.uniport.gateway.core.config.StaticConfiguration;
import ch.uniport.gateway.core.config.model.ModelStyle;
import ch.uniport.gateway.core.config.model.ProviderModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = DockerProviderModel.Builder.class)
public abstract class AbstractDockerProviderModel implements ProviderModel {

    public static final String DEFAULT_ENDPOINT = "unix:///var/run/docker.sock";
    public static final boolean DEFAULT_EXPOSED_BY_DEFAULT = true;
    public static final String DEFAULT_RULE_TEMPLATE = "Host('${name}')";
    public static final boolean DEFAULT_WATCH = true;
    public static final boolean DEFAULT_USE_TLS = false;

    @Override
    @JsonProperty(StaticConfiguration.PROVIDER_NAME)
    public abstract String getName();

    @Default
    @JsonProperty(StaticConfiguration.PROVIDER_DOCKER_ENDPOINT)
    public String getEndpoint() {
        return DEFAULT_ENDPOINT;
    }

    @Default
    @JsonProperty(StaticConfiguration.PROVIDER_DOCKER_EXPOSED_BY_DEFAULT)
    public boolean isExposedByDefault() {
        return DEFAULT_EXPOSED_BY_DEFAULT;
    }

    @JsonProperty(StaticConfiguration.PROVIDER_DOCKER_NETWORK)
    public abstract String getNetwork();

    @Default
    @JsonProperty(StaticConfiguration.PROVIDER_DOCKER_DEFAULT_RULE)
    public String getDefaultRule() {
        return DEFAULT_RULE_TEMPLATE;
    }

    @Default
    @JsonProperty(StaticConfiguration.PROVIDER_DOCKER_WATCH)
    public boolean isWatch() {
        return DEFAULT_WATCH;
    }

}
