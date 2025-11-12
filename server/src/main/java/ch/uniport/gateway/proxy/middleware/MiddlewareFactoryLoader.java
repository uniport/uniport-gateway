package ch.uniport.gateway.proxy.middleware;

import ch.uniport.gateway.Runtime;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
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

    public static class Holder {
        private static final MiddlewareFactoryLoader INSTANCE = new MiddlewareFactoryLoader();
    }

    public static MiddlewareFactoryLoader getInstance() {
        return Holder.INSTANCE;
    }

    private URLClassLoader classLoader;

    private MiddlewareFactoryLoader() {
        // prevent reflection-based instantiation
        if (Holder.INSTANCE != null) {
            throw new IllegalStateException("Singleton already initialized.");
        }

        // a globally unique class loader is required (caveat: blocks dynamic extensions
        // reloading)
        final List<URL> urls = getJarURLs();
        final ClassLoader parent = MiddlewareFactory.class.getClassLoader();
        classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), parent);
    }

    public synchronized Optional<MiddlewareFactory> getFactory(String middlewareName) {
        logger.debug("Middleware factory for '{}'", middlewareName);
        return listFactories().stream()
            .filter(instance -> instance.provides().equals(middlewareName))
            .findFirst();
    }

    public synchronized List<MiddlewareFactory> listFactories() {
        final ServiceLoader<MiddlewareFactory> loader = ServiceLoader.load(MiddlewareFactory.class, classLoader);

        logger.debug("Discovered middleware factories:");
        for (MiddlewareFactory factory : loader) {
            logger.debug(factory.provides());
        }

        return loader.stream()
            .map(ServiceLoader.Provider::get)
            .toList();
    }

    private static List<URL> getJarURLs() {
        final String extensionsPath = Runtime.getExtensionsPath();
        final File extensionsDirectory = new File(extensionsPath);

        if (!extensionsDirectory.exists() || !extensionsDirectory.isDirectory()) {
            logger.warn("Extensions directory not found: " + extensionsPath);
            return List.of();
        }

        final File[] jarFiles = extensionsDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jarFiles == null) {
            return List.of();
        }

        final List<URL> jarUrls = new ArrayList<>();
        for (File file : jarFiles) {
            final URL url;
            try {
                url = file.toURI().toURL();
                jarUrls.add(url);
            } catch (MalformedURLException e) {
                logger.warn(e.toString());
                continue;
            }
        }
        return jarUrls;
    }
}
