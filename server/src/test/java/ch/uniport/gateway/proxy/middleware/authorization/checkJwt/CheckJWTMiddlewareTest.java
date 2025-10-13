package ch.uniport.gateway.proxy.middleware.authorization.checkJwt;

import static ch.uniport.gateway.proxy.middleware.MiddlewareServerBuilder.uniportGateway;
import static ch.uniport.gateway.proxy.middleware.VertxAssertions.assertEquals;
import static ch.uniport.gateway.proxy.middleware.VertxAssertions.assertNotNull;
import static ch.uniport.gateway.proxy.middleware.VertxAssertions.assertTrue;
import static io.vertx.core.http.HttpMethod.GET;

import ch.uniport.gateway.proxy.middleware.authorization.MockOAuth2Auth;
import ch.uniport.gateway.proxy.middleware.authorization.shared.customIssuerChecker.JWTAuthMultipleIssuersOptions;
import ch.uniport.gateway.proxy.middleware.authorization.shared.customIssuerChecker.JWTAuthMultipleIssuersProvider;
import ch.uniport.gateway.proxy.middleware.authorization.shared.tokenLoader.JWTAuthTokenLoadHandler;
import ch.uniport.gateway.proxy.middleware.authorization.shared.tokenLoader.TokenSource;
import ch.uniport.gateway.proxy.middleware.mock.TestBearerOnlyJWTProvider;
import ch.uniport.gateway.proxy.middleware.oauth2.AuthenticationUserContext;
import com.google.common.io.Resources;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jakarta.json.Json;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class CheckJWTMiddlewareTest {

    private static final String HOST = "localhost";
    private static final String PUBLIC_KEY_PATH = "FOR_DEVELOPMENT_PURPOSE_ONLY-publicKey.pem";
    private static final String PUBLIC_KEY_ALGORITHM = "RS256";
    private static final jakarta.json.JsonObject VALID_PAYLOAD_TEMPLATE = Json.createObjectBuilder()
        .add("typ", "Bearer")
        .add("exp", 1893452400)
        .add("iat", 1627053747)
        .add("iss", "http://test.issuer:1234/auth/realms/test")
        .add("azp", "test-authorized-parties")
        .add("aud", "test-audience")
        .add("scope", "openid email profile Test")
        .build();

    @Test
    public void shouldReadFromSessionScopeTokenSource(Vertx vertx, VertxTestContext testCtx) {
        // given
        final String sessionScope = "someScope";
        final String sessionJwt = "some.session.jwt";
        final String bearerJwt = "some.header.jwt";

        // mock OAuth2 authentication
        final Handler<RoutingContext> injectTokenHandler = ctx -> {
            authContext(sessionJwt).toSessionAtScope(ctx.session(), sessionScope);
            ctx.next();
        };

        // mock authR handler
        final AuthenticationHandler authHandler = ctx -> {
            final TokenSource source = ctx.get(JWTAuthTokenLoadHandler.TOKEN_SOURCE_KEY);
            final String scope = ctx.get(JWTAuthTokenLoadHandler.SESSION_SCOPE_KEY);

            // then
            assertNotNull(testCtx, source, "should contain token source");
            assertEquals(testCtx, source, TokenSource.SESSION_SCOPE, "should be equal");
            assertTrue(testCtx, scope.length() > 0, "should contain session scope");
            assertEquals(testCtx, scope, sessionScope, "should be equal");

            ctx.next();
        };

        uniportGateway(vertx, HOST, testCtx)
            .withSessionMiddleware()
            .withMiddleware(injectTokenHandler)
            .withCheckJwtMiddleware(sessionScope, authHandler)
            .build(ctx -> {
                // then
                assertEquals(testCtx, ctx.request().headers().get(HttpHeaders.AUTHORIZATION), bearer(bearerJwt), "should contain the original request auth header");
                ctx.response().setStatusCode(200).end("ok");
            })
            .start()
            // when
            .incomingRequest(GET, "/",
                new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(bearerJwt)),
                (resp) -> {
                    // then
                    assertEquals(testCtx, 200, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    @Test
    public void shouldValidateSessionJwt(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String sessionScope = "someScope";
        final String validToken = TestBearerOnlyJWTProvider.signToken(VALID_PAYLOAD_TEMPLATE);

        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("test-audience");

        // mock OAuth2 authentication
        final Handler<RoutingContext> injectTokenHandler = ctx -> {
            authContext(validToken).toSessionAtScope(ctx.session(), sessionScope);
            ctx.next();
        };

        uniportGateway(vertx, HOST, testCtx)
            .withSessionMiddleware()
            .withMiddleware(injectTokenHandler)
            .withCheckJwtMiddleware(sessionScope, jwtAuth(vertx, expectedIssuer, expectedAudience))
            .build()
            .start()
            // when
            .incomingRequest(GET, "/",
                (resp) -> {
                    // then
                    assertEquals(testCtx, 200, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    private AuthenticationUserContext authContext(String jwt) {
        final JsonObject principal = JsonObject.of("access_token", jwt);
        final OAuth2Auth authProvider = new MockOAuth2Auth(principal);
        final User user = MockOAuth2Auth.createUser(principal);
        return AuthenticationUserContext.of(authProvider, user);
    }

    private JWTAuth jwtAuth(Vertx vertx, String expectedIssuer, List<String> expectedAudience) {
        return jwtAuthWithAdditionalIssuers(vertx, expectedIssuer, expectedAudience, new JsonArray());
    }

    private JWTAuth jwtAuthWithAdditionalIssuers(Vertx vertx, String expectedIssuer, List<String> expectedAudience, JsonArray additionalIssuers) {
        String publicKeyRS256 = null;
        try {
            publicKeyRS256 = Resources.toString(Resources.getResource(PUBLIC_KEY_PATH), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return JWTAuthMultipleIssuersProvider.create(vertx,
            new JWTAuthOptions()
                .addPubSecKey(new PubSecKeyOptions().setAlgorithm(PUBLIC_KEY_ALGORITHM).setBuffer(publicKeyRS256))
                .setJWTOptions(new JWTOptions().setIssuer(expectedIssuer).setAudience(expectedAudience)),
            new JWTAuthMultipleIssuersOptions().setAdditionalIssuers(additionalIssuers));
    }

    private String bearer(String value) {
        return "Bearer " + value;
    }
}
