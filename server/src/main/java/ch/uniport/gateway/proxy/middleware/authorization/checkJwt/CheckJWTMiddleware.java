package ch.uniport.gateway.proxy.middleware.authorization.checkJwt;

import ch.uniport.gateway.proxy.middleware.TraceMiddleware;
import ch.uniport.gateway.proxy.middleware.authorization.shared.tokenLoader.JWTAuthTokenLoadHandler;
import ch.uniport.gateway.proxy.middleware.authorization.shared.tokenLoader.TokenSource;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The CheckJWT middleware checks the JWT in the configured session scope.
 * <p>
 * The token has to be a valid JWT matching its signature, expiration, audience
 * and issuer with the properties configured for this middleware.
 * <p>
 * If no valid token is provided, then a '401 Unauthorized' is returned.
 * Otherwise, the request is forwarded.
 */
public class CheckJWTMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckJWTMiddleware.class);

    private final AuthenticationHandler authHandler;

    private final String name;
    private final String sessionScope;

    public CheckJWTMiddleware(Vertx vertx, String name, String sessionScope, AuthenticationHandler authHandler) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(sessionScope, "sessionScope must not be null");
        Objects.requireNonNull(authHandler, "authHandler must not be null");

        this.name = name;
        this.sessionScope = sessionScope;
        this.authHandler = authHandler;
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        ctx.put(JWTAuthTokenLoadHandler.TOKEN_SOURCE_KEY, TokenSource.SESSION_SCOPE);
        ctx.put(JWTAuthTokenLoadHandler.SESSION_SCOPE_KEY, sessionScope);
        authHandler.handle(ctx);
    }
}
