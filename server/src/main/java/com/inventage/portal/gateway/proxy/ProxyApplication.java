package com.inventage.portal.gateway.proxy;

import java.util.Arrays;
import java.util.Optional;
import com.inventage.portal.gateway.core.application.Application;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.proxy.config.ConfigurationWatcher;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.listener.RouterSwitchListener;
import com.inventage.portal.gateway.proxy.provider.aggregator.ProviderAggregator;
import com.inventage.portal.gateway.proxy.router.RouterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

/**
 * Application for the proxy feature of the portal gateway. The routers will be read from the
 * incoming configurations provided by different providers.
 */
public class ProxyApplication implements Application {

    // env variable
    public static final String PORTAL_GATEWAY_PUBLIC_PROTOCOL = "PORTAL_GATEWAY_PUBLIC_PROTOCOL";
    public static final String PORTAL_GATEWAY_PUBLIC_PROTOCOL_DEFAULT = "http";
    public static final String PORTAL_GATEWAY_PUBLIC_HOSTNAME = "PORTAL_GATEWAY_PUBLIC_HOSTNAME";
    public static final String PORTAL_GATEWAY_PUBLIC_HOSTNAME_DEFAULT = "localhost";
    public static final String PORTAL_GATEWAY_PUBLIC_PORT = "PORTAL_GATEWAY_PUBLIC_PORT";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyApplication.class);

    /**
     * the name of this instance
     */
    private final String name;

    /**
     * the public url as it is seen from outside (e.g. by a browser)
     */
    private final String publicUrl;

    /**
     * the name of the entrypoint this application should be mounted on
     */
    private final String entrypoint;

    /**
     * the port of the entrypoint this application should be mounted on
     */
    private String entrypointPort;

    /**
     * the selection criteria for delegating incoming requests to this application
     */
    private String rootPath = "/";

    /**
     * the router on which the routes for this application will be added
     */
    private Router router;

    /**
     * the providers that should be launched for this applicaiton to retrieve the dynamic
     * configuration
     */
    private JsonArray providers;

    private JsonObject env;

    private int providersThrottleDuration;

    public ProxyApplication(String name, String entrypoint, JsonObject staticConfig, Vertx vertx) {
        this.name = name;
        this.entrypoint = entrypoint;
        this.router = Router.router(vertx);

        this.providers = staticConfig.getJsonArray(StaticConfiguration.PROVIDERS);
        this.providersThrottleDuration =
                staticConfig.getInteger(StaticConfiguration.PROVIDERS_THROTTLE_INTERVAL_MS, 2000);

        this.env = staticConfig.copy();
        this.env.remove(StaticConfiguration.ENTRYPOINTS);
        this.env.remove(StaticConfiguration.APPLICATIONS);
        this.env.remove(StaticConfiguration.PROVIDERS);

        String publicProtocol = env.getString(PORTAL_GATEWAY_PUBLIC_PROTOCOL,
                PORTAL_GATEWAY_PUBLIC_PROTOCOL_DEFAULT);
        String publicHostname = env.getString(PORTAL_GATEWAY_PUBLIC_HOSTNAME,
                PORTAL_GATEWAY_PUBLIC_HOSTNAME_DEFAULT);
        String publicPort = env.getString(PORTAL_GATEWAY_PUBLIC_PORT);

        this.entrypointPort = DynamicConfiguration
                .getObjByKeyWithValue(staticConfig.getJsonArray(StaticConfiguration.ENTRYPOINTS),
                        StaticConfiguration.ENTRYPOINT_NAME, this.entrypoint)
                .getString(StaticConfiguration.ENTRYPOINT_PORT);

        this.publicUrl = String.format("%s://%s:%s", publicProtocol, publicHostname,
                publicPort != null ? publicPort : entrypointPort);
    }

    public String toString() {
        return String.format("%s:%s", getClass().getSimpleName(), name);
    }

    @Override
    public String rootPath() {
        return rootPath;
    }

    @Override
    public Optional<Router> router() {
        return Optional.of(router);
    }

    @Override
    public String entrypoint() {
        return entrypoint;
    }

    @Override
    public Future<?> deployOn(Vertx vertx) {
        String configurationAddress = "configuration-announce-address";

        ProviderAggregator aggregator =
                new ProviderAggregator(vertx, configurationAddress, providers, this.env);

        ConfigurationWatcher watcher =
                new ConfigurationWatcher(vertx, aggregator, configurationAddress,
                        this.providersThrottleDuration, Arrays.asList(this.entrypoint));

        RouterFactory routerFactory = new RouterFactory(vertx, publicUrl);

        watcher.addListener(new RouterSwitchListener(this.router, routerFactory));

        return watcher.start();
    }

}
