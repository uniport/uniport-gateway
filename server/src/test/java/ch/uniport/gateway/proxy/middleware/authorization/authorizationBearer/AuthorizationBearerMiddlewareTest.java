package ch.uniport.gateway.proxy.middleware.authorization.authorizationBearer;

import static ch.uniport.gateway.TestUtils.buildConfiguration;
import static ch.uniport.gateway.TestUtils.withMiddleware;
import static ch.uniport.gateway.TestUtils.withMiddlewareOpts;
import static ch.uniport.gateway.TestUtils.withMiddlewares;
import static ch.uniport.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static ch.uniport.gateway.proxy.middleware.VertxAssertions.assertEquals;
import static ch.uniport.gateway.proxy.middleware.VertxAssertions.assertFalse;
import static ch.uniport.gateway.proxy.middleware.VertxAssertions.assertNotEquals;
import static ch.uniport.gateway.proxy.middleware.VertxAssertions.assertTrue;

import ch.uniport.gateway.proxy.middleware.MiddlewareServer;
import ch.uniport.gateway.proxy.middleware.MiddlewareTestBase;
import ch.uniport.gateway.proxy.middleware.authorization.MockOAuth2Auth;
import ch.uniport.gateway.proxy.middleware.oauth2.AuthenticationUserContext;
import ch.uniport.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(VertxExtension.class)
public class AuthorizationBearerMiddlewareTest extends MiddlewareTestBase {

