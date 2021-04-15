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

/**
 * Configures keycloak as the OAuth2 provider. It patches the authorization path to ensure all
 * follow up requests are routed through this application as well.
 */
public class OAuth2MiddlewareFactory implements MiddlewareFactory {

        private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2MiddlewareFactory.class);

        public final static String SESSION_SCOPE_USER_FORMAT = "%s_user";
        public final static String SESSION_SCOPE_ACCESS_TOKEN_FORMAT = "%s_access_token";
        public final static String ID_TOKEN = "id_token";

        private final static String ACCESS_TOKEN = "access_token";

        private static final String OAUTH2_CALLBACK_PREFIX = "/callback/";
        private static final String OIDC_SCOPE = "openid";

        @Override
        public String provides() {
                return DynamicConfiguration.MIDDLEWARE_OAUTH2;
        }

        @Override
        public Future<Middleware> create(Vertx vertx, Router router, JsonObject middlewareConfig) {
                String sessionScope = middlewareConfig
                                .getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_SESSION_SCOPE);

                Route callback = router.get(OAUTH2_CALLBACK_PREFIX + sessionScope.toLowerCase());

                OAuth2Options oauth2Options = new OAuth2Options()
                                .setClientID(middlewareConfig.getString(
                                                DynamicConfiguration.MIDDLEWARE_OAUTH2_CLIENTID))
                                .setClientSecret(middlewareConfig.getString(
                                                DynamicConfiguration.MIDDLEWARE_OAUTH2_CLIENTSECRET))
                                .setSite(middlewareConfig.getString(
                                                DynamicConfiguration.MIDDLEWARE_OAUTH2_DISCOVERYURL));

                Future<OAuth2Auth> keycloakDiscoveryFuture =
                                KeycloakAuth.discover(vertx, oauth2Options);

                Promise<Middleware> oauth2Promise = Promise.promise();
                keycloakDiscoveryFuture.onSuccess(authProvider -> {
                        LOGGER.debug("create: Successfully completed Keycloak discovery");

                        callback.handler(ctx -> {
                                LOGGER.debug("create: Handling callback");
                                ctx.addEndHandler(event -> {
                                        if (ctx.user() != null) {
                                                LOGGER.debug("create: Setting session scope user");
                                                ctx.session().put(String.format(
                                                                SESSION_SCOPE_USER_FORMAT,
                                                                sessionScope), ctx.user());

                                                if (ctx.user().principal() != null) {
                                                        JsonObject principal =
                                                                        ctx.user().principal();

                                                        LOGGER.debug("create: Setting id token");
                                                        String idToken = principal
                                                                        .getString(ID_TOKEN);
                                                        ctx.session().put(ID_TOKEN, idToken);

                                                        LOGGER.debug("create: Setting access token for scope '{}'",
                                                                        sessionScope);
                                                        String accessToken = principal
                                                                        .getString(ACCESS_TOKEN);
                                                        ctx.session().put(String.format(
                                                                        SESSION_SCOPE_ACCESS_TOKEN_FORMAT,
                                                                        sessionScope), accessToken);
                                                }
                                        }
                                });
                                ctx.next();
                        });

                        OAuth2Options keycloakOAuth2Options =
                                        ((OAuth2AuthProviderImpl) authProvider).getConfig();

                        String publicHostname = middlewareConfig.getString("publicHostname");
                        String entrypointPort = middlewareConfig.getString("entrypointPort");
                        try {
                                final URI uri = new URI(
                                                keycloakOAuth2Options.getAuthorizationPath());
                                final String newAuthorizationPath = String.format("%s://%s:%s%s",
                                                "http", publicHostname, entrypointPort,
                                                uri.getPath());
                                keycloakOAuth2Options.setAuthorizationPath(newAuthorizationPath);
                        } catch (Exception e) {
                                LOGGER.warn("create: Failed to patch authorization path");
                        }

                        String callbackURL = String.format("http://%s:%s%s", publicHostname,
                                        entrypointPort, callback.getPath());

                        OAuth2AuthHandler authHandler = OAuth2AuthHandler
                                        .create(vertx, authProvider, callbackURL)
                                        // add the sessionScope as a OIDC scope for "aud" in JWT
                                        // see https://www.keycloak.org/docs/latest/server_admin/index.html#_audience
                                        .setupCallback(callback).withScope(OIDC_SCOPE + " " + sessionScope);

                        oauth2Promise.complete(new OAuth2AuthMiddleware(authHandler, sessionScope));
                        LOGGER.debug("create: Created '{}' middleware successfully",
                                        DynamicConfiguration.MIDDLEWARE_OAUTH2);
                }).onFailure(err -> {
                        LOGGER.warn("create: Failed to create OAuth2 Middleware to due failing Keycloak discovery '{}'",
                                        err.getMessage());
                        oauth2Promise.fail("Failed to create OAuth2 Middleware '" + err.getMessage()
                                        + "'");
                });

                return oauth2Promise.future();
        }

}

