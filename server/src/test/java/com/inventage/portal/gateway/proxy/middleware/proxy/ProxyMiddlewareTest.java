package com.inventage.portal.gateway.proxy.middleware.proxy;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class ProxyMiddlewareTest {

    private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
    private static final String X_FORWARDED_HOST = "X-Forwarded-Host";
    private static final String X_FORWARDED_PORT = "X-Forwarded-Port";

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.MINUTES)
    void correctHostHeader(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final int backendPort = TestUtils.findFreePort();

        final Checkpoint hostVerified = testCtx.checkpoint();
        final Handler<RoutingContext> backendHandler = ctx -> {
            // then
            testCtx.verify(() -> {
                Assertions.assertEquals("localdev.me" + ":" + backendPort, ctx.request().headers().get(HttpHeaderNames.HOST));
                hostVerified.flag();
            });
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withProxyMiddleware("localdev.me", backendPort)
            .withBackend(vertx, backendPort, backendHandler)
            .build().start();

        // when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            testCtx.verify(() -> {
                Assertions.assertEquals(HttpResponseStatus.OK.code(), response.statusCode());
            });
            testCtx.completeNow();
        });
    }

    @Test
    void proxyTest(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String host = "localhost";
        final int proxyPort = TestUtils.findFreePort();
        final int backendPort = TestUtils.findFreePort();

        final Handler<RoutingContext> backendHandler = ctx -> {
            testCtx.verify(() -> {
                assertEquals("http", ctx.request().headers().get(X_FORWARDED_PROTO));
                assertEquals(host + ":" + proxyPort, ctx.request().headers().get(X_FORWARDED_HOST));
                assertEquals(String.valueOf(proxyPort), ctx.request().headers().get(X_FORWARDED_PORT));
            });

            ctx.response().end();
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withProxyMiddleware(host, backendPort)
            .withBackend(vertx, backendPort, backendHandler)
            .build().start(proxyPort);

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            response.body().onComplete((body) -> {
                //then
                testCtx.verify(() -> {
                    Assertions.assertEquals(HttpResponseStatus.OK.code(), response.statusCode());
                });
                testCtx.completeNow();
            });
        });
    }

    @Test
    void proxyRequestToHTTPSServer(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //given
        final String host = "localhost";
        final int backendPort = TestUtils.findFreePort();
        final File trustStoreFile = loadFile("truststore-test.jks");
        final String trustStorePassword = "123456";
        final HttpServerOptions serverOptions = new HttpServerOptions()
            .setSsl(true)
            .setKeyCertOptions(new JksOptions()
                .setPath(trustStoreFile.getPath())
                .setPassword(trustStorePassword));

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withProxyMiddleware(host, backendPort, "https", false, false, trustStoreFile.getPath(), trustStorePassword)
            .withBackend(vertx, backendPort, serverOptions)
            .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            response.body().onComplete((body) -> {
                //then
                testCtx.verify(() -> {
                    Assertions.assertEquals(HttpResponseStatus.OK.code(), response.statusCode());
                });
                testCtx.completeNow();
            });
        });
    }

    @Test
    void proxyRequestToHTTPSServerTrustAllCertificates(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //given
        final String host = "localhost";
        final int backendPort = TestUtils.findFreePort();
        final File file = loadFile("truststore-test.jks");
        final boolean trustAllCertificates = true;
        final HttpServerOptions serverOptions = new HttpServerOptions()
            .setSsl(true)
            .setKeyCertOptions(new JksOptions()
                .setPath(file.getPath())
                .setPassword("123456"));

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withProxyMiddleware(host, backendPort, "https", trustAllCertificates, false, null, null)
            .withBackend(vertx, backendPort, serverOptions)
            .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            //then
            testCtx.verify(() -> {
                Assertions.assertEquals(HttpResponseStatus.OK.code(), response.statusCode());
            });
            testCtx.completeNow();
        });
    }

    @Test
    void proxyRequestToHTTPSServerWithoutCertificate(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //given
        final String host = "localhost";
        final int backendPort = TestUtils.findFreePort();
        final boolean trustAllCertificates = true;

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withProxyMiddleware(host, backendPort, "https", trustAllCertificates, false, null, null)
            .withBackend(vertx, backendPort, new HttpServerOptions().setSsl(true))
            .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            testCtx.verify(() -> {
                Assertions.assertEquals(response.statusCode(), HttpResponseStatus.BAD_GATEWAY.code());
            });
            testCtx.completeNow();
        });
    }

    @Test
    void proxyRequestToNotHTTPSServer(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //given
        final String host = "localhost";
        final int backendPort = TestUtils.findFreePort();
        final boolean trustAllCertificates = true;

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withProxyMiddleware(host, backendPort, "https", trustAllCertificates, false, null, null)
            .withBackend(vertx, backendPort)
            .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            //then
            testCtx.verify(() -> {
                Assertions.assertEquals(response.statusCode(), HttpResponseStatus.BAD_GATEWAY.code());
            });
            testCtx.completeNow();
        });
    }

    @Test
    @Disabled // test implementation is instable, add/execution/remove of response modifiers must be per request
    void AKKP_1003(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final int backendPort = TestUtils.findFreePort();

        final AtomicReference<Integer> counter = new AtomicReference<>();
        counter.set(0);
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withRoutingContextHolder(routingContext)
            .withSessionMiddleware()
            .withMiddleware(incrementDecrementResponseModifier(counter))
            .withProxyMiddleware(backendPort)
            .withBackend(vertx, backendPort)
            .build().start();
        // when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            // should be 1, put is actually 0 because of bug
            testCtx.verify(() -> {
                Assertions.assertEquals(1, counter.get().intValue());
            });
        });
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            testCtx.verify(() -> {
                Assertions.assertEquals(0, counter.get().intValue());
            });
            // then
            testCtx.completeNow();
        });
    }

    // increment and decrement a counter during request processing
    private Handler<RoutingContext> incrementDecrementResponseModifier(AtomicReference<Integer> counter) {
        return ctx -> {
            List<Handler> modifiers = ctx.get(Middleware.RESPONSE_HEADERS_MODIFIERS);
            if (modifiers == null) {
                modifiers = new ArrayList<>();
            }
            // add a modifier (= ProxyInterceptor) when receiving the response from the backend
            modifiers.add(new Handler() {
                @Override
                public void handle(Object event) {
                    // decrement during outgoing response
                    counter.set(counter.get().intValue() - 1);
                }
            });
            // increment during incoming request
            counter.set(counter.get().intValue() + 1);
            ctx.put(Middleware.RESPONSE_HEADERS_MODIFIERS, modifiers);
            ctx.next();
        };
    }

    private static File loadFile(String path) {
        final ClassLoader classLoader = ProxyMiddlewareTest.class.getClassLoader();
        return new File(classLoader.getResource(path).getFile());
    }

}
