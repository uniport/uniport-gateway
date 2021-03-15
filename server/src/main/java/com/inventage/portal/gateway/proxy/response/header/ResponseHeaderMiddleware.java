package com.inventage.portal.gateway.proxy.response.header;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Function for manipulating the headers of the response before sent back to the caller.
 */

public interface ResponseHeaderMiddleware<RoutingContext, MultiMap> extends
        BiFunction</* input1 */ RoutingContext, /* input2 */ MultiMap, /* return */ MultiMap> {

    ResponseHeaderMiddleware IDENTITY = new ResponseHeaderIdentity();

    // make chaining BiFunctions possible
    // (https://stackoverflow.com/questions/26679628/how-to-chain-bifunctions)
    default ResponseHeaderMiddleware<RoutingContext, MultiMap> andThen(
            ResponseHeaderMiddleware<RoutingContext, MultiMap> after) {
        Objects.requireNonNull(after);
        return (RoutingContext t, MultiMap u) -> after.apply(t, apply(t, u));
    }

}
