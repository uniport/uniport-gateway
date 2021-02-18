package com.inventage.portal.gateway.proxy.request.uri;

import java.util.function.UnaryOperator;

/**
 * Function for manipulation the uri of the request sent to the service.
 */
public interface UriMiddleware extends UnaryOperator<String> {

    String apply(String uri);

}
