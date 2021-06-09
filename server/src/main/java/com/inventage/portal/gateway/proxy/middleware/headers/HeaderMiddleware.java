package com.inventage.portal.gateway.proxy.middleware.headers;

import java.util.List;
import java.util.Map.Entry;

import com.inventage.portal.gateway.proxy.middleware.Middleware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

// -- request headers --> HeaderMiddleware -- updated request headers -->
// <-- updated response headers -- HeaderMiddleware <-- response headers --
/**
 * Manages request/response headers. It can add/remove headers on both requests and responses.
 */
public class HeaderMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderMiddleware.class);

    private MultiMap requestHeaders;
    private MultiMap responseHeaders;

    public HeaderMiddleware(MultiMap requestHeaders, MultiMap responseHeaders) {
        this.requestHeaders = requestHeaders;
        this.responseHeaders = responseHeaders;
    }

    @Override
    public void handle(RoutingContext ctx) {
        for (Entry<String, String> header : this.requestHeaders.entries()) {
            switch (header.getValue()) {
                case "": {
                    LOGGER.debug("handler: removing request header '{}'", header.getKey());
                    ctx.request().headers().remove(header.getKey());
                    break;
                }
                default: {
                    LOGGER.debug("handler: setting request header '{}:{}'", header.getKey(), header.getValue());
                    ctx.request().headers().add(header.getKey(), header.getValue());
                }
            }
        }

        Handler<MultiMap> respHeadersModifier = headers -> {
            for (Entry<String, String> header : this.responseHeaders.entries()) {
                switch (header.getValue()) {
                    case "": {
                        if (headers.contains(header.getKey())) {
                            LOGGER.debug("handler: removing response header '{}'", header.getKey());
                            headers.remove(header.getKey());
                        }
                        break;
                    }
                    default: {
                        List<String> hs = headers.getAll(header.getKey());
                        if (hs == null || !hs.contains(header.getValue())) {
                            LOGGER.debug("handler: setting response header '{}:{}'", header.getKey(),
                                    header.getValue());
                            headers.add(header.getKey(), header.getValue());
                        }
                    }
                }
            }
        };
        this.addModifier(ctx, respHeadersModifier, Middleware.RESPONSE_HEADERS_MODIFIERS);

        ctx.next();
    }

}
