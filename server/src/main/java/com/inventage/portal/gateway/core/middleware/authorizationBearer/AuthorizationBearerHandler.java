package com.inventage.portal.gateway.core.middleware.authorizationBearer;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public interface AuthorizationBearerHandler extends Handler<RoutingContext> {
    static AuthorizationBearerHandler create() {
        return new AuthorizationBearerHandlerImpl();
    }
}
