package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.router.RouterFactory;
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
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Configures keycloak as the OAuth2 provider. It patches the authorization path to ensure all
 * follow up requests are routed through this application as well.
 */
public class OAuth2MiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2MiddlewareFactory.class);

    public final static String SESSION_SCOPE_SUFFIX = "_session";

    private static final String OAUTH2_CALLBACK_PREFIX = "/callback/";
    private static final String OIDC_SCOPE = "openid";
    private static final String OAUTH2_RESPONSE_MODE = "response_mode";
    private static final String OAUTH2_RESPONSE_MODE_FORM_POST = "form_post";

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_OAUTH2;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, Router router, JsonObject middlewareConfig) {
        String sessionScope = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_SESSION_SCOPE);

        OAuth2Options oauth2Options = new OAuth2Options()
                .setClientID(middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_CLIENTID))
                .setClientSecret(middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_CLIENTSECRET))
                .setSite(middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_DISCOVERYURL))
                .setValidateIssuer(false);

        Future<OAuth2Auth> keycloakDiscoveryFuture = KeycloakAuth.discover(vertx, oauth2Options);

        Promise<Middleware> oauth2Promise = Promise.promise();
        keycloakDiscoveryFuture.onSuccess(authProvider -> {
            LOGGER.debug("create: Successfully completed Keycloak discovery");

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

            Route callback = createCallbackRoute(router, sessionScope, authProvider);
            String callbackURL = String.format("%s%s", publicUrl, callback.getPath());

            OAuth2AuthHandler authHandler = OAuth2AuthHandler.create(vertx, authProvider, callbackURL)
                    // add the sessionScope as a OIDC scope for "aud" in JWT
                    // see https://www.keycloak.org/docs/latest/server_admin/index.html#_audience
                    .setupCallback(callback)
                    .withScope(OIDC_SCOPE + " " + sessionScope)
                    // https://openid.net/specs/oauth-v2-form-post-response-mode-1_0.html
                    .extraParams(new JsonObject().put(OAUTH2_RESPONSE_MODE, OAUTH2_RESPONSE_MODE_FORM_POST))
                    // https://datatracker.ietf.org/doc/html/rfc7636#section-4.1
                    .pkceVerifierLength(43);

            oauth2Promise.complete(new OAuth2AuthMiddleware(authHandler, sessionScope));
            LOGGER.debug("create: Created '{}' middleware successfully", DynamicConfiguration.MIDDLEWARE_OAUTH2);
        }).onFailure(err -> {
            LOGGER.warn("create: Failed to create OAuth2 Middleware to due failing Keycloak discovery '{}'",
                    err.getMessage());
            oauth2Promise.fail("Failed to create OAuth2 Middleware '" + err.getMessage() + "'");
        });

        return oauth2Promise.future();
    }

    protected Route createCallbackRoute(Router router, String sessionScope, OAuth2Auth authProvider) {
        Route callback = router.post(OAUTH2_CALLBACK_PREFIX + sessionScope.toLowerCase());
        // BodyHandler is necessary to support response_mode=form_post
        callback.handler(BodyHandler.create());
        callback.handler(ctx -> {
            LOGGER.debug("create: Handling callback");
            ctx.addEndHandler(event -> {
                if (ctx.user() != null) {
                    LOGGER.debug("create: Setting user of session scope '{}'", sessionScope);
                    // AccessToken from vertx-auth was the glue to bind the OAuth2Auth and User objects together.
                    // However, it is marked as deprecated and therefore we use our own glue.
                    Pair<OAuth2Auth, User> authPair = ImmutablePair.of(authProvider, ctx.user());
                    ctx.session().put(String.format("%s%s", sessionScope, SESSION_SCOPE_SUFFIX), authPair);
                }
            });
            ctx.next();
        });

        return callback;
    }

    protected String authorizationPath(String publicUrl, URI keycloakAuthorizationEndpoint) {
        return String.format("%s%s", publicUrl, keycloakAuthorizationEndpoint.getPath());
    }
}
