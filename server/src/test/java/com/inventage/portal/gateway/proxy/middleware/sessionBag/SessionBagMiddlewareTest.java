package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.stream.Stream;
import com.inventage.portal.gateway.TestUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpClientRequest;
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
    static final String sessionCookieName = "test-portal-gateway.session";

    static Stream<Arguments> sessionBagTestData() {
        return Stream.of(Arguments.of("example"));
    }

    // TODO @Test
    void sessionBagTest(Vertx vertx, VertxTestContext testCtx) {
        String errMsg = String.format("'%s' failed.", "test session bag");

        int port = TestUtils.findFreePort();

        Checkpoint serverStarted = testCtx.checkpoint();
        Checkpoint requestServed = testCtx.checkpoint();
        Checkpoint responseReceived = testCtx.checkpoint();

        SessionHandler sessionHandler = SessionHandler.create(LocalSessionStore.create(vertx))
                .setSessionCookieName(sessionCookieName);
        SessionBagMiddleware sessionBag = new SessionBagMiddleware();
        Handler<RoutingContext> setCookieHandler = ctx -> {
            ctx.response().addCookie(Cookie.cookie("blub-cookie", "foobar"));
            ctx.next();
        };
        Handler<RoutingContext> endHandler = ctx -> ctx.response().end("ok");

        Router router = Router.router(vertx);
        router.route().handler(sessionHandler).handler(sessionBag).handler(setCookieHandler)
                .handler(endHandler);
        vertx.createHttpServer().requestHandler(req -> {
            router.handle(req);
            requestServed.flag();
        }).listen(port).onComplete(testCtx.succeeding(httpServer -> serverStarted.flag()));

        Future<HttpClientRequest> reqFuture =
                vertx.createHttpClient().request(HttpMethod.GET, port, host, "/");

        reqFuture.onComplete(
                testCtx.succeeding(req -> req.send().onComplete(testCtx.succeeding(resp -> {
                    testCtx.verify(() -> {
                        int expectedCookies = 1;
                        assertTrue(resp.cookies().size() <= expectedCookies,
                                String.format(
                                        "%s: Wrong amount of cookies. Expected: '%d' but was '%s'",
                                        errMsg, expectedCookies, resp.cookies().size()));

                        if (resp.cookies().size() == 1) {
                            String sessionCookie = resp.cookies().get(0);
                            String expectedStartsWith = String.format("%s=", sessionCookieName);
                            assertTrue(sessionCookie.startsWith(expectedStartsWith),
                                    String.format(
                                            "%s: Wrong cookie name. Expected '%s' but was '%s'",
                                            errMsg, expectedCookies, sessionCookie));
                        }
                    });
                    responseReceived.flag();
                }))));

    }
}
