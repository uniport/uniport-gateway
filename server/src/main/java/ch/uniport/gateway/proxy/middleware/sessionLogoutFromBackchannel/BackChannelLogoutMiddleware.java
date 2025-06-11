package ch.uniport.gateway.proxy.middleware.sessionLogoutFromBackchannel;

import static ch.uniport.gateway.proxy.middleware.oauth2.OAuth2AuthMiddleware.SSO_SID_TO_INTERNAL_SID_MAP_SESSION_DATA_KEY;
import static ch.uniport.gateway.proxy.middleware.session.SessionMiddleware.SESSION_MIDDLEWARE_SESSION_STORE_KEY;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.vertx.core.http.HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED;

import ch.uniport.gateway.proxy.middleware.HttpResponder;
import ch.uniport.gateway.proxy.middleware.TraceMiddleware;
import ch.uniport.gateway.proxy.middleware.authorization.JWKAccessibleAuthHandler;
import ch.uniport.gateway.proxy.middleware.log.SessionAdapter;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.ext.auth.impl.jose.JWK;
import io.vertx.ext.auth.impl.jose.JWT;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.sstore.SessionStore;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware for handling back channel logout requests from Keycloak and for
 * checking incoming
 * request for logged-out sessions.
 *
 * Every received sid is stored and then checked upon incoming requests.
 *
 * https://openid.net/specs/openid-connect-backchannel-1_0.html
 */
