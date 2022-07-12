package com.inventage.portal.gateway.proxy.middleware.controlapi;

import static com.inventage.portal.gateway.proxy.middleware.sessionBag.SessionBagMiddleware.SESSION_BAG_COOKIES;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;

import io.netty.handler.codec.http.cookie.Cookie;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;

/**
 * Handles control api actions provided as values from a "IPS_GW_CONTROL" cookie.
 * Supported actions:
 * - SESSION_TERMINATE: invalidates the session and calls "end_session_endpoint" on Keycloak
 */
public class ControlApiMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControlApiMiddleware.class);

    private static final String CONTROL_COOKIE_NAME = "IPS_GW_CONTROL";
    private static final String SESSION_TERMINATE_ACTION = "SESSION_TERMINATE";
    private final String action;
    private final WebClient webClient;

    public ControlApiMiddleware(final String action, final WebClient webClient) {
        this.action = action;
        this.webClient = webClient;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (ctx.session() == null) {
            LOGGER.warn("No session initialized. Skipping session termination");
            ctx.next();
            return;
        }

        // on response
        final Handler<MultiMap> respHeadersModifier = headers -> {
            final Set<Cookie> storedCookies = ctx.session().get(SESSION_BAG_COOKIES);
            if (isNotEmpty(storedCookies)) {
                // find the first control api cookie with an action equals to the configured one
                final List<Cookie> actionCookies = storedCookies.stream()
                        .filter(cookie -> Objects.equals(cookie.name(), CONTROL_COOKIE_NAME))
                        .filter(cookie -> Objects.equals(action, cookie.value()))
                        .collect(Collectors.toList());

                final Optional<Cookie> controlApiActionToExecute = actionCookies.stream().findFirst();
                if (controlApiActionToExecute.isPresent()) {
                    LOGGER.info("Provided cookie {}", controlApiActionToExecute.get());
                    handleAction(action, ctx);
                }

                // remove action cookies from session bag
                actionCookies.forEach(cookieToRemove -> {
                    if (storedCookies.remove(cookieToRemove)) {
                        LOGGER.debug("Removing handled control api cookie from SessionBag '{}'", cookieToRemove.name());
                    }
                });
            }
        };
        this.addModifier(ctx, respHeadersModifier, Middleware.RESPONSE_HEADERS_MODIFIERS);
        ctx.next();
    }

    private void handleAction(String action, RoutingContext ctx) {
        switch (action) {
            case SESSION_TERMINATE_ACTION:
                // calls OIDC end_session_endpoint given by OAuth2Auth and destroys the vertx session
                LOGGER.info("Terminate session {}", action);

                // 1. look for a key that references to any session scope in the current session. ID tokens are the same in all session scopes hence take the first one.
                // 2. get the OAuth2Auth context out of the session scope
                // 3. let compute the endSession url
                // 4. call the url
                // 5. destroy the vertx session
                ctx.session().data().keySet().stream()
                        .filter(key -> key.endsWith(OAuth2MiddlewareFactory.SESSION_SCOPE_SUFFIX))
                        .findFirst()
                        .map(key -> ctx.session().data().get(key))
                        .map(obj -> {
                            if ((obj instanceof Pair) && (((Pair<?, ?>) obj).getLeft() instanceof OAuth2Auth)) {
                                return (OAuth2Auth) ((Pair<?, ?>) obj).getLeft();
                            } else {
                                return null;
                            }
                        })
                        .map(auth -> auth.endSessionURL(ctx.user()))
                        .ifPresent(endSessionURL -> {
                            LOGGER.debug("EndSessionURL {}", endSessionURL);
                            webClient.getAbs(endSessionURL).send()
                                    .onSuccess(response -> LOGGER.info("End_session_endpoint call succeeded"))
                                    .onFailure(throwable -> LOGGER.warn("{}", throwable.getMessage()));
                        });

                // destroy gateway session anyway
                LOGGER.info("Vertx session destroyed");
                ctx.session().destroy();
                break;
            default:
                break;
        }
    }

}
