package com.inventage.portal.gateway.proxy;

import com.inventage.portal.gateway.GatewayRouterInternal;
import com.inventage.portal.gateway.core.application.Application;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.proxy.config.ConfigurationWatcher;
import com.inventage.portal.gateway.proxy.listener.RouterSwitchListener;
import com.inventage.portal.gateway.proxy.provider.aggregator.ProviderAggregator;
import com.inventage.portal.gateway.proxy.router.RouterFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import java.util.Collections;
import java.util.Optional;

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

    /**
     * the name of this instance
     */
    private final String name;

    /**
     * the public protocol as it is seen from outside (e.g. by a browser)
     */
    private final String publicProtocol;

    /**
     * the public hostname as it is seen from outside (e.g. by a browser)
     */
    private final String publicHostname;

    /**
     * the public port as it is seen from outside (e.g. by a browser)
     */
    private final String publicPort;

    /**
     * the name of the entrypoint this application should be mounted on
     */
    private final String entrypointName;

    /**
     * the selection criteria for delegating incoming requests to this application
     */
    private static final String ROOT_PATH = "/";

    /**
     * the router on which the routes for this application will be added
     */
    private final GatewayRouterInternal router;

    /**
     * the providers that should be launched for this application to retrieve the dynamic
     * configuration
     */
    private final JsonArray providerConfigs;

    private final JsonObject env;

    private final int providersThrottleDuration;

    public ProxyApplication(Vertx vertx, String name, String entrypointName, int entrypointPort, JsonArray providerConfigs, JsonObject env) {
        this.name = name;
        this.entrypointName = entrypointName;
        this.router = GatewayRouterInternal.router(vertx, String.format("application %s", name));

        this.providerConfigs = providerConfigs.copy();
        this.providersThrottleDuration = env.getInteger(StaticConfiguration.PROVIDERS_THROTTLE_INTERVAL_MS, 2000);

        this.env = env.copy();

        this.publicProtocol = env.getString(PORTAL_GATEWAY_PUBLIC_PROTOCOL, PORTAL_GATEWAY_PUBLIC_PROTOCOL_DEFAULT);
        this.publicHostname = env.getString(PORTAL_GATEWAY_PUBLIC_HOSTNAME, PORTAL_GATEWAY_PUBLIC_HOSTNAME_DEFAULT);
        this.publicPort = env.getString(PORTAL_GATEWAY_PUBLIC_PORT, String.format("%d", entrypointPort));
    }

    @Override
    public String toString() {
        return String.format("%s:%s", getClass().getSimpleName(), name);
    }

    @Override
    public String rootPath() {
        return ROOT_PATH;
    }

    @Override
    public Optional<Router> router() {
        return Optional.of(router);
    }

    @Override
    public String entrypoint() {
        return entrypointName;
    }

    @Override
    public Future<?> deployOn(Vertx vertx) {
        final String configurationAddress = "configuration-announce-address";

        final ProviderAggregator aggregator = new ProviderAggregator(vertx, configurationAddress, providerConfigs, this.env);

        final ConfigurationWatcher watcher = new ConfigurationWatcher(vertx, aggregator, configurationAddress,
            this.providersThrottleDuration, Collections.singletonList(this.entrypointName));

        final RouterFactory routerFactory = new RouterFactory(vertx, publicProtocol, publicHostname, publicPort, entrypointName);

        watcher.addListener(new RouterSwitchListener(this.router, routerFactory));

        return vertx.deployVerticle(watcher);
    }

}
