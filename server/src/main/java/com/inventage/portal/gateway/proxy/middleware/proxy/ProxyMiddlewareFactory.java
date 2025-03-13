package com.inventage.portal.gateway.proxy.middleware.proxy;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayService;
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
    public static final String NAME = "name";
    public static final String SERVERS = "servers";
    public static final String SERVER_PROTOCOL = "protocol";
    public static final String SERVER_HOST = "host";
    public static final String SERVER_PORT = "port";
    public static final String SERVER_HTTPS_OPTIONS = "httpsOptions";
    public static final String VERIFY_HOSTNAME = "verifyHostname";
    public static final String TRUST_ALL = "trustAll";
    public static final String TRUST_STORE_PATH = "trustStorePath";
    public static final String TRUST_STORE_PASSWORD = "trustStorePassword";
    public static final String VERBOSE = "verbose";

    // defaults
    public static final String DEFAULT_SERVER_PROTOCOL = "http";
    public static final boolean DEFAULT_TRUST_ALL = true;
    public static final boolean DEFAULT_VERIFY_HOSTNAME = false;
    public static final String DEFAULT_TRUST_STORE_PATH = null;
    public static final String DEFAULT_TRUST_STORE_PASSWORD = null;
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
    public Class<GatewayService> modelType() {
        return GatewayService.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, GatewayMiddlewareOptions config) {
        final GatewayService options = castOptions(config, modelType());
        final ServerOptions serverConfig = options.getServers().get(0); // TODO support multiple servers
        final HTTPsOptions httpsOptions = serverConfig.getHTTPs();
        if (httpsOptions == null) {
            return Future.failedFuture(
                new IllegalStateException("expected https options to be non-empty"));
        }

        LOGGER.debug("Created '{}' middleware successfully", PROXY);
        return Future.succeededFuture(
            new ProxyMiddleware(vertx,
                name,
                serverConfig.getHost(),
                serverConfig.getPort(),
                serverConfig.getProtocol(),
                httpsOptions.trustAll(),
                httpsOptions.verifyHostname(),
                httpsOptions.getTrustStorePath(),
                httpsOptions.getTrustStorePassword(),
                options.isVerbose()));
    }
}
