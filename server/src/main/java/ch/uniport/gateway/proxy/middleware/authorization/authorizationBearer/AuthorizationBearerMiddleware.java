package ch.uniport.gateway.proxy.middleware.authorization.authorizationBearer;

import ch.uniport.gateway.proxy.middleware.TraceMiddleware;
import ch.uniport.gateway.proxy.middleware.authorization.shared.tokenLoader.SessionScopeAuthTokenLoader;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the authorization bearer. If the user is authenticated it provides
 * either an ID token or an access token as defined in the sessionScope.
 * Access tokens are only provided if the sessionScope matches the corresponding
 * scope of the OAuth2 provider. It also ensures that no token is sent to the Client.
 */
public class AuthorizationBearerMiddleware extends TraceMiddleware {

    public static final String BEARER = "Bearer ";

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationBearerMiddleware.class);

    private final Vertx vertx;
    private final String sessionScope;

    public AuthorizationBearerMiddleware(Vertx vertx, String name, String sessionScope) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(sessionScope, "sessionScope must not be null");

        this.vertx = vertx;
        this.sessionScope = sessionScope;
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        SessionScopeAuthTokenLoader.load(vertx, ctx.session(), sessionScope)
            .onSuccess(token -> {
                setAuthorizationBearer(ctx.request(), token);
                ctx.addHeadersEndHandler(v -> removeAuthorizationHeader(ctx.response()));
                ctx.next();
            }).onFailure(err -> {
                LOGGER.debug("Providing no token: '{}'", err.getMessage());
                ctx.next();
            });
    }

    private void setAuthorizationBearer(HttpServerRequest request, String token) {
        if (token == null || token.length() == 0) {
            LOGGER.debug("Skipping empty token");
            return;
        }

        LOGGER.debug("Providing token for session scope: '{}'", sessionScope);
        request.headers().add(HttpHeaders.AUTHORIZATION, BEARER + token);
    }

    private void removeAuthorizationHeader(HttpServerResponse response) {
        LOGGER.debug("Removing authorization bearer on response");
        response.headers().remove(HttpHeaders.AUTHORIZATION);
    }

}
