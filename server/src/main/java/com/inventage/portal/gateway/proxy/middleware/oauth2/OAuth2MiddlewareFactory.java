package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.inventage.portal.gateway.core.session.SessionAdapter;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.router.RouterFactory;
import io.reactiverse.contextual.logging.ContextualData;
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
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import static com.inventage.portal.gateway.core.log.RequestResponseLogger.CONTEXTUAL_DATA_SESSION_ID;

/**
 * Configures keycloak as the OAuth2 provider. It patches the authorization path to ensure all
 * follow up requests are routed through this application as well.
 */
public class OAuth2MiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2MiddlewareFactory.class);

    public final static String SESSION_SCOPE_SUFFIX = "_session";

    private static final String OAUTH2_CALLBACK_PREFIX = "/callback/";
    private static final String OIDC_SCOPE = "openid";

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_OAUTH2;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, Router router, JsonObject middlewareConfig) {
        String sessionScope = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_SESSION_SCOPE);

        Route callback = router.get(OAUTH2_CALLBACK_PREFIX + sessionScope.toLowerCase());

        OAuth2Options oauth2Options = new OAuth2Options()
                .setClientID(middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_CLIENTID))
                .setClientSecret(middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_CLIENTSECRET))
                .setSite(middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_DISCOVERYURL))
                .setValidateIssuer(false);

        Future<OAuth2Auth> keycloakDiscoveryFuture = KeycloakAuth.discover(vertx, oauth2Options);

        Promise<Middleware> oauth2Promise = Promise.promise();
        keycloakDiscoveryFuture.onSuccess(authProvider -> {
            LOGGER.debug("create: Successfully completed Keycloak discovery");

            callback.handler(ctx -> {
                LOGGER.debug("callback: processing request for state '{}' and code '{}...'", ctx.request().getParam("state"), ctx.request().getParam("code").substring(0, 5));
                OAuth2AuthMiddleware.restoreStateParameterFromRequest(ctx, sessionScope);
                ctx.addEndHandler(event -> {
                    if (event.succeeded()) {
                        if (ctx.user() != null) {
                            LOGGER.debug("create: Setting user of session scope '{}' with updated sessionId '{}'", sessionScope, SessionAdapter.displaySessionId(ctx.session()));
                            ContextualData.put(CONTEXTUAL_DATA_SESSION_ID, SessionAdapter.displaySessionId(ctx.session()));
                            // AccessToken from vertx-auth was the glue to bind the OAuth2Auth and User objects together.
                            // However, it is marked as deprecated and therefore we use our own glue.
                            Pair<OAuth2Auth, User> authPair = ImmutablePair.of(authProvider, ctx.user());
                            ctx.session().put(String.format("%s%s", sessionScope, SESSION_SCOPE_SUFFIX), authPair);
                        }
                    }
                    if (event.failed()) {
                        LOGGER.warn("callback: end handler failed '{}'", event.cause());
                    }
                    OAuth2AuthMiddleware.endAndRemovePendingAuth(ctx, sessionScope);
                });
                ctx.next(); // io.vertx.ext.web.handler.impl.OAuth2AuthHandlerImpl.setupCallback#route.handler(ctx -> {...})
                //
                LOGGER.debug("callback: processed request for state '{}'", ctx.request().getParam("state"));
            });
            callback.failureHandler(ctx -> {
                LOGGER.debug("callback: processing failed for state '{}' caused by '{}'", ctx.request().getParam("state"),
                        ctx.failure() == null ? "unknown error" : ctx.failure().getMessage());
            });

            OAuth2Options keycloakOAuth2Options = ((OAuth2AuthProviderImpl) authProvider).getConfig();

            // the protocol, hostname or port can be different than the portal-gateway knows, therefor
            // in RouterFactory.createMiddleware the publicUrl configuration is added to this middleware
            // configuration
            String publicUrl = middlewareConfig.getString(RouterFactory.PUBLIC_URL);
            try {
                final URI uri = new URI(keycloakOAuth2Options.getAuthorizationPath());
                final String newAuthorizationPath = authorizationPath(publicUrl, uri);
                keycloakOAuth2Options.setAuthorizationPath(newAuthorizationPath);
            } catch (Exception e) {
                LOGGER.warn("create: Failed to patch authorization path");
            }

            String callbackURL = String.format("%s%s", publicUrl, callback.getPath());

            OAuth2AuthHandler authHandler = OAuth2AuthHandler.create(vertx, authProvider, callbackURL)
                    .setupCallback(callback)
                    // add the sessionScope as a OIDC scope for "aud" in JWT
                    // see https://www.keycloak.org/docs/latest/server_admin/index.html#_audience
                    .withScope(OIDC_SCOPE + " " + sessionScope);

            oauth2Promise.complete(new OAuth2AuthMiddleware(authHandler, sessionScope));
            LOGGER.debug("create: Created '{}' middleware successfully", DynamicConfiguration.MIDDLEWARE_OAUTH2);
        }).onFailure(err -> {
            LOGGER.warn("create: Failed to create OAuth2 Middleware to due failing Keycloak discovery '{}'",
                    err.getMessage());
            oauth2Promise.fail("Failed to create OAuth2 Middleware '" + err.getMessage() + "'");
        });

        return oauth2Promise.future();
    }

    protected String authorizationPath(String publicUrl, URI keycloakAuthorizationEndpoint) {
        return String.format("%s%s", publicUrl, keycloakAuthorizationEndpoint.getPath());
    }

}
