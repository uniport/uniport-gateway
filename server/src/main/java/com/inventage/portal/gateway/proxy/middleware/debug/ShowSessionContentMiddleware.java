package com.inventage.portal.gateway.proxy.middleware.debug;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.sessionBag.SessionBagMiddleware;
import io.netty.handler.codec.http.cookie.Cookie;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Set;

/**
 * Returns an HTML page with information from the current session if "_session_" is in the requested URL.
 */
public class ShowSessionContentMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShowSessionContentMiddleware.class);

    @Override
    public void handle(RoutingContext ctx) {
        // Bail if we're not on the debug URL
        if (!ctx.request().absoluteURI().contains(DynamicConfiguration.MIDDLEWARE_SHOW_SESSION_CONTENT)) {
            ctx.next();
            return;
        }

        LOGGER.info("Handling URL '{}'", ctx.request().absoluteURI());
        ctx.end(getHtml(ctx.session()));
    }

    // TODO: usage of vert.x templating for HTML generation
    private String getHtml(Session session) {
        final StringBuilder html = new StringBuilder();

        html.append("session ID:\n").append(session.id());
        html.append("\n");
        html.append("session last access:\n").append(session.lastAccessed()); // https://www.epochconverter.com/?q=ms
        html.append("\n\n");

        final Set<Cookie> storedCookies = session.get(SessionBagMiddleware.SESSION_BAG_COOKIES);
        if (storedCookies != null) {
            if (!storedCookies.isEmpty()) {
                html.append("cookies stored in session bag (each block is one cookie):\n\n");
            }
            for (Cookie cookie : storedCookies) {
                html.append(String.join("\n", cookie.toString().replace(", ", "\n")));
                html.append("\n\n");
            }
        }

        boolean idTokenDisplayed = false;
        for (String key : session.data().keySet()) {
            LOGGER.debug("Processing {}: {}", key, session.data().get(key));
            if (!key.endsWith(OAuth2MiddlewareFactory.SESSION_SCOPE_SUFFIX)) {
                continue;
            }

            Pair<OAuth2Auth, User> authPair = (Pair<OAuth2Auth, User>) session.data().get(key);
            User user = authPair.getRight();
            if (!idTokenDisplayed) {
                String rawIdToken = user.principal().getString("id_token");
                html.append("id token:\n").append(decodeJWT(rawIdToken)).append("\n").append(rawIdToken);
                idTokenDisplayed = true;
            }

            String rawAccessToken = user.principal().getString("access_token");
            html.append("\n\n").append(key).append(":\n").append(decodeJWT(rawAccessToken)).append("\n")
                    .append(rawAccessToken);
        }

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
