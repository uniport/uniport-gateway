package com.inventage.portal.gateway.proxy.middleware.debug;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.redirectRegex.RedirectRegexMiddleware;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Returns an HTML page with information from the current session if "_session_" is in the requested URL.
 */
public class ShowSessionContentMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShowSessionContentMiddleware.class);

    @Override
    public void handle(RoutingContext ctx) {
        if (ctx.request().absoluteURI().contains(DynamicConfiguration.MIDDLEWARE_SHOW_SESSION_CONTENT)) {
            LOGGER.info("handle: url '{}'", ctx.request().absoluteURI());
            String oldURI = ctx.request().uri();

            ctx.end(getHtml(ctx));
        }
        else {
            LOGGER.info("handle: ignoring url '{}'", ctx.request().absoluteURI());
            ctx.next();
        }
    }

    private String getHtml(RoutingContext ctx) {
        final StringBuffer html = new StringBuffer();

        String idToken = ctx.session().get(OAuth2MiddlewareFactory.ID_TOKEN);
        if (idToken != null) {
            html.append("id token:\n");
            html.append(idToken);
        }

        final Map<String, Object> data = ctx.session().data();
        data.keySet().stream()
                .filter(key -> key.endsWith("_access_token"))
                .map(key -> data.get(key))
                .forEach(value -> html.append("\n\n").append("access token:\n").append(value));

        return html.toString();
    }

}
