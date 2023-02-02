/*
 * Copyright 2014 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.web.handler.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.VertxContextPRNG;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import io.vertx.ext.web.impl.Origin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is copied from:
 *
 * @author <a href="http://pmlopes@gmail.com">Paulo Lopes</a>
 * @see OAuth2AuthHandlerImpl
 * <p>
 * The following changes were made:
 * - rename class from OAuth2AuthHandlerImpl to ReplyingPartyHandler
 * - method parseCredentials in line 143: wrap state parameter with StateWithUri
 */
public class RelyingPartyHandler extends HTTPAuthorizationHandler<OAuth2Auth> implements OAuth2AuthHandler {

    private static final Logger LOG = LoggerFactory.getLogger(OAuth2AuthHandlerImpl.class);

    private final VertxContextPRNG prng;
    private final String host;
    private final String callbackPath;
    private final MessageDigest sha256;

    private Route callback;
    private JsonObject extraParams;
    private final List<String> scopes = new ArrayList<>();
    private String prompt;
    private int pkce = -1;

    // explicit signal that tokens are handled as bearer only (meaning, no backend server known)
    private boolean bearerOnly = true;

    public RelyingPartyHandler(Vertx vertx, OAuth2Auth authProvider, String callbackURL) {
        super(authProvider, Type.BEARER);
        // get a reference to the prng
        this.prng = VertxContextPRNG.current(vertx);
        // get a reference to the sha-256 digest
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot get instance of SHA-256 MessageDigest", e);
        }
        // process callback
        if (callbackURL != null) {
            final Origin origin = Origin.parse(callbackURL);
            this.host = origin.toString();
            this.callbackPath = origin.resource();
        }
        else {
            this.host = null;
            this.callbackPath = null;
        }
    }

    @Override
    public void parseCredentials(RoutingContext context, Handler<AsyncResult<Credentials>> handler) {
        // when the handler is working as bearer only, then the `Authorization` header is required
        parseAuthorization(context, !bearerOnly, parseAuthorization -> {
            if (parseAuthorization.failed()) {
                handler.handle(Future.failedFuture(parseAuthorization.cause()));
                return;
            }
            // Authorization header can be null when in not in bearerOnly mode
            final String token = parseAuthorization.result();

            if (token == null) {
                // redirect request to the oauth2 server as we know nothing about this request
                if (bearerOnly || callback == null) {
                    // it's a failure both cases but the cause is not the same
                    handler.handle(Future.failedFuture("callback route is not configured."));
                    return;
                }
                // when this handle is mounted as a catch all, the callback route must be configured before,
                // as it would shade the callback route. When a request matches the callback path and has the
                // method GET the exceptional case should not redirect to the oauth2 server as it would become
                // an infinite redirect loop. In this case an exception must be raised.
                if (context.request().method() == HttpMethod.GET && context.normalizedPath().equals(callback.getPath())) {
                    LOG.warn("The callback route is shaded by the OAuth2AuthHandler, ensure the callback route is added BEFORE the OAuth2AuthHandler route!");
                    handler.handle(Future.failedFuture(new HttpStatusException(500, "Infinite redirect loop [oauth2 callback]")));
                }
                else {
                    if (context.request().method() != HttpMethod.GET) {
                        // we can only redirect GET requests
                        LOG.error("OAuth2 redirect attempt to non GET resource");
                        context.fail(405, new IllegalStateException("OAuth2 redirect attempt to non GET resource"));
                        return;
                    }

                    // the redirect is processed as a failure to abort the chain
                    final String redirectUri = context.request().uri();
                    String state = null;
                    String codeVerifier = null;

                    if (context.session() == null) {
                        if (pkce > 0) {
                            // we can only handle PKCE with a session
                            context.fail(500, new IllegalStateException("OAuth2 PKCE requires a session to be present"));
                            return;
                        }
                    }
                    else {
                        // there's a session we can make this request comply to the Oauth2 spec and add an opaque state
                        context.session()
                                .put("redirect_uri", context.request().uri());

                        // create a state value to mitigate replay attacks
                        state = new StateWithUri(prng.nextString(6), context.request().uri())
                                .toStateParameter();
                        // store the state in the session
                        context.session()
                                .put("state", state);

                        if (pkce > 0) {
                            codeVerifier = prng.nextString(pkce);
                            // store the code verifier in the session
                            context.session()
                                    .put("pkce", codeVerifier);
                        }
                    }
                    handler.handle(Future.failedFuture(new HttpStatusException(302, authURI(redirectUri, state, codeVerifier))));
                }
            }
            else {
                // continue
                handler.handle(Future.succeededFuture(new TokenCredentials(token).setScopes(scopes)));
            }
        });
    }

    private String authURI(String redirectURL, String state, String codeVerifier) {
        final JsonObject config = new JsonObject()
                .put("state", state != null ? state : redirectURL);

        if (host != null) {
            config.put("redirect_uri", host + callback.getPath());
        }

        if (scopes.size() > 0) {
            config.put("scopes", scopes);
        }

        if (prompt != null) {
            config.put("prompt", prompt);
        }

        if (codeVerifier != null) {
            synchronized (sha256) {
                sha256.update(codeVerifier.getBytes(StandardCharsets.US_ASCII));
                config
                        .put("code_challenge", sha256.digest())
                        .put("code_challenge_method", "S256");
            }
        }

        if (extraParams != null) {
            config.mergeIn(extraParams);
        }

        return authProvider.authorizeURL(config);
    }

    @Override
    public OAuth2AuthHandler extraParams(JsonObject extraParams) {
        this.extraParams = extraParams;
        return this;
    }

    @Override
    public OAuth2AuthHandler withScope(String scope) {
        this.scopes.add(scope);
        return this;
    }

    @Override
    public OAuth2AuthHandler prompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    @Override
    public OAuth2AuthHandler pkceVerifierLength(int length) {
        if (length >= 0) {
            // requires verification
            if (length < 43 || length > 128) {
                throw new IllegalArgumentException("Length must be between 34 and 128");
            }
        }
        this.pkce = length;
        return this;
    }

    @Override
    public OAuth2AuthHandler setupCallback(final Route route) {

        if (callbackPath != null && !"".equals(callbackPath)) {
            if (!callbackPath.equals(route.getPath())) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("route path changed to match callback URL");
                }
                // no matter what path was provided we will make sure it is the correct one
                route.path(callbackPath);
            }
        }

        route.method(HttpMethod.GET);

        route.handler(ctx -> {
            // Some IdP's (e.g.: AWS Cognito) returns errors as query arguments
            final String error = ctx.request().getParam("error");

            if (error != null) {
                final int errorCode;
                // standard error's from the Oauth2 RFC
                switch (error) {
                    case "invalid_token":
                        errorCode = 401;
                        break;
                    case "insufficient_scope":
                        errorCode = 403;
                        break;
                    case "invalid_request":
                    default:
                        errorCode = 400;
                        break;
                }

                final String errorDescription = ctx.request().getParam("error_description");
                if (errorDescription != null) {
                    ctx.fail(errorCode, new IllegalStateException(error + ": " + errorDescription));
                }
                else {
                    ctx.fail(errorCode, new IllegalStateException(error));
                }
                return;
            }

            // Handle the callback of the flow
            final String code = ctx.request().getParam("code");

            // code is a require value
            if (code == null) {
                ctx.fail(400, new IllegalStateException("Missing code parameter"));
                return;
            }

            final Oauth2Credentials credentials = new Oauth2Credentials()
                    .setCode(code)
                    .setExtra(extraParams);

            // the state that was passed to the IdP server. The state can be
            // an opaque random string (to protect against replay attacks)
            // or if there was no session available the target resource to
            // server after validation
            final String state = ctx.request().getParam("state");

            // state is a required field
            if (state == null) {
                ctx.fail(400, new IllegalStateException("Missing IdP state parameter to the callback endpoint"));
                return;
            }

            final String resource;

            if (ctx.session() != null) {
                // validate the state. Here we are a bit lenient, if there is no session
                // we always assume valid, however if there is session it must match
                final String ctxState = ctx.session().remove("state");
                // if there's a state in the context they must match
                if (!state.equals(ctxState)) {
                    // forbidden, the state is not valid (this is a replay attack)
                    ctx.fail(401, new IllegalStateException("Invalid oauth2 state"));
                    return;
                }

                // remove the code verifier, from the session as it will be trade for the
                // token during the final leg of the oauth2 handshake
                final String codeVerifier = ctx.session().remove("pkce");
                if (codeVerifier != null) {
                    // if there are already extras by the end user
                    // we make a copy to avoid leaking the verifier
                    final JsonObject extras = credentials.getExtra();
                    if (extras != null) {
                        credentials
                                .setExtra(new JsonObject()
                                        .mergeIn(extras)
                                        .put("code_verifier", codeVerifier));
                    }
                    else {
                        credentials
                                .setExtra(new JsonObject()
                                        .put("code_verifier", codeVerifier));
                    }
                }
                // state is valid, extract the redirectUri from the session
                resource = ctx.session().get("redirect_uri");
            }
            else {
                resource = state;
            }

            if (host == null) {
                // warn that the setup is wrong, if this route is called
                // we most likely needed a host to redirect to
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Cannot compute: 'redirect_uri' variable. OAuth2AuthHandler was created without a origin/callback URL.");
                }
            }
            else {
                // The valid callback URL set in your IdP application settings.
                // This must exactly match the redirect_uri passed to the authorization URL in the previous step.
                credentials.setRedirectUri(host + route.getPath());
            }

            authProvider.authenticate(credentials, res -> {
                if (res.failed()) {
                    ctx.fail(res.cause());
                }
                else {
                    ctx.setUser(res.result());
                    final Session session = ctx.session();
                    final String location = resource != null ? resource : "/";
                    if (session != null) {
                        // the user has upgraded from unauthenticated to authenticated
                        // session should be upgraded as recommended by owasp
                        session.regenerateId();
                    }
                    else {
                        // there is no session object so we cannot keep state.
                        // if there is no session and the resource is relative
                        // we will reroute to "location"
                        if (location.length() != 0 && location.charAt(0) == '/') {
                            ctx.reroute(location);
                            return;
                        }
                    }

                    // we should redirect the UA so this link becomes invalid
                    ctx.response()
                            // disable all caching
                            .putHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                            .putHeader("Pragma", "no-cache")
                            .putHeader(HttpHeaders.EXPIRES, "0")
                            // redirect (when there is no state, redirect to home
                            .putHeader(HttpHeaders.LOCATION, location)
                            .setStatusCode(302)
                            .end("Redirecting to " + location + ".");
                }
            });
        });

        // the redirect handler has been setup so we can process this
        // handler has full oauth2
        bearerOnly = false;
        callback = route;

        return this;
    }
}
