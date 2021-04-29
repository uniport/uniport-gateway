package com.inventage.portal.gateway.proxy.middleware.debug;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.sessionBag.SessionBagMiddleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Returns an HTML page with information from the current session if "_session_" is in the requested URL.
 */
public class ShowSessionContentMiddleware implements Middleware {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ShowSessionContentMiddleware.class);

    @Override
    public void handle(RoutingContext ctx) {
        if (ctx.request().absoluteURI()
                .contains(DynamicConfiguration.MIDDLEWARE_SHOW_SESSION_CONTENT)) {
            LOGGER.info("handle: url '{}'", ctx.request().absoluteURI());
            ctx.end(getHtml(ctx));
        } else {
            LOGGER.info("handle: ignoring url '{}'", ctx.request().absoluteURI());
            ctx.next();
        }
    }

    // TODO: usage of vert.x templating for HTML generation
    private String getHtml(RoutingContext ctx) {
        final StringBuffer html = new StringBuffer();

        html.append("vertx-web.session=").append(ctx.session().id());
        html.append("\n\n");

        List<String> storedCookies = ctx.session().get(SessionBagMiddleware.SESSION_BAG_COOKIES);
        if (storedCookies != null) {
            if (!storedCookies.isEmpty()) {
                html.append("cookies stored in session bag (each block is one cookie):\n");
            }
            for (String cookie : storedCookies) {
                html.append(String.join("\n", cookie.toString().split("; ")));
                html.append("\n\n");
            }
        }

        String idToken = ctx.session().get(OAuth2MiddlewareFactory.ID_TOKEN);
        if (idToken != null) {
            html.append("id token:\n");
            html.append(decodeJWT(idToken));
        }

        final Map<String, Object> data = ctx.session().data();
        data.keySet().stream().filter(key -> key.endsWith("_access_token"))
                .peek(key -> html.append("\n\n").append(key).append(":\n"))
                .map(key -> data.get(key)).forEach(value -> html.append(decodeJWT((String) value)));

        return html.toString();
    }

    private String decodeJWT(String jwt) {
        String[] chunks = jwt.split("\\.");
        Base64.Decoder decoder = Base64.getDecoder();
        String header = new String(decoder.decode(chunks[0]));
        String payload = new String(decoder.decode(chunks[1]));
        return new JsonObject(payload).encodePrettily();
    }


}
