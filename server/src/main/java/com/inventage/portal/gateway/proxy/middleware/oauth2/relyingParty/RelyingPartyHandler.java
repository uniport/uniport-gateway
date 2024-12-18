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

package com.inventage.portal.gateway.proxy.middleware.oauth2.relyingParty;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.VertxContextPRNG;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2AuthorizationURL;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import io.vertx.ext.web.handler.impl.HTTPAuthorizationHandler;
import io.vertx.ext.web.handler.impl.OAuth2AuthHandlerImpl;
import io.vertx.ext.web.handler.impl.ScopedAuthentication;
import io.vertx.ext.web.impl.Origin;
import io.vertx.ext.web.impl.ParsableMIMEValue;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is copied from:
 * https://github.com/vert-x3/vertx-web/blob/4.5.8/vertx-web/src/main/java/io/vertx/ext/web/handler/impl/OAuth2AuthHandlerImpl.java
 *
 * The following changes were made:
 * - rename class from OAuth2AuthHandlerImpl to RelyingPartyHandler
 * - wrap oauth2 state parameter with StateWithUri
 * - dont implement OrderListener interface (mount callback immediately)
 * - set prompt=none if accept headers do not allow text/html
 * - non-interactive requests are not redirected, but served a 401 directly
 * 
 * @see OAuth2AuthHandlerImpl
 */