public class BackChannelLogoutMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackChannelLogoutMiddleware.class);

    private static final String LOGOUT_TOKEN_PREFIX = "logout_token=";
    private static final String SUB_KEY = "sub";
    private static final String SID_KEY = "sid";
    private static final String EVENTS_KEY = "events";
    private static final String EVENTS_BACK_CHANNEL_LOGOUT_KEY = "http://schemas.openid.net/event/backchannel-logout";
    private static final String NONCE_KEY = "nonce";

    private final String name;

    private final JWKAccessibleAuthHandler jwkFetcher;
    // contains mappings from SSO session ID to internal session ID
    private AsyncMap<String, String> sessionIDMap;

    /**
     * Constructor.
     *
     * @param vertx
     *            the instance
     * @param name
     *            of this middleware as set in the configuration
     * @param backChannelLogoutPath
     *            the URI path, which should be used as the
     *            back-channel logout request
     */
    public BackChannelLogoutMiddleware(Vertx vertx, String name, JWKAccessibleAuthHandler authHandler) {
        Objects.requireNonNull(vertx, "vertx must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(authHandler, "authHandler must not be null");

        this.name = name;
        this.jwkFetcher = authHandler;

        vertx.sharedData().<String, String>getAsyncMap(SSO_SID_TO_INTERNAL_SID_MAP_SESSION_DATA_KEY)
            .onSuccess(sidMap -> this.sessionIDMap = sidMap);
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        if (!isBackChannelLogoutRequest(ctx.request())) {
            LOGGER.warn("invalid back channel logout request");
            HttpResponder.respondWithStatusCode(BAD_REQUEST, ctx);
            return;
        }

        handleBackChannelLogoutRequest(ctx);
    }

    protected boolean isBackChannelLogoutRequest(final HttpServerRequest request) {
        return HttpMethod.POST.equals(request.method())
            && APPLICATION_X_WWW_FORM_URLENCODED.toString().equals(request.getHeader(HttpHeaders.CONTENT_TYPE));
    }

    protected void handleBackChannelLogoutRequest(RoutingContext ctx) {
        readRequestBody(ctx)
            .compose(body -> parseAndVerifyLogoutToken(body))
            .compose(logoutToken -> getSsoSID(logoutToken))
            .compose(ssoSID -> aggregateWithInternalSID(ssoSID))
            .compose(sidPair -> destroyInternalSession(ctx, sidPair))
            .onSuccess(v -> {
                ctx.end();
            })
            .onFailure(err -> {
                LOGGER.warn("failed to handle back channel logout request", err);
                if (err instanceof IllegalArgumentException) {
                    HttpResponder.respondWithStatusCode(BAD_REQUEST, ctx);
                } else {
                    HttpResponder.respondWithStatusCode(INTERNAL_SERVER_ERROR, ctx);
                }
            });
    }

    protected Future<Buffer> readRequestBody(RoutingContext ctx) {
        return ctx.request().body();
    }

    protected Future<JsonObject> parseAndVerifyLogoutToken(Buffer body) {
        if (body == null) {
            return Future.failedFuture(new IllegalArgumentException("back channel logout request has no body"));
        }

        final String bodyContent = body.toString();
        // https://openid.net/specs/openid-connect-backchannel-1_0.html#BCRequest
        if (!bodyContent.startsWith(LOGOUT_TOKEN_PREFIX)) {
            return Future.failedFuture(new IllegalArgumentException(
                String.format("'logout_token' key not found in body: '%s'", bodyContent)));
        }

        final JWT jwt = initJWT(jwkFetcher.getJwks());
        LOGGER.debug("Loaded verifying JWKs");

        // Validate logout token according to:
        // https://openid.net/specs/openid-connect-backchannel-1_0.html#Validation

        // 1. If the Logout Token is encrypted, decrypt it using the keys and algorithms
        // that the Client specified during Registration that the OP
        // was to use to encrypt ID Tokens. If ID Token encryption was negotiated with
        // the OP at Registration time and the Logout Token is not
        // encrypted, the RP SHOULD reject it.
        // 2. Validate the Logout Token signature in the same way that an ID Token
        // signature is validated, with the following refinements.
        // 3. Validate the alg (algorithm) Header Parameter in the same way it is
        // validated for ID Tokens. Like ID Tokens, selection of the algorithm
        // used is governed by the id_token_signing_alg_values_supported Discovery
        // parameter and the id_token_signed_response_alg Registration
        // parameter when they are used; otherwise, the value SHOULD be the default of
        // RS256. Additionally, an alg with the value none MUST NOT
        // be used for Logout Tokens.
        // 4. Validate the iss, aud, iat, and exp Claims in the same way they are
        // validated in ID Tokens.
        final JsonObject logoutToken;
        try {
            // jwt.decode := read & verify
            logoutToken = jwt.decode(bodyContent.substring(LOGOUT_TOKEN_PREFIX.length()));
            LOGGER.debug("Verified logout token signature");
        } catch (Exception e) {
            return Future.failedFuture(e);
        }

        // 5. Verify that the Logout Token contains a sub Claim, a sid Claim, or both.
        if (!logoutToken.containsKey(SUB_KEY)) {
            return Future.failedFuture(new IllegalArgumentException(
                String.format("'%s' claim not found in logout token: '%s'", SUB_KEY, logoutToken)));
        }
        if (!logoutToken.containsKey(SID_KEY)) {
            return Future.failedFuture(new IllegalArgumentException(
                String.format("'%s' claim not found in logout token: '%s'", SID_KEY, logoutToken)));
        }
        LOGGER.debug("Checked presence of required logout token claims");

        // 6. Verify that the Logout Token contains an events Claim whose value is JSON
        // object containing the member name
        // http://schemas.openid.net/event/backchannel-logout.
        final JsonObject events = logoutToken.getJsonObject(EVENTS_KEY);
        if (events == null || events.isEmpty()) {
            return Future.failedFuture(new IllegalArgumentException(
                String.format("'%s' claim not found in logout token: '%s'", EVENTS_KEY, logoutToken)));
        }
        if (!events.containsKey(EVENTS_BACK_CHANNEL_LOGOUT_KEY)) {
            return Future
                .failedFuture(new IllegalArgumentException(String.format("'%s' key not found in '%s' claim: '%s'",
                    EVENTS_BACK_CHANNEL_LOGOUT_KEY, EVENTS_KEY, logoutToken)));
        }
        LOGGER.debug("Checked correctness of logout token events claim");

        // 7. Verify that the Logout Token does not contain a nonce Claim.
        if (logoutToken.containsKey(NONCE_KEY)) {
            return Future.failedFuture(new IllegalArgumentException(
                String.format("forbidden '%s' claim found in logout token: '%s'", NONCE_KEY, logoutToken)));
        }
        LOGGER.debug("Checked absence of logout token nonce claim");

        // 8. (OMITTED) Optionally verify that another Logout Token with the same jti
        // value has not been recently received.
        // 9. (OMITTED) Optionally verify that the iss Logout Token Claim matches the
        // iss Claim in an ID Token issued for the current session or a recent session
        // of this RP with the OP.
        // 10. (OMITTED) Optionally verify that any sub Logout Token Claim matches the
        // sub Claim in an ID Token issued for the current session or a recent session
        // of this RP with the OP.
        // 11. (OMITTED) Optionally verify that any sid Logout Token Claim matches the
        // sid Claim in an ID Token issued for the current session or a recent session
        // of this RP with the OP.

        LOGGER.debug("validated logout request successfully");
        return Future.succeededFuture(logoutToken);
    }

    protected Future<String> getSsoSID(JsonObject logoutToken) {
        final String sub = logoutToken.getString(SUB_KEY);
        if (sub == null || sub.isEmpty()) {
            return Future.failedFuture(new IllegalArgumentException(
                String.format("'%s' claim not found in logout token: '%s'", SUB_KEY, logoutToken)));
        }

        final String sid = logoutToken.getString(SID_KEY);
        if (sid == null || sid.isEmpty()) {
            return Future.failedFuture(new IllegalArgumentException(
                String.format("'%s' claim not found in logout token: '%s'", SID_KEY, logoutToken)));
        }

        LOGGER.debug("logging out session '{}' for subject '{}'", sid, sub);
        return Future.succeededFuture(sid);
    }

    protected Future<SimpleEntry<String, String>> aggregateWithInternalSID(String ssoSID) {
        return sessionIDMap.get(ssoSID)
            .compose(internalSID -> {
                if (internalSID == null || internalSID.isEmpty()) {
                    return Future.failedFuture(new IllegalStateException(
                        String.format("no mapping found for the ssoID '%s'", ssoSID)));
                }

                LOGGER.debug("resolved SSO session ID '{}' to internal session ID '{}'", ssoSID,
                    SessionAdapter.displaySessionId(internalSID));
                return Future.succeededFuture(new SimpleEntry<String, String>(ssoSID, internalSID));
            });
    }

    protected Future<Void> destroyInternalSession(RoutingContext ctx, SimpleEntry<String, String> sidPair) {
        final String ssoSID = sidPair.getKey();
        final String internalSID = sidPair.getValue();
        final SessionStore sessionStore = ctx.get(SESSION_MIDDLEWARE_SESSION_STORE_KEY);
        if (sessionStore == null) {
            return Future.failedFuture(new IllegalStateException(
                "no session store passed on the routing context (session handler has to run before this middleware)"));
        }
        LOGGER.debug("Loaded session store");

        return sessionStore.get(internalSID)
            .compose(session -> {
                session.destroy();
                LOGGER.debug("Destroyed session '{}'", SessionAdapter.displaySessionId(internalSID));
                return Future.succeededFuture(session.id());
            })
            .compose(id -> {
                LOGGER.debug("Removed session '{}' from session store", SessionAdapter.displaySessionId(internalSID));
                return sessionStore.delete(id);
            })
            .compose(v -> {
                LOGGER.debug("Removed SSO session id '{}' from map", SessionAdapter.displaySessionId(ssoSID));
                return sessionIDMap.remove(ssoSID);
            })
            .mapEmpty();
    }

    private JWT initJWT(final List<JsonObject> jwks) {
        final JWT jwt = new JWT();
        boolean added = false;
        for (JsonObject rawJWK : jwks) {
            try {
                final JWK jwk = new JWK(rawJWK);
                jwt.addJWK(jwk);
                added = true;
                LOGGER.debug("Added JWK with: alg '{}', kid '{}'", jwk.getAlgorithm(), jwk.getId());
            } catch (Exception e) {
                LOGGER.warn("Unsupported JWK '{}'", e.getMessage());
            }
        }
        if (!added) {
            LOGGER.warn("No JWK added");
        }
        return jwt;
    }
}
