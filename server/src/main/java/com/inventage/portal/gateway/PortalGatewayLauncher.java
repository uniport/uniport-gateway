package com.inventage.portal.gateway;

import ch.qos.logback.classic.util.ContextInitializer;
import com.inventage.portal.gateway.core.PortalGatewayVerticle;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import io.vertx.core.Launcher;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.tracing.opentelemetry.OpenTelemetryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Custom Vert.x Launcher for the portal gateway.
 */
public class PortalGatewayLauncher extends Launcher {

    private static final String LOGGING_CONFIG_PROPERTY = "PORTAL_GATEWAY_LOGGING_CONFIG";
    private static final String DEFAULT_LOGGING_CONFIG_FILE_PATH = "/etc/portal-gateway";
    private static final String DEFAULT_LOGGING_CONFIG_FILE_NAME = "logback.xml";

    private static final String METRICS_PORT_CONFIG_PROPERTY = "PORTAL_GATEWAY_METRICS_PORT";
    private static final int DEFAULT_METRICS_PORT = 9090;
    private static final String METRICS_PATH_CONFIG_PROPERTY = "PORTAL_GATEWAY_METRICS_PATH";
    private static final String DEFAULT_METRICS_PATH = "/metrics";

    private static Logger logger;

    private PortalGatewayLauncher() {
    }

    /**
     * main method to start the server.
     *
     * @param args startup arguments
     */
    public static void main(String[] args) {
        // https://logback.qos.ch/manual/configuration.html#configFileProperty
        final Optional<Path> loggingConfigPath = getLoggingConfigPath();
        loggingConfigPath.ifPresent(path -> System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, path.toString()));

        // https://vertx.io/docs/vertx-core/java/#_logging
        System.setProperty("vertx.logger-delegate-factory-class-name", SLF4JLogDelegateFactory.class.getName());

        logger = LoggerFactory.getILoggerFactory().getLogger(PortalGatewayLauncher.class.getName());
        logger.info("Portal Gateway is starting....");

        if (loggingConfigPath.isPresent()) {
            logger.info("Using logback configuration file from '{}'", loggingConfigPath.get());
        }
        else {
            logger.info("No custom logback configuration file found");
        }

        // enable metrics
        System.setProperty("vertx.metrics.options.enabled", "true");
        // name of the metrics registry
        System.setProperty("vertx.metrics.options.registryName", "PortalGatewayMetrics");
        // increase timeout for worker execution to 2 min
        System.setProperty("vertx.options.maxWorkerExecuteTime", "240000000000");

        if (Runtime.isDevelopment()) {
            // increase the max event loop time to 10 min (default is 2000000000 ns = 2s) to omit
            // thread blocking warnings
            System.setProperty("vertx.options.maxEventLoopExecuteTime", "600000000000");
        }
        final String[] arguments = new String[]{"run", PortalGatewayVerticle.class.getName(), "--instances",
                Runtime.numberOfVerticleInstances()};
        new PortalGatewayLauncher().dispatch(arguments);
        logger.info("PortalGatewayLauncher started.");
    }

    /**
     * The logback.xml for the logback configuration is taken from one of these places:
     * 1. File pointed to by the env variable 'PORTAL_GATEWAY_LOGGING_CONFIG'
     * 2. File pointed to by the system property 'PORTAL_GATEWAY_LOGGING_CONFIG'
     * 3. File 'logback.xml' in '/etc/portal-gateway/logback.xml'
     * 4. Normal configuration discovery process as configured by logback
     */
    private static Optional<Path> getLoggingConfigPath() {
        // take path from env var
        String loggingConfigFileName = System.getenv(LOGGING_CONFIG_PROPERTY);
        if (existsAsFile(loggingConfigFileName)) {
            return Optional.of(Path.of(loggingConfigFileName));
        }

        loggingConfigFileName = System.getProperty(LOGGING_CONFIG_PROPERTY);
        if (existsAsFile(loggingConfigFileName)) {
            return Optional.of(Path.of(loggingConfigFileName));
        }

        // take path from the default path
        loggingConfigFileName = String.format("%s/%s", DEFAULT_LOGGING_CONFIG_FILE_PATH,
                DEFAULT_LOGGING_CONFIG_FILE_NAME);
        if (existsAsFile(loggingConfigFileName)) {
            return Optional.of(Path.of(loggingConfigFileName));
        }

        return Optional.empty();
    }

    private static boolean existsAsFile(String fileName) {
        return fileName != null && new File(fileName).exists();
    }

    @Override
    public void beforeStartingVertx(VertxOptions options) {
        logger.info("Before starting Vertx");
        options.setTracingOptions(new OpenTelemetryOptions(configureOpenTelemetry()));
        options.setMetricsOptions(new MicrometerMetricsOptions()
                .setPrometheusOptions(configurePrometheus())
                .setEnabled(true));
    }

    @Override
    public void afterStartingVertx(Vertx vertx) {
        logger.info("After starting Vertx");
        bindJVMMetrics();
    }

    private OpenTelemetry configureOpenTelemetry() {
        return OpenTelemetrySdkAutoConfiguration.initialize();
    }

    private VertxPrometheusOptions configurePrometheus() {
        int metricsPort = DEFAULT_METRICS_PORT;
        final String metricsPortStr = System.getenv(METRICS_PORT_CONFIG_PROPERTY);
        if (metricsPortStr != null) {
            try {
                metricsPort = Integer.parseInt(metricsPortStr);
            }
            catch (NumberFormatException ignored) {
                // Noop…
            }
        }

        final String metricsPath = System.getenv().getOrDefault(METRICS_PATH_CONFIG_PROPERTY, DEFAULT_METRICS_PATH);

        logger.info("Configuring prometheus endpoint on port '{}' on path '{}'", metricsPort, metricsPath);
        return new VertxPrometheusOptions()
                .setEnabled(true)
                .setStartEmbeddedServer(true)
                .setEmbeddedServerOptions(new HttpServerOptions().setPort(metricsPort))
                .setEmbeddedServerEndpoint(metricsPath);
    }

    private void bindJVMMetrics() {
        final MeterRegistry registry = BackendRegistries.getDefaultNow();
        new JvmMemoryMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
    }
}
