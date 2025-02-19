package com.inventage.portal.gateway.proxy.middleware.controlapi;

import static com.inventage.portal.gateway.proxy.middleware.sessionBag.SessionBagMiddleware.SESSION_BAG_COOKIES;

import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import com.inventage.portal.gateway.proxy.middleware.oauth2.AuthenticationUserContext;
import com.inventage.portal.gateway.proxy.middleware.sessionBag.CookieBag;
import com.inventage.portal.gateway.proxy.middleware.sessionBag.CookieUtil;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles control api actions provided as values from a "IPS_GW_CONTROL" cookie in a response's set-cookie header and also removes it.
 * Supported actions:
 * - SESSION_TERMINATE: invalidates the session and calls "end_session_endpoint" on Keycloak
 * - SESSION_RESET: resets the session by deleting all oauth2 tokens (removing all JWTs from this session)
 * and drain the session bag (removing all not keycloak related cookies).
 */
public class ControlApiMiddleware extends TraceMiddleware {

    public static final String CONTROL_COOKIE_NAME = "IPS_GW_CONTROL";

    private static final Logger LOGGER = LoggerFactory.getLogger(ControlApiMiddleware.class);

    private static final String KEYCOAK_COOKIE_NAME_PREFIX = "KEYCLOAK_";
    private static final String KEYCLOAK_IDENTITY_COOKIE_NAME = "KEYCLOAK_IDENTITY";

    private final Vertx vertx;
    private final String name;
    private final ControlApiAction action;
    private final String resetUri;
    private final WebClient webClient;

    public ControlApiMiddleware(Vertx vertx, String name, final ControlApiAction action, final WebClient webClient) {
        this(vertx, name, action, null, webClient);
    }

