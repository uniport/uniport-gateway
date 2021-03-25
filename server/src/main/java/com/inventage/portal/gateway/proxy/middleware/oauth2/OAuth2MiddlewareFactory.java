package com.inventage.portal.gateway.proxy.middleware.oauth2;

import java.net.URI;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import io.vertx.ext.web.handler.OAuth2AuthHandler;

public class OAuth2MiddlewareFactory implements MiddlewareFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2MiddlewareFactory.class);

    private static final String OAUTH2_CALLBACK_PREFIX = "/callback/";
    private static final String OAUTH2_SCOPE = "openid";

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_OAUTH2;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, Router router, JsonObject middlewareConfig) {

        String sessionScope =
                middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_SESSION_SCOPE);

        Route callback = router.get(OAUTH2_CALLBACK_PREFIX + sessionScope.toLowerCase());

        OAuth2Options oauth2Options = new OAuth2Options()
                .setClientID(
                        middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_CLIENTID))
                .setClientSecret(middlewareConfig
                        .getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_CLIENTSECRET))
                .setSite(middlewareConfig
                        .getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_DISCOVERYURL));

        Future<OAuth2Auth> keycloakDiscoveryFuture = KeycloakAuth.discover(vertx, oauth2Options);

        Promise<Middleware> oauth2Promise = Promise.promise();
        keycloakDiscoveryFuture.onSuccess(authProvider -> {
            LOGGER.debug("Successfully completed Keycloak discovery");

            // TODO maybe we can do this with postAuthentication
            callback.handler(ctx -> {
                LOGGER.debug("Handling callback");
                ctx.addEndHandler(event -> {
                    // TODO set access and id tokens
                    if (ctx.user() != null) {
                        ctx.session().put(sessionScope, ctx.user());
                    }
                });
                ctx.next();
            });

            OAuth2Options keycloakOAuth2Options =
                    ((OAuth2AuthProviderImpl) authProvider).getConfig();

            // TODO whats the prupose of this? to ensure this request is routed through the gateway
            // as well
            String publicHostname = "localhost";
            String entrypointPort = "8000";
            try {
                final URI uri = new URI(keycloakOAuth2Options.getAuthorizationPath());
                final String newAuthorizationPath = String.format("%s://%s:%s%s", "http",
                        publicHostname, entrypointPort, uri.getPath());
                keycloakOAuth2Options.setAuthorizationPath(newAuthorizationPath);
            } catch (Exception e) {
                LOGGER.error("Failed to patch authorization path");
            }

            String callbackURL = String.format("http://%s:%s%s", publicHostname, entrypointPort,
                    callback.getPath());

            OAuth2AuthHandler authHandler =
                    OAuth2AuthHandler.create(vertx, authProvider, callbackURL)
                            .setupCallback(callback).withScope(OAUTH2_SCOPE);

            oauth2Promise.complete(new OAuth2AuthMiddleware(authHandler));
            LOGGER.debug("Created OAuth2 middleware");
        }).onFailure(handler -> {
            LOGGER.error(
                    "Failed to create OAuth2 Middleware to due failing Keycloak discovery '{}'",
                    handler.getCause());
            oauth2Promise.fail("Failed to create OAuth2 Middleware '" + handler.getCause() + "'");
        });

        return oauth2Promise.future();
    }

}

