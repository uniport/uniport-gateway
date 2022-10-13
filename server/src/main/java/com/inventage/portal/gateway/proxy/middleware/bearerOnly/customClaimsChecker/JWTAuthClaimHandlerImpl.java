package com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker;


import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.impl.HttpStatusException;

/**
 * In order for our custom jwt claim check to be invoked, we copied and modified some classes of the vertx library.
 * This class is a copy of its superclass, with the difference that we extend from our custom HTTPAuthHandler
 */
public class JWTAuthClaimHandlerImpl extends HTTPAuthClaimHandler<JWTAuth> implements JWTAuthHandler {


    public JWTAuthClaimHandlerImpl(JWTAuth authProvider) {
        super(authProvider, Type.BEARER);
    }

    @Override
    public void parseCredentials(RoutingContext context, Handler<AsyncResult<Credentials>> handler) {

        parseAuthorization(context, false, parseAuthorization -> {
            if (parseAuthorization.failed()) {
                handler.handle(Future.failedFuture(parseAuthorization.cause()));
                return;
            }

            handler.handle(Future.succeededFuture(new TokenCredentials(parseAuthorization.result())));
        });
    }

    //We override the post authentication because the handle method in the
    //super class calls our authenticate method in the JWTAuthClaimProviderImpl
    // only if the user is not null.    @Override
    public void postAuthentication(RoutingContext ctx) {
        final HttpServerRequest request = ctx.request();
        final boolean parseEnded = request.isEnded();
        if (!parseEnded) {
            request.pause();
        }
        // parse the request in order to extract the credentials object
        parseCredentials(ctx, res -> {
            if (res.failed()) {
                resume(request, parseEnded);
                processException(ctx, res.cause());
                return;
            }
            // proceed to authN
            getAuthProvider(ctx).authenticate(res.result(), authN -> {
                if (authN.succeeded()) {
                    final User authenticated = authN.result();
                    ctx.setUser(authenticated);
                    final Session session = ctx.session();
                    if (session != null) {
                        // the user has upgraded from unauthenticated to authenticated
                        // session should be upgraded as recommended by owasp
                        session.regenerateId();
                    }
                    // proceed with the router
                    resume(request, parseEnded);
                    super.postAuthentication(ctx);
                }
                else {
                    final String header = authenticateHeader(ctx);
                    if (header != null) {
                        ctx.response()
                            .putHeader("WWW-Authenticate", header);
                    }
                    // to allow further processing if needed
                    resume(request, parseEnded);
                    final Throwable cause = authN.cause();
                    processException(ctx, cause instanceof HttpStatusException ? cause : new HttpStatusException(401, cause));
                }
            });
        });
    }

    private void resume(HttpServerRequest request, boolean parseEnded) {
        // resume as the error handler may allow this request to become valid again
        if (!parseEnded && !request.headers().contains(HttpHeaders.UPGRADE, HttpHeaders.WEBSOCKET, true)) {
            request.resume();
        }
    }
}
