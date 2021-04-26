package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.common.net.HttpHeaders;
import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.proxy.ProxyMiddleware;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
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
        testHarness(vertx, testCtx, cookie, ctx -> {
            if (ctx.request().getCookie(cookie.getName()) == null) {
                ctx.response().addCookie(cookie);
            }
            ctx.response().end();
        }, resp -> {
            testCtx.verify(() -> {
                int expectedCookies = 1;
                assertTrue(resp.cookies().size() <= expectedCookies,
                        String.format("%s: Wrong amount of cookies. Expected: '%d' but was '%s'",
                                errMsg, expectedCookies, resp.cookies().size()));

                if (resp.cookies().size() == 1) {
                    String sessionCookie = resp.cookies().get(0);
                    String expectedStartsWith = String.format("%s=", sessionCookieName);
                    assertTrue(sessionCookie.startsWith(expectedStartsWith),
                            String.format("%s: Wrong cookie name. Expected '%s' but was '%s'",
                                    errMsg, expectedCookies, sessionCookie));
                }
            });
        }, resp -> {
        });

    }

    @Test
    void cookiesAreIncludedInFollowUpRequests(Vertx vertx, VertxTestContext testCtx) {
        String errMsg = "'test cookies are included in follow up requests' failed.";

        Cookie cookie = Cookie.cookie("blub-cookie", "foobar");
        testHarness(vertx, testCtx, cookie, ctx -> {
            if (!ctx.request().cookieMap().isEmpty()) {
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
            ctx.response().end();
        }, resp -> {
        }, resp -> {
        });
    }

    @Test
    void laterReturnedCookiesAreSavedToo(Vertx vertx, VertxTestContext testCtx) {
        String errMsg = "'test cookies included in follow up responses are saved too' failed.";

        Cookie cookie = Cookie.cookie("blub-cookie", "foobar");
        Cookie followUpCookie = Cookie.cookie("moose", "test");
        AtomicBoolean isFollowUpRequest = new AtomicBoolean(false);
        testHarness(vertx, testCtx, cookie, ctx -> {
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
        }, resp -> {
        });
    }

    void testHarness(Vertx vertx, VertxTestContext testCtx, Cookie cookie,
            Handler<RoutingContext> proxiedReqHandler, 
            Handler<HttpClientResponse> respHandler,
            Handler<HttpClientResponse> followUpRespHandler) {
        int port = TestUtils.findFreePort();
        int servicePort = TestUtils.findFreePort();

        Checkpoint serverStarted = testCtx.checkpoint();
        Checkpoint serviceStarted = testCtx.checkpoint();
        Checkpoint requestServed = testCtx.checkpoint(2);
        Checkpoint responseServed = testCtx.checkpoint();
        Checkpoint followUpResponseServed = testCtx.checkpoint();

        SessionHandler sessionHandler = SessionHandler.create(LocalSessionStore.create(vertx))
                .setSessionCookieName(sessionCookieName);
        SessionBagMiddleware sessionBag = new SessionBagMiddleware();

        Router serviceRouter = Router.router(vertx);
        serviceRouter.route().handler(proxiedReqHandler);
        vertx.createHttpServer().requestHandler(req -> {
            serviceRouter.handle(req);
        }).listen(servicePort).onComplete(testCtx.succeeding(httpServer -> serviceStarted.flag()));
        ProxyMiddleware proxy = new ProxyMiddleware(vertx, host, servicePort, null);

        Router router = Router.router(vertx);
        router.route().handler(sessionHandler).handler(sessionBag).handler(proxy);

        vertx.createHttpServer().requestHandler(req -> {
            router.handle(req);
            requestServed.flag();
        }).listen(port).onComplete(testCtx.succeeding(httpServer -> serverStarted.flag()));

        HttpClient client = vertx.createHttpClient();
        client.request(HttpMethod.GET, port, host, "/").onComplete(
                testCtx.succeeding(req -> req.send().onComplete(testCtx.succeeding(resp -> {
                    respHandler.handle(resp);
                    responseServed.flag();
                }))));

        client.request(HttpMethod.GET, port, host, "/").onComplete(testCtx.succeeding(req -> {
            req.putHeader(HttpHeaders.COOKIE, cookie.encode());
            req.send().onComplete(testCtx.succeeding(resp -> {
                followUpRespHandler.handle(resp);
                followUpResponseServed.flag();
            }));
        }));

    }
}
