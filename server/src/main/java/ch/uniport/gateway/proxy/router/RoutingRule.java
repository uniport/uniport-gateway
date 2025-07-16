package ch.uniport.gateway.proxy.router;

import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

/**
 * To define a routing rule this interface has to be implemented.
 */
public interface RoutingRule {

    Route apply(Router router);

}
