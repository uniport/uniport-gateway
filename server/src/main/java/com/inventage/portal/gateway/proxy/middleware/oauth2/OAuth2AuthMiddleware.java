package com.inventage.portal.gateway.proxy.middleware.oauth2;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.proxy.middleware.Middleware;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;

/**
 * Middleware for handling parallel OAuth2 flows for a Vert.x session.
 * The authentication can be required for the same scope (scope = "OIDC client id") of for different scopes.
 * Redirects the user if not authenticated.
 */
public class OAuth2AuthMiddleware implements Middleware {

    // the following keys are used by OAuth2AuthHandlerImpl
    public static final String OIDC_PARAM_STATE = "state";
    public static final String OIDC_PARAM_REDIRECT_URI = "redirect_uri";
    public static final String OIDC_PARAM_PKCE = "pkce";

    // prefix of the key for storing the oauth2 states in the session as a JSON structure
    private static final String PREFIX_STATE = "oauth2_state_";
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2AuthMiddleware.class);

    private AuthenticationHandler authHandler;
    private String sessionScope;

    public OAuth2AuthMiddleware(AuthenticationHandler authHandler, String sessionScope) {
        LOGGER.debug("For session scope '{}'", sessionScope);
        this.authHandler = authHandler;
        this.sessionScope = sessionScope;
    }

    /**
     *
     * @param ctx
     */
    @Override
    public void handle(RoutingContext ctx) {
        LOGGER.debug("URI '{}'", ctx.request().uri());
        final User user = ctx.user();
        final User userForScope = setUserForScope(this.sessionScope, ctx);

        // synchronized: to prevent conflicts in writing the "state" value into the session data
        synchronized (ctx.session()) {
            authHandler.handle(ctx);
            if (userForScope == null) {
                ctx.setUser(user);
            }
            startAndStorePendingAuth(ctx);
            LOGGER.debug("Done for URI '{}'", ctx.request().uri());
        }
    }

    /**
     * Update the RoutingContext with the user for the given sessionScope or clear the user if not available.
     * @param sessionScope an OAuth2 authentication is requested
     * @param ctx
     */
    private User setUserForScope(String sessionScope, RoutingContext ctx) {
        String key = String.format("%s%s", sessionScope, OAuth2MiddlewareFactory.SESSION_SCOPE_SUFFIX);
        Pair<OAuth2Auth, User> authPair = ctx.session().get(key);
        if (authPair != null) {
            ctx.setUser(authPair.getRight());
        } else {
            ctx.clearUser();
        }
        return ctx.user();
    }

    /**
     * Create a promise if there is a "state" key in the session data.
     * @param ctx
     */
    private void startAndStorePendingAuth(RoutingContext ctx) {
        final Object state = ctx.session().get(OIDC_PARAM_STATE);
        if (oAuth2FlowStarted(ctx)) {
            // create JSON object for authentication parameters and store in session at "state_<state>"
            final JsonObject oAuth2FlowState = oAuth2FlowState(ctx);
            ctx.session().put(PREFIX_STATE + oAuth2FlowState.getString(OIDC_PARAM_STATE), oAuth2FlowState);
            LOGGER.debug("For scope '{}'", sessionScope);
        }
    }

    private boolean oAuth2FlowStarted(RoutingContext ctx) {
        return ctx.session().get(OIDC_PARAM_STATE) != null;
    }

    private JsonObject oAuth2FlowState(RoutingContext ctx) {
        final JsonObject oAuth2FlowState = new JsonObject();
        oAuth2FlowState.put(OIDC_PARAM_STATE, ctx.session().get(OIDC_PARAM_STATE));
        oAuth2FlowState.put(OIDC_PARAM_REDIRECT_URI, ctx.session().get(OIDC_PARAM_REDIRECT_URI));
        oAuth2FlowState.put(OIDC_PARAM_PKCE, ctx.session().get(OIDC_PARAM_PKCE));
        return oAuth2FlowState;
    }

    /**
     * Remove the OAuth2 state JSON structure for a specific state from the session.
     * @param ctx
     * @param sessionScope
     */
    public static void removeOAuth2FlowState(RoutingContext ctx, String sessionScope) {
        final String requestState = ctx.request().getParam(OIDC_PARAM_STATE);
        if (requestState != null) {
            ctx.session().remove(PREFIX_STATE + requestState);
        }
    }

    /**
     * Put the OAuth2 flow parameters back into the session for the state parameter from the request.
     *
     * @param ctx
     * @param sessionScope
     */
    public static void restoreStateParameterFromRequest(RoutingContext ctx, String sessionScope) {
        final String requestState = ctx.request().getParam(OIDC_PARAM_STATE);
        if (requestState != null) {
            final Object authParameters = ctx.session().get(PREFIX_STATE + requestState);
            if (authParameters instanceof JsonObject) {
                JsonObject oAuth2FlowState = (JsonObject) authParameters;
                ctx.session().put(OIDC_PARAM_STATE, oAuth2FlowState.getString(OIDC_PARAM_STATE));
                ctx.session().put(OIDC_PARAM_REDIRECT_URI, oAuth2FlowState.getString(OIDC_PARAM_REDIRECT_URI));
                ctx.session().put(OIDC_PARAM_PKCE, oAuth2FlowState.getString(OIDC_PARAM_PKCE));

                LOGGER.debug("For state '{}' and scope '{}'", requestState, sessionScope);
            } else {
                LOGGER.warn("No OAuth2 state found in session for state '{}' and scope '{}'", requestState,
                        sessionScope);
            }
        } else {
            LOGGER.warn("Not state found in request for scope '{}'", sessionScope);
        }
    }

}
