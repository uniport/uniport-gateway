package com.inventage.portal.gateway.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventage.portal.gateway.GatewayRouterInternal;
import com.inventage.portal.gateway.Runtime;
import com.inventage.portal.gateway.core.config.ConfigAdapter;
import com.inventage.portal.gateway.core.config.PortalGatewayConfigRetriever;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.core.entrypoint.Entrypoint;
import com.inventage.portal.gateway.core.model.Gateway;
import com.inventage.portal.gateway.core.model.GatewayEntrypoint;
import com.inventage.portal.gateway.core.model.GatewayProvider;
import com.inventage.portal.gateway.proxy.config.ConfigurationWatcher;
import com.inventage.portal.gateway.proxy.listener.RouterSwitchListener;
import com.inventage.portal.gateway.proxy.model.GatewayMiddleware;
import com.inventage.portal.gateway.proxy.provider.aggregator.ProviderAggregator;
import com.inventage.portal.gateway.proxy.router.PublicProtoHostPort;
import com.inventage.portal.gateway.proxy.router.RouterFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main verticle of the portal gateway. It reads the configuration for the entrypoints and
 * creates an HTTP listener for each of them.
 */
public class PortalGatewayVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortalGatewayVerticle.class);

    private static final String CONFIGURATION_ADDRESS = "configuration-announce-address";

    /**
     * Default maximum length of all headers for HTTP/1.x in bytes = {@code 10240}, i.e. 10 kilobytes
     */
    private static final int DEFAULT_HEADER_LIMIT = 10 * 1024;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        LOGGER.info("Portal-Gateway verticle is starting...");

        PortalGatewayConfigRetriever.create(vertx).getConfig()
            .onSuccess(rawConfigWithEnv -> handleRawConfig(rawConfigWithEnv.copy(), startPromise))
            .onFailure(err -> {
                final String errMsg = String.format("failed to retrieve static configuration '{}'", err.getMessage());
                shutdownOnStartupFailure(new RuntimeException(errMsg));
            });
    }

    private void handleRawConfig(JsonObject rawConfigWithEnv, Promise<Void> startPromise) {
        try {
            final JsonObject env = filterEnvVars(rawConfigWithEnv);
            final JsonObject rawConfig = filterStaticConfig(rawConfigWithEnv);
            final JsonObject config = substituteConfigurationVariables(rawConfig, env);

            StaticConfiguration.validate(vertx, config)
                .onFailure(err -> LOGGER.error("Failed to validate static configuration"))
                .compose(v -> mapToModel(config))
                .compose(model -> runServer(model, env))
                .onFailure(err -> {
                    LOGGER.error("Failed to run server");
                    shutdownOnStartupFailure(err);
                })
                .onSuccess(servers -> startPromise.complete());
        } catch (Exception e) {
            shutdownOnStartupFailure(e);
        }
    }

    private JsonObject filterEnvVars(JsonObject rawConfig) {
        final JsonObject env = rawConfig.copy();
        env.remove(StaticConfiguration.ENTRYPOINTS);
        env.remove(StaticConfiguration.PROVIDERS);

        LOGGER.debug("Environment variables:\n\t{}", env.stream()
            .sorted(Map.Entry.comparingByKey())
            .map(Object::toString)
            .collect(Collectors.joining("\n\t")));
        return env;
    }

    private JsonObject filterStaticConfig(JsonObject rawConfig) {
        final JsonObject config = new JsonObject();

        final JsonArray entrypoints = rawConfig.getJsonArray(StaticConfiguration.ENTRYPOINTS);
        if (entrypoints != null) {
            config.put(StaticConfiguration.ENTRYPOINTS, entrypoints);
        }

        final JsonArray providers = rawConfig.getJsonArray(StaticConfiguration.PROVIDERS);
        if (providers != null) {
            config.put(StaticConfiguration.PROVIDERS, providers);
        }

        LOGGER.debug("Static configuration:\n{}", config.encodePrettily());
        return config;
    }

    private JsonObject substituteConfigurationVariables(JsonObject config, JsonObject env) {
        return new JsonObject(ConfigAdapter.replaceEnvVariables(config.encode(), env));
    }

    private Future<Gateway> mapToModel(JsonObject config) {
        final ObjectMapper codec = new ObjectMapper();
        Gateway gateway = null;
        try {
            gateway = codec.readValue(config.encode(), Gateway.class);
        } catch (JsonProcessingException e) {
            return Future.failedFuture(e);
        }
        return Future.succeededFuture(gateway);
    }

    private Future<List<HttpServer>> runServer(Gateway config, JsonObject env) {
        final Function<ConfigurationWatcher, List<Future<HttpServer>>> deployEntrypoints = w -> config.getEntrypoints().stream()
            .map(ep -> createEntrypoint(ep, env, w)
                .compose(entrypoint -> entryPointListen(entrypoint)))
            .toList();

        return deployConfigurationWatcher(config.getEntrypoints(), config.getProviders(), env)
            .map(w -> Future.join(deployEntrypoints.apply(w)))
            .mapEmpty();
    }

    private Future<ConfigurationWatcher> deployConfigurationWatcher(List<GatewayEntrypoint> entrypoints, List<GatewayProvider> providers, JsonObject env) {
        final ProviderAggregator aggregator = new ProviderAggregator(vertx, CONFIGURATION_ADDRESS, providers, env);

        final int pvdThrottleDuration = env.getInteger(StaticConfiguration.PROVIDERS_THROTTLE_INTERVAL_MS, 2000);
        final List<String> epNames = entrypoints.stream()
            .map(config -> config.getName())
            .toList();
        final ConfigurationWatcher watcher = new ConfigurationWatcher(vertx, aggregator, CONFIGURATION_ADDRESS, pvdThrottleDuration, epNames);

        return Future.succeededFuture(watcher);
    }

    private Future<Entrypoint> createEntrypoint(GatewayEntrypoint entrypoint, JsonObject env, ConfigurationWatcher watcher) {
        final String epName = entrypoint.getName();
        final int epPort = entrypoint.getPort();
        final List<GatewayMiddleware> epMiddlewares = entrypoint.getMiddlewares();

        final PublicProtoHostPort publicProtoHostPort = PublicProtoHostPort.of(env, epPort);
        final RouterFactory routerFactory = new RouterFactory(vertx, publicProtoHostPort, epName);

        final Entrypoint ep = new Entrypoint(vertx, epName, epPort, epMiddlewares);

        // this glues the entrypoint and the dynamic configuration
        // there are 3 routers in play here:
        // * the top level router containing the entrypoint middlewares created by the Entrypoint
        // * the bottom level router containing the router middlewares created by the RouterFactory 
        // * and the mid level router glueing the top and the bottom level router together that can be cleared on a dynamic configuration change without affecting the top level router  
        final GatewayRouterInternal glueRouter = GatewayRouterInternal.router(vertx, "glue");
        ep.router().mountSubRouter("/", glueRouter);
        watcher.addListener(new RouterSwitchListener(glueRouter, routerFactory));

        return vertx.deployVerticle(watcher)
            .map(id -> ep);
    }

    private Future<HttpServer> entryPointListen(Entrypoint entrypoint) {
        final HttpServerOptions options = new HttpServerOptions()
            .setMaxHeaderSize(DEFAULT_HEADER_LIMIT)
            .setSsl(entrypoint.isTls())
            .setKeyCertOptions(entrypoint.jksOptions());

        LOGGER.info("Listening on entrypoint '{}' at port '{}'", entrypoint.name(), entrypoint.port());
        return vertx
            .createHttpServer(options)
            .requestHandler(entrypoint.router())
            .listen(entrypoint.port());
    }

    private void shutdownOnStartupFailure(Throwable throwable) {
        Runtime.fatal(vertx, throwable.getMessage());
    }
}
