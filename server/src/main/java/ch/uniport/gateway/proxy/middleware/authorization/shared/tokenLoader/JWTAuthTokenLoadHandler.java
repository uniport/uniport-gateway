package ch.uniport.gateway.proxy.middleware.authorization.shared.tokenLoader;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.impl.JWTAuthHandlerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JWTAuthTokenLoadHandler extends JWTAuthHandlerImpl {

    public static final String TOKEN_SOURCE_KEY = "uniport.jwtauth.token-source";
    public static final String SESSION_SCOPE_KEY = "uniport.jwtauth.session-scope";

    static final String TOKEN_SOURCE_HEADER = "header";
    static final String TOKEN_SOURCE_SESSION_SCOPE = "sessionScope";

    private static final Logger LOGGER = LoggerFactory.getLogger(JWTAuthTokenLoadHandler.class);

    private final Vertx vertx;

    public JWTAuthTokenLoadHandler(
        Vertx vertx, JWTAuth authProvider
    ) {
        super(authProvider, null);
        this.vertx = vertx;
    }

    @Override
    public void authenticate(RoutingContext context, Handler<AsyncResult<User>> handler) {
        final TokenSource tokenSource = context.get(TOKEN_SOURCE_KEY, TokenSource.HEADER);

        LOGGER.info("Loading token from source '{}'", tokenSource);
        switch (tokenSource) {
            case HEADER:
                super.authenticate(context, handler);
                break;
            case SESSION_SCOPE:
                final String sessionScope = context.get(SESSION_SCOPE_KEY);
                if (sessionScope == null) {
                    LOGGER.error("session scope missing");
                    break;
                }
                LOGGER.info("Loading token from sessionScope '{}'", sessionScope);
                validateAuthTokenInSession(vertx, context.session(), sessionScope, handler);
                break;
            default:
                handler.handle(Future.failedFuture("invalid token source"));
                break;
        }
    }

    /**
     * This is mostly the same as JWTAuthHandlerImpl.authenticate with the difference of the token source.
     * In JWTAuthHandlerImpl the token source is always the header, here we make this configurable.
     * Unfortunately, the HTTPAuthorizationHandler.parseAuthorization is final so we cant overwrite but have to copy.
     * 
     * https://github.com/vert-x3/vertx-web/blob/4.5.20/vertx-web/src/main/java/io/vertx/ext/web/handler/impl/JWTAuthHandlerImpl.java#L60-L91
     * 
     * @param vert
     * @param session
     * @param sessionScope
     * @param handler
     */
    private void validateAuthTokenInSession(Vertx vert, Session session, String sessionScope, Handler<AsyncResult<User>> handler) {
        SessionScopeAuthTokenLoader.load(vertx, session, sessionScope, loadToken -> {
            if (loadToken.failed()) {
                handler.handle(Future.failedFuture(loadToken.cause()));
                return;
            }

            final String token = loadToken.result();
            int segments = 0;
            for (int i = 0; i < token.length(); i++) {
                final char c = token.charAt(i);
                if (c == '.') {
                    if (++segments == 3) {
                        handler.handle(Future.failedFuture(new HttpException(400, "Too many segments in token")));
                        return;
                    }
                    continue;
                }
                if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                    continue;
                }
                // invalid character
                handler.handle(Future.failedFuture(new HttpException(400, "Invalid character in token: " + (int) c)));
                return;
            }

            authProvider.authenticate(new TokenCredentials(token), authn -> {
                if (authn.failed()) {
                    handler.handle(Future.failedFuture(new HttpException(401, authn.cause())));
                } else {
                    handler.handle(authn);
                }
            });
        });
    }
}
