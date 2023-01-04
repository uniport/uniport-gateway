package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.proxy.ProxyMiddleware;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.CookieImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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

        Cookie cookie = Cookie.cookie("blub-cookie", "foobar").setPath("/").setMaxAge(3600);
        SessionStore sessionStore = LocalSessionStore.create(vertx);
        AtomicBoolean isFirstReq = new AtomicBoolean(true);
        AtomicReference<String> sessionId = new AtomicReference<>();
        testHarness(vertx, testCtx, sessionStore, new JsonArray(), ctx -> {
            if (isFirstReq.get()) {
                ctx.response().addCookie(cookie);
                isFirstReq.set(false);
            }
            ctx.response().end();
        }, null, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, new JsonArray(), resp,
                    new ArrayList<Cookie>(Arrays.asList(cookie)));
        }, null, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, new JsonArray(), resp,
                    new ArrayList<Cookie>(Arrays.asList(cookie)));
        });
    }

    @Test
    void cookiesAreIncludedInFollowUpRequests(Vertx vertx, VertxTestContext testCtx) {
        String errMsg = "'test cookies are included in follow up requests' failed.";

        Cookie cookie = Cookie.cookie("blub-cookie", "foobar").setPath("/").setMaxAge(3600);
        SessionStore sessionStore = LocalSessionStore.create(vertx);
        AtomicBoolean isFirstReq = new AtomicBoolean(true);
        AtomicReference<String> sessionId = new AtomicReference<>();
        testHarness(vertx, testCtx, sessionStore, new JsonArray(), ctx -> {
            if (isFirstReq.get()) {
                ctx.response().addCookie(cookie);
                isFirstReq.set(false);
            } else {
                testCtx.verify(() -> {
                    assertTrue(ctx.request().cookieMap().containsKey(cookie.getName()),
                            String.format("%s: Expected cookies to be included in follow up requests but was '%s'",
                                    errMsg, ctx.request().cookieMap()));
                });
            }
            ctx.response().end();
        }, null, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, new JsonArray(), resp,
                    new ArrayList<Cookie>(Arrays.asList(cookie)));
        }, null, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, new JsonArray(), resp,
                    new ArrayList<Cookie>(Arrays.asList(cookie)));
        });
    }

    @Test
    void laterReturnedCookiesAreSavedToo(Vertx vertx, VertxTestContext testCtx) {
        String errMsg = "'test cookies included in follow up responses are saved too' failed.";

        Cookie cookie = Cookie.cookie("blub-cookie", "foobar").setPath("/").setMaxAge(3600);
        Cookie followUpCookie = Cookie.cookie("moose", "test").setPath("/").setMaxAge(3600);
        SessionStore sessionStore = LocalSessionStore.create(vertx);
        AtomicBoolean isFirstReq = new AtomicBoolean(true);
        AtomicReference<String> sessionId = new AtomicReference<>();
        testHarness(vertx, testCtx, sessionStore, new JsonArray(), ctx -> {
            if (isFirstReq.get()) {
                ctx.response().addCookie(cookie);
                isFirstReq.set(false);
            } else {
                ctx.response().addCookie(followUpCookie);
            }
            ctx.response().end();
        }, null, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, new JsonArray(), resp,
                    new ArrayList<Cookie>(Arrays.asList(cookie)));
        }, null, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, new JsonArray(), resp,
                    new ArrayList<Cookie>(Arrays.asList(cookie, followUpCookie)));
        });
    }

    @Test
    void expiredCookieIsRemoved(Vertx vertx, VertxTestContext testCtx) {
        String errMsg = "'test expired cookie are removed from session bag' failed.";
        Cookie cookie = Cookie.cookie("blub", "foobar").setPath("/").setMaxAge(3600);
        Cookie expiredCookie = Cookie.cookie("blub", "foobar").setPath("/").setMaxAge(0);
        SessionStore sessionStore = LocalSessionStore.create(vertx);
        AtomicBoolean isFirstReq = new AtomicBoolean(true);
        AtomicReference<String> sessionId = new AtomicReference<>();

        testHarness(vertx, testCtx, sessionStore, new JsonArray(), ctx -> {
            if (isFirstReq.get()) {
                ctx.response().addCookie(cookie);
                isFirstReq.set(false);
            } else {
                ctx.response().addCookie(expiredCookie);
            }
            ctx.response().end();
        }, null, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, new JsonArray(), resp,
                    new ArrayList<Cookie>(Arrays.asList(cookie)));
        }, null, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, new JsonArray(), resp, new ArrayList<Cookie>());
        });
    }

    @Test
    void keycloakMasterRealmCookieIsPassedInResponse(Vertx vertx, VertxTestContext testCtx) {
        String errMsg = "'test keycloak master realm cookie is passed to user agent' failed";
        Cookie masterRealmCookie = Cookie.cookie("KEYCLOAK_SESSION", "foobar").setPath("/auth/realms/master/")
                .setMaxAge(3600);
        Cookie portalRealmCookie = Cookie.cookie("KEYCLOAK_SESSION", "foobar").setPath("/auth/realms/portal/")
                .setMaxAge(3600);
        JsonArray whitelistedCookies = new JsonArray().add(new JsonObject()
                .put(DynamicConfiguration.MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIE_NAME, "KEYCLOAK_SESSION")
                .put(DynamicConfiguration.MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIE_PATH, "/auth/realms/master/"))
                .add(new JsonObject()
                        .put(DynamicConfiguration.MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIE_NAME, "KEYCLOAK_SESSION")
                        .put(DynamicConfiguration.MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIE_PATH,
                                "/auth/realms/portal/"));
        SessionStore sessionStore = LocalSessionStore.create(vertx);
        AtomicBoolean isFirstReq = new AtomicBoolean(true);
        AtomicReference<String> sessionId = new AtomicReference<>();

        testHarness(vertx, testCtx, sessionStore, whitelistedCookies, ctx -> {
            if (isFirstReq.get()) {
                ctx.response().addCookie(masterRealmCookie);
                // vert.x uses a cookie name as its unique identifier
                // but it should be the tuple (name, domain, path)
                // https://github.com/eclipse-vertx/vert.x/issues/3916
                // TODO set once this issue is closed
                // ctx.response().addCookie(portalRealmCookie);
                isFirstReq.set(false);
            }
            ctx.response().end();
        }, null, resp -> {
            testCtx.verify(() -> {
                boolean foundMasterRealmCookie = false;
                for (String respCookie : resp.cookies()) {
                    io.netty.handler.codec.http.cookie.Cookie decodedRespCookie = ClientCookieDecoder.STRICT
                            .decode(respCookie);
                    if (decodedRespCookie.name().equals(masterRealmCookie.getName())
                            && decodedRespCookie.path().equals(masterRealmCookie.getPath())) {
                        foundMasterRealmCookie = true;
                        break;
                    }
                }
                assertTrue(foundMasterRealmCookie,
                        String.format("%s: Expected master realm cookie to be included in response but was '%s'",
                                errMsg, resp.cookies()));
            });
            // masterRealmCookie should not be saved in the session bag
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, whitelistedCookies, resp,
                    new ArrayList<Cookie>(Arrays.asList()));
        }, null, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, whitelistedCookies, resp,
                    new ArrayList<Cookie>(Arrays.asList()));
        });
    }

    @Test
    void storedCookieHasPrecedenceOverRequestCookie(Vertx vertx, VertxTestContext testCtx) {
        String errMsg = "'test stored cookies have precedence over request cookies with the same name' failed";
        Cookie storedCookie = Cookie.cookie("blub", "foo").setPath("/").setMaxAge(3600);
        Cookie reqCookie = Cookie.cookie("blub", "bar").setPath("/").setMaxAge(3600);
        SessionStore sessionStore = LocalSessionStore.create(vertx);
        AtomicBoolean isFirstReq = new AtomicBoolean(true);
        AtomicReference<String> sessionId = new AtomicReference<>();

        testHarness(vertx, testCtx, sessionStore, new JsonArray(), ctx -> {
            if (isFirstReq.get()) {
                ctx.response().addCookie(storedCookie);
                isFirstReq.set(false);
            } else {
                testCtx.verify(() -> {
                    for (String cookieName : ctx.request().cookieMap().keySet()) {
                        if (cookieName.equals("blub")) {
                            String cookieVal = ctx.request().cookieMap().get(cookieName).getValue();
                            assertTrue(cookieVal.equals("foo"), String.format(
                                    "%s: Expected cookie value to be '%s' but was '%s'", errMsg, "foo", cookieVal));
                        }
                    }
                });
            }
            ctx.response().end();
        }, null, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, new JsonArray(), resp,
                    new ArrayList<Cookie>(Arrays.asList(storedCookie)));
        }, new ArrayList<Cookie>(Arrays.asList(reqCookie)), resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, new JsonArray(), resp,
                    new ArrayList<Cookie>(Arrays.asList(storedCookie)));
        });

    }

    void testHarness(Vertx vertx, VertxTestContext testCtx, SessionStore sessionStore, JsonArray whitelistedCookies,
            Handler<RoutingContext> serverReqHandler, List<Cookie> reqCookies, Handler<HttpClientResponse> respHandler,
            List<Cookie> followUpReqCookies, Handler<HttpClientResponse> followUpRespHandler) {
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
            SessionHandler sessionHandler = SessionHandler.create(sessionStore).setSessionCookieName(sessionCookieName);
            SessionBagMiddleware sessionBag = new SessionBagMiddleware(whitelistedCookies,
                    "inventage-portal-gateway.session");
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
                client.request(new RequestOptions().setMethod(HttpMethod.GET).setPort(port).setHost(host))
                        .onComplete(testCtx.succeeding(req -> {
                            req.send().onComplete(testCtx.succeeding(resp -> {
                                respHandler.handle(resp);
                                responseServed.flag();

                                // send second request
                                MultiMap headers = MultiMap.caseInsensitiveMultiMap();
                                for (String respCookie : resp.cookies()) {
                                    headers.add(HttpHeaders.COOKIE, respCookie);
                                }
                                if (followUpReqCookies != null) {
                                    for (Cookie followUpReqCookie : followUpReqCookies) {
                                        headers.add(HttpHeaders.COOKIE, followUpReqCookie.encode());
                                    }
                                }
                                client.request(new RequestOptions().setMethod(HttpMethod.GET).setPort(port)
                                        .setHost(host).setURI("").setHeaders(headers))
                                        .onComplete(testCtx.succeeding(followUpReq -> {
                                            followUpReq.send().onComplete(testCtx.succeeding(followUpResp -> {
                                                followUpRespHandler.handle(followUpResp);
                                                followUpResponseServed.flag();
                                            }));
                                        }));
                            }));
                        }));
            }));
        }));
    }

    /**
     * Verifies that
     * a) a session cookie is returned to the user agent
     * b) no other cookies are returned to the user agent but the session cookie
     * c) all expectedSessionBagCookies are present in the session bag
     *
     * @param testCtx test context
     * @param errMsg error message
     * @param sessionStore contains stored cookies
     * @param sessionId session ID as returned in the session cookie
     * @param resp response returned to the user agent
     * @param expectedSessionBagCookies cookie expected to be stored in the session bag
     */
    void expectedCookies(VertxTestContext testCtx, String errMsg, SessionStore sessionStore,
            AtomicReference<String> sessionId, JsonArray whitelistedCookies, HttpClientResponse resp,
            List<Cookie> expectedSessionBagCookies) {
        testCtx.verify(() -> {
            io.netty.handler.codec.http.cookie.Cookie sessionCookie = null;
            List<io.netty.handler.codec.http.cookie.Cookie> decodedRespCookies = new ArrayList<>();
            for (String respCookie : resp.cookies()) {
                io.netty.handler.codec.http.cookie.Cookie decodedRespCookie = ClientCookieDecoder.STRICT
                        .decode(respCookie);
                decodedRespCookies.add(decodedRespCookie);

                // only session cookie and whitelisted cookies are passed
                if (decodedRespCookie.name().equals(sessionCookieName)) {
                    sessionCookie = decodedRespCookie;
                    continue;
                }

                assertTrue(
                        whitelistedCookies.contains(new JsonObject()
                                .put(DynamicConfiguration.MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIE_NAME,
                                        decodedRespCookie.name())
                                .put(DynamicConfiguration.MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIE_PATH,
                                        decodedRespCookie.path())),
                        String.format("%s: Not whitelisted cookie was passed to user agent '%s'", errMsg, respCookie));
            }

            // extract session ID to inspect session content i.e. session bag
            if (sessionId == null || sessionId.get() == null || sessionId.get().isEmpty()) {
                assertNotNull(sessionCookie, String.format("%s: No session cookie returned.", errMsg));
                sessionId.set(sessionCookie.value());
                assertEquals(sessionCookieName, sessionCookie.name(), String.format("%s: Wrong cookie name", errMsg));
            }

            // check if cookies are stored in session
            sessionStore.get(sessionId.get()).onSuccess(session -> {
                assertNotNull(session, "Expected session to be present.");
                Set<io.netty.handler.codec.http.cookie.Cookie> actualSessionBagCookies = session
                        .get(SessionBagMiddleware.SESSION_BAG_COOKIES);
                assertNotNull(actualSessionBagCookies, "Expected session bag to be present");
                List<String> actualSessionBagCookiesStr = new ArrayList<String>();
                for (io.netty.handler.codec.http.cookie.Cookie nettyCookie : actualSessionBagCookies) {
                    Cookie c = new CookieImpl(nettyCookie.name(), nettyCookie.value());
                    actualSessionBagCookiesStr.add(c.encode());
                }
                for (Cookie cookie : expectedSessionBagCookies) {
                    assertTrue(actualSessionBagCookiesStr.contains(cookie.encode()), String
                            .format("%s: Expected cookie '%s' was not present in session", errMsg, cookie.encode()));
                }
            }).onFailure(err -> testCtx.failNow(err));
        });
    }
}
