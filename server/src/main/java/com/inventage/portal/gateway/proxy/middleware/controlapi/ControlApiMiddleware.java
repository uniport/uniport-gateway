package com.inventage.portal.gateway.proxy.middleware.controlapi;

import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;
import io.netty.handler.codec.http.cookie.Cookie;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.inventage.portal.gateway.proxy.middleware.sessionBag.SessionBagMiddleware.SESSION_BAG_COOKIES;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

/**
 * Handles control api actions provided as values from a "IPS_GW_CONTROL" cookie.
 * Supported actions:
 * - SESSION_TERMINATE:     invalidates the session and calls "end_session_endpoint" on Keycloak
 * - SESSION_RESET:         resets the session by deleting all session scopes (removing all JWTs from this session)
 * and empty the session bag (removing all not whitelisted cookies).
 */
public class ControlApiMiddleware implements Middleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControlApiMiddleware.class);

    public static final String CONTROL_COOKIE_NAME = "IPS_GW_CONTROL";
    public static final String SESSION_TERMINATE_ACTION = "SESSION_TERMINATE";
    public static final String SESSION_RESET_ACTION = "SESSION_RESET";
    private final String action;
    private final WebClient webClient;

    public ControlApiMiddleware(final String action, final WebClient webClient) {
        this.action = action;
        this.webClient = webClient;
    }

    @Override
    public void handle(RoutingContext ctx) {
        LOGGER.debug("entering control api middleware");

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
                        .filter(cookie -> Objects.equals(cookie.value(), action))
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
                handleSessionTermination(ctx);
                break;
            case SESSION_RESET_ACTION:
                handleSessionReset(ctx);
                break;
            default:
                final String errorMessage = "Unknown action name: '" + action + "'";
                throw new IllegalArgumentException(errorMessage);
        }
    }

    private void handleSessionTermination(RoutingContext ctx) {
        // calls OIDC end_session_endpoint given by OAuth2Auth and destroys the vertx session
        LOGGER.info("terminate session {}", action);

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
                    LOGGER.debug("endSessionURL {}", endSessionURL);
                    webClient.getAbs(endSessionURL).send()
                            .onSuccess(response -> LOGGER.info("end_session_endpoint call succeeded"))
                            .onFailure(throwable -> LOGGER.warn("end_session_endpoint call failed: {}", throwable.getMessage()));
                });

        // destroy gateway session anyway
        LOGGER.info("vertx session destroyed");
        ctx.session().destroy();
    }

    private void handleSessionReset(RoutingContext ctx) {
        LOGGER.info("reset session {}", action);

        removeAllSessionScopesFrom(ctx);
        removeAllCookiesFromSessionBagExceptForKeycloakCookiesFrom(ctx);
    }

    private void removeAllSessionScopesFrom(RoutingContext ctx) {
        final List<String> toDeleteSessionScopes = ctx.session().data().keySet().stream()
                .filter(key -> key.endsWith(OAuth2MiddlewareFactory.SESSION_SCOPE_SUFFIX))
                .collect(Collectors.toList());

        for (String sessionScope : toDeleteSessionScopes) {
            LOGGER.debug("removing following session scope: '{}'", sessionScope);
            ctx.session().remove(sessionScope);
        }
    }

    private void removeAllCookiesFromSessionBagExceptForKeycloakCookiesFrom(RoutingContext ctx) {
        LOGGER.debug("empty session bag");
        final Set<Cookie> cookiesInSessionBag = ctx.session().get(SESSION_BAG_COOKIES);

        if (cookiesInSessionBag != null) {
            Set<Cookie> filteredCookies = getKeycloakCookiesFrom(cookiesInSessionBag);
            ctx.session().put(SESSION_BAG_COOKIES, filteredCookies);
        }
    }

    private Set<Cookie> getKeycloakCookiesFrom(Set<Cookie> cookiesInSessionBag) {
        return cookiesInSessionBag.stream()
                .filter(cookie -> cookie.name().startsWith("KEYCLOAK_"))
                .collect(Collectors.toSet());
    }
}
