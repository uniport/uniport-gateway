package com.inventage.portal.gateway.proxy.middleware.oauth2;

import static com.inventage.portal.gateway.proxy.middleware.openTelemetry.OpenTelemetryMiddleware.CONTEXTUAL_DATA_SESSION_ID;

import com.inventage.portal.gateway.proxy.middleware.HttpResponder;
import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import com.inventage.portal.gateway.proxy.middleware.log.SessionAdapter;
import com.inventage.portal.gateway.proxy.middleware.oauth2.relyingParty.StateWithUri;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.opentelemetry.api.trace.Span;
import io.reactiverse.contextual.logging.ContextualData;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware for handling parallel OAuth2 flows for a Vert.x session.
 * The authentication can be required for the same scope (scope = "OIDC client id") or for different scopes.
 * Redirects the user if not authenticated.
 */
public class OAuth2AuthMiddleware extends TraceMiddleware {

    public static final String SSO_SID_TO_INTERNAL_SID_MAP_SESSION_DATA_KEY = "uniport.sso-sid-to-internal-sid-map";

    // the following keys are used by RelyingPartyHandler
    public static final String OIDC_PARAM_STATE = "state";
    public static final String OIDC_PARAM_REDIRECT_URI = "redirect_uri";
    public static final String OIDC_PARAM_PKCE = "pkce"; // aka code_verifier (vertx decided to call this pkce)
    public static final String OIDC_PARAM_CODE = "code";

    public static final String SINGLE_SIGN_ON_SID = "sso-sid";

