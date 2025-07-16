package ch.uniport.gateway.proxy.provider;

import ch.uniport.gateway.core.config.model.ProviderModel;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.Optional;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service interface for providing providers. Implementations must add an entry
 * with the fully
 * qualified class name into
 * META-INF/services/ch.uniport.gateway.proxy.provider.ProviderFactory
 */
public interface ProviderFactory {

    Logger LOGGER = LoggerFactory.getLogger(ProviderFactory.class);

    String provides();

    Provider create(Vertx vertx, String configurationAddress, ProviderModel providerConfig, JsonObject env);

    Class<? extends ProviderModel> modelType();

    @SuppressWarnings("unchecked")
    default <T extends ProviderModel> T castProvider(ProviderModel provider, Class<T> clazz) {
        if (provider == null) {
            return null;
        }
        if (!clazz.isInstance(provider)) {
            throw new IllegalStateException(
                String.format("unexpected provider type: '%s'", provider.getClass()));
        }
        return (T) provider;
    }

    final class Loader {
        private Loader() {
        }

        public static Optional<ProviderFactory> getFactory(String providerName) {
            LOGGER.debug("Get provider factory '{}'", providerName);
            return ServiceLoader.load(ProviderFactory.class).stream()
                .map(ServiceLoader.Provider::get)
                .filter(instance -> instance.provides().equals(providerName))
                .findFirst();
        }
    }
}
