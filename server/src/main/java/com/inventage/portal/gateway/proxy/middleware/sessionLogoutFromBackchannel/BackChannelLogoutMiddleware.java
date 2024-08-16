package com.inventage.portal.gateway.proxy.middleware.sessionLogoutFromBackchannel;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.vertx.core.http.HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED;

import com.inventage.portal.gateway.proxy.middleware.HttpResponder;
import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddleware;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.auth.impl.jose.JWK;
import io.vertx.ext.auth.impl.jose.JWT;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware for handling back channel logout requests from Keycloak and for checking incoming
 * request for logged-out sessions.
 *
 * Every received sid is stored and then checked upon incoming requests.
 *
 * https://openid.net/specs/openid-connect-backchannel-1_0.html
 */
public class BackChannelLogoutMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackChannelLogoutMiddleware.class);

    protected static final String SHARED_DATA_KEY_SESSIONS = "uniport.logged-out-sso-sessions";
    protected static final String SHARED_DATA_KEY_USERS = "uniport.logged-out-users";

    private final Vertx vertx;
    private final String name;
    private final String backChannelLogoutPath;
    private final LocalMap<String /*sso id*/, String /*subject*/> loggedOutSSOSessionMap;
    private final LocalMap<String /*subject*/, Instant /*before*/> loggedOutUserMap;
    //    private final AuthenticationHandler authHandler;

    private final JWT jwt;

    /**
     * Constructor.
     *
     * @param vertx
     *            the instance
     * @param name
     *            of this middleware as set in the configuration
     * @param backChannelLogoutPath
     *            the URI path, which should be used as the backchannel logout request
     */
    public BackChannelLogoutMiddleware(Vertx vertx, String name, String backChannelLogoutPath) {
        this.vertx = vertx;
        this.name = name;
        this.backChannelLogoutPath = backChannelLogoutPath;
        this.loggedOutSSOSessionMap = this.backChannelLogoutPath != null ? vertx.sharedData().getLocalMap(SHARED_DATA_KEY_SESSIONS) : null;
        this.loggedOutUserMap = this.backChannelLogoutPath != null ? vertx.sharedData().getLocalMap(SHARED_DATA_KEY_USERS) : null;
        this.jwt = new JWT(); //initJWT(authHandler.getJwks());
        //        this.authHandler = authHandler;
    }

    private JWT initJWT(final List<JsonObject> jwks) {
        final JWT jwt = new JWT();
        for (JsonObject jwk : jwks) {
            try {
                jwt.addJWK(new JWK((JsonObject) jwk));
            } catch (Exception e) {
                LOGGER.warn("Unsupported JWK", e);
            }
        }
        return jwt;
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        if (!isBackChannelLogoutUri(ctx.request().uri())) {
            checkForLoggedOutSSOSession(ctx);
            ctx.next();
            return;
        }
        if (!isBackChannelLogoutRequest(ctx.request())) {
            LOGGER.warn("handleWithTraceSpan: invalid back channel logout request in '{}'", name);
            HttpResponder.respondWithStatusCode(BAD_REQUEST, ctx);
        }
        storeLoggedOutSsoSID(ctx);
        ctx.end();
    }

    private boolean isBackChannelLogoutUri(final String uri) {
        return backChannelLogoutPath != null
            && backChannelLogoutPath.equals(uri);
    }

    protected boolean isBackChannelLogoutRequest(final HttpServerRequest request) {
        return backChannelLogoutPath != null
            && backChannelLogoutPath.equals(request.uri())
            && HttpMethod.POST.equals(request.method())
            && APPLICATION_X_WWW_FORM_URLENCODED.toString().equals(request.getHeader(HttpHeaders.CONTENT_TYPE));
    }

    protected void checkForLoggedOutSSOSession(final RoutingContext ctx) {
        if (loggedOutSSOSessionMap != null && !loggedOutSSOSessionMap.isEmpty() && ctx.session() != null) {
            final String ssoId = getSsoId(ctx.session());
            if (isSessionMarkedAsLoggedOut(ssoId)) {
                LOGGER.info("checkForLoggedOutSSOSession: invalidated SSO session detected with id '{}' in '{}'", ssoId, name);
                ctx.session().destroy();
                loggedOutSSOSessionMap.remove(ssoId);
            }
        }
    }

    private boolean isSessionMarkedAsLoggedOut(String ssoId) {
        return ssoId != null && loggedOutSSOSessionMap.containsKey(ssoId);
    }

    private String getSsoId(final Session session) {
        return session.get(OAuth2AuthMiddleware.SINGLE_SIGN_ON_SID);
    }

    protected void storeLoggedOutSsoSID(final RoutingContext ctx) {
        ctx.request().body().onSuccess(body -> {
            if (body != null) {
                final String bodyContent = body.toString();
                // https://openid.net/specs/openid-connect-backchannel-1_0.html#BCRequest
                if (bodyContent.startsWith("logout_token=")) {
                    try {
                        // jwt.parse := read, jwt.decode := read & verify
                        final JsonObject logoutToken = this.jwt.parse(bodyContent.substring("logout_token=".length()));
                        final JsonObject payload = logoutToken.getJsonObject("payload");
                        final String sub = payload.getString("sub");
                        final String sid = payload.getString("sid");
                        if (sid == null || sid.isEmpty()) {
                            loggedOutUserMap.put(sub, Instant.now());
                        } else {
                            loggedOutSSOSessionMap.put(sid, sub);
                        }
                        LOGGER.debug("storeLoggedOutSsoSID: '{}' for sub '{}' in '{}'", sid, sub, name);
                    } catch (Exception e) {
                        LOGGER.warn("storeLoggedOutSsoSID: ", e);
                    }
                } else {
                    LOGGER.warn("storeLoggedOutSsoSID: 'logout_token' key not found in body: '{}'", bodyContent);
                }
            }
        }).onFailure(error -> {
            LOGGER.debug("getLogoutTokenFromRequest: no logout token found in request");
        });
    }

}
