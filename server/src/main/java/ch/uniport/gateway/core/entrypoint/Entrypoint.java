package ch.uniport.gateway.core.entrypoint;

import ch.uniport.gateway.GatewayRouterInternal;
import ch.uniport.gateway.Runtime;
import ch.uniport.gateway.core.config.model.TlsModel;
import ch.uniport.gateway.proxy.config.model.MiddlewareModel;
import ch.uniport.gateway.proxy.config.model.MiddlewareOptionsModel;
import ch.uniport.gateway.proxy.middleware.Middleware;
import ch.uniport.gateway.proxy.middleware.MiddlewareFactory;
import ch.uniport.gateway.proxy.middleware.MiddlewareFactoryLoader;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the uniport-gateway.
 */
public final class Entrypoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(Entrypoint.class);

    /**
     * Default maximum length of all headers for HTTP/1.x in bytes = {@code 10240},
     * i.e. 10 kilobytes
     */
    private static final int DEFAULT_HEADER_LIMIT = 10 * 1024;

    private final Vertx vertx;

    private final String name;
    private final int port;
    private final List<MiddlewareModel> middlewares;

    private GatewayRouterInternal router;
    private Tls tls;

    public Entrypoint(Vertx vertx, String name, int port, TlsModel tls, List<MiddlewareModel> entryMiddlewares) {
        Objects.requireNonNull(vertx, "vertx must not be null");
        Objects.requireNonNull(name, "name must not be null");
        this.vertx = vertx;
        this.name = name;
        this.port = port;
        this.middlewares = entryMiddlewares;

        if (tls != null) {
            this.tls = new Tls(
                new PemKeyCertOptions()
                    .setKeyPath(tls.getKeyFile())
                    .setCertPath(tls.getCertFile()));
        }
    }

    public Future<HttpServer> listen() {
        final HttpServerOptions options = new HttpServerOptions()
            .setMaxHeaderSize(DEFAULT_HEADER_LIMIT)
            .setUseAlpn(true)
            .setSsl(isTls())
            .setKeyCertOptions(keyCertOptions());

        LOGGER.info("Listening on entrypoint '{}' at port '{}'", name, port);
        return vertx
            .createHttpServer(options)
            .requestHandler(router())
            .listen(port);
    }

    public GatewayRouterInternal router() {
        if (router != null) {
            return router;
        }
        router = GatewayRouterInternal.router(vertx, String.format("entrypoint %s", name));
        if (middlewares != null) {
            LOGGER.info("Setup EntryMiddlewares");
            createAndMountMiddlewares(middlewares, router)
                .onSuccess(v -> LOGGER.info("EntryMiddlewares created successfully"))
                .onFailure(err -> Runtime.fatal(vertx, err.getMessage()));
        }
        return router;
    }

    private Future<Void> createAndMountMiddlewares(List<MiddlewareModel> entryMiddlewares, Router router) {
        final List<Future<Middleware>> entryMiddlewaresFutures = entryMiddlewares.stream()
            .map(entryMiddleware -> createEntryMiddleware(entryMiddleware, router))
            .toList();

        return Future.all(entryMiddlewaresFutures)
            .map(cf -> mountMiddlewares(
                entryMiddlewaresFutures.stream()
                    .map(Future::result)
                    .toList(),
                router))
            .mapEmpty();
    }

    private Future<Void> mountMiddlewares(List<Middleware> entryMiddlewares, Router router) {
        for (Middleware mw : entryMiddlewares) {
            router.route()
                .setName(mw.getClass().getSimpleName())
                .handler((Handler<RoutingContext>) mw);
        }
        return Future.succeededFuture();
    }

    private Future<Middleware> createEntryMiddleware(MiddlewareModel middlewareConfig, Router router) {
        final String middlewareType = middlewareConfig.getType();
        final MiddlewareOptionsModel middlewareOptions = middlewareConfig.getOptions();

        final Optional<MiddlewareFactory> middlewareFactory = MiddlewareFactoryLoader.getFactory(middlewareType);
        if (middlewareFactory.isEmpty()) {
            final String errMsg = String.format("Unknown middleware '%s'", middlewareType);
            LOGGER.error(errMsg);
            return Future.failedFuture(errMsg);
        }

        final String middlewareName = middlewareConfig.getName();
        return middlewareFactory.get()
            .create(vertx, middlewareName, router, middlewareOptions);
    }

    private boolean isTls() {
        return tls != null;
    }

    private KeyCertOptions keyCertOptions() {
        if (isTls()) {
            return tls.keyCertOptions();
        }
        return null;
    }

    static class Tls {
        private KeyCertOptions options;

        Tls(KeyCertOptions options) {
            this.options = options;
        }

        public KeyCertOptions keyCertOptions() {
            return options;
        }
    }
}
