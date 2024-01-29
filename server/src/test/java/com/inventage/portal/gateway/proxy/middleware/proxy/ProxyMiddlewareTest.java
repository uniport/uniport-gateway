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
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class ProxyMiddlewareTest {

    private static final String X_FORWARDED_HOST = "X-Forwarded-Host";
    private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";

    @Test
    void correctHostHeader(Vertx vertx, VertxTestContext testCtx) {
        // given
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withRoutingContextHolder(routingContext)
            .withProxyMiddleware("test.host.com", 8000)
            .build().start();
        // when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            Assertions.assertEquals("test.host.com",
                routingContext.get().request().headers().get(HttpHeaderNames.HOST));
            testCtx.completeNow();
        });
        // then
    }

    @Test
    void proxyTest(Vertx vertx, VertxTestContext testCtx) {
        final String host = "localhost";
        final int proxyPort = TestUtils.findFreePort();
        final int serverPort = TestUtils.findFreePort();
        final String proxyResponse = "proxy";
        final String serverResponse = "server";

        final ProxyMiddleware proxy = new ProxyMiddleware(vertx, "proxy", host, serverPort);

        final Checkpoint proxyStarted = testCtx.checkpoint();
        final Checkpoint serverStarted = testCtx.checkpoint();
        final Checkpoint requestProxied = testCtx.checkpoint();
        final Checkpoint requestServed = testCtx.checkpoint();
        final Checkpoint responseReceived = testCtx.checkpoint();

        final Router router = Router.router(vertx);
        router.route().handler(proxy).handler(ctx -> ctx.response().end(proxyResponse));
        vertx.createHttpServer().requestHandler(req -> {
            router.handle(req);
            requestProxied.flag();
        }).listen(proxyPort).onComplete(testCtx.succeeding(s -> {
            proxyStarted.flag();

            vertx.createHttpServer().requestHandler(req -> {
                assertEquals(String.format("%s:%s", host, proxyPort), req.headers().get(X_FORWARDED_HOST));
                assertEquals("http", req.headers().get(X_FORWARDED_PROTO));
                req.response().end(serverResponse);
                requestServed.flag();
            }).listen(serverPort).onComplete(testCtx.succeeding(p -> {
                serverStarted.flag();

                vertx.createHttpClient().request(HttpMethod.GET, proxyPort, host, "/blub")
                    .compose(HttpClientRequest::send)
                    .onComplete(testCtx.succeeding(resp -> {
                        testCtx.verify(() -> {
                            assertEquals(HttpResponseStatus.OK.code(), resp.statusCode());
                            responseReceived.flag();
                        });
                        resp.body().onComplete(testCtx.succeeding(
                            body -> testCtx.verify(() -> assertEquals(serverResponse, body.toString()))));
                    }));
            }));
        }));

    }

    @Test
    void proxyRequestToHTTPSServer(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String host = "localhost";
        final int serverPort = TestUtils.findFreePort();
        final File trustStoreFile = loadFile("truststore-test.jks");
        final String trustStorePassword = "123456";
        final String serverResponse = "server";

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withProxyMiddleware(host, serverPort, "https", false, false, trustStoreFile.getPath(), trustStorePassword)
            .build().start();

        final HttpServerOptions options = new HttpServerOptions()
            .setSsl(true)
            .setKeyStoreOptions(new JksOptions()
                .setPath(trustStoreFile.getPath())
                .setPassword(trustStorePassword));

        vertx.createHttpServer(options).requestHandler(req -> {
            req.response().end(serverResponse);
        }).listen(serverPort);
        //when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            response.body().onComplete((body) -> {
                //then
                Assertions.assertEquals(body.result().toString(), serverResponse);
                testCtx.completeNow();
            });
        });
    }

    @Test
    void proxyRequestToHTTPSServerTrustAllCertificates(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String host = "localhost";
        final int serverPort = TestUtils.findFreePort();
        final File file = loadFile("truststore-test.jks");
        final String serverResponse = "server";
        final boolean trustAllCertificates = true;

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withProxyMiddleware(host, serverPort, "https", trustAllCertificates, false, null, null)
            .build().start();

        final HttpServerOptions options = new HttpServerOptions()
            .setSsl(true)
            .setKeyStoreOptions(new JksOptions()
                .setPath(file.getPath())
                .setPassword("123456"));

        vertx.createHttpServer(options).requestHandler(req -> {
            req.response().end(serverResponse);
        }).listen(serverPort);
        //when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            response.body().onComplete((body) -> {
                //then
                Assertions.assertEquals(body.result().toString(), serverResponse);
                testCtx.completeNow();
            });
        });
    }

    @Test
    void proxyRequestToHTTPSServerWithoutCertificate(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String host = "localhost";
        final int serverPort = TestUtils.findFreePort();
        final String serverResponse = "server";
        final boolean trustAllCertificates = true;

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withProxyMiddleware(host, serverPort, "https", trustAllCertificates, false, null, null)
            .build().start();

        final HttpServerOptions options = new HttpServerOptions()
            .setSsl(true);

        vertx.createHttpServer(options).requestHandler(req -> {
            req.response().end(serverResponse);
        }).listen(serverPort);
        //when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            Assertions.assertEquals(response.statusCode(), HttpResponseStatus.BAD_GATEWAY.code());
            testCtx.completeNow();
        });
    }

    @Test
    void proxyRequestToNotHTTPSServer(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String host = "localhost";
        final int serverPort = TestUtils.findFreePort();
        final String serverResponse = "server";
        final boolean trustAllCertificates = true;

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withProxyMiddleware(host, serverPort, "https", trustAllCertificates, false, null, null)
            .build().start();

        final HttpServerOptions options = new HttpServerOptions();

        vertx.createHttpServer(options).requestHandler(req -> {
            req.response().end(serverResponse);
        }).listen(serverPort);
        //when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            //then
            Assertions.assertEquals(response.statusCode(), HttpResponseStatus.BAD_GATEWAY.code());
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
            .withBackend(vertx, backendPort, backendHandler())
            .build().start();
        // when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            // should be 1, put is actually 0 because of bug
            Assertions.assertEquals(1, counter.get().intValue());
        });
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            Assertions.assertEquals(0, counter.get().intValue());
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

    private Handler<RoutingContext> backendHandler() {
        return ctx -> {
            ctx.response().end();
        };
    }

    private static File loadFile(String path) {
        final ClassLoader classLoader = ProxyMiddlewareTest.class.getClassLoader();
        return new File(classLoader.getResource(path).getFile());
    }

}
