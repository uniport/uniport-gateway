package ch.uniport.gateway.core.config.model;

import ch.uniport.gateway.core.config.StaticConfiguration;
import ch.uniport.gateway.proxy.config.model.MiddlewareModel;
import ch.uniport.gateway.proxy.middleware.ModelStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@ModelStyle
@JsonDeserialize(builder = EntrypointModel.Builder.class)
public abstract class AbstractEntrypointModel {

    @JsonProperty(StaticConfiguration.ENTRYPOINT_NAME)
    public abstract String getName();

    @JsonProperty(StaticConfiguration.ENTRYPOINT_PORT)
    public abstract int getPort();

    @JsonProperty(StaticConfiguration.ENTRYPOINT_MIDDLEWARES)
    public abstract List<MiddlewareModel> getMiddlewares();

    @Default
    @Nullable
    @JsonProperty(StaticConfiguration.ENTRYPOINT_TLS)
    public TlsModel getTls() {
        return null;
    }

    @Immutable
    @ModelStyle
    @JsonDeserialize(builder = TlsModel.Builder.class)
    public abstract static class AbstractTlsModel {

        @JsonProperty(StaticConfiguration.ENTRYPOINT_TLS_CERT_FILE)
        public abstract String getCertFile();

        @JsonProperty(StaticConfiguration.ENTRYPOINT_TLS_KEY_FILE)
        public abstract String getKeyFile();
    }
}
