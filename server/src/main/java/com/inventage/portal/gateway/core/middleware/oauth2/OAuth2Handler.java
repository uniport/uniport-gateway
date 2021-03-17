package com.inventage.portal.gateway.core.middleware.oauth2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.OAuth2AuthHandler;

public interface OAuth2Handler extends Handler<RoutingContext> {

    public static final String OAUTH2 = "oauth2";
    public static final String OAUTH2_CLIENTID = "clientId";
    public static final String OAUTH2_CLIENTSECRET = "clientSecret";
    public static final String OAUTH2_DISCOVERYURL = "discoveryUrl";
    public static final String OAUTH2_CALLBACK_PREFIX = "/callback/";
    public static final String OAUTH2_SCOPE = "openid";

    static OAuth2Handler create(Vertx vertx, JsonObject middlewareConfig, Router router,
            String routerName, String sessionScope, String publicHostname, String entrypointPort) {


        // if (sessionScope == null || "".equals(sessionScope)) {
        // sessionScope = routerName;
        // }

        Route callback = router.get(OAUTH2_CALLBACK_PREFIX + routerName.toLowerCase());

        callback.handler(ctx -> {
            ctx.addEndHandler(event -> {
                if (ctx.user() != null) {
                    ctx.session().put(sessionScope, ctx.user());
                }
            });
            ctx.next();
        });

        OAuth2Options oauth2Options =
                new OAuth2Options().setClientID(middlewareConfig.getString(OAUTH2_CLIENTID))
                        .setClientSecret(middlewareConfig.getString(OAUTH2_CLIENTSECRET))
                        .setSite(middlewareConfig.getString(OAUTH2_DISCOVERYURL));

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

        // TODO solve this asynch problem
        // return new OAuth2HandlerImpl(authHandler, sessionScope);
        return new OAuth2HandlerImpl(null, sessionScope);
    }
}