    String host = "localhost";

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {

        final JsonObject minimal = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", AuthorizationBearerMiddlewareFactory.TYPE,
                    withMiddlewareOpts(
                        JsonObject.of(AuthorizationBearerMiddlewareFactory.SESSION_SCOPE, "blub")))));

        final JsonObject missingRequiredProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", AuthorizationBearerMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of()))));

        final JsonObject missingOptions = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", AuthorizationBearerMiddlewareFactory.TYPE)));

        final JsonObject unknownProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", AuthorizationBearerMiddlewareFactory.TYPE,
                    withMiddlewareOpts(
                        JsonObject.of("bar", "blub")))));

        return Stream.of(
            Arguments.of("accept minimal config", minimal, complete, expectedTrue),
            Arguments.of("reject config with no options", missingOptions, complete, expectedFalse),
            Arguments.of("reject config with missing required property", missingRequiredProperty, complete, expectedFalse),
            Arguments.of("reject config with unknown property", unknownProperty, complete, expectedFalse)

        );
    }

    /*
    Example of a principal returned by Keycloak after a successfull login:
    {
        "access_token":"eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ1Y0p4dWNXRDFWZnFSNU56QmtKZng2RnNZYmJHeEcxOHk5bVZrazFYYWJZIn0.eyJleHAiOjE2MjcwOTY5NDcsImlhdCI6MTYyNzA1Mzc0NywiYXV0aF90aW1lIjoxNjI3MDUzNzQ3LCJqdGkiOiI5ZjYzNmVkNy02Y2Q2LTQwNmMtYThhMC0wZDhmNGM2NzgzNGEiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjIwMDAwL2F1dGgvcmVhbG1zL3BvcnRhbCIsImF1ZCI6IkRvY3VtZW50Iiwic3ViIjoiZjpiYWNiN2Y5OC0zMGUzLTRlZjktODU4MS0zYTZhZGU5YmE5ZTA6MSIsInR5cCI6IkJlYXJlciIsImF6cCI6IlBvcnRhbC1HYXRld2F5Iiwic2Vzc2lvbl9zdGF0ZSI6IjlhZWNlMTk3LTI0OTgtNDVhYy04NWY3LTJiMjBhMjQ1N2RkMyIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsicG9ydGFsdXNlciJdfSwic2NvcGUiOiJvcGVuaWQgZW1haWwgRG9jdW1lbnQgcHJvZmlsZSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJodHRwczovL2hhc3VyYS5pby9qd3QvY2xhaW1zIjp7IngtaGFzdXJhLXVzZXItaWQiOiJmOmJhY2I3Zjk4LTMwZTMtNGVmOS04NTgxLTNhNmFkZTliYTllMDoxIiwieC1oYXN1cmEtb3JnYW5pc2F0aW9uLWlkIjoiaW52ZW50YWdlLmNvbSIsIngtaGFzdXJhLWRlZmF1bHQtcm9sZSI6InBvcnRhbHVzZXIiLCJ4LWhhc3VyYS1hbGxvd2VkLXJvbGVzIjpbInBvcnRhbHVzZXIiXX0sIm5hbWUiOiJudWxsIG51bGwiLCJvcmdhbmlzYXRpb24iOiJpbnZlbnRhZ2UuY29tIiwicHJlZmVycmVkX3VzZXJuYW1lIjoiaXBzQGludmVudGFnZS5jb20iLCJnaXZlbl9uYW1lIjoibnVsbCIsImZhbWlseV9uYW1lIjoibnVsbCIsImVtYWlsIjoiaXBzQGludmVudGFnZS5jb20ifQ.K7J3P6P8ZJmtF_CJSkvDEB1Of7jXFaV7NKFqzF-2A0w2vQI49Qcpb1bZslcTKZHqi_YbmF1_aXFxapSoz1ZV5cIXCG5IPghrtp0AbMsjPR8D40hBNB7wdHwKmsJz91PJogXSDrSWEtJfSPwUrx68qQV08dMi-UJODblm9r3snjkCoN2sEKeKraP8sasJRsbP7F9F0vibTTu4E20v_eUt3sA-scuJmgTr2ErTbYwT-yVTz8uvHawlXUJUE3yLygR-UOFapjgU03hJ7dtrac2yH2BaXZWWhx7FVwDP6ungWiYLrRd_o_YfvWtRwC4DjPUwAAIATrcx_N3FU7vhhaueoA",
        "expires_in":43200,
        "refresh_expires_in":43200,
        "refresh_token":"eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIxN2IwODg5ZS02NDYwLTRkNjItYjgwZC05OGYxNjAwODUyZjgifQ.eyJleHAiOjE2MjcwOTY5NDcsImlhdCI6MTYyNzA1Mzc0NywianRpIjoiMzRmNTliMzEtZGRiYy00MGE5LThjMzItNGY1YjM0YzI2MTc2IiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDoyMDAwMC9hdXRoL3JlYWxtcy9wb3J0YWwiLCJhdWQiOiJodHRwOi8vbG9jYWxob3N0OjIwMDAwL2F1dGgvcmVhbG1zL3BvcnRhbCIsInN1YiI6ImY6YmFjYjdmOTgtMzBlMy00ZWY5LTg1ODEtM2E2YWRlOWJhOWUwOjEiLCJ0eXAiOiJSZWZyZXNoIiwiYXpwIjoiUG9ydGFsLUdhdGV3YXkiLCJzZXNzaW9uX3N0YXRlIjoiOWFlY2UxOTctMjQ5OC00NWFjLTg1ZjctMmIyMGEyNDU3ZGQzIiwic2NvcGUiOiJvcGVuaWQgZW1haWwgRG9jdW1lbnQgcHJvZmlsZSJ9.Pu59VWNio1QH16ndOJURJ9fs3Ov4vtreBKfWVSZegLE",
        "token_type":"Bearer",
        "id_token":"eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ1Y0p4dWNXRDFWZnFSNU56QmtKZng2RnNZYmJHeEcxOHk5bVZrazFYYWJZIn0.eyJleHAiOjE2MjcwOTY5NDcsImlhdCI6MTYyNzA1Mzc0NywiYXV0aF90aW1lIjoxNjI3MDUzNzQ3LCJqdGkiOiI1YzliYzE1MS1iZWRiLTRkZWItOGEwMy03ZTIzYzc0NDYxMDgiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjIwMDAwL2F1dGgvcmVhbG1zL3BvcnRhbCIsImF1ZCI6IlBvcnRhbC1HYXRld2F5Iiwic3ViIjoiZjpiYWNiN2Y5OC0zMGUzLTRlZjktODU4MS0zYTZhZGU5YmE5ZTA6MSIsInR5cCI6IklEIiwiYXpwIjoiUG9ydGFsLUdhdGV3YXkiLCJzZXNzaW9uX3N0YXRlIjoiOWFlY2UxOTctMjQ5OC00NWFjLTg1ZjctMmIyMGEyNDU3ZGQzIiwiYXRfaGFzaCI6IjVxZFVzRFRhNjA2dHA5cWVKazBWRXciLCJhY3IiOiIxIiwicmVzb3VyY2VfYWNjZXNzIjp7Ik9yZ2FuaXNhdGlvbiI6eyJyb2xlcyI6WyJBRE1JTklTVFJBVE9SIl19fSwiZW1haWxfdmVyaWZpZWQiOnRydWUsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJwb3J0YWx1c2VyIl19LCJuYW1lIjoibnVsbCBudWxsIiwib3JnYW5pc2F0aW9uIjoiaW52ZW50YWdlLmNvbSIsInByZWZlcnJlZF91c2VybmFtZSI6Imlwc0BpbnZlbnRhZ2UuY29tIiwiZ2l2ZW5fbmFtZSI6Im51bGwiLCJmYW1pbHlfbmFtZSI6Im51bGwiLCJlbWFpbCI6Imlwc0BpbnZlbnRhZ2UuY29tIn0.JsjtuBzvp92blccdEcreDpKGZH7EqBfraNos91Ni_GsFhps-5IOMlq7LBfy1SlOgebaTBY3WekVpMOJBakWcGA_ccf1_OpXhPt0PiLMcdKvQl1DHGJtLTa6JOE1Wm2w_rLho0NMqNmGTLkfFkQ0ehrM4SRGT3hKi0lp2wAhiBfI_G7xrzJOmvXFgMsVnxseB6upVWlWxKhNeDRf_zpHOvnn1mDTpFPbwQLTJw5W_UkaNWnsIfRmFrQlLS6COzQZezuU2b6FjLl-vwbLt72huhQ4BL433K5jJVuYBiPMp5ayGrB5kWj6EWK9VphK3KqvyCb6BleA_nd5YVTFs2uKzYw",
        "not-before-policy":0,
        "session_state":"9aece197-2498-45ac-85f7-2b20a2457dd3",
        "scope":"openid email Document profile"
    }
    */

    @Test
    void setIdToken(Vertx vertx, VertxTestContext testCtx) {
        // given
        final String sessionScope = "testScope";
        final String rawIdToken = "thisIsWhoIAm";
        final JsonObject principal = new JsonObject().put("id_token", rawIdToken);
        final OAuth2Auth authProvider = new MockOAuth2Auth(principal, 0);
        final User user = MockOAuth2Auth.createUser(principal);
        final AuthenticationUserContext authContext = AuthenticationUserContext.of(authProvider, user);

        // mock OAuth2 authentication
        final Handler<RoutingContext> injectTokenHandler = ctx -> {
            authContext.toSessionAtScope(ctx.session(), sessionScope);
            ctx.next();
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withMiddleware(injectTokenHandler)
            .withAuthorizationBearerMiddleware(OAuth2MiddlewareFactory.SESSION_SCOPE_ID)
            .build(ctx -> {
                // then
                assertAuthorizationBearer(testCtx, ctx.request(), rawIdToken);
                ctx.response().setStatusCode(200).end("ok");
            })
            .start();

        // when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            // then
            assertNoAuthorizationBearer(testCtx, response);
            testCtx.completeNow();
        });
    }

    @Test
    void setAccessToken(Vertx vertx, VertxTestContext testCtx) {
        // given
        final String sessionScope = "testScope";
        final String rawAccessToken = "mayIAccessThisRessource";
        final JsonObject principal = new JsonObject().put("access_token", rawAccessToken);
        final OAuth2Auth authProvider = new MockOAuth2Auth(principal, 0);
        final User user = MockOAuth2Auth.createUser(principal);
        final AuthenticationUserContext authContext = AuthenticationUserContext.of(authProvider, user);

        // mock OAuth2 authentication
        final Handler<RoutingContext> injectTokenHandler = ctx -> {
            authContext.toSessionAtScope(ctx.session(), sessionScope);
            ctx.next();
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withMiddleware(injectTokenHandler)
            .withAuthorizationBearerMiddleware(sessionScope)
            .build(ctx -> {
                // then
                assertAuthorizationBearer(testCtx, ctx.request(), rawAccessToken);
                ctx.response().setStatusCode(200).end("ok");
            })
            .start();

        // when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            // then
            assertNoAuthorizationBearer(testCtx, response);
            testCtx.completeNow();
        });
    }

    @Test
    void refreshAccessToken(Vertx vertx, VertxTestContext testCtx) {
        // given
        final String sessionScope = "testScope";
        final int refreshedExpiresIn = 42;
        final String rawRefreshToken = "iAmARefresher";
        final String rawAccessToken = "mayIAccessThisRessource";
        final int expiresIn = 1;
        final JsonObject principal = new JsonObject()
            .put("access_token", rawAccessToken)
            .put("expires_in", expiresIn)
            .put("refresh_token", rawRefreshToken);

        final OAuth2Auth initialAuthProvider = new MockOAuth2Auth(principal, refreshedExpiresIn);
        final User initialUser = MockOAuth2Auth.createUser(principal);
        final AuthenticationUserContext initialAuthContext = AuthenticationUserContext.of(initialAuthProvider, initialUser);

        // wait for access token to expire
        try {
            final CountDownLatch waiter = new CountDownLatch(1);
            waiter.await(AuthorizationBearerMiddleware.EXPIRATION_LEEWAY_SECONDS + expiresIn + 1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // mock OAuth2 authentication
        final Handler<RoutingContext> injectTokenHandler = ctx -> {
            initialAuthContext.toSessionAtScope(ctx.session(), sessionScope);
            ctx.next();
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withMiddleware(injectTokenHandler)
            .withAuthorizationBearerMiddleware(sessionScope)
            .build(ctx -> {
                // then
                final Optional<AuthenticationUserContext> refreshedAuthContext = AuthenticationUserContext.fromSessionAtScope(ctx.session(), sessionScope);
                assertTrue(testCtx, refreshedAuthContext.isPresent(), "user should exist");
                final User refreshedUser = refreshedAuthContext.get().getUser();
                assertEquals(testCtx, refreshedUser.principal().getInteger("expires_in", -1), refreshedExpiresIn, "should have been updated");
                assertTrue(testCtx, refreshedUser.attributes().getInteger("exp", -1) > initialUser.attributes().getInteger("exp", -1), "'exp' should be updated");

                assertAuthorizationBearer(testCtx, ctx.request(), rawAccessToken);
                ctx.response().setStatusCode(200).end("ok");
            })
            .start();

        // when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            // then
            assertNoAuthorizationBearer(testCtx, response);
            testCtx.completeNow();
        });
    }

    @Test
    void setNoToken(Vertx vertx, VertxTestContext testCtx) {
        final String token = "mayIAccessThisRessource";
        final String sessionScope = "someScope";
        final String someOtherSessionScope = "anotherScope";
        assertNotEquals(testCtx, sessionScope, someOtherSessionScope);

        // mock OAuth2 authentication
        final Handler<RoutingContext> injectTokenHandler = ctx -> {
            ctx.session().put(someOtherSessionScope, token);
            ctx.next();
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withMiddleware(injectTokenHandler)
            .withAuthorizationBearerMiddleware(sessionScope)
            .build(ctx -> {
                // then
                assertNoAuthorizationBearer(testCtx, ctx.request());
                ctx.response().setStatusCode(200).end("ok");
            })
            .start();

        // when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            // then
            assertNoAuthorizationBearer(testCtx, response);
            testCtx.completeNow();
        });
    }

    void assertNoAuthorizationBearer(VertxTestContext testCtx, HttpClientResponse response) {
        assertFalse(testCtx, response.headers().contains(HttpHeaders.AUTHORIZATION), "should not contain auth header");
    }

    void assertNoAuthorizationBearer(VertxTestContext testCtx, HttpServerRequest request) {
        assertFalse(testCtx, request.headers().contains(HttpHeaders.AUTHORIZATION), "should not contain auth header");
    }

    void assertAuthorizationBearer(VertxTestContext testCtx, HttpServerRequest request, String expected) {
        assertTrue(testCtx, request.headers().contains(HttpHeaders.AUTHORIZATION), "should contain auth header");
        assertEquals(testCtx, String.format("Bearer %s", expected), request.headers().get(HttpHeaders.AUTHORIZATION), "should match token");
    }
}
