package com.inventage.portal.gateway.core.middleware.oauth2;

import java.net.URI;
import com.inventage.portal.gateway.core.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.core.middleware.Middleware;
import com.inventage.portal.gateway.core.middleware.MiddlewareFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2Options;
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
    public Middleware create(Vertx vertx, JsonObject middlewareConfig) {

        // TODO configure
        String sessionScope =
                middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_SESSION_SCOPE);
        String publicHostname = null;
        String entrypointPort = null;
        Router router = null;

        Route callback = router.get(OAUTH2_CALLBACK_PREFIX + sessionScope.toLowerCase());

        callback.handler(ctx -> {
            ctx.addEndHandler(event -> {
                // TODO set access and id tokens
                System.out.println(event);
                if (ctx.user() != null) {
                    ctx.session().put(sessionScope, ctx.user());
                }
            });
            ctx.next();
        });

        OAuth2Options oauth2Options = new OAuth2Options()
                .setClientID(
                        middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_CLIENTID))
                .setClientSecret(middlewareConfig
                        .getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_CLIENTSECRET))
                .setSite(middlewareConfig
                        .getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_DISCOVERYURL));

        Future<OAuth2Auth> future = KeycloakAuth.discover(vertx, oauth2Options);

        future.onSuccess(authProvider -> {
            try {
                final URI uri = new URI(oauth2Options.getAuthorizationPath());
                final String newAuthorizationPath = String.format("%s://%s:%s%s", "http",
                        publicHostname, entrypointPort, uri.getPath());
                oauth2Options.setAuthorizationPath(newAuthorizationPath);
            } catch (Exception e) {

            }

            OAuth2AuthHandler authHandler = OAuth2AuthHandler.create(vertx, authProvider)
                    .setupCallback(callback).withScope(OAUTH2_SCOPE);

        });

        // TODO/ASK solve this asynch problem
        // return new OAuth2HandlerImpl(authHandler, sessionScope);
        return new OAuth2AuthMiddleware(null, sessionScope);
    }

}

