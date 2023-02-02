package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.HttpResponder;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.log.SessionAdapter;
import com.inventage.portal.gateway.proxy.router.RouterFactory;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactiverse.contextual.logging.ContextualData;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.impl.OAuth2AuthProviderImpl;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import io.vertx.ext.web.handler.impl.RelyingPartyHandler;
import io.vertx.ext.web.handler.impl.StateWithUri;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Optional;

import static com.inventage.portal.gateway.proxy.middleware.log.RequestResponseLogger.CONTEXTUAL_DATA_SESSION_ID;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddleware.OIDC_PARAM_STATE;

/**
 * Configures keycloak as the OAuth2 provider. It patches the authorization path to ensure all
 * follow up requests are routed through this application as well.
 */
public class OAuth2MiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2MiddlewareFactory.class);

    public static final String SESSION_SCOPE_SUFFIX = "_session";
    private static final String OAUTH2_CALLBACK_PREFIX = "/callback/";
    private static final String OIDC_SCOPE = "openid";

    public static final String OIDC_RESPONSE_MODE = "response_mode";
    public static final String OIDC_RESPONSE_MODE_DEFAULT = "form_post";

    private static final String OIDC_CODE = "code";

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_OAUTH2;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, Router router, JsonObject middlewareConfig) {
        final Promise<Middleware> result = Promise.promise();
        final String sessionScope = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_SESSION_SCOPE);
        // PORTAL-1196: value for "response_mode" must be configurable
        final String responseMode = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_RESPONSE_MODE);
        final JsonObject oidcParams = new JsonObject();
        oidcParams.put(OIDC_RESPONSE_MODE, responseMode == null ? OIDC_RESPONSE_MODE_DEFAULT : responseMode);
        final Route callback;
        if (isFormPost(oidcParams.getString(OIDC_RESPONSE_MODE))) {
            // PORTAL-513: Forces the OIDC Provider to send the authorization code in the body
            callback = router.post(OAUTH2_CALLBACK_PREFIX + sessionScope.toLowerCase()).handler(BodyHandler.create());
        }
        else {
            callback = router.get(OAUTH2_CALLBACK_PREFIX + sessionScope.toLowerCase()).handler(BodyHandler.create());
        }

        final Future<OAuth2Auth> keycloakDiscoveryFuture = KeycloakAuth.discover(vertx,
                oAuth2Options(middlewareConfig));
        keycloakDiscoveryFuture.onSuccess(authProvider -> {
            LOGGER.debug("Successfully completed Keycloak discovery");

            callback.handler(ctx -> whenAuthenticationResponseReceived(ctx, sessionScope, authProvider));
            callback.failureHandler(ctx -> {
                // PORTAL-1184: retry with initial uri
                LOGGER.warn("Processing failed for state '{}' caused by '{}'", ctx.request().getParam(OIDC_PARAM_STATE),
                        ctx.failure() == null ? "unknown error" : ctx.failure().getMessage());
                HttpResponder.respondWithStatusCode(ctx.statusCode(), ctx);
            });

            final OAuth2Options keycloakOAuth2Options = ((OAuth2AuthProviderImpl) authProvider).getConfig();

            // the protocol, hostname or port can be different from what the portal-gateway knows, therefor
            // in RouterFactory.createMiddleware the publicUrl configuration is added to this middleware
            // configuration
            final String publicUrl = middlewareConfig.getString(RouterFactory.PUBLIC_URL);
            try {
                final URI uri = new URI(keycloakOAuth2Options.getAuthorizationPath());
                final String newAuthorizationPath = authorizationPath(publicUrl, uri);
                keycloakOAuth2Options.setAuthorizationPath(newAuthorizationPath);
            }
            catch (Exception e) {
                LOGGER.warn("Failed to patch authorization path");
            }

            final String callbackURL = String.format("%s%s", publicUrl, callback.getPath());

            // PORTAL-1184 we are using a patched OAuth2AuthHandlerImpl class as OAuth2AuthHandler implementation.
            final OAuth2AuthHandler authHandler = new RelyingPartyHandler(vertx, authProvider, callbackURL)
                    .setupCallback(callback)
                    .pkceVerifierLength(64)
                    .extraParams(oidcParams)
                    // add the sessionScope as a OIDC scope for "aud" in JWT
                    // see https://www.keycloak.org/docs/latest/server_admin/index.html#_audience
                    .withScope(OIDC_SCOPE + " " + sessionScope);

            result.complete(new OAuth2AuthMiddleware(authHandler, sessionScope));
            LOGGER.debug("Created '{}' middleware successfully", DynamicConfiguration.MIDDLEWARE_OAUTH2);
        }).onFailure(err -> {
            LOGGER.warn("Failed to create OAuth2 Middleware to due failing Keycloak discovery '{}'",
                    err.getMessage());
            result.fail("Failed to create OAuth2 Middleware '" + err.getMessage() + "'");
        });

        return result.future();
    }

    private boolean isFormPost(String aResponseMode) {
        return OIDC_RESPONSE_MODE_DEFAULT.equals(aResponseMode);
    }

    protected String authorizationPath(String publicUrl, URI keycloakAuthorizationEndpoint) {
        return String.format("%s%s", publicUrl, keycloakAuthorizationEndpoint.getPath());
    }

    protected OAuth2Options oAuth2Options(JsonObject middlewareConfig) {
        return new OAuth2Options()
                .setClientID(middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_CLIENTID))
                .setClientSecret(middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_CLIENTSECRET))
                .setSite(middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_DISCOVERYURL))
                .setValidateIssuer(false);
    }

    // this method is called when the IAM finishs the authentication flow and sends a redirect (callback) with the code
    protected void whenAuthenticationResponseReceived(RoutingContext ctx, String sessionScope,
                                                      OAuth2Auth authProvider) {
        final String stateParameter = ctx.request().getParam(OIDC_PARAM_STATE);
        final String code = ctx.request().getParam(OIDC_CODE);
        if (OAuth2AuthMiddleware.restoreStateParameterFromRequest(ctx, sessionScope)) {
            LOGGER.debug("processing for state '{}' and code '{}...'", stateParameter, code.substring(0, 5));
            ctx.addEndHandler(asyncResult -> whenTokenForCodeReceived(asyncResult, ctx, authProvider, sessionScope));
            ctx.next(); // io.vertx.ext.web.handler.impl.OAuth2AuthHandlerImpl.setupCallback#route.handler(ctx -> {...})
        }
        else {
            LOGGER.info("failed because state '{}' wasn't found in session", stateParameter);
            sendResponseFor(stateParameter, ctx);
        }
    }

    // if the enhanced state parameter contains an uri, we send a redirect to it, otherwise a status code 410 (GONE)
    private void sendResponseFor(String stateParameter, RoutingContext ctx) {
        final Optional<String> uri = new StateWithUri(stateParameter).uri();
        if (uri.isPresent()) {
            HttpResponder.respondWithRedirectWithoutSetCookie(uri.get(), ctx);
        }
        else {
            HttpResponder.respondWithStatusCode(HttpResponseStatus.GONE.code(), ctx);
        }
    }

    protected void whenTokenForCodeReceived(AsyncResult<Void> asyncResult, RoutingContext ctx, OAuth2Auth authProvider,
                                            String sessionScope) {
        if (asyncResult.succeeded()) {
            if (ctx.user() != null) {
                LOGGER.debug("Setting user of session scope '{}' with updated sessionId '{}'", sessionScope,
                        SessionAdapter.displaySessionId(ctx.session()));
                ContextualData.put(CONTEXTUAL_DATA_SESSION_ID,
                        SessionAdapter.displaySessionId(ctx.session()));
                // AccessToken from vertx-auth was the glue to bind the OAuth2Auth and User objects together.
                // However, it is marked as deprecated, and therefore we use our own glue.
                final Pair<OAuth2Auth, User> authPair = ImmutablePair.of(authProvider, ctx.user());
                ctx.session().put(String.format("%s%s", sessionScope, SESSION_SCOPE_SUFFIX), authPair);
            }
        }
        if (asyncResult.failed()) {
            LOGGER.warn("End handler failed '{}'", asyncResult.cause());
        }
        OAuth2AuthMiddleware.removeOAuth2FlowState(ctx, sessionScope);
    }
}
