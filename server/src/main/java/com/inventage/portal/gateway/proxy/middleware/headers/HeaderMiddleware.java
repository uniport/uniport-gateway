package com.inventage.portal.gateway.proxy.middleware.headers;

import java.util.Map;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

// -- request headers --> HeaderMiddleware -- updated request headers -->
// <-- updated response headers -- HeaderMiddleware <-- response headers --
/**
 * Manages request/response headers. It can add/remove headers on both requests and responses.
 */
public class HeaderMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderMiddleware.class);

    private Map<String, String> requestHeaders;
    private Map<String, String> responseHeaders;

    public HeaderMiddleware(Map<String, String> requestHeaders,
            Map<String, String> responseHeaders) {
        this.requestHeaders = requestHeaders;
        this.responseHeaders = responseHeaders;
    }

    @Override
    public void handle(RoutingContext ctx) {
        for (String header : this.requestHeaders.keySet()) {
            String value = this.requestHeaders.get(header);
            switch (value) {
                case "": {
                    LOGGER.debug("handler: removing request header '{}'", header);
                    ctx.request().headers().remove(header);
                    break;
                }
                default: {
                    LOGGER.debug("handler: setting request header '{}:{}'", header, value);
                    ctx.request().headers().set(header, value);
                }
            }
        }

        for (String header : this.responseHeaders.keySet()) {
            String value = this.responseHeaders.get(header);
            switch (value) {
                case "": {
                    LOGGER.debug("handler: removing response header '{}'", header);
                    ctx.response().headers().remove(header);
                    break;
                }
                default: {
                    LOGGER.debug("handler: setting response header '{}:{}'", header, value);
                    ctx.response().headers().set(header, value);
                }
            }
        }

        ctx.next();
    }
}
