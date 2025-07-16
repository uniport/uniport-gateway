package ch.uniport.gateway.core.entrypoint;

import ch.uniport.gateway.GatewayRouterInternal;
import ch.uniport.gateway.Runtime;
import ch.uniport.gateway.proxy.config.model.MiddlewareModel;
import ch.uniport.gateway.proxy.config.model.MiddlewareOptionsModel;
import ch.uniport.gateway.proxy.middleware.Middleware;
import ch.uniport.gateway.proxy.middleware.MiddlewareFactory;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the uniport-gateway.
 */
public class Entrypoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(Entrypoint.class);

    private final Vertx vertx;

    private final String name;
    private final int port;
    private final List<MiddlewareModel> middlewares;

    private GatewayRouterInternal router;
    private Tls tls;

    public Entrypoint(Vertx vertx, String name, int port, List<MiddlewareModel> entryMiddlewares) {
        this.vertx = vertx;
        this.name = name;
        this.port = port;
        this.middlewares = entryMiddlewares;
    }

    public String name() {
        return name;
    }

    public int port() {
        return port;
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

        final Optional<MiddlewareFactory> middlewareFactory = MiddlewareFactory.Loader.getFactory(middlewareType);
        if (middlewareFactory.isEmpty()) {
            final String errMsg = String.format("Unknown middleware '%s'", middlewareType);
            LOGGER.error(errMsg);
            return Future.failedFuture(errMsg);
        }

        final String middlewareName = middlewareConfig.getName();
        return middlewareFactory.get()
            .create(vertx, middlewareName, router, middlewareOptions);
    }

    public boolean isTls() {
        return tls != null;
    }

    public void setJksOptions(JksOptions jksOptions) {
        tls = new Tls(jksOptions);
    }

    public JksOptions jksOptions() {
        if (isTls()) {
            return tls.jksOptions();
        }
        return null;
    }

    static class Tls {
        private JksOptions jksOptions;

        Tls(JksOptions jksOptions) {
            this.jksOptions = jksOptions;
        }

        public JksOptions jksOptions() {
            return jksOptions;
        }
    }

}
