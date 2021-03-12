package com.inventage.portal.gateway.core.provider.file;

import java.io.File;

import com.inventage.portal.gateway.core.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.core.provider.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class FileConfigProvider implements Provider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileConfigProvider.class);

    private Vertx vertx;
    private EventBus eb;

    private String filename;
    private String directory;

    private Boolean watch;
    private int scanPeriodMs = 5000;

    public FileConfigProvider(Vertx vertx, String filename, String directory, Boolean watch) {
        this.vertx = vertx;
        this.eb = vertx.eventBus();

        this.filename = filename;
        this.directory = directory;

        this.watch = watch;
    }

    public FileConfigProvider(Vertx vertx, String filename, String directory) {
        this(vertx, filename, directory, false);
    }

    @Override
    public void provide(String configurationAddress) {
        ConfigRetriever retriever = ConfigRetriever.create(vertx, getOptions());
        retriever.getConfig(ar -> {
            if (ar.succeeded()) {
                LOGGER.info("configuration retrieved");
                final JsonObject config = ar.result();
                this.validateAndPublish(config, configurationAddress);
            } else {
                LOGGER.error("cannot retrieve configuration");
            }
        });
        if (this.watch) {
            retriever.listen(ar -> {
                JsonObject config = ar.getNewConfiguration();
                this.validateAndPublish(config, configurationAddress);
            });
        }
    }

    private ConfigRetrieverOptions getOptions() {
        ConfigRetrieverOptions options = new ConfigRetrieverOptions();
        if (this.watch) {
            LOGGER.info("settting scan period to '{}'", this.scanPeriodMs);
            options.setScanPeriod(this.scanPeriodMs);
        }
        if (this.filename != null && this.filename.length() != 0) {
            LOGGER.info("reading file '{}'", this.filename);

            final File file = new File(this.filename);
            ConfigStoreOptions fileStore = new ConfigStoreOptions().setType("json")
                    .setConfig(new JsonObject().put("path", file.getAbsolutePath()));

            return options.addStore(fileStore);
        }

        if (this.directory != null && this.directory.length() != 0) {
            LOGGER.info("reading directory '{}'", this.directory);

            ConfigStoreOptions dirStore = new ConfigStoreOptions().setType("directory")
                    .setConfig(new JsonObject().put("path", this.directory).put("filesets",
                            new JsonArray().add(new JsonObject().put("pattern", "*.json"))));

            return options.addStore(dirStore);
        }

        LOGGER.error("neither filename or directory defined");
        return options;
    }

    private void validateAndPublish(JsonObject config, String configurationAddress) {
        DynamicConfiguration.validate(vertx, config).onComplete(validateAr -> {
            if (validateAr.succeeded()) {
                LOGGER.info("configuration published");
                eb.publish(configurationAddress, config);
            } else {
                LOGGER.error("invalid configuration");
            }
        });
    }
}
