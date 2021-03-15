package com.inventage.portal.gateway.proxy.request.header;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Function for manipulating the headers of the request before sent to the service.
 */

public interface RequestHeaderMiddleware<RoutingContext, MultiMap> extends
        BiFunction</* input1 */ RoutingContext, /* input2 */ MultiMap, /* return */ MultiMap> {

    RequestHeaderMiddleware IDENTITY = new RequestHeaderIdentity();

    // make chaining BiFunctions possible
    // (https://stackoverflow.com/questions/26679628/how-to-chain-bifunctions)
    default RequestHeaderMiddleware<RoutingContext, MultiMap> andThen(
            RequestHeaderMiddleware<RoutingContext, MultiMap> after) {
        Objects.requireNonNull(after);
        return (RoutingContext t, MultiMap u) -> after.apply(t, apply(t, u));
    }

}
