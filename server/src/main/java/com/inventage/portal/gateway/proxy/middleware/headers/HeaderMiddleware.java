package com.inventage.portal.gateway.proxy.middleware.headers;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map.Entry;

// -- request headers --> HeaderMiddleware -- updated request headers -->
// <-- updated response headers -- HeaderMiddleware <-- response headers --

/**
 * Manages request/response headers. It can add/remove headers on both requests and responses.
 */
public class HeaderMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderMiddleware.class);

    private final String name;
    private final MultiMap requestHeaders;
    private final MultiMap responseHeaders;

    public HeaderMiddleware(String name, MultiMap requestHeaders, MultiMap responseHeaders) {
        this.name = name;
        this.requestHeaders = requestHeaders;
        this.responseHeaders = responseHeaders;
    }

    @Override
    public void handle(RoutingContext ctx) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        for (Entry<String, String> header : this.requestHeaders.entries()) {
            if (header.getValue().equals("")) {
                LOGGER.debug("Removing request header '{}'", header.getKey());
                ctx.request().headers().remove(header.getKey());
            }
            else {
                LOGGER.debug("Setting request header '{}:{}'", header.getKey(), header.getValue());
                ctx.request().headers().add(header.getKey(), header.getValue());
            }
        }

        final Handler<MultiMap> respHeadersModifier = headers -> {
            for (Entry<String, String> header : this.responseHeaders.entries()) {
                if (header.getValue().equals("")) {
                    if (headers.contains(header.getKey())) {
                        LOGGER.debug("Removing response header '{}'", header.getKey());
                        headers.remove(header.getKey());
                    }
                }
                else {
                    final List<String> hs = headers.getAll(header.getKey());
                    if (hs == null || !hs.contains(header.getValue())) {
                        LOGGER.debug("Setting response header '{}:{}'", header.getKey(),
                                header.getValue());
                        headers.add(header.getKey(), header.getValue());
                    }
                }
            }
        };
        this.addModifier(ctx, respHeadersModifier, Middleware.RESPONSE_HEADERS_MODIFIERS);

        ctx.next();
    }

}
