package com.inventage.portal.gateway.proxy.middleware.debug;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.oauth2.AuthenticationUserContext;
import com.inventage.portal.gateway.proxy.middleware.sessionBag.SessionBagMiddleware;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Returns an HTML page with information from the current session if "_session_"
 * is in the requested URL.
 */
public class ShowSessionContentMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShowSessionContentMiddleware.class);

    private final String name;
    private final String instanceName;

    private static final String INSTANCE_NAME_PROPERTY = "PORTAL_GATEWAY_INSTANCE_NAME";
    private static final String DEFAULT_INSTANCE_NAME = "unknown";

    public ShowSessionContentMiddleware(String name) {
        this.name = name;
        this.instanceName = System.getenv().getOrDefault(INSTANCE_NAME_PROPERTY, DEFAULT_INSTANCE_NAME);
    }

    @Override
    public void handle(RoutingContext ctx) {
        // Bail if we're not on the debug URL
        if (!ctx.request().absoluteURI().contains(DynamicConfiguration.MIDDLEWARE_SHOW_SESSION_CONTENT)) {
            ctx.next();
            return;
        }

        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());
        ctx.end(getHtml(ctx.session()));
    }

    // TODO: usage of vert.x templating for HTML generation
    private String getHtml(Session session) {
        final StringBuilder html = new StringBuilder();

        html.append("instance:\n").append(this.instanceName);
        html.append("\n");
        html.append("session ID:\n").append(session.id());
        html.append("\n");
        html.append("session last access (seconds since epoch:\n").append(session.lastAccessed() / 1000); // https://www.epochconverter.com/?q=ms
        html.append("\n\n");

        final Set<Cookie> storedCookies = session.get(SessionBagMiddleware.SESSION_BAG_COOKIES);
        if (storedCookies != null) {
            if (!storedCookies.isEmpty()) {
                html.append("cookies stored in session bag (each block is one cookie):\n\n");
            }
            for (Cookie cookie : storedCookies) {
                html.append(String.join("\n", getDisplayString(cookie).replace(", ", "\n")));
                html.append("\n\n");
            }
        }

        boolean idTokenDisplayed = false;
        for (AuthenticationUserContext authContext : AuthenticationUserContext.all(session)) {
            if (!idTokenDisplayed) {
                final String rawIdToken = authContext.getIdToken();
                html.append("id token:\n").append(getFormattedPayloadFromJWT(rawIdToken)).append("\n")
                    .append(rawIdToken);
                idTokenDisplayed = true;
            }

            final String rawAccessToken = authContext.getAccessToken();
            html.append("\n\n")
                .append(authContext.getSessionScope())
                .append(":\n")
                .append(getFormattedPayloadFromJWT(rawAccessToken))
                .append("\n")
                .append(rawAccessToken);
        }

        return html.toString();
    }

    private String getFormattedPayloadFromJWT(String jwt) {
        final String[] chunks = jwt.split("\\.");
        final Base64.Decoder decoder = Base64.getDecoder();
        // header: chunks[0], signature: chunks[2]
        final String payload = new String(decoder.decode(chunks[1]), StandardCharsets.UTF_8);

        return new JsonObject(payload).encodePrettily();
    }

    // Set-Cookie:
    //uniport.session=abc; Path=/; HTTPOnly; SameSite=Strict
    private String getDisplayString(Cookie cookie) {
        final StringBuilder display = new StringBuilder();
        display.append(cookie.getName());
        display.append("; Domain=");
        display.append(cookie.getDomain());
        display.append("; Path=");
        display.append(cookie.getPath());
        if (cookie.isHttpOnly()) {
            display.append("; HttpOnly");
        }
        return display.toString();
    }
}