    public ControlApiMiddleware(Vertx vertx, String name, final ControlApiAction action, String resetUri, final WebClient webClient) {
        Objects.requireNonNull(vertx, "vertx must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(webClient, "webClient must not be null");
        // resetUri is allowed to be null

        this.vertx = vertx;
        this.name = name;
        this.action = action;
        this.resetUri = resetUri;
        this.webClient = webClient;
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        if ((action.equals(ControlApiAction.SESSION_TERMINATE) || action.equals(ControlApiAction.SESSION_RESET)) && ctx.session() == null) {
            LOGGER.warn("No session initialized. Skipping session termination/reset");
            ctx.next();
            return;
        }

        ctx.addHeadersEndHandler(v -> handleControlCookies(ctx, ctx.response().headers()));
        ctx.next();
    }

    private void handleControlCookies(RoutingContext ctx, MultiMap headers) {
        LOGGER.debug("{}: Handling response of '{}'", name, ctx.request().absoluteURI());

        final List<Cookie> cookiesToSet = headers.getAll(HttpHeaders.SET_COOKIE).stream()
            .map(s -> ClientCookieDecoder.STRICT.decode(s))
            .filter(cookie -> cookie != null)
            .map(cookie -> CookieUtil.fromNettyCookie(cookie))
            .collect(Collectors.toCollection(ArrayList::new));

        final List<Cookie> actionCookies = cookiesToSet.stream()
            .filter(cookie -> Objects.equals(cookie.getName(), CONTROL_COOKIE_NAME))
            .filter(cookie -> Objects.equals(cookie.getValue(), action.toString()))
            .toList();

        final Optional<Cookie> controlApiActionToExecute = actionCookies.stream().findFirst();
        if (controlApiActionToExecute.isPresent()) {
            LOGGER.info("Provided control cookie {}", controlApiActionToExecute.get());
            handleAction(ctx, headers, action);
        }

        final List<String> cookiesToSetWithoutActionCookies = cookiesToSet.stream()
            .filter(cookie -> !actionCookies.contains(cookie))
            .map(cookie -> cookie.encode())
            .toList();

        headers.remove(HttpHeaders.SET_COOKIE.toString());
        cookiesToSetWithoutActionCookies.forEach(cookie -> headers.add(HttpHeaders.SET_COOKIE, cookie));
    }

    private void handleAction(RoutingContext ctx, MultiMap headers, ControlApiAction action) {
        switch (action) {
            case SESSION_TERMINATE:
                handleSessionTermination(ctx);
                break;
            case SESSION_RESET:
                handleSessionReset(ctx, headers);
                break;
            default:
                final String errorMessage = "Unknown action name: '" + action + "'";
                throw new IllegalArgumentException(errorMessage);
        }
    }

    private void handleSessionTermination(RoutingContext ctx) {
        // calls OIDC end_session_endpoint given by OAuth2Auth and destroys the vertx session
        LOGGER.info("terminate session {}", action);

        // 1. look for an AuthenticationUserContext for any session scope in the current session. ID tokens are the same in all session scopes hence take the anyone.
        // 2. get the OAuth2Auth context out of the session scope
        // 3. let compute the endSession url
        // 4. call the url
        // 5. destroy the vertx session
        AuthenticationUserContext.fromSessionAtAnyScope(ctx.session())
            .ifPresent(ac -> {
                final String endSessionURL = ac.getAuthenticationProvider(vertx).endSessionURL(ctx.user());
                LOGGER.debug("endSessionURL {}", endSessionURL);
                webClient.getAbs(endSessionURL).send()
                    .onSuccess(response -> LOGGER.info("end_session_endpoint call succeeded"))
                    .onFailure(throwable -> LOGGER.warn("end_session_endpoint call failed: {}",
                        throwable.getMessage()));
            });

        // destroy gateway session anyway
        LOGGER.info("vertx session destroyed");
        ctx.session().destroy();
    }

    private void handleSessionReset(RoutingContext ctx, MultiMap headers) {
        LOGGER.info("reset session {}", action);

        informKeycloakAboutSessionReset(ctx, headers);
        removeAllOauth2TokensFromSession(ctx);
        removeAllCookiesFromSessionBagExceptForKeycloakCookies(ctx);
    }

    private void informKeycloakAboutSessionReset(RoutingContext ctx, MultiMap headers) {
        if (this.resetUri == null) {
            LOGGER.warn("Keycloak will not be informed of session reset because 'resetUri' is undefined.");
            return;
        }
        LOGGER.info("informing Keycloak about session reset");

        try {
            final URI uri = new URI(resetUri);
            webClient
                .post(uri.getPort(), uri.getHost(), uri.getPath())
                .putHeader(HttpHeaders.COOKIE.toString(), getKeycloakIdentityCookie(ctx, headers).encode())
                .send()
                .onSuccess(response -> LOGGER.info("keycloak was successfully informed of session reset. Response: {}", response))
                .onFailure(throwable -> LOGGER.warn("keycloak was not informed of session reset: {}", throwable.getMessage()));
        } catch (URISyntaxException | NoSuchElementException e) {
            LOGGER.warn(e.getMessage());
        }
    }

    private Cookie getKeycloakIdentityCookie(RoutingContext ctx, MultiMap headers) {
        // 1. priority: search cookie in response header set-cookie
        Optional<Cookie> authCookie = headers.getAll(HttpHeaders.SET_COOKIE).stream()
            .map(s -> ClientCookieDecoder.STRICT.decode(s))
            .filter(cookie -> cookie != null)
            .map(cookie -> CookieUtil.fromNettyCookie(cookie))
            .filter(cookie -> cookie.getName().equals(KEYCLOAK_IDENTITY_COOKIE_NAME))
            .findFirst();

        if (authCookie.isPresent()) {
            return authCookie.get();
        }

        // 2. priority: search cookie in session bag
        final Set<Cookie> cookies = ctx.session().get(SESSION_BAG_COOKIES);
        authCookie = cookies.stream()
            .filter(cookie -> cookie.getName().equals(KEYCLOAK_IDENTITY_COOKIE_NAME))
            .findFirst();

        if (authCookie.isPresent()) {
            return authCookie.get();
        }

        throw new NoSuchElementException("No keycloak identity cookie");
    }

    private void removeAllOauth2TokensFromSession(RoutingContext ctx) {
        AuthenticationUserContext.deleteAll(ctx.session());
    }

    private void removeAllCookiesFromSessionBagExceptForKeycloakCookies(RoutingContext ctx) {
        LOGGER.debug("drain session bag");

        final Set<Cookie> cookiesInSessionBag = ctx.session().get(SESSION_BAG_COOKIES);
        if (cookiesInSessionBag == null) {
            return;
        }

        final Set<Cookie> filteredCookies = getKeycloakCookiesFrom(cookiesInSessionBag);
        ctx.session().put(SESSION_BAG_COOKIES, filteredCookies);
    }

    private Set<Cookie> getKeycloakCookiesFrom(Set<Cookie> cookiesInSessionBag) {
        return cookiesInSessionBag.stream()
            .filter(cookie -> cookie.getName().startsWith(KEYCOAK_COOKIE_NAME_PREFIX))
            .collect(Collectors.toCollection(CookieBag::new));
    }
}
