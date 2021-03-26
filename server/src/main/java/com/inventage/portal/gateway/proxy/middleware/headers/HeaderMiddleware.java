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
        LOGGER.trace("construcutor");
        this.requestHeaders = requestHeaders;
        this.responseHeaders = responseHeaders;
    }

    @Override
    public void handle(RoutingContext ctx) {
        LOGGER.trace("handle");
        for (String header : this.requestHeaders.keySet()) {
            String value = this.requestHeaders.get(header);
            switch (value) {
                case "": {
                    ctx.request().headers().remove(header);
                }
                default: {
                    ctx.request().headers().set(header, value);
                }
            }
        }

        for (String header : this.responseHeaders.keySet()) {
            String value = this.responseHeaders.get(header);
            switch (value) {
                case "": {
                    ctx.response().headers().remove(header);
                }
                default: {
                    ctx.response().headers().set(header, value);
                }
            }
        }

        ctx.next();
    }
}
