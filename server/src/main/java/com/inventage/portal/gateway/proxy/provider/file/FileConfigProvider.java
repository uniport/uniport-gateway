package com.inventage.portal.gateway.proxy.provider.file;

import com.inventage.portal.gateway.core.config.ConfigAdapter;
import com.inventage.portal.gateway.core.config.PortalGatewayConfigRetriever;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.provider.Provider;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

/**
 * Generates a complete dynamic configuration from a file.
 */
public class FileConfigProvider extends Provider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileConfigProvider.class);

    private final Vertx vertx;

    private final EventBus eb;
    private final String configurationAddress;
    private Path staticConfigDir;

    private Path filename;
    private Path directory;
    private final Boolean watch;

    private final JsonObject env;
    private String source;

    private int scanPeriodMs = 5000;

    public FileConfigProvider(Vertx vertx, String configurationAddress, String filename, String directory,
                              Boolean watch, JsonObject env) {
        this.vertx = vertx;
        this.eb = vertx.eventBus();
        this.configurationAddress = configurationAddress;
        PortalGatewayConfigRetriever.getStaticConfigPath().ifPresent(path -> this.staticConfigDir = path.getParent());

        if (filename != null && filename.length() != 0) {
            this.filename = Path.of(filename);
        }
        if (directory != null && directory.length() != 0) {
            this.directory = Path.of(directory);
        }
        this.watch = watch;

        if (env == null) {
            env = new JsonObject();
        }
        this.env = env;
    }

    public void start(Promise<Void> startPromise) {
        provide(startPromise);
    }

    @Override
    public void provide(Promise<Void> startPromise) {
        final ConfigRetriever retriever = ConfigRetriever.create(vertx, getOptions());
        retriever.getConfig().onSuccess(config -> {
            this.validateAndPublish(parseServerPorts(substituteConfigurationVariables(env, config)));
        }).onFailure(err -> {
            final String errMsg = String.format("provide: cannot retrieve configuration '{}'", err.getMessage());
            LOGGER.warn(errMsg);
        });

        if (this.watch) {
            LOGGER.info("Listening to configuration changes");
            retriever.listen(ar -> {
                final JsonObject config = ar.getNewConfiguration();
                this.validateAndPublish(parseServerPorts(substituteConfigurationVariables(env, config)));
            });
        }
        startPromise.complete();
    }

    public String toString() {
        return StaticConfiguration.PROVIDER_FILE;
    }

    private ConfigRetrieverOptions getOptions() {
        final ConfigRetrieverOptions options = new ConfigRetrieverOptions();
        if (this.watch) {
            LOGGER.info("Setting scan period to '{}'", this.scanPeriodMs);
            options.setScanPeriod(this.scanPeriodMs);
        }

        if (this.filename != null) {
            final File file = this.getAbsoluteConfigPath(this.filename).toFile();
            LOGGER.info("Reading file '{}'", file.getAbsolutePath());

            final ConfigStoreOptions fileStore = new ConfigStoreOptions().setType("file").setFormat("json")
                .setConfig(new JsonObject().put("path", file.getAbsolutePath()));

            this.source = "file";
            return options.addStore(fileStore);
        }

        if (this.directory != null) {
            final Path path = this.getAbsoluteConfigPath(this.directory);
            if (path == null) {
                LOGGER.warn("Failed to create absolute config path of '{}'", this.directory);
                this.source = "undefined";
                return options;
            }

            LOGGER.info("Reading directory '{}'", path);
            final JsonArray fileSets = new JsonArray();
            final File[] children = path.toFile().listFiles();
            if (children == null) {
                throw new IllegalArgumentException("The `path` must be a directory");
            }
            else {
                for (File file : children) {
                    // we only take files into account at depth 2 (like general/test.json)
                    if (file.isDirectory()) {
                        LOGGER.debug("Attempting to read json config from '{}'", file.getAbsolutePath());
                        fileSets.add(new JsonObject().put("pattern", String.format("%s/*.json", file.getName())));
                    }
                }
            }

            final ConfigStoreOptions dirStore = new ConfigStoreOptions().setType("jsonDirectory")
                .setConfig(new JsonObject().put("path", path.toString()).put("filesets", fileSets));

            this.source = "directory";
            return options.addStore(dirStore);
        }

        this.source = "undefined";
        LOGGER.warn("Neither filename or directory defined");
        return options;
    }

    private Path getAbsoluteConfigPath(Path path) {
        if (path.isAbsolute()) {
            LOGGER.debug("Using absolute file path");
            return path;
        }
        if (this.staticConfigDir == null) {
            LOGGER.warn(
                "No static config dir defined. Cannot assemble absolute config path from '{}'",
                path);
            return null;
        }
        LOGGER.debug("Using path relative to the static config file in '{}'",
            this.staticConfigDir.toAbsolutePath());
        return this.staticConfigDir.resolve(path).normalize();
    }

    private JsonObject substituteConfigurationVariables(JsonObject env, JsonObject config) {
        // TODO parse parsable values like boolean and integers (rhen rm parseServerPorts and optionalStr)
        return new JsonObject(ConfigAdapter.replaceEnvVariables(env, config.toString()));
    }

    // To allow variable substitution by environment variables in the file config
    // the server ports of the incoming dynamic file configuration need to be
    // converted to integers.
    private JsonObject parseServerPorts(JsonObject config) {
        if (!config.containsKey(DynamicConfiguration.HTTP)) {
            return config;
        }
        final JsonObject http = config.getJsonObject(DynamicConfiguration.HTTP);
        if (!http.containsKey(DynamicConfiguration.SERVICES)) {
            return config;
        }
        final JsonArray services = http.getJsonArray(DynamicConfiguration.SERVICES);
        for (int i = 0; i < services.size(); i++) {
            final JsonObject service = services.getJsonObject(i);
            if (!service.containsKey(DynamicConfiguration.SERVICE_SERVERS)) {
                continue;
            }
            final JsonArray servers = service.getJsonArray(DynamicConfiguration.SERVICE_SERVERS);
            for (int j = 0; j < servers.size(); j++) {
                final JsonObject server = servers.getJsonObject(j);
                if (!server.containsKey(DynamicConfiguration.SERVICE_SERVER_PORT)) {
                    continue;
                }
                final String portStr = server.getString(DynamicConfiguration.SERVICE_SERVER_PORT);
                final int port;

                try {
                    port = Integer.parseInt(portStr);
                }
                catch (NumberFormatException e) {
                    LOGGER.warn("Failed to parse server port '{}'", portStr);
                    return config;
                }

                server.put(DynamicConfiguration.SERVICE_SERVER_PORT, port);
            }
        }
        return config;
    }

    private void validateAndPublish(JsonObject config) {
        DynamicConfiguration.validate(this.vertx, config, false).onSuccess(f -> {
            this.eb.publish(this.configurationAddress,
                new JsonObject().put(Provider.PROVIDER_NAME, StaticConfiguration.PROVIDER_FILE)
                    .put(Provider.PROVIDER_CONFIGURATION, config));
            LOGGER.info("Configuration published from '{}'", this.source);
        }).onFailure(err -> {
            LOGGER.warn("Ignoring invalid configuration '{}' from '{}'", err.getMessage(),
                this.source);
        });
    }
}
