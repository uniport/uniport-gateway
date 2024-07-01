package com.inventage.portal.gateway.proxy.middleware.proxy;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class ProxyMiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyMiddlewareFactory.class);

    public static final String DEFAULT_SERVER_PROTOCOL = "http";
    public static final boolean DEFAULT_HTTPS_TRUST_ALL = true;
    public static final boolean DEFAULT_HTTPS_VERIFY_HOSTNAME = false;
    public static final String DEFAULT_HTTPS_TRUST_STORE_PATH = null;
    public static final String DEFAULT_HTTPS_TRUST_STORE_PASSWORD = null;

    private static final String MIDDLEWARE_PROXY = "proxy";

    @Override
    public String provides() {
        return MIDDLEWARE_PROXY;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject serviceConfig) {
        LOGGER.debug("Created '{}' middleware successfully", MIDDLEWARE_PROXY);

        final String serverProtocol = serviceConfig.getString(DynamicConfiguration.SERVICE_SERVER_PROTOCOL, DEFAULT_SERVER_PROTOCOL);
        Boolean trustAll = null;
        Boolean verifyHost = null;
        String storePath = null;
        String storePassword = null;

        final JsonObject httpsOptions = serviceConfig.getJsonObject(DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS);
        if (httpsOptions != null) {
            trustAll = httpsOptions.getBoolean(
                DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_ALL,
                DEFAULT_HTTPS_TRUST_ALL);
            verifyHost = httpsOptions.getBoolean(
                DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS_VERIFY_HOSTNAME,
                DEFAULT_HTTPS_VERIFY_HOSTNAME);
            storePath = httpsOptions.getString(
                DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PATH,
                DEFAULT_HTTPS_TRUST_STORE_PATH);
            storePassword = httpsOptions.getString(
                DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PASSWORD,
                DEFAULT_HTTPS_TRUST_STORE_PASSWORD);
        }

        return Future.succeededFuture(
            new ProxyMiddleware(vertx,
                name,
                serviceConfig.getString(DynamicConfiguration.SERVICE_SERVER_HOST),
                serviceConfig.getInteger(DynamicConfiguration.SERVICE_SERVER_PORT),
                serverProtocol,
                trustAll,
                verifyHost,
                storePath,
                storePassword));
    }
}
