package com.inventage.portal.gateway.proxy.middleware;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public interface Middleware extends Handler<RoutingContext> {

}
