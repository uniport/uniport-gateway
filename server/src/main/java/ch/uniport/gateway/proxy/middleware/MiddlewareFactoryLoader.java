package ch.uniport.gateway.proxy.middleware;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service interface for providing middlewares. Implementations must add an
 * entry with the fully qualified class name into
 * META-INF/services/ch.uniport.gateway.proxy.middleware.MiddlewareFactory
 */
public final class MiddlewareFactoryLoader {

    private static Logger logger = LoggerFactory.getLogger(MiddlewareFactoryLoader.class);

    private MiddlewareFactoryLoader() {
    }

    public static List<MiddlewareFactory> listFactories() {
        return ServiceLoader.load(MiddlewareFactory.class).stream()
            .map(ServiceLoader.Provider::get)
            .toList();
    }

    public static Optional<MiddlewareFactory> getFactory(String middlewareName) {
        logger.debug("Middleware factory for '{}'", middlewareName);
        return listFactories().stream()
            .filter(instance -> instance.provides().equals(middlewareName))
            .findFirst();
    }
}
