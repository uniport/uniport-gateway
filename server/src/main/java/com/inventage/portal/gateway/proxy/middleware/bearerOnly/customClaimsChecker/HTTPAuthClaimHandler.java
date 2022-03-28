package com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.AuthenticationHandlerImpl;
import io.vertx.ext.web.handler.impl.HttpStatusException;

/**
 In order for our custom jwt claim check to be invoked, we copied and modified some classes of the vertx library.
 This class is a copy of its superclass, except that its constructor is public.

 HTTPAuthorizationHandler's constructor is not public, hence we copy the class and make our version's constructor public.
 Required for our custom JWTClaimHandlerImpl implementation
 */
public abstract class HTTPAuthClaimHandler<T extends AuthenticationProvider> extends AuthenticationHandlerImpl<T> {

    static final HttpStatusException UNAUTHORIZED = new HttpStatusException(401);
    static final HttpStatusException BAD_REQUEST = new HttpStatusException(400);
    static final HttpStatusException BAD_METHOD = new HttpStatusException(405);
    // this should match the IANA registry: https://www.iana.org/assignments/http-authschemes/http-authschemes.xhtml
    enum Type {
        BASIC("Basic"),
        DIGEST("Digest"),
        BEARER("Bearer"),
        // these have no known implementation
        HOBA("HOBA"),
        MUTUAL("Mutual"),
        NEGOTIATE("Negotiate"),
        OAUTH("OAuth"),
        SCRAM_SHA_1("SCRAM-SHA-1"),
        SCRAM_SHA_256("SCRAM-SHA-256");

        private final String label;

        Type(String label) {
            this.label = label;
        }

        public boolean is(String other) {
            return label.equalsIgnoreCase(other);
        }

        @Override
        public String toString() {
            return label;
        }
    }

    protected final HTTPAuthClaimHandler.Type type;

    HTTPAuthClaimHandler(T authProvider, HTTPAuthClaimHandler.Type type) {
        super(authProvider);
        this.type = type;
    }

    HTTPAuthClaimHandler(T authProvider, String realm, HTTPAuthClaimHandler.Type type) {
        super(authProvider, realm);
        this.type = type;
    }

    protected final void parseAuthorization(RoutingContext ctx, boolean optional, Handler<AsyncResult<String>> handler) {

        final HttpServerRequest request = ctx.request();
        final String authorization = request.headers().get(HttpHeaders.AUTHORIZATION);

        if (authorization == null) {
            if (optional) {
                // this is allowed
                handler.handle(Future.succeededFuture());
            } else {
                handler.handle(Future.failedFuture(UNAUTHORIZED));
            }
            return;
        }

        try {
            int idx = authorization.indexOf(' ');

            if (idx <= 0) {
                handler.handle(Future.failedFuture(BAD_REQUEST));
                return;
            }

            if (!type.is(authorization.substring(0, idx))) {
                handler.handle(Future.failedFuture(UNAUTHORIZED));
                return;
            }

            handler.handle(Future.succeededFuture(authorization.substring(idx + 1)));
        } catch (RuntimeException e) {
            handler.handle(Future.failedFuture(e));
        }
    }

    @Override
    public String authenticateHeader(RoutingContext context) {
        if (realm != null && realm.length() > 0) {
            return type + " realm=\"" +realm + "\"";
        }
        return null;
    }

}

