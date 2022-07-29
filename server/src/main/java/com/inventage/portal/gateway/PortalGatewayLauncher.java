package com.inventage.portal.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.core.PortalGatewayVerticle;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import io.vertx.core.Launcher;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.tracing.opentelemetry.OpenTelemetryOptions;

/**
 * Custom Vert.x Launcher for the portal gateway.
 */
public class PortalGatewayLauncher extends Launcher {

    private static Logger LOGGER;

    private PortalGatewayLauncher() {
    }

    /**
     * main method to start the server.
     *
     * @param args startup arguments
     */
    public static void main(String[] args) {
        // https://vertx.io/docs/vertx-core/java/#_logging
        System.setProperty("vertx.logger-delegate-factory-class-name", SLF4JLogDelegateFactory.class.getName());
        LOGGER = LoggerFactory.getILoggerFactory().getLogger(PortalGatewayLauncher.class.getName());
        LOGGER.info("Portal Gateway is starting....");

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
        final String[] arguments = new String[] { "run", PortalGatewayVerticle.class.getName(), "--instances",
                Runtime.numberOfVerticleInstances() };
        new PortalGatewayLauncher().dispatch(arguments);
        LOGGER.info("PortalGatewayLauncher started.");
    }

    @Override
    public void beforeStartingVertx(VertxOptions options) {
        LOGGER.info("Before starting Vertx");
        options.setTracingOptions(new OpenTelemetryOptions(configureOpenTelemetry()));
    }

    public OpenTelemetry configureOpenTelemetry() {
        return OpenTelemetrySdkAutoConfiguration.initialize();
    }
}
