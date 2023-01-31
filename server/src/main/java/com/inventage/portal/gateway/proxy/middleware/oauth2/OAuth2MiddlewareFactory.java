package com.inventage.portal.gateway.proxy.middleware.oauth2;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.oauth2.relyingParty.RelyingPartyHandler;
import com.inventage.portal.gateway.proxy.router.RouterFactory;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.impl.OAuth2AuthProviderImpl;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;

/**
 * Configures keycloak as the OAuth2 provider. It patches the authorization path to ensure all
 * follow up requests are routed through this application as well.
 */
public class OAuth2MiddlewareFactory implements MiddlewareFactory {

    public static final String SESSION_SCOPE_SUFFIX = "_session";
    public static final String OIDC_RESPONSE_MODE = "response_mode";
    public static final String OIDC_RESPONSE_MODE_DEFAULT = "form_post";

    private static final String OAUTH2_CALLBACK_PREFIX = "/callback/";
    private static final String OIDC_SCOPE = "openid";
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2MiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_OAUTH2;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, Router router, JsonObject middlewareConfig) {
        final Route callback;
        final JsonObject oidcParams = oidcParams(middlewareConfig);
        final String sessionScope = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_SESSION_SCOPE);
        if (isFormPost(oidcParams.getString(OIDC_RESPONSE_MODE))) {
            // PORTAL-513: Forces the OIDC Provider to send the authorization code in the body
            callback = router.post(OAUTH2_CALLBACK_PREFIX + sessionScope.toLowerCase()).handler(BodyHandler.create());
        } else {
            callback = router.get(OAUTH2_CALLBACK_PREFIX + sessionScope.toLowerCase()).handler(BodyHandler.create());
        }

        final Promise<Middleware> result = Promise.promise();
        final Future<OAuth2Auth> keycloakDiscoveryFuture = KeycloakAuth.discover(vertx,
                oAuth2Options(middlewareConfig));
        keycloakDiscoveryFuture.onSuccess(authProvider -> {
            LOGGER.debug("Successfully completed Keycloak discovery");

            // the protocol, hostname or port can be different from what the portal-gateway knows, therefore
            // in RouterFactory.createMiddleware the publicUrl configuration is added to this middleware
            // configuration
            final String publicUrl = middlewareConfig.getString(RouterFactory.PUBLIC_URL);
            try {
                final OAuth2Options keycloakOAuth2Options = ((OAuth2AuthProviderImpl) authProvider).getConfig();

                // path issuer
                final URI issuerPath = new URI(keycloakOAuth2Options.getJWTOptions().getIssuer());
                final String newIssuerPath = patchPath(publicUrl, issuerPath);
                keycloakOAuth2Options.getJWTOptions().setIssuer(newIssuerPath);
                LOGGER.debug("patched issuer: {} -> {}", issuerPath, newIssuerPath);

                // patch authorization_endpoint
                final URI authorizationPath = new URI(keycloakOAuth2Options.getAuthorizationPath());
                final String newAuthorizationPath = patchPath(publicUrl, authorizationPath);
                keycloakOAuth2Options.setAuthorizationPath(newAuthorizationPath);
                LOGGER.debug("patched authorization endpoint: {} -> {}", authorizationPath, newAuthorizationPath);
            } catch (Exception e) {
                LOGGER.warn("Failed to patch authorization path");
            }

            OAuth2AuthMiddleware.registerCallbackHandlers(callback, sessionScope, authProvider);
            final String callbackURL = String.format("%s%s", publicUrl, callback.getPath());

            // PORTAL-1184 we are using a patched OAuth2AuthHandlerImpl class as OAuth2AuthHandler implementation.
            final OAuth2AuthHandler authHandler = new RelyingPartyHandler(vertx, authProvider, callbackURL)
                    .setupCallback(callback)
                    .pkceVerifierLength(64)
                    // add the sessionScope as a OIDC scope for "aud" in JWT
                    // see https://www.keycloak.org/docs/latest/server_admin/index.html#_audience
                    .withScopes(List.of(OIDC_SCOPE, sessionScope))
                    .extraParams(oidcParams);

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

    private JsonObject oidcParams(JsonObject middlewareConfig) {
        // PORTAL-1196: value for "response_mode" must be configurable
        final String responseMode = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_RESPONSE_MODE);
        final JsonObject oidcParams = new JsonObject().put(OIDC_RESPONSE_MODE,
                responseMode == null ? OIDC_RESPONSE_MODE_DEFAULT : responseMode);
        return oidcParams;
    }

    private OAuth2Options oAuth2Options(JsonObject middlewareConfig) {
        final OAuth2Options oauth2Options = new OAuth2Options()
                .setClientId(middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_CLIENTID))
                .setClientSecret(middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_CLIENTSECRET))
                .setSite(middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_DISCOVERYURL))
                .setValidateIssuer(false);
        return oauth2Options;
    }

    private String patchPath(String publicUrl, URI keycloakUrl) {
        return String.format("%s%s", publicUrl, keycloakUrl.getPath());
    }
}
