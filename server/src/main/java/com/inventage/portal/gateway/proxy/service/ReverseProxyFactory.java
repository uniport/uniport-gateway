package com.inventage.portal.gateway.proxy.service;

import com.inventage.portal.gateway.proxy.model.GatewayService;
import com.inventage.portal.gateway.proxy.model.HTTPsOptions;
import com.inventage.portal.gateway.proxy.model.ServerOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link ReverseProxy}.
 */
public class ReverseProxyFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReverseProxyFactory.class);

    public static Future<Handler<RoutingContext>> of(Vertx vertx, String name, GatewayService options) {
        return new ReverseProxyFactory().create(vertx, name, options);
    }

    public Future<Handler<RoutingContext>> create(Vertx vertx, String name, GatewayService options) {
        final ServerOptions serverConfig = options.getServers().get(0); // TODO support multiple servers
        final HTTPsOptions httpsOptions = serverConfig.getHTTPs();
        if (httpsOptions == null) {
            return Future.failedFuture(
                new IllegalStateException("expected https options to be non-empty"));
        }

        LOGGER.debug("Created proxy '{}' successfully", name);
        return Future.succeededFuture(
            new ReverseProxy(vertx,
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
