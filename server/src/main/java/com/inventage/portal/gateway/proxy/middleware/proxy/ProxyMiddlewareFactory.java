package com.inventage.portal.gateway.proxy.middleware.proxy;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link ProxyMiddleware}.
 * This a special case middleware, as it implements the "services" specified in a configuration.
 * It is not allowed to appear in the "middlewares", hence {@link ProxyMiddlewareFactory#validate(JsonObject)}
 * always returns a failed {@code Future}.
 */
public class ProxyMiddlewareFactory implements MiddlewareFactory {

    // schema
    public static final String SERVICE_SERVERS = "servers";
    public static final String SERVICE_SERVER_PROTOCOL = "protocol";
    public static final String SERVICE_SERVER_HTTPS_OPTIONS = "httpsOptions";
    public static final String SERVICE_SERVER_HTTPS_OPTIONS_VERIFY_HOSTNAME = "verifyHostname";
    public static final String SERVICE_SERVER_HTTPS_OPTIONS_TRUST_ALL = "trustAll";
    public static final String SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PATH = "trustStorePath";
    public static final String SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PASSWORD = "trustStorePassword";
    public static final String SERVICE_SERVER_HOST = "host";
    public static final String SERVICE_SERVER_PORT = "port";
    public static final String SERVICE_VERBOSE = "verbose";

    // defaults
    public static final String DEFAULT_SERVER_PROTOCOL = "http";
    public static final boolean DEFAULT_HTTPS_TRUST_ALL = true;
    public static final boolean DEFAULT_HTTPS_VERIFY_HOSTNAME = false;
    public static final String DEFAULT_HTTPS_TRUST_STORE_PATH = null;
    public static final String DEFAULT_HTTPS_TRUST_STORE_PASSWORD = null;
    public static final boolean DEFAULT_VERBOSE = false;

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyMiddlewareFactory.class);

    private static final String PROXY = "proxy";

    @Override
    public String provides() {
        return PROXY;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return Future.failedFuture("configuration is not allowed");
    }

    @Override
    public Class<? extends GatewayMiddlewareOptions> modelType() {
        return ProxyMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject serverConfig) {
        LOGGER.debug("Created '{}' middleware successfully", PROXY);

        final String serverProtocol = serverConfig.getString(SERVICE_SERVER_PROTOCOL, DEFAULT_SERVER_PROTOCOL);
        final String serverHost = serverConfig.getString(SERVICE_SERVER_HOST);
        final Integer serverPort = serverConfig.getInteger(SERVICE_SERVER_PORT);

        Boolean trustAll = DEFAULT_HTTPS_TRUST_ALL;
        Boolean verifyHost = DEFAULT_HTTPS_VERIFY_HOSTNAME;
        String storePath = DEFAULT_HTTPS_TRUST_STORE_PATH;
        String storePassword = DEFAULT_HTTPS_TRUST_STORE_PASSWORD;

        final JsonObject httpsOptions = serverConfig.getJsonObject(SERVICE_SERVER_HTTPS_OPTIONS);
        if (httpsOptions != null) {
            trustAll = httpsOptions.getBoolean(SERVICE_SERVER_HTTPS_OPTIONS_TRUST_ALL, DEFAULT_HTTPS_TRUST_ALL);
            verifyHost = httpsOptions.getBoolean(SERVICE_SERVER_HTTPS_OPTIONS_VERIFY_HOSTNAME, DEFAULT_HTTPS_VERIFY_HOSTNAME);
            storePath = httpsOptions.getString(SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PATH, DEFAULT_HTTPS_TRUST_STORE_PATH);
            storePassword = httpsOptions.getString(SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PASSWORD, DEFAULT_HTTPS_TRUST_STORE_PASSWORD);
        }

        final boolean verbose = serverConfig.getBoolean(SERVICE_VERBOSE, DEFAULT_VERBOSE);

        return Future.succeededFuture(
            new ProxyMiddleware(vertx,
                name,
                serverHost,
                serverPort,
                serverProtocol,
                trustAll,
                verifyHost,
                storePath,
                storePassword,
                verbose));
    }
}
