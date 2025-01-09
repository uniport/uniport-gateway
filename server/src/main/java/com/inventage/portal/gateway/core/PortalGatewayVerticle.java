package com.inventage.portal.gateway.core;

import com.inventage.portal.gateway.Runtime;
import com.inventage.portal.gateway.core.application.Application;
import com.inventage.portal.gateway.core.application.ApplicationFactory;
import com.inventage.portal.gateway.core.config.ConfigAdapter;
import com.inventage.portal.gateway.core.config.PortalGatewayConfigRetriever;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.core.entrypoint.Entrypoint;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main verticle of the portal gateway. It reads the configuration for the entrypoints and
 * creates an HTTP listener for each of them. Additionally it reads the configuration for the
 * applications and mounts them using the entrypoints.
 */
public class PortalGatewayVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortalGatewayVerticle.class);

    /**
     * Default maximum length of all headers for HTTP/1.x in bytes = {@code 10240}, i.e. 10 kilobytes
     */
    private static final int DEFAULT_HEADER_LIMIT = 10 * 1024;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        LOGGER.info("Portal-Gateway verticle is starting...");

        final ConfigRetriever retriever = PortalGatewayConfigRetriever.create(vertx);
        retriever.getConfig().onSuccess(rawConfigWithEnv -> {
            try {
                final JsonObject env = rawConfigWithEnv.copy();
                env.remove(StaticConfiguration.ENTRYPOINTS);
                env.remove(StaticConfiguration.APPLICATIONS);
                env.remove(StaticConfiguration.PROVIDERS);
                LOGGER.debug("Environemnt variables:\n{}", env.stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(Object::toString)
                    .collect(Collectors.joining("\n\t")));

                final JsonObject config = substituteConfigurationVariables(env, rawConfigWithEnv);
                final JsonArray entrypointConfigs = config.getJsonArray(StaticConfiguration.ENTRYPOINTS, JsonArray.of());
                final JsonArray applicationConfigs = config.getJsonArray(StaticConfiguration.APPLICATIONS, JsonArray.of());
                final JsonArray providerConfigs = config.getJsonArray(StaticConfiguration.PROVIDERS, JsonArray.of());
                LOGGER.debug("entrypoint configurations:\n{}", entrypointConfigs.encodePrettily());
                LOGGER.debug("application configurations:\n{}", applicationConfigs.encodePrettily());
                LOGGER.debug("provider configurations:\n{}", providerConfigs.encodePrettily());

                StaticConfiguration.validate(vertx, config).onSuccess(handler -> {
                    // get the entrypoints from the configuration
                    final List<Entrypoint> entrypoints = entrypoints(entrypointConfigs);

                    // get the applications from the configuration
                    final List<Application> applications = applications(entrypointConfigs, applicationConfigs, providerConfigs, env);

                    deployAndMountApplications(applications, entrypoints);
                    createListenersForEntrypoints(entrypoints, startPromise);
                }).onFailure(err -> {
                    LOGGER.error("Failed to validate static configuration");
                    shutdownOnStartupFailure(err);
                });
            } catch (Exception e) {
                shutdownOnStartupFailure(e);
            }
        }).onFailure(err -> {
            final String errMsg = String.format("failed to retrieve static configuration '{}'", err.getMessage());
            shutdownOnStartupFailure(new RuntimeException(errMsg));
        });
    }

    private JsonObject substituteConfigurationVariables(JsonObject env, JsonObject config) {
        return new JsonObject(ConfigAdapter.replaceEnvVariables(env, config.toString()));
    }

    private void deployAndMountApplications(List<Application> applications, List<Entrypoint> entrypoints) {
        LOGGER.debug("Number of applications '{}' and entrypoints {}'", applications.size(),
            entrypoints.size());
        applications.stream().forEach(application -> {
            application.deployOn(vertx).onSuccess(handler -> {
                entrypoints.stream().forEach(entrypoint -> entrypoint.mount(application));
            }).onFailure(this::shutdownOnStartupFailure);
        });
    }

    private void createListenersForEntrypoints(List<Entrypoint> entrypoints, Promise<Void> startPromise) {
        LOGGER.debug("Number of entrypoints {}'", entrypoints.size());
        listenOnEntrypoints(entrypoints).onSuccess(handler -> {
            startPromise.complete();
            LOGGER.info("Start succeeded.");
        }).onFailure(err -> {
            startPromise.fail(err);
            LOGGER.error("Start failed.");
        });
    }

    private Future<?> listenOnEntrypoints(List<Entrypoint> entrypoints) {
        return Future.join(entrypoints.stream().map(this::listOnEntrypoint).collect(Collectors.toList()));
    }

    private Future<?> listOnEntrypoint(Entrypoint entrypoint) {
        if (entrypoint.port() > 0) {
            final HttpServerOptions options = new HttpServerOptions()
                .setMaxHeaderSize(DEFAULT_HEADER_LIMIT)
                .setSsl(entrypoint.isTls())
                .setKeyCertOptions(entrypoint.jksOptions());
            LOGGER.info("Listening on entrypoint '{}' at port '{}'", entrypoint.name(), entrypoint.port());
            return vertx
                .createHttpServer(options)
                .requestHandler(entrypoint.router())
                .listen(entrypoint.port());
        } else {
            entrypoint.disable();
            LOGGER.warn("Disabling endpoint '{}' because its port ('{}') must be great 0",
                entrypoint.name(), entrypoint.port());
            return Future.succeededFuture();
        }
    }

    private void shutdownOnStartupFailure(Throwable throwable) {
        Runtime.fatal(vertx, throwable.getMessage());
    }

    private List<Entrypoint> entrypoints(JsonArray entrypointConfigs) {
        LOGGER.debug("Reading from config key '{}'", StaticConfiguration.ENTRYPOINTS);
        try {
            final List<Entrypoint> entrypoints = new ArrayList<>();
            if (entrypointConfigs != null) {
                entrypointConfigs.stream().map(object -> new JsonObject(Json.encode(object)))
                    .map(entrypointConfig -> new Entrypoint(vertx,
                        entrypointConfig.getString(StaticConfiguration.ENTRYPOINT_NAME),
                        entrypointConfig.getInteger(StaticConfiguration.ENTRYPOINT_PORT),
                        entrypointConfig.getJsonArray(DynamicConfiguration.MIDDLEWARES, JsonArray.of()).copy()))
                    .forEach(entrypoints::add);
            }
            return entrypoints;
        } catch (Exception e) {
            throw new IllegalStateException(
                String.format("Couldn't read '%s' configuration '%s'", StaticConfiguration.ENTRYPOINTS, e.getMessage()));
        }
    }

    private List<Application> applications(JsonArray entrypointConfigs, JsonArray applicationConfigs, JsonArray providerConfigs, JsonObject env) {
        LOGGER.debug("Reading from config key '{}'", StaticConfiguration.APPLICATIONS);
        try {
            final List<Application> applications = new ArrayList<>();
            if (applicationConfigs != null) {
                applicationConfigs.stream().map(object -> new JsonObject(Json.encode(object)))
                    .map(applicationConfig -> ApplicationFactory.Loader
                        .getProvider(applicationConfig.getString(StaticConfiguration.APPLICATION_PROVIDER))
                        .create(vertx, applicationConfig, entrypointConfigs, providerConfigs, env))
                    .forEach(applications::add);
            }
            return applications;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                String.format("Couldn't read '%s' configuration '%s'", StaticConfiguration.APPLICATIONS, e.getMessage()));
        }
    }
}
