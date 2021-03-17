package com.inventage.portal.gateway.proxy;

import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

public interface RoutingRule {

    public Route apply(Router router);

}
