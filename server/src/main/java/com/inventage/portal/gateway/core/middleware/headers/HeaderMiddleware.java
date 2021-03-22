package com.inventage.portal.gateway.core.middleware.headers;

import java.util.Map;
import com.inventage.portal.gateway.core.middleware.Middleware;
import io.vertx.ext.web.RoutingContext;

public class HeaderMiddleware implements Middleware {

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
