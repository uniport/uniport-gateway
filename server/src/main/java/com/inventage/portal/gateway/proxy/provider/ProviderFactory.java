package com.inventage.portal.gateway.proxy.provider;

import com.inventage.portal.gateway.core.model.GatewayProvider;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.Optional;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service interface for providing providers. Implementations must add an entry with the fully
 * qualified class name into
 * META-INF/services/com.inventage.portal.gateway.proxy.provider.ProviderFactory
 */
public interface ProviderFactory {

    Logger LOGGER = LoggerFactory.getLogger(ProviderFactory.class);

    String provides();

    Provider create(Vertx vertx, String configurationAddress, GatewayProvider providerConfig, JsonObject env);

    @SuppressWarnings("unchecked")
    default <T extends GatewayProvider> T castProvider(GatewayProvider provider, Class<T> clazz) {
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

        public static ProviderFactory getFactory(String providerName) {
            LOGGER.debug("Get provider factory '{}'", providerName);
            final Optional<ProviderFactory> provider = ServiceLoader.load(ProviderFactory.class).stream()
                .map(ServiceLoader.Provider::get).filter(instance -> instance.provides().equals(providerName))
                .findFirst();
            return provider.orElse(null);
        }
    }
}