    // prefix of the key for storing the oauth2 states in the session as a JSON structure
    static final String PREFIX_STATE = "oauth2_state_";
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2AuthMiddleware.class);

    private final String name;
    private final AuthenticationHandler authHandler;
    private final String sessionScope;

    public OAuth2AuthMiddleware(Vertx vertx, String name, AuthenticationHandler authHandler, String sessionScope) {
        Objects.requireNonNull(vertx, "vertx must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(authHandler, "authHandler must not be null");
        Objects.requireNonNull(sessionScope, "sessionScope must not be null");

        LOGGER.debug("For session scope '{}'", sessionScope);
        this.name = name;
        this.authHandler = authHandler;
        this.sessionScope = sessionScope;
    }

    /**
     * Remove the OAuth2 state JSON structure for a specific state from the session.
     *
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
     *            context of the callback request
     * @param sessionScope
     * @return true if the session was updated successfully otherwise false
     */
    public static boolean restoreStateParameterFromRequest(RoutingContext ctx, String sessionScope) {
        final String requestState = ctx.request().getParam(OIDC_PARAM_STATE);
        if (requestState != null) {
            final Object authParameters = ctx.session().get(PREFIX_STATE + requestState);
            if (authParameters instanceof JsonObject) {
                final JsonObject oAuth2FlowState = (JsonObject) authParameters;
                ctx.session().put(OIDC_PARAM_STATE, oAuth2FlowState.getString(OIDC_PARAM_STATE));
                ctx.session().put(OIDC_PARAM_REDIRECT_URI, oAuth2FlowState.getString(OIDC_PARAM_REDIRECT_URI));
                ctx.session().put(OIDC_PARAM_PKCE, oAuth2FlowState.getString(OIDC_PARAM_PKCE));

                LOGGER.debug("For state parameter '{}' and scope '{}'", requestState, sessionScope);
                return true;
            } else {
                LOGGER.warn("No OAuth2 state found in session for state parameter '{}' and scope '{}'", requestState,
                    sessionScope);
                return false;
            }
        } else {
            LOGGER.warn("Not state parameter found in request for scope '{}'", sessionScope);
            return false;
        }
    }

    public static boolean isStateForPendingAuth(RoutingContext ctx) {
        final String requestState = ctx.request().getParam(OIDC_PARAM_STATE);
        return ctx.session().get(PREFIX_STATE + requestState) != null;
    }

    protected static void registerCallbackHandlers(Vertx vertx, Route callback, String sessionScope, OAuth2Auth authProvider) {
        callback.handler(ctx -> whenAuthenticationResponseReceived(vertx, ctx, sessionScope, authProvider));
        callback.failureHandler(ctx -> {
            // PORTAL-1184: retry with initial uri
            LOGGER.warn("Processing failed for state '{}' caused by '{}'", ctx.request().getParam(OIDC_PARAM_STATE),
                ctx.failure() == null ? "unknown error" : ctx.failure().getMessage());
            HttpResponder.respondWithStatusCode(ctx.statusCode(), ctx);
        });

    }

    // this method is called when the IAM finishs the authentication flow and sends a redirect (callback) with the code
    private static void whenAuthenticationResponseReceived(
        Vertx vertx,
        RoutingContext ctx,
        String sessionScope,
        OAuth2Auth authProvider
    ) {
        final String stateParameter = ctx.request().getParam(OIDC_PARAM_STATE);
        if (stateParameter == null) {
            ctx.response()
                .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                .end("missing required state parameter");
            return;
        }

        final String code = ctx.request().getParam(OIDC_PARAM_CODE);
        if (OAuth2AuthMiddleware.restoreStateParameterFromRequest(ctx, sessionScope)) {
            LOGGER.debug("processing for state '{}' and code '{}...'", stateParameter, code != null ? code.substring(0, 5) : null);
            ctx.addEndHandler(asyncResult -> whenTokenForCodeReceived(vertx, asyncResult, ctx, authProvider, sessionScope));
            ctx.next(); // io.vertx.ext.web.handler.impl.OAuth2AuthHandlerImpl.setupCallback#route.handler(ctx -> {...})
        } else {
            LOGGER.info("failed because state '{}' wasn't found in session", stateParameter);
            sendResponseFor(stateParameter, ctx);
        }
    }

    // if the enhanced state parameter contains an uri, we send a redirect to it, otherwise a status code 410 (GONE)
    // depending on the response_mode configuration the incoming request is GET or POST
    private static void sendResponseFor(String stateParameter, RoutingContext ctx) {
        final StateWithUri stateWithUri = new StateWithUri(stateParameter);
        final Optional<String> uri = stateWithUri.uri();
        if (uri.isPresent()) {
            // fix PORTAL-1417: die HTTP Methode ist aufgrund response_mode=FORM_POST immer POST, aber
            // es muss die Methode vom ursprünglichen Request berücksichtigt werden!
            final Optional<String> httpMethod = stateWithUri.httpMethod();
            if (httpMethod.isPresent()) {
                if (ctx.request().method().name().equals(httpMethod.get())) {
                    HttpResponder.respondWithRedirectSameMethodWithoutSetCookie(uri.get(), ctx);
                }
                HttpResponder.respondWithRedirectWithoutSetCookie(uri.get(), ctx);
            } else {
                HttpResponder.respondWithRedirectWithoutSetCookie(uri.get(), ctx);
            }
        } else {
            HttpResponder.respondWithStatusCode(HttpResponseStatus.GONE.code(), ctx);
        }
    }

    private static void whenTokenForCodeReceived(
        Vertx vertx,
        AsyncResult<Void> asyncResult,
        RoutingContext ctx,
        OAuth2Auth authProvider,
        String sessionScope
    ) {
        if (asyncResult.succeeded()) {
            if (ctx.user() != null) {
                LOGGER.debug("Setting user of session scope '{}' with updated sessionId '{}'",
                    sessionScope, SessionAdapter.displaySessionId(ctx.session()));
                ContextualData.put(CONTEXTUAL_DATA_SESSION_ID, SessionAdapter.displaySessionId(ctx.session()));

                // AccessToken from vertx-auth was the glue to bind the OAuth2Auth and User objects together.
                // However, it is marked as deprecated, and therefore we use our own glue.
                AuthenticationUserContext.of(authProvider, ctx.user()).toSessionAtScope(ctx.session(), sessionScope);
                storeKeycloakSID(vertx, ctx);
            }
        }
        if (asyncResult.failed()) {
            LOGGER.warn("End handler failed '{}'", asyncResult.cause());
        }
        OAuth2AuthMiddleware.removeOAuth2FlowState(ctx, sessionScope);
    }

    private static void storeKeycloakSID(Vertx vertx, final RoutingContext ctx) {
        final String internalSessionID = ctx.session().id();
        ctx.user().attributes().stream()
            .filter(attr -> "idToken".equals(attr.getKey()))
            .map(attr -> (JsonObject) attr.getValue())
            .map(json -> json.getString("sid"))
            .forEach(sid -> {
                ctx.session().put(SINGLE_SIGN_ON_SID, sid);
                keycloakSIDtoInternalSIDAsyncMap(vertx).compose(map -> map.put(sid, internalSessionID));
            });
    }

    private static Future<AsyncMap<String, String>> keycloakSIDtoInternalSIDAsyncMap(Vertx vertx) {
        return vertx.sharedData().getAsyncMap(SSO_SID_TO_INTERNAL_SID_MAP_SESSION_DATA_KEY);
    }

    /**
     * @param ctx
     */
    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        final User user = ctx.user();
        final User userForScope = setUserForScope(this.sessionScope, ctx);

        // synchronized: to prevent conflicts in writing the "state" value into the session data
        synchronized (ctx.session()) {
            authHandler.handle(ctx);
            if (userForScope == null) {
                ctx.setUser(user);
            }
            startAndStorePendingAuth(ctx);
            LOGGER.debug("Handled URI '{}'", ctx.request().uri());
        }
    }

    /**
     * Update the RoutingContext with the user for the given sessionScope or clear the user if not available.
     *
     * @param sessionScope
     *            an OAuth2 authentication is requested
     * @param ctx
     */
    private User setUserForScope(String sessionScope, RoutingContext ctx) {
        AuthenticationUserContext.fromSessionAtScope(ctx.session(), sessionScope)
            .ifPresentOrElse(
                ac -> ctx.setUser(ac.getUser()),
                () -> ctx.clearUser());

        return ctx.user();
    }

    /**
     * Create a promise if there is a "state" key in the session data.
     *
     * @param ctx
     */
    private void startAndStorePendingAuth(RoutingContext ctx) {
        if (oAuth2FlowStarted(ctx)) {
            // create JSON object for authentication parameters and store in session at "state_<state>"
            final JsonObject oAuth2FlowState = oAuth2FlowState(ctx);
            ctx.session().put(PREFIX_STATE + oAuth2FlowState.getString(OIDC_PARAM_STATE), oAuth2FlowState);
            ctx.session().remove(OIDC_PARAM_STATE);
            LOGGER.debug("For scope '{}'", sessionScope);
        }
    }

    private boolean oAuth2FlowStarted(RoutingContext ctx) {
        return ctx.session().get(OIDC_PARAM_STATE) != null;
    }

    private JsonObject oAuth2FlowState(RoutingContext ctx) {
        final JsonObject oAuth2FlowState = new JsonObject()
            .put(OIDC_PARAM_STATE, ctx.session().get(OIDC_PARAM_STATE))
            .put(OIDC_PARAM_REDIRECT_URI, ctx.session().get(OIDC_PARAM_REDIRECT_URI))
            .put(OIDC_PARAM_PKCE, ctx.session().get(OIDC_PARAM_PKCE));
        return oAuth2FlowState;
    }

}
