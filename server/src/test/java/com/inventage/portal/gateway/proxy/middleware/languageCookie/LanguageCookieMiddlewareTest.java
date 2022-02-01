package com.inventage.portal.gateway.proxy.middleware.languageCookie;

import com.inventage.portal.gateway.TestUtils;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.web.Router;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.inventage.portal.gateway.proxy.middleware.languageCookie.LanguageCookieMiddleware.IPS_LANGUAGE_COOKIE_NAME;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
public class LanguageCookieMiddlewareTest {

    // necessary for jaeger (OpenTracing)
    static {
        System.setProperty("JAEGER_SERVICE_NAME", "portal-gateway");
    }

    private static final String host = "localhost";

    @Test
    void removeCookieInRequests(Vertx vertx, VertxTestContext testCtx) {
        int port = TestUtils.findFreePort();
        final Checkpoint serverStarted = testCtx.checkpoint();
        final Checkpoint requestServed = testCtx.checkpoint();
        final LanguageCookieMiddleware languageHandler = new LanguageCookieMiddleware();
        final Router router = Router.router(vertx);

        router.route().handler(languageHandler);
        vertx.createHttpServer().requestHandler(req -> {
            router.handle(req);
            testCtx.verify(() -> {
                assertTrue(req.headers().contains(HttpHeaders.ACCEPT_LANGUAGE), "should contain accept language header");
                assertEquals("de", req.headers().get(HttpHeaders.ACCEPT_LANGUAGE),
                        "should match language");
            });
            requestServed.flag();
        }).listen(port).onComplete(testCtx.succeeding(s -> {
            serverStarted.flag();
            final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
            headers.add(HttpHeaders.COOKIE, IPS_LANGUAGE_COOKIE_NAME + "=de");
            vertx.createHttpClient().request(new RequestOptions().setMethod(HttpMethod.GET).setPort(port)
                    .setHost(host).setURI("/blub").setHeaders(headers)).compose(HttpClientRequest::send);
        }));
    }

    @Test
    void noCookieAvailable(Vertx vertx, VertxTestContext testCtx) {
        int port = TestUtils.findFreePort();
        final Checkpoint serverStarted = testCtx.checkpoint();
        final Checkpoint requestServed = testCtx.checkpoint();
        final LanguageCookieMiddleware languageHandler = new LanguageCookieMiddleware();
        final Router router = Router.router(vertx);

        router.route().handler(languageHandler);
        vertx.createHttpServer().requestHandler(req -> {
            router.handle(req);
            testCtx.verify(() -> {
                assertFalse(req.headers().contains(HttpHeaders.ACCEPT_LANGUAGE), "should contain accept language header");
            });
            requestServed.flag();
        }).listen(port).onComplete(testCtx.succeeding(s -> {
            serverStarted.flag();
            vertx.createHttpClient().request(new RequestOptions().setMethod(HttpMethod.GET).setPort(port)
                    .setHost(host)).compose(HttpClientRequest::send);
        }));
    }
}
