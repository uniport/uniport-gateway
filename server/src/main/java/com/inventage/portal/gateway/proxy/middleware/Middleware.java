package com.inventage.portal.gateway.proxy.middleware;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * The incoming requests can be prepared by different middlewares before being routed to its final
 * destination. Every one has to implement this interface.
 */
public interface Middleware extends Handler<RoutingContext> {

}
