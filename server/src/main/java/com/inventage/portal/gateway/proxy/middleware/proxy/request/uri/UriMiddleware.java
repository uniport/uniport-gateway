package com.inventage.portal.gateway.proxy.middleware.proxy.request.uri;

/**
 * Function for manipulation the uri of the request sent to the service.
 */
public interface UriMiddleware {

    String apply(String uri);

}
