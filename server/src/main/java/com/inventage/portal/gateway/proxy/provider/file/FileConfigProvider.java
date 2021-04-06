package com.inventage.portal.gateway.proxy.provider.file;

import java.io.File;
import com.inventage.portal.gateway.core.config.ConfigAdapter;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.provider.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Generates a complete dynamic configuration from a file.
 */
public class FileConfigProvider extends Provider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileConfigProvider.class);

    private Vertx vertx;

    private EventBus eb;
    private String configurationAddress;

    private String filename;
    private String directory;
    private Boolean watch;

    private JsonObject env;

    private int scanPeriodMs = 5000;

    public FileConfigProvider(Vertx vertx, String configurationAddress, String filename,
            String directory, Boolean watch, JsonObject env) {
        LOGGER.trace("construcutor");
        this.vertx = vertx;
        this.eb = vertx.eventBus();
        this.configurationAddress = configurationAddress;

        this.filename = filename;
        this.directory = directory;
        this.watch = watch;

        this.env = env;
    }

    public void start(Promise<Void> startPromise) {
        LOGGER.trace("start");
        provide(startPromise);
    }

    @Override
    public void provide(Promise<Void> startPromise) {
        LOGGER.trace("provide");
        ConfigRetriever retriever = ConfigRetriever.create(vertx, getOptions());
        retriever.getConfig().onSuccess(config -> {
            this.validateAndPublish(substituteConfigurationVariables(env, config));
        }).onFailure(err -> {
            String errorMsg =
                    String.format("provide: cannot retrieve configuration '{}'", err.getMessage());
            LOGGER.warn(errorMsg);
        });

        if (this.watch) {
            LOGGER.info("provider: Listening to configuration changes");
            retriever.listen(ar -> {
                JsonObject config = ar.getNewConfiguration();
                this.validateAndPublish(substituteConfigurationVariables(env, config));
            });
        }
        startPromise.complete();
    }

    private ConfigRetrieverOptions getOptions() {
        LOGGER.trace("getOptions");
        ConfigRetrieverOptions options = new ConfigRetrieverOptions();
        if (this.watch) {
            LOGGER.info("getOptions: setting scan period to '{}'", this.scanPeriodMs);
            options.setScanPeriod(this.scanPeriodMs);
        }
        if (this.filename != null && this.filename.length() != 0) {
            LOGGER.info("getOptions: reading file '{}'", this.filename);

            final File file = new File(this.filename);
            ConfigStoreOptions fileStore =
                    new ConfigStoreOptions().setType("file").setFormat("json")
                            .setConfig(new JsonObject().put("path", file.getAbsolutePath()));

            return options.addStore(fileStore);
        }

        if (this.directory != null && this.directory.length() != 0) {
            LOGGER.info("getOptions: reading directory '{}'", this.directory);

            // TODO document
            ConfigStoreOptions dirStore = new ConfigStoreOptions().setType("jsonDirectory")
                    .setConfig(new JsonObject().put("path", this.directory).put("filesets",
                            new JsonArray().add(new JsonObject().put("pattern", "general/*.json"))
                                    .add(new JsonObject().put("pattern", "auth/*.json"))));

            return options.addStore(dirStore);
        }

        LOGGER.warn("getOptions: neither filename or directory defined");
        return options;
    }

    private JsonObject substituteConfigurationVariables(JsonObject env, JsonObject config) {
        return new JsonObject(ConfigAdapter.replaceEnvVariables(env, config.toString()));
    }

    private void validateAndPublish(JsonObject config) {
        LOGGER.trace("validateAndPublish");
        LOGGER.debug("validateAndPublish: configuration from file '{}'", config);
        DynamicConfiguration.validate(this.vertx, config, false).onSuccess(f -> {
            LOGGER.info("validateAndPublish: configuration published");
            this.eb.publish(this.configurationAddress,
                    new JsonObject().put(Provider.PROVIDER_NAME, StaticConfiguration.PROVIDER_FILE)
                            .put(Provider.PROVIDER_CONFIGURATION, config));

        }).onFailure(err -> {
            LOGGER.warn("validateAndPublish: Ignoring invalid configuration '{}'",
                    err.getMessage());
        });
    }
}
