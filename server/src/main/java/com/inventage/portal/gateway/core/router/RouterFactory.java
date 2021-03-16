package com.inventage.portal.gateway.core.router;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class RouterFactory {
    // router

    Vertx vertx;

    public RouterFactory(Vertx vertx) {
        this.vertx = vertx;
    }

    public Router createRouter(JsonObject dynamicConfig) {
        Router router = Router.router(this.vertx);

        // TODO
        //
        // route for each pathprefix/host (path/virtualhost)
        // each middleware is a handler
        // each route has at least the proxy handler
        //
        // dont forget default route (404) (failureHandler)

        return router;
    }
}
