package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.proxy.ProxyMiddleware;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class SessionBagMiddlewareTest {

    static final String host = "localhost";
    static final String sessionCookieName = "portal-gateway-test.session";

    @Test
    void cookiesAreRemovedInResponses(Vertx vertx, VertxTestContext testCtx) {
        String errMsg = "'test removal of response cookies' failed.";

        Cookie cookie = Cookie.cookie("blub-cookie", "foobar");
        SessionStore sessionStore = LocalSessionStore.create(vertx);
        AtomicReference<String> sessionId = new AtomicReference<>();
        testHarness(vertx, testCtx, sessionStore, ctx -> {
            if (ctx.request().getCookie(cookie.getName()) == null) {
                ctx.response().addCookie(cookie);
            }
            ctx.response().end();
        }, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, resp,
                    new ArrayList<Cookie>(Arrays.asList(cookie)));
        }, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, resp,
                    new ArrayList<Cookie>());
        });
    }

    @Test
    void cookiesAreIncludedInFollowUpRequests(Vertx vertx, VertxTestContext testCtx) {
        String errMsg = "'test cookies are included in follow up requests' failed.";

        Cookie cookie = Cookie.cookie("blub-cookie", "foobar");
        SessionStore sessionStore = LocalSessionStore.create(vertx);
        AtomicBoolean isFollowUpRequest = new AtomicBoolean(false);
        AtomicReference<String> sessionId = new AtomicReference<>();
        testHarness(vertx, testCtx, sessionStore, ctx -> {
            if (isFollowUpRequest.get()) {
                testCtx.verify(() -> {
                    assertTrue(ctx.request().cookieMap().containsKey(cookie.getName()),
                            String.format(
                                    "%s: Expected cookies to be included in follow up requests but was '%s'",
                                    errMsg, ctx.request().cookieMap()));
                });
            }
            if (ctx.request().getCookie(cookie.getName()) == null) {
                ctx.response().addCookie(cookie);
            }
            isFollowUpRequest.set(true);
            ctx.response().end();
        }, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, resp,
                    new ArrayList<Cookie>(Arrays.asList(cookie)));
        }, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, resp,
                    new ArrayList<Cookie>());
        });
    }

    @Test
    void laterReturnedCookiesAreSavedToo(Vertx vertx, VertxTestContext testCtx) {
        String errMsg = "'test cookies included in follow up responses are saved too' failed.";

        Cookie cookie = Cookie.cookie("blub-cookie", "foobar");
        Cookie followUpCookie = Cookie.cookie("moose", "test");
        SessionStore sessionStore = LocalSessionStore.create(vertx);
        AtomicBoolean isFollowUpRequest = new AtomicBoolean(false);
        AtomicReference<String> sessionId = new AtomicReference<>();
        testHarness(vertx, testCtx, sessionStore, ctx -> {
            if (ctx.request().getCookie(cookie.getName()) == null) {
                ctx.response().addCookie(cookie);
            }
            if (isFollowUpRequest.get()
                    && ctx.request().getCookie(followUpCookie.getName()) == null) {
                ctx.response().addCookie(followUpCookie);
            }
            isFollowUpRequest.set(true);
            ctx.response().end();
        }, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, resp,
                    new ArrayList<Cookie>(Arrays.asList(cookie)));
        }, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, resp,
                    new ArrayList<Cookie>(Arrays.asList(cookie, followUpCookie)));
        });
    }

    void testHarness(Vertx vertx, VertxTestContext testCtx, SessionStore sessionStore,
            Handler<RoutingContext> serverReqHandler, Handler<HttpClientResponse> respHandler,
            Handler<HttpClientResponse> followUpRespHandler) {
        int port = TestUtils.findFreePort();
        int servicePort = TestUtils.findFreePort();

        Checkpoint proxyStarted = testCtx.checkpoint();
        Checkpoint serverStarted = testCtx.checkpoint();
        Checkpoint requestServed = testCtx.checkpoint(2);
        Checkpoint responseServed = testCtx.checkpoint();
        Checkpoint followUpResponseServed = testCtx.checkpoint();

        // setup server
        Router serverRouter = Router.router(vertx);
        serverRouter.route().handler(serverReqHandler);
        vertx.createHttpServer().requestHandler(req -> {
            serverRouter.handle(req);
        }).listen(servicePort).onComplete(testCtx.succeeding(s -> {
            serverStarted.flag();

            // setup proxy
            SessionHandler sessionHandler =
                    SessionHandler.create(sessionStore).setSessionCookieName(sessionCookieName);
            SessionBagMiddleware sessionBag = new SessionBagMiddleware();

            ProxyMiddleware proxy = new ProxyMiddleware(vertx, host, servicePort);
            Router proxyRouter = Router.router(vertx);
            proxyRouter.route().handler(sessionHandler).handler(sessionBag).handler(proxy);

            vertx.createHttpServer().requestHandler(req -> {
                proxyRouter.handle(req);
                requestServed.flag();
            }).listen(port).onComplete(testCtx.succeeding(p -> {
                proxyStarted.flag();

                HttpClient client = vertx.createHttpClient();
                // send first request
                client.request(new RequestOptions().setMethod(HttpMethod.GET).setPort(port)
                        .setHost(host).setURI("")).onComplete(testCtx.succeeding(req -> {
                            req.send().onComplete(testCtx.succeeding(resp -> {
                                respHandler.handle(resp);
                                responseServed.flag();

                                // send second request
                                MultiMap headers = MultiMap.caseInsensitiveMultiMap();
                                for (String respCookie : resp.cookies()) {
                                    headers.add(HttpHeaders.COOKIE, respCookie);
                                }
                                client.request(new RequestOptions().setMethod(HttpMethod.GET)
                                        .setPort(port).setHost(host).setURI("").setHeaders(headers))
                                        .onComplete(testCtx.succeeding(followUpReq -> {
                                            followUpReq.send()
                                                    .onComplete(testCtx.succeeding(followUpResp -> {
                                                        followUpRespHandler.handle(followUpResp);
                                                        followUpResponseServed.flag();
                                                    }));
                                        }));
                            }));
                        }));
            }));
        }));
    }

    void expectedCookies(VertxTestContext testCtx, String errMsg, SessionStore sessionStore,
            AtomicReference<String> sessionId, HttpClientResponse resp,
            List<Cookie> expectedCookies) {
        testCtx.verify(() -> {
            int expectedMaxNumberOfCookies = 1;
            assertTrue(resp.cookies().size() <= expectedMaxNumberOfCookies,
                    String.format("%s: Wrong amount of cookies. Expected at most '%s' but got '%s'",
                            errMsg, expectedMaxNumberOfCookies, resp.cookies().size()));

            if (sessionId == null || sessionId.get() == null || sessionId.get().isEmpty()) {
                assertTrue(resp.cookies().size() == 1,
                        String.format("%s: No session cookie returned.", errMsg));
                String sessionCookie = resp.cookies().get(0);
                String[] cookieParts = sessionCookie.split(";|=");
                String actualSessionCookieName = cookieParts[0];
                sessionId.set(cookieParts[1]);
                assertEquals(sessionCookieName, actualSessionCookieName,
                        String.format("%s: Wrong cookie name", errMsg));
            }

            // check if cookies are stored in session
            sessionStore.get(sessionId.get()).onSuccess(session -> {
                assertNotNull(session, "Expected session to be present.");
                List<String> actualSessionBagCookies =
                        session.get(SessionBagMiddleware.SESSION_BAG_COOKIES);
                assertNotNull(actualSessionBagCookies,
                        "Expected cookies stored in session bag to not null");
                for (Cookie cookie : expectedCookies) {
                    assertTrue(actualSessionBagCookies.contains(cookie.encode()), String.format(
                            "%s: Expected cookie '%s' was not present in session", errMsg, cookie));
                }
            }).onFailure(err -> testCtx.failNow(err));
        });
    }
}
