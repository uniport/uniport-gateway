package com.inventage.portal.gateway.proxy.middleware.authorizationBearer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.impl.AccessTokenImpl;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class AuthorizationBearerMiddlewareTest {

    String host = "localhost";

    @Test
    void setIdToken(Vertx vertx, VertxTestContext testCtx) {
        int port = TestUtils.findFreePort();
        String sessionScope = "testScope";
        String rawIdToken = "thisIsWhoIAm";
        AccessToken accessToken = new AccessTokenImpl(new JsonObject().put("id_token", rawIdToken), null);

        Checkpoint serverStarted = testCtx.checkpoint();
        Checkpoint requestServed = testCtx.checkpoint();
        Checkpoint responseReceived = testCtx.checkpoint();

        // needed to populate a session
        Handler<RoutingContext> sessionHandler = SessionHandler.create(LocalSessionStore.create(vertx));

        // mock OAuth2 authentication
        Handler<RoutingContext> injectTokenHandler = ctx -> {
            ctx.session().put(String.format(OAuth2MiddlewareFactory.SESSION_SCOPE_ACCESS_TOKEN_FORMAT, sessionScope),
                    accessToken);
            ctx.next();
        };

        AuthorizationBearerMiddleware authBearer = new AuthorizationBearerMiddleware(
                DynamicConfiguration.MIDDLEWARE_OAUTH2_SESSION_SCOPE_ID);

        Handler<RoutingContext> endHandler = ctx -> ctx.response().end("ok");
        ;

        Router router = Router.router(vertx);
        router.route().handler(sessionHandler).handler(injectTokenHandler).handler(authBearer).handler(endHandler);
        vertx.createHttpServer().requestHandler(req -> {
            router.handle(req);
            testCtx.verify(() -> {
                assertTrue(req.headers().contains(HttpHeaders.AUTHORIZATION), "should contain auth header");
                assertEquals(String.format("Bearer %s", rawIdToken), req.headers().get(HttpHeaders.AUTHORIZATION),
                        "should match token");
            });
            requestServed.flag();
        }).listen(port).onComplete(testCtx.succeeding(s -> {
            serverStarted.flag();
            // server is started, we can proceed
            vertx.createHttpClient().request(HttpMethod.GET, port, host, "/blub").compose(req -> req.send())
                    .onComplete(testCtx.succeeding(resp -> {
                        testCtx.verify(() -> {
                            assertFalse(resp.headers().contains(HttpHeaders.AUTHORIZATION),
                                    "should not contain auth header");
                        });
                        responseReceived.flag();
                    }));
        }));
    }

    @Test
    void setAccessToken(Vertx vertx, VertxTestContext testCtx) {
        int port = TestUtils.findFreePort();
        String sessionScope = "testScope";
        String rawAccessToken = "mayIAccessThisRessource";
        AccessToken accessToken = new AccessTokenImpl(new JsonObject().put("access_token", rawAccessToken), null);
        /* {
            "access_token":"eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ1Y0p4dWNXRDFWZnFSNU56QmtKZng2RnNZYmJHeEcxOHk5bVZrazFYYWJZIn0.eyJleHAiOjE2MjcwOTY5NDcsImlhdCI6MTYyNzA1Mzc0NywiYXV0aF90aW1lIjoxNjI3MDUzNzQ3LCJqdGkiOiI5ZjYzNmVkNy02Y2Q2LTQwNmMtYThhMC0wZDhmNGM2NzgzNGEiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjIwMDAwL2F1dGgvcmVhbG1zL3BvcnRhbCIsImF1ZCI6IkRvY3VtZW50Iiwic3ViIjoiZjpiYWNiN2Y5OC0zMGUzLTRlZjktODU4MS0zYTZhZGU5YmE5ZTA6MSIsInR5cCI6IkJlYXJlciIsImF6cCI6IlBvcnRhbC1HYXRld2F5Iiwic2Vzc2lvbl9zdGF0ZSI6IjlhZWNlMTk3LTI0OTgtNDVhYy04NWY3LTJiMjBhMjQ1N2RkMyIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsicG9ydGFsdXNlciJdfSwic2NvcGUiOiJvcGVuaWQgZW1haWwgRG9jdW1lbnQgcHJvZmlsZSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJodHRwczovL2hhc3VyYS5pby9qd3QvY2xhaW1zIjp7IngtaGFzdXJhLXVzZXItaWQiOiJmOmJhY2I3Zjk4LTMwZTMtNGVmOS04NTgxLTNhNmFkZTliYTllMDoxIiwieC1oYXN1cmEtb3JnYW5pc2F0aW9uLWlkIjoiaW52ZW50YWdlLmNvbSIsIngtaGFzdXJhLWRlZmF1bHQtcm9sZSI6InBvcnRhbHVzZXIiLCJ4LWhhc3VyYS1hbGxvd2VkLXJvbGVzIjpbInBvcnRhbHVzZXIiXX0sIm5hbWUiOiJudWxsIG51bGwiLCJvcmdhbmlzYXRpb24iOiJpbnZlbnRhZ2UuY29tIiwicHJlZmVycmVkX3VzZXJuYW1lIjoiaXBzQGludmVudGFnZS5jb20iLCJnaXZlbl9uYW1lIjoibnVsbCIsImZhbWlseV9uYW1lIjoibnVsbCIsImVtYWlsIjoiaXBzQGludmVudGFnZS5jb20ifQ.K7J3P6P8ZJmtF_CJSkvDEB1Of7jXFaV7NKFqzF-2A0w2vQI49Qcpb1bZslcTKZHqi_YbmF1_aXFxapSoz1ZV5cIXCG5IPghrtp0AbMsjPR8D40hBNB7wdHwKmsJz91PJogXSDrSWEtJfSPwUrx68qQV08dMi-UJODblm9r3snjkCoN2sEKeKraP8sasJRsbP7F9F0vibTTu4E20v_eUt3sA-scuJmgTr2ErTbYwT-yVTz8uvHawlXUJUE3yLygR-UOFapjgU03hJ7dtrac2yH2BaXZWWhx7FVwDP6ungWiYLrRd_o_YfvWtRwC4DjPUwAAIATrcx_N3FU7vhhaueoA",
            "expires_in":43200,
            "refresh_expires_in":43200,
            "refresh_token":"eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIxN2IwODg5ZS02NDYwLTRkNjItYjgwZC05OGYxNjAwODUyZjgifQ.eyJleHAiOjE2MjcwOTY5NDcsImlhdCI6MTYyNzA1Mzc0NywianRpIjoiMzRmNTliMzEtZGRiYy00MGE5LThjMzItNGY1YjM0YzI2MTc2IiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDoyMDAwMC9hdXRoL3JlYWxtcy9wb3J0YWwiLCJhdWQiOiJodHRwOi8vbG9jYWxob3N0OjIwMDAwL2F1dGgvcmVhbG1zL3BvcnRhbCIsInN1YiI6ImY6YmFjYjdmOTgtMzBlMy00ZWY5LTg1ODEtM2E2YWRlOWJhOWUwOjEiLCJ0eXAiOiJSZWZyZXNoIiwiYXpwIjoiUG9ydGFsLUdhdGV3YXkiLCJzZXNzaW9uX3N0YXRlIjoiOWFlY2UxOTctMjQ5OC00NWFjLTg1ZjctMmIyMGEyNDU3ZGQzIiwic2NvcGUiOiJvcGVuaWQgZW1haWwgRG9jdW1lbnQgcHJvZmlsZSJ9.Pu59VWNio1QH16ndOJURJ9fs3Ov4vtreBKfWVSZegLE",
            "token_type":"Bearer",
            "id_token":"eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ1Y0p4dWNXRDFWZnFSNU56QmtKZng2RnNZYmJHeEcxOHk5bVZrazFYYWJZIn0.eyJleHAiOjE2MjcwOTY5NDcsImlhdCI6MTYyNzA1Mzc0NywiYXV0aF90aW1lIjoxNjI3MDUzNzQ3LCJqdGkiOiI1YzliYzE1MS1iZWRiLTRkZWItOGEwMy03ZTIzYzc0NDYxMDgiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjIwMDAwL2F1dGgvcmVhbG1zL3BvcnRhbCIsImF1ZCI6IlBvcnRhbC1HYXRld2F5Iiwic3ViIjoiZjpiYWNiN2Y5OC0zMGUzLTRlZjktODU4MS0zYTZhZGU5YmE5ZTA6MSIsInR5cCI6IklEIiwiYXpwIjoiUG9ydGFsLUdhdGV3YXkiLCJzZXNzaW9uX3N0YXRlIjoiOWFlY2UxOTctMjQ5OC00NWFjLTg1ZjctMmIyMGEyNDU3ZGQzIiwiYXRfaGFzaCI6IjVxZFVzRFRhNjA2dHA5cWVKazBWRXciLCJhY3IiOiIxIiwicmVzb3VyY2VfYWNjZXNzIjp7Ik9yZ2FuaXNhdGlvbiI6eyJyb2xlcyI6WyJBRE1JTklTVFJBVE9SIl19fSwiZW1haWxfdmVyaWZpZWQiOnRydWUsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJwb3J0YWx1c2VyIl19LCJuYW1lIjoibnVsbCBudWxsIiwib3JnYW5pc2F0aW9uIjoiaW52ZW50YWdlLmNvbSIsInByZWZlcnJlZF91c2VybmFtZSI6Imlwc0BpbnZlbnRhZ2UuY29tIiwiZ2l2ZW5fbmFtZSI6Im51bGwiLCJmYW1pbHlfbmFtZSI6Im51bGwiLCJlbWFpbCI6Imlwc0BpbnZlbnRhZ2UuY29tIn0.JsjtuBzvp92blccdEcreDpKGZH7EqBfraNos91Ni_GsFhps-5IOMlq7LBfy1SlOgebaTBY3WekVpMOJBakWcGA_ccf1_OpXhPt0PiLMcdKvQl1DHGJtLTa6JOE1Wm2w_rLho0NMqNmGTLkfFkQ0ehrM4SRGT3hKi0lp2wAhiBfI_G7xrzJOmvXFgMsVnxseB6upVWlWxKhNeDRf_zpHOvnn1mDTpFPbwQLTJw5W_UkaNWnsIfRmFrQlLS6COzQZezuU2b6FjLl-vwbLt72huhQ4BL433K5jJVuYBiPMp5ayGrB5kWj6EWK9VphK3KqvyCb6BleA_nd5YVTFs2uKzYw",
            "not-before-policy":0,
            "session_state":"9aece197-2498-45ac-85f7-2b20a2457dd3",
            "scope":"openid email Document profile"
        } */

        Checkpoint serverStarted = testCtx.checkpoint();
        Checkpoint requestServed = testCtx.checkpoint();
        Checkpoint responseReceived = testCtx.checkpoint();

        // needed to populate a session
        Handler<RoutingContext> sessionHandler = SessionHandler.create(LocalSessionStore.create(vertx));

        // mock OAuth2 authentication
        Handler<RoutingContext> injectTokenHandler = ctx -> {
            ctx.session().put(String.format(OAuth2MiddlewareFactory.SESSION_SCOPE_ACCESS_TOKEN_FORMAT, sessionScope),
                    accessToken);
            ctx.next();
        };

        AuthorizationBearerMiddleware authBearer = new AuthorizationBearerMiddleware(sessionScope);

        Handler<RoutingContext> endHandler = ctx -> ctx.response().end("ok");
        ;

        Router router = Router.router(vertx);
        router.route().handler(sessionHandler).handler(injectTokenHandler).handler(authBearer).handler(endHandler);
        vertx.createHttpServer().requestHandler(req -> {
            router.handle(req);
            testCtx.verify(() -> {
                assertTrue(req.headers().contains(HttpHeaders.AUTHORIZATION), "should contain auth header");
                assertEquals(String.format("Bearer %s", rawAccessToken), req.headers().get(HttpHeaders.AUTHORIZATION),
                        "should match token");
            });
            requestServed.flag();
        }).listen(port).onComplete(testCtx.succeeding(s -> {
            serverStarted.flag();
            // server is started, we can proceed
            vertx.createHttpClient().request(HttpMethod.GET, port, host, "/blub").compose(req -> req.send())
                    .onComplete(testCtx.succeeding(resp -> {
                        testCtx.verify(() -> {
                            assertFalse(resp.headers().contains(HttpHeaders.AUTHORIZATION),
                                    "should not contain auth header");
                        });
                        responseReceived.flag();
                    }));
        }));

    }

    @Test
    void setNoToken(Vertx vertx, VertxTestContext testCtx) {
        int port = TestUtils.findFreePort();
        String token = "mayIAccessThisRessource";
        String sessionScope = "someScope";
        String someOtherSessionScope = "anotherScope";
        testCtx.verify(() -> assertNotEquals(sessionScope, someOtherSessionScope));

        Checkpoint serverStarted = testCtx.checkpoint();
        Checkpoint requestServed = testCtx.checkpoint();
        Checkpoint responseReceived = testCtx.checkpoint();

        // needed to populate a session
        Handler<RoutingContext> sessionHandler = SessionHandler.create(LocalSessionStore.create(vertx));

        // mock OAuth2 authentication
        Handler<RoutingContext> injectTokenHandler = ctx -> {
            ctx.session().put(someOtherSessionScope, token);
            ctx.next();
        };

        AuthorizationBearerMiddleware authBearer = new AuthorizationBearerMiddleware(sessionScope);

        Handler<RoutingContext> endHandler = ctx -> ctx.response().end("ok");
        ;

        Router router = Router.router(vertx);
        router.route().handler(sessionHandler).handler(injectTokenHandler).handler(authBearer).handler(endHandler);
        vertx.createHttpServer().requestHandler(req -> {
            router.handle(req);
            testCtx.verify(() -> {
                assertFalse(req.headers().contains(HttpHeaders.AUTHORIZATION), "should not contain auth header");
            });
            requestServed.flag();
        }).listen(port).onComplete(testCtx.succeeding(s -> {
            serverStarted.flag();
            // server is started, we can proceed
            vertx.createHttpClient().request(HttpMethod.GET, port, host, "/blub").compose(req -> req.send())
                    .onComplete(testCtx.succeeding(resp -> {
                        testCtx.verify(() -> {
                            assertFalse(resp.headers().contains(HttpHeaders.AUTHORIZATION),
                                    "should not contain auth header");
                        });
                        responseReceived.flag();
                    }));
        }));

    }
}
