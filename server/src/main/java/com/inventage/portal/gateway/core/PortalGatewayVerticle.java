package com.inventage.portal.gateway.core;

import com.inventage.portal.gateway.core.application.Application;
import com.inventage.portal.gateway.core.application.ApplicationFactory;
import com.inventage.portal.gateway.core.config.PortalGatewayConfigRetriever;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.core.entrypoint.Entrypoint;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The main verticle of the portal gateway. It reads the configuration for the entrypoints and
 * creates an HTTP listener for each of them. Additionally it reads the configuration for the
 * applications and mounts them using the entrypoints.
 */
public class PortalGatewayVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortalGatewayVerticle.class);

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        LOGGER.info("Portal-Gateway verticle is starting...");

        final ConfigRetriever retriever = PortalGatewayConfigRetriever.create(vertx);
        retriever.getConfig(asyncResult -> {
            try {
                final JsonObject staticConfig = asyncResult.result();
                StaticConfiguration.validate(vertx, staticConfig).onSuccess(handler -> {
                    // get the entrypoints from the configuration
                    final List<Entrypoint> entrypoints = entrypoints(staticConfig);

                    // get the applications from the configuration
                    final List<Application> applications = applications(staticConfig);

                    deployAndMountApplications(applications, entrypoints);
                    createListenersForEntrypoints(entrypoints, staticConfig, startPromise);
                }).onFailure(err -> {
                    LOGGER.error("Failed to validate static configuration");
                    shutdownOnStartupFailure(err);
                });
            }
            catch (Exception e) {
                shutdownOnStartupFailure(e);
            }
        });
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

    private void createListenersForEntrypoints(List<Entrypoint> entrypoints, JsonObject config,
                                               Promise<Void> startPromise) {
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
        return CompositeFuture.join(entrypoints.stream().map(this::listOnEntrypoint).collect(Collectors.toList()));
    }

    private Future<?> listOnEntrypoint(Entrypoint entrypoint) {
        if (entrypoint.port() > 0) {
            final HttpServerOptions options = new HttpServerOptions()
                    .setMaxHeaderSize(1024 * 20)
                    .setSsl(entrypoint.isTls())
                    .setKeyStoreOptions(entrypoint.jksOptions());
            LOGGER.info("Listening on entrypoint '{}' at port '{}'", entrypoint.name(), entrypoint.port());
            return vertx.createHttpServer(options).requestHandler(entrypoint.router()).listen(entrypoint.port());
        }
        else {
            entrypoint.disable();
            LOGGER.warn("Disabling endpoint '{}' because its port ('{}') must be great 0",
                    entrypoint.name(), entrypoint.port());
            return Future.succeededFuture();
        }
    }

    private void shutdownOnStartupFailure(Throwable throwable) {
        if (throwable instanceof IllegalArgumentException) {
            LOGGER.error("Will shut down because '{}'", throwable.getMessage());
        }
        else {
            LOGGER.error("Will shut down because '{}'", throwable.getMessage(), throwable);
        }
        vertx.close();
    }

    private List<Entrypoint> entrypoints(JsonObject config) {
        LOGGER.debug("Reading from config key '{}'", StaticConfiguration.ENTRYPOINTS);
        try {
            final List<Entrypoint> entrypoints = new ArrayList<>();
            final JsonArray configs = config.getJsonArray(StaticConfiguration.ENTRYPOINTS);
            if (configs != null) {
                configs.stream().map(object -> new JsonObject(Json.encode(object)))
                        .map(entrypoint -> new Entrypoint(vertx,
                                entrypoint.getString(StaticConfiguration.ENTRYPOINT_NAME),
                                entrypoint.getInteger(StaticConfiguration.ENTRYPOINT_PORT),
                                entrypoint.getJsonArray(DynamicConfiguration.MIDDLEWARES)))
                        .forEach(entrypoints::add);
            }
            return entrypoints;
        }
        catch (Exception e) {
            throw new IllegalStateException(
                    String.format("Couldn't read '%s' configuration", StaticConfiguration.ENTRYPOINTS));
        }
    }

    private List<Application> applications(JsonObject config) {
        LOGGER.debug("Reading from config key '{}'", StaticConfiguration.APPLICATIONS);
        try {
            final List<Application> applications = new ArrayList<>();
            final JsonArray configs = config.getJsonArray(StaticConfiguration.APPLICATIONS);
            if (configs != null) {
                configs.stream().map(object -> new JsonObject(Json.encode(object)))
                        .map(application -> ApplicationFactory.Loader
                                .getProvider(application.getString(StaticConfiguration.APPLICATION_PROVIDER))
                                .create(application, config, vertx))
                        .forEach(applications::add);
            }
            return applications;
        }
        catch (IllegalStateException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IllegalStateException(
                    String.format("Couldn't read '%s' configuration", StaticConfiguration.APPLICATIONS));
        }
    }

}
