package com.inventage.portal.gateway.core;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.inventage.portal.gateway.core.application.Application;
import com.inventage.portal.gateway.core.application.ApplicationFactory;
import com.inventage.portal.gateway.core.config.PortalGatewayConfigRetriever;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.core.entrypoint.Entrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * The main verticle of the portal gateway. It reads the configuration for the entrypoints and
 * creates an HTTP listener for each of them. Additionally it reads the configuration for the
 * applications and mounts them using the entrypoints.
 */
public class PortalGatewayVerticle extends AbstractVerticle {

    // env variable
    public static final String PORTAL_GATEWAY_PUBLIC_HOSTNAME = "PORTAL_GATEWAY_PUBLIC_HOSTNAME";
    public static final String PORTAL_GATEWAY_PUBLIC_HOSTNAME_DEFAULT = "localhost";

    private static final Logger LOGGER = LoggerFactory.getLogger(PortalGatewayVerticle.class);

    private String publicHostname;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        LOGGER.info("start: ...");

        final ConfigRetriever retriever = PortalGatewayConfigRetriever.create(vertx);
        retriever.getConfig(asyncResult -> {
            try {
                final JsonObject staticConfig = asyncResult.result();
                StaticConfiguration.validate(vertx, staticConfig).onComplete(ar -> {
                    if (ar.failed()) {
                        LOGGER.error("start: Failed to validate static configuration");
                        shutdownOnStartupFailure(ar.cause());
                    }

                    publicHostname = staticConfig.getString(PORTAL_GATEWAY_PUBLIC_HOSTNAME,
                            PORTAL_GATEWAY_PUBLIC_HOSTNAME_DEFAULT);

                    // get the entrypoints from the configuration
                    List<Entrypoint> entrypoints = entrypoints(staticConfig);

                    // get the applications from the configuration
                    final List<Application> applications = applications(staticConfig);

                    deployAndMountApplications(applications, entrypoints);
                    createListenersForEntrypoints(entrypoints, staticConfig, startPromise);
                });
            } catch (Exception e) {
                shutdownOnStartupFailure(e);
            }
        });
    }

    private void deployAndMountApplications(List<Application> applications,
            List<Entrypoint> entrypoints) {
        LOGGER.debug("deployAndMountApplications: number of applications '{}' and entrypoints {}'",
                applications.size(), entrypoints.size());
        applications.stream().forEach(application -> {
            application.deployOn(vertx).onComplete(deployed -> {
                if (deployed.succeeded()) {
                    entrypoints.stream().forEach(entrypoint -> entrypoint.mount(application));
                } else {
                    shutdownOnStartupFailure(deployed.cause());
                }
            });
        });
    }

    private void createListenersForEntrypoints(List<Entrypoint> entrypoints, JsonObject config,
            Promise<Void> startPromise) {
        LOGGER.debug("createListenersForEntrypoints: number of entrypoints {}'",
                entrypoints.size());
        listenOnEntrypoints(entrypoints).onComplete(all -> {
            if (all.succeeded()) {
                startPromise.complete();
                LOGGER.info("createListenersForEntrypoints: start succeeded.");
            } else {
                startPromise.fail(all.cause());
                LOGGER.error("createListenersForEntrypoints: start failed.");
            }
        });
    }

    private Future<?> listenOnEntrypoints(List<Entrypoint> entrypoints) {
        return CompositeFuture.join(
                entrypoints.stream().map(this::listOnEntrypoint).collect(Collectors.toList()));
    }

    private Future<?> listOnEntrypoint(Entrypoint entrypoint) {
        if (entrypoint.port() > 0) {
            final HttpServerOptions options = new HttpServerOptions().setMaxHeaderSize(1024 * 20)
                    .setSsl(entrypoint.isTls()).setKeyStoreOptions(entrypoint.jksOptions());
            LOGGER.info("listOnEntrypoint: '{}' at port '{}'", entrypoint.name(),
                    entrypoint.port());
            return vertx.createHttpServer(options).requestHandler(entrypoint.router())
                    .listen(entrypoint.port());
        } else {
            entrypoint.disable();
            LOGGER.warn(
                    "listOnEntrypoint: disabling endpoint '{}' because its port ('{}') must be great 0",
                    entrypoint.name(), entrypoint.port());
            return Future.succeededFuture();
        }
    }

    private void shutdownOnStartupFailure(Throwable throwable) {
        if (throwable instanceof IllegalArgumentException) {
            LOGGER.error("shutdownOnStartupFailure: will shut down because '{}'",
                    throwable.getMessage());
        } else {
            LOGGER.error("shutdownOnStartupFailure: will shut down because '{}'",
                    throwable.getMessage(), throwable);
        }
        vertx.close();
    }

    private List<Entrypoint> entrypoints(JsonObject config) {
        LOGGER.debug("entrypoints: reading from config key '{}'", Entrypoint.ENTRYPOINTS);
        try {
            final List<Entrypoint> entrypoints = new ArrayList<>();
            final JsonArray configs = config.getJsonArray(Entrypoint.ENTRYPOINTS);
            if (configs != null) {
                configs.stream().map(object -> new JsonObject(Json.encode(object)))
                        .map(entrypoint -> new Entrypoint(entrypoint.getString(Entrypoint.NAME),
                                publicHostname, entrypoint.getInteger(Entrypoint.PORT), vertx))
                        .forEach(entrypoints::add);
            }
            return entrypoints;
        } catch (Exception e) {
            throw new IllegalStateException(
                    String.format("Couldn't read %s configuration", Entrypoint.ENTRYPOINTS));
        }
    }

    private List<Application> applications(JsonObject config) {
        LOGGER.debug("applications: reading from config key '{}'", Application.APPLICATIONS);
        try {
            final List<Application> applications = new ArrayList<>();
            final JsonArray configs = config.getJsonArray(Application.APPLICATIONS);
            if (configs != null) {
                configs.stream().map(object -> new JsonObject(Json.encode(object)))
                        .map(application -> ApplicationFactory.Loader
                                .getProvider(application.getString(Application.PROVIDER))
                                .create(application, config, vertx))
                        .forEach(applications::add);
            }
            return applications;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    String.format("Couldn't read %s configuration", Application.APPLICATIONS));
        }
    }

}
