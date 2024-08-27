package com.inventage.portal.gateway.proxy.middleware.authorization;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.AuthenticationHandler;
import java.util.List;

/**
 * 
 */
public interface JWKAccessibleAuthHandler extends AuthenticationHandler {
    /**
     * Get the JSON web keys.
     *
     * see https://openid.net/specs/draft-jones-json-web-key-03.html
     * 
     * @return a list of JSON web keys as JSON data structure
     */
    List<JsonObject> getJwks();
}
