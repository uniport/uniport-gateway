package ch.uniport.gateway.proxy.middleware.authorization.passAuthorization;

import ch.uniport.gateway.proxy.middleware.TraceMiddleware;
import ch.uniport.gateway.proxy.middleware.authorization.shared.tokenLoader.SessionScopeAuthTokenLoader;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks the internal token to make sure the user is allowed to access the
 * backend.
 * It then adds the token that came with the original request to the backend.
 */
public class PassAuthorizationMiddleware extends TraceMiddleware {

    public static final String BEARER = "Bearer ";

    private static final Logger LOGGER = LoggerFactory.getLogger(PassAuthorizationMiddleware.class);

    private final Vertx vertx;
    private final String name;
    private final String sessionScope;
    private final AuthenticationHandler authHandler;

    public PassAuthorizationMiddleware(
        Vertx vertx, String name, String sessionScope,
        AuthenticationHandler authHandler
    ) {
        Objects.requireNonNull(vertx, "vertx must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(sessionScope, "sessionScope must not be null");
        Objects.requireNonNull(authHandler, "authHandler must not be null");

        this.vertx = vertx;
        this.name = name;
        this.sessionScope = sessionScope;
        this.authHandler = authHandler;
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        SessionScopeAuthTokenLoader.load(vertx, ctx.session(), sessionScope)
            .onSuccess(token -> {
                LOGGER.debug("authToken: " + token);

                final String incomingAuthHeader = getAndReplaceAuthHeader(ctx, BEARER + token);

                LOGGER.debug("incomingAuthHeader: " + incomingAuthHeader);

                LOGGER.debug("Handling jwt auth request");
                authHandler.handle(ctx);
                LOGGER.debug("Handled jwt auth request");

                getAndReplaceAuthHeader(ctx, incomingAuthHeader);

                ctx.next();
            }).onFailure(err -> {
                LOGGER.debug("Failed to get token '{}'", err.getMessage());
                ctx.fail(401, err);
            });
    }

    private String getAndReplaceAuthHeader(RoutingContext ctx, String newHeader) {
        final String originalHeader = ctx.request().getHeader(HttpHeaders.AUTHORIZATION);

        ctx.request().headers().remove(HttpHeaders.AUTHORIZATION);
        if (newHeader != null) {
            ctx.request().headers().add(HttpHeaders.AUTHORIZATION, newHeader);
        }

        return originalHeader;
    }

}