public class RelyingPartyHandler extends HTTPAuthorizationHandler<OAuth2Auth> implements OAuth2AuthHandler, ScopedAuthentication<OAuth2AuthHandler> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelyingPartyHandler.class);
    private static final Set<String> OPENID_SCOPES = new HashSet<>();

    static {
        OPENID_SCOPES.add("openid");
        OPENID_SCOPES.add("profile");
        OPENID_SCOPES.add("email");
        OPENID_SCOPES.add("phone");
        OPENID_SCOPES.add("offline");
    }

    private final VertxContextPRNG prng;
    private final Origin callbackURL;
    private final MessageDigest sha256;

    private final List<String> scopes;
    private final boolean openId;
    private Map<String, String> extraParams;
    private Set<String> passthroughParams;
    private String prompt;
    private int pkce = -1;
    // explicit signal that tokens are handled as bearer only (meaning, no backend server known)
    private boolean bearerOnly = true;

    private int order = -1;
    private Route callback;

    public RelyingPartyHandler(Vertx vertx, OAuth2Auth authProvider, String callbackURL) {
        this(vertx, authProvider, callbackURL, null);
    }

    public RelyingPartyHandler(Vertx vertx, OAuth2Auth authProvider, String callbackURL, String realm) {
        super(authProvider, Type.BEARER, realm);
        // get a reference to the prng
        this.prng = VertxContextPRNG.current(vertx);
        // get a reference to the sha-256 digest
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot get instance of SHA-256 MessageDigest", e);
        }
        // process callback
        if (callbackURL != null) {
            this.callbackURL = Origin.parse(callbackURL);
        } else {
            this.callbackURL = null;
        }
        // scopes are empty by default
        this.scopes = new ArrayList<>();
        this.openId = false;
    }

    private RelyingPartyHandler(RelyingPartyHandler base, List<String> scopes) {
        super(base.authProvider, Type.BEARER, base.realm);
        this.prng = base.prng;
        this.callbackURL = base.callbackURL;
        this.prompt = base.prompt;
        this.pkce = base.pkce;
        this.bearerOnly = base.bearerOnly;

        // get a new reference to the sha-256 digest
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot get instance of SHA-256 MessageDigest", e);
        }
        // state copy
        if (base.extraParams != null) {
            extraParams = new HashMap<String, String>(base.extraParams);
        }
        if (base.passthroughParams != null) {
            passthroughParams = new HashSet<String>(base.passthroughParams);
        }
        this.callback = base.callback;
        this.order = base.order;
        // apply the new scopes
        this.scopes = scopes;
        this.openId = scopes != null && scopes.contains("openid");
    }

    @Override
    public void authenticate(RoutingContext context, Handler<AsyncResult<User>> handler) {
        // when the handler is working as bearer only, then the `Authorization` header is required
        parseAuthorization(context, !bearerOnly, parseAuthorization -> {
            if (parseAuthorization.failed()) {
                handler.handle(Future.failedFuture(parseAuthorization.cause()));
                return;
            }
            // Authorization header can be null when in not in bearerOnly mode
            final String token = parseAuthorization.result();

            if (token == null) {
                if (isNoneInteractive(context.request())) {
                    // in case the request is non-interactive, redirecting serves no purpose
                    context.fail(401, new VertxException("OAuth2 No valid authentication in non-interactive request. Access Denied.", true));
                    return;
                }

                // redirect request to the oauth2 server as we know nothing about this request
                if (bearerOnly) {
                    // it's a failure both cases but the cause is not the same
                    handler.handle(Future.failedFuture("callback route is not configured."));
                    return;
                }
                // when this handle is mounted as a catch all, the callback route must be configured before,
                // as it would shade the callback route. When a request matches the callback path and has the
                // method GET the exceptional case should not redirect to the oauth2 server as it would become
                // an infinite redirect loop. In this case an exception must be raised.
                if (context.request().method() == HttpMethod.GET && context.normalizedPath().equals(callbackURL.resource())) {
                    LOGGER.warn("The callback route is shaded by the OAuth2AuthHandler, ensure the callback route is added BEFORE the OAuth2AuthHandler route!");
                    handler.handle(Future.failedFuture(new HttpException(500, "Infinite redirect loop [oauth2 callback]")));
                } else {
                    if (context.request().method() != HttpMethod.GET) {
                        // we can only redirect GET requests
                        LOGGER.error("OAuth2 redirect attempt to non GET resource '{}'", context.request().uri());
                        context.fail(405, new VertxException("OAuth2 redirect attempt to non GET resource", true));
                        return;
                    }

                    // the redirect is processed as a failure to abort the chain
                    final String redirectUri = context.request().uri();
                    String state = null;
                    String codeVerifier = null;

                    final Session session = context.session();

                    if (session == null) {
                        if (pkce > 0) {
                            // we can only handle PKCE with a session
                            context.fail(500, new VertxException("OAuth2 PKCE requires a session to be present", true));
                            return;
                        }
                    } else {
                        // there's a session we can make this request comply to the Oauth2 spec and add an opaque state
                        session.put("redirect_uri", context.request().uri());

                        // create a state value to mitigate replay attacks
                        state = new StateWithUri(prng.nextString(6), context.request().uri(), context.request().method())
                            .toStateParameter();
                        // store the state in the session
                        session.put("state", state);

                        if (pkce > 0) {
                            codeVerifier = prng.nextString(pkce);
                            // store the code verifier in the session
                            session.put("pkce", codeVerifier);
                        }
                    }
                    handler.handle(Future.failedFuture(new HttpException(302, authURI(redirectUri, state, codeVerifier, context.request()))));
                }
            } else {
                // continue
                final Credentials credentials = scopes.size() > 0 ? new TokenCredentials(token).setScopes(scopes) : new TokenCredentials(token);

                authProvider.authenticate(credentials, authn -> {
                    if (authn.failed()) {
                        handler.handle(Future.failedFuture(new HttpException(401, authn.cause())));
                    } else {
                        handler.handle(authn);
                    }
                });
            }
        });
    }

    private String authURI(String redirectURL, String state, String codeVerifier, HttpServerRequest request) {
        final OAuth2AuthorizationURL config = new OAuth2AuthorizationURL();

        if (extraParams != null) {
            for (Entry<String, String> kv : extraParams.entrySet()) {
                config.putAdditionalParameter(kv.getKey(), kv.getValue());
            }
        }

        if (passthroughParams != null) {
            final Map<String, List<String>> queryParams = getQueryParameters(redirectURL);
            for (Map.Entry<String, List<String>> query : queryParams.entrySet()) {
                if (passthroughParams.contains(query.getKey())) {
                    if (query.getValue().size() == 0) {
                        // should not happen
                        continue;
                    }
                    final String v = query.getValue().get(0);
                    config.putAdditionalParameter(query.getKey(), v);

                    if (query.getValue().size() > 1) {
                        // this is due to a limitation in the vert.x API
                        LOGGER.warn("Query parameter '{}' is supplied multiple times. Will use the first '{}' and ignore the rest", query.getKey(), v);
                    }
                }
            }
        }

        String promptOverride = null;
        if (!acceptsTextHTML(request)) {
            // PORTAL-2004: change response_mode to `query` if `Accept:` header is not `text/html`, 
            // because JS code doesn't execute `onload="javascript:document.forms[0].submit()"` trigger
            config.putAdditionalParameter("response_mode", "query");
            // in case the request does not accept any html response, set prompt=none
            promptOverride = "none";
        }

        config.setState(state != null ? state : redirectURL);

        if (callbackURL != null) {
            config.setRedirectUri(callbackURL.href());
        }

        if (scopes.size() > 0) {
            config.setScopes(scopes);
        }

        if (promptOverride != null) {
            config.putAdditionalParameter("prompt", promptOverride);
        } else if (prompt != null) {
            config.putAdditionalParameter("prompt", prompt);
        }

        if (codeVerifier != null) {
            synchronized (sha256) {
                final byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
                sha256.update(bytes);
                final byte[] digest = sha256.digest();
                config
                    .putAdditionalParameter("code_challenge", Base64.getUrlEncoder().withoutPadding().encodeToString(digest))
                    .putAdditionalParameter("code_challenge_method", "S256");
            }
        }

        return authProvider.authorizeURL(config);
    }

    private boolean isNoneInteractive(HttpServerRequest request) {
        return acceptsApplicationJson(request);
    }

    private boolean acceptsTextHTML(HttpServerRequest request) {
        return acceptsMimeType(request, HttpHeaderValues.TEXT_HTML.toString());
    }

    private boolean acceptsApplicationJson(HttpServerRequest request) {
        return acceptsMimeType(request, HttpHeaderValues.APPLICATION_JSON.toString());
    }

    private boolean acceptsMimeType(HttpServerRequest request, String mimeTypePattern) {
        final List<String> acceptValues = request.headers().getAll(HttpHeaders.ACCEPT);
        if (acceptValues == null) {
            return false;
        }
        // Iterate over multiple Accept headers, i.e.
        // Accept: application/json
        // Accept: text/plain
        for (String mimeTypes : acceptValues) {
            // Iterate over multiple mimetypes in a single header, i.e.
            // Accept: application/json, text/plain, */*
            for (String mimeType : mimeTypes.split(",")) {
                if (matchesMimeType(mimeTypePattern, mimeType)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesMimeType(String mimeTypePattern, String mimeType) {
        return new ParsableMIMEValue(mimeTypePattern.trim()).isMatchedBy(
            new ParsableMIMEValue(mimeType.trim()));
    }

    // See https://github.com/vert-x3/vertx-web/issues/2602
    @Override
    public OAuth2AuthHandler extraParams(JsonObject extraParams) {
        this.extraParams = new HashMap<String, String>();
        for (Entry<String, Object> kv : extraParams.getMap().entrySet()) {
            final Object v = kv.getValue();
            if (!(v instanceof String)) {
                throw new IllegalArgumentException(String.format("value for extra param '%s' must be a string", kv.getKey()));
            }
            this.extraParams.put(kv.getKey(), (String) v);
        }
        return this;
    }

    // NOTE: this can be removed once issue #2602 has been resolved
    public OAuth2AuthHandler extraParams(Map<String, String> extraParams) {
        this.extraParams = new HashMap<String, String>(extraParams);
        return this;
    }

    public RelyingPartyHandler passthroughParameters(List<String> passthroughParams) {
        this.passthroughParams = new HashSet<String>(passthroughParams);
        return this;
    }

    @Override
    public OAuth2AuthHandler withScope(String scope) {
        final List<String> updatedScopes = new ArrayList<>(this.scopes);
        updatedScopes.add(scope);
        return new RelyingPartyHandler(this, updatedScopes);
    }

    @Override
    public OAuth2AuthHandler withScopes(List<String> scopes) {
        return new RelyingPartyHandler(this, scopes);
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
                throw new IllegalArgumentException("Length must be between 43 and 128");
            }
        }
        this.pkce = length;
        return this;
    }

    public RelyingPartyHandler setupCallbacks(final List<Route> routes) {
        for (Route route : routes) {
            if (route != null) {
                this.setupCallback(route);
            }
        }
        return this;
    }

    @Override
    public OAuth2AuthHandler setupCallback(final Route route) {
        if (callbackURL == null) {
            // warn that the setup is probably wrong
            throw new IllegalStateException("OAuth2AuthHandler was created without a origin/callback URL");
        }

        final String routePath = route.getPath();

        if (routePath == null) {
            // warn that the setup is probably wrong
            throw new IllegalStateException("OAuth2AuthHandler callback route created without a path");
        }

        final String callbackPath = callbackURL.resource();

        if (callbackPath != null && !"".equals(callbackPath)) {
            if (!callbackPath.endsWith(routePath)) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("callback route doesn't match OAuth2AuthHandler origin configuration");
                }
            }
        }

        this.callback = route;
        mountCallback();

        // the redirect handler has been setup so we can process this
        // handler has full oauth2 support, not just basic JWT
        bearerOnly = false;
        return this;
    }

    /**
     * The default behavior for post-authentication
     */
    @Override
    public void postAuthentication(RoutingContext ctx) {
        // the user is authenticated, however the user may not have all the required scopes
        if (scopes != null && scopes.size() > 0) {
            final User user = ctx.user();
            if (user == null) {
                // bad state
                ctx.fail(403, new VertxException("no user in the context", true));
                return;
            }

            if (user.principal().containsKey("scope")) {
                final String handlerScopes = user.principal().getString("scope");
                LOGGER.debug("handlerScopes is " + handlerScopes);
                if (handlerScopes != null) {
                    // user principal contains scope, a basic assertion is required to ensure that
                    // the scopes present match the required ones
                    for (String scope : this.scopes) {
                        // do not assert openid scopes if openid is active
                        if (openId && OPENID_SCOPES.contains(scope)) {
                            continue;
                        }

                        final int idx = handlerScopes.indexOf(scope);
                        if (idx != -1) {
                            // match, but is it valid?
                            if ((idx != 0 && handlerScopes.charAt(idx - 1) != ' ') ||
                                (idx + scope.length() != handlerScopes.length()
                                    && handlerScopes.charAt(idx + scope.length()) != ' ')) {
                                // invalid scope assignment
                                ctx.fail(403, new VertxException("principal scope != handler scopes. principal scope is " + scope, true));
                                return;
                            }
                        } else {
                            // invalid scope assignment
                            ctx.fail(403, new VertxException("principal scope != handler scopes. principal scope is " + scope, true));
                            return;
                        }
                    }
                }
            }
        }
        ctx.next();
    }

    @Override
    public boolean performsRedirect() {
        // depending on the time this method is invoked
        // we can deduct with more accuracy if a redirect is possible or not
        if (!bearerOnly) {
            // we know that a redirect is definitely possible
            // as the callback handler has been created
            return true;
        } else {
            // the callback hasn't been mounted so we need to assume
            // that if no callbackURL is provided, then there isn't
            // a redirect happening in this application
            return callbackURL != null;
        }
    }

    private void mountCallback() {
        this.callback.handler(ctx -> {
            // Some IdP's (e.g.: AWS Cognito) returns errors as query arguments
            final String error = ctx.request().getParam("error");

            if (error != null) {
                final int errorCode;
                // standard error's from the Oauth2 RFC
                switch (error) {
                    case "invalid_token", "login_required":
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
                    ctx.fail(errorCode, new VertxException(error + ": " + errorDescription, true));
                } else {
                    ctx.fail(errorCode, new VertxException(error, true));
                }
                return;
            }

            // Handle the callback of the flow
            final String code = ctx.request().getParam("code");

            // code is a require value
            if (code == null) {
                ctx.fail(400, new VertxException("Missing code parameter", true));
                return;
            }

            final Oauth2Credentials credentials = new Oauth2Credentials()
                .setFlow(OAuth2FlowType.AUTH_CODE)
                .setCode(code);

            // the state that was passed to the IdP server. The state can be
            // an opaque random string (to protect against replay attacks)
            // or if there was no session available the target resource to
            // server after validation
            final String state = ctx.request().getParam("state");

            // state is a required field
            if (state == null) {
                ctx.fail(400, new VertxException("Missing IdP state parameter to the callback endpoint", true));
                return;
            }

            final String resource;
            final Session session = ctx.session();

            if (session != null) {
                // validate the state. Here we are a bit lenient, if there is no session
                // we always assume valid, however if there is session it must match
                final String ctxState = session.remove("state");
                // if there's a state in the context they must match
                if (!state.equals(ctxState)) {
                    // forbidden, the state is not valid (this is a replay attack)
                    ctx.fail(401, new VertxException("Invalid oauth2 state", true));
                    return;
                }

                // remove the code verifier, from the session as it will be trade for the
                // token during the final leg of the oauth2 handshake
                final String codeVerifier = session.remove("pkce");
                credentials.setCodeVerifier(codeVerifier);
                // state is valid, extract the redirectUri from the session
                resource = session.get("redirect_uri");
            } else {
                resource = state;
            }

            // The valid callback URL set in your IdP application settings.
            // This must exactly match the redirect_uri passed to the authorization URL in the previous step.
            credentials.setRedirectUri(callbackURL.href());

            authProvider.authenticate(credentials, res -> {
                if (res.failed()) {
                    ctx.fail(res.cause());
                } else {
                    ctx.setUser(res.result());
                    final String location = resource != null ? resource : "/";
                    if (session != null) {
                        // the user has upgraded from unauthenticated to authenticated
                        // session should be upgraded as recommended by owasp
                        session.regenerateId();
                    } else {
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
    }

    private Map<String, List<String>> getQueryParameters(String url) {
        return new QueryStringDecoder(url).parameters();
    }
}
