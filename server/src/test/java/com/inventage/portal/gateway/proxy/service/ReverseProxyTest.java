package com.inventage.portal.gateway.proxy.service;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import com.inventage.portal.gateway.proxy.middleware.VertxAssertions;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.UpgradeRejectedException;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.http.WebsocketVersion;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

@ExtendWith(VertxExtension.class)
public class ReverseProxyTest {

    private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
    private static final String X_FORWARDED_HOST = "X-Forwarded-Host";
    private static final String X_FORWARDED_PORT = "X-Forwarded-Port";
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    @Test
    void correctHostHeader(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final int backendPort = TestUtils.findFreePort();

        final Checkpoint hostVerified = testCtx.checkpoint();
        final Handler<RoutingContext> backendHandler = ctx -> {
            // then
            VertxAssertions.assertEquals(testCtx, "local.uniport.ch" + ":" + backendPort, ctx.request().headers().get(HttpHeaderNames.HOST));
            hostVerified.flag();
            ctx.response().end();
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withProxyMiddleware("local.uniport.ch", backendPort)
            .withBackend(vertx, backendPort, backendHandler)
            .build().start();

        // when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            VertxAssertions.assertEquals(testCtx, HttpResponseStatus.OK.code(), response.statusCode());
            testCtx.completeNow();
        });
    }

    @Test
    void proxyTest(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String host = "127.0.0.1";
        final int proxyPort = TestUtils.findFreePort();
        final int backendPort = TestUtils.findFreePort();
        final AtomicReference<String> clientAddressHolder = new AtomicReference<>();

        final Handler<RoutingContext> backendHandler = ctx -> {
            VertxAssertions.assertEquals(testCtx, "http", ctx.request().headers().get(X_FORWARDED_PROTO));
            VertxAssertions.assertEquals(testCtx, host + ":" + proxyPort, ctx.request().headers().get(X_FORWARDED_HOST));
            VertxAssertions.assertEquals(testCtx, String.valueOf(proxyPort), ctx.request().headers().get(X_FORWARDED_PORT));

            VertxAssertions.assertEquals(testCtx, host + ":" + backendPort, ctx.request().headers().get(HttpHeaderNames.HOST));
            VertxAssertions.assertEquals(testCtx, clientAddressHolder.get(), ctx.request().headers().get(X_FORWARDED_FOR));

            ctx.response().end();
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withProxyMiddleware(host, backendPort)
            .withBackend(vertx, backendPort, backendHandler)
            .build().start(proxyPort);

        // in order to capture the local address of the client we need to assemble it http client ourself
        final RequestOptions opt = new RequestOptions()
            .setHost(host)
            .setPort(proxyPort)
            .setURI("/")
            .setMethod(HttpMethod.GET);

        // when
        vertx.httpClientBuilder()
            .withConnectHandler(connection -> clientAddressHolder.set(connection.localAddress().toString()))
            .build()
            .request(opt)
            .compose(HttpClientRequest::send)
            .onComplete(testCtx.succeeding(response -> {
                response.body().onComplete((body) -> {
                    // then
                    VertxAssertions.assertEquals(testCtx, HttpResponseStatus.OK.code(), response.statusCode());
                    testCtx.completeNow();
                });
            }));
    }

    @Test
    void proxyPreserveXForwardedHeadersTest(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String host = "localhost";
        final int proxyPort = TestUtils.findFreePort();
        final int backendPort = TestUtils.findFreePort();

        final String proto = "foo";
        final String port = String.valueOf(1234);

        final Handler<RoutingContext> backendHandler = ctx -> {
            VertxAssertions.assertEquals(testCtx, proto, ctx.request().headers().get(X_FORWARDED_PROTO));
            VertxAssertions.assertEquals(testCtx, port, ctx.request().headers().get(X_FORWARDED_PORT));

            ctx.response().end();
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withProxyMiddleware(host, backendPort)
            .withBackend(vertx, backendPort, backendHandler)
            .build().start(proxyPort);

        //when
        gateway.incomingRequest(HttpMethod.GET, "/",
            new RequestOptions()
                .addHeader(X_FORWARDED_PROTO, proto)
                .addHeader(X_FORWARDED_PORT, port),
            response -> {
                response.body().onComplete((body) -> {
                    //then
                    VertxAssertions.assertEquals(testCtx, HttpResponseStatus.OK.code(), response.statusCode());
                    testCtx.completeNow();
                });
            });
    }

    @Test
    void shouldLogRequest(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final ListAppender<ILoggingEvent> listAppender = new ListAppender<ILoggingEvent>();
        final Logger logger = setupLogger(listAppender);
        final int backendPort = TestUtils.findFreePort();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withProxyMiddleware(backendPort, true)
            .withBackend(vertx, backendPort, ctx -> ctx.response().end("ok"))
            .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            //then
            VertxAssertions.assertTrue(testCtx, listAppender.list.stream().anyMatch(event -> event.getFormattedMessage().matches(".*" + "http/1.1 GET / - Host: " + ".*")));
            testCtx.completeNow();
            logger.detachAppender(listAppender);
        });
    }

    @Test
    void shouldLogResponse(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final ListAppender<ILoggingEvent> listAppender = new ListAppender<ILoggingEvent>();
        final Logger logger = setupLogger(listAppender);
        final int backendPort = TestUtils.findFreePort();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withProxyMiddleware(backendPort, true)
            .withBackend(vertx, backendPort, ctx -> ctx.response().end("ok"))
            .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            //then
            VertxAssertions.assertTrue(testCtx, listAppender.list.stream().anyMatch(event -> event.getFormattedMessage().matches(".*" + "200 OK -" + ".*")));
            testCtx.completeNow();
            logger.detachAppender(listAppender);
        });
    }

    private Logger setupLogger(ListAppender<ILoggingEvent> listAppender) {
        final Logger logger = (Logger) LoggerFactory.getLogger(ReverseProxy.class);
        logger.addAppender(listAppender);
        listAppender.start();
        return logger;
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
                VertxAssertions.assertEquals(testCtx, HttpResponseStatus.OK.code(), response.statusCode());
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
            VertxAssertions.assertEquals(testCtx, HttpResponseStatus.OK.code(), response.statusCode());
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
            VertxAssertions.assertEquals(testCtx, response.statusCode(), HttpResponseStatus.BAD_GATEWAY.code());
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
            VertxAssertions.assertEquals(testCtx, response.statusCode(), HttpResponseStatus.BAD_GATEWAY.code());
            testCtx.completeNow();
        });
    }

    private static File loadFile(String path) {
        final ClassLoader classLoader = ReverseProxyTest.class.getClassLoader();
        return new File(classLoader.getResource(path).getFile());
    }

    @Test
    public void testWebSocketV00(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        testWebSocket(vertx, testCtx, WebsocketVersion.V00);
    }

    @Test
    public void testWebSocketV07(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        testWebSocket(vertx, testCtx, WebsocketVersion.V07);
    }

    @Test
    public void testWebSocketV08(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        testWebSocket(vertx, testCtx, WebsocketVersion.V08);
    }

    @Test
    public void testWebSocketV13(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        testWebSocket(vertx, testCtx, WebsocketVersion.V13);
    }

    private void testWebSocket(Vertx vertx, VertxTestContext testCtx, WebsocketVersion version) throws InterruptedException {
        // given
        final String host = "localhost";
        final int proxyPort = TestUtils.findFreePort();
        final int backendPort = TestUtils.findFreePort();
        final String message = "ping";
        final Checkpoint wsClosed = testCtx.checkpoint();

        final Handler<RoutingContext> backendHandler = ctx -> {
            final Future<ServerWebSocket> fut = ctx.request().toWebSocket();
            fut.onComplete(testCtx.succeeding(ws -> {
                ws.handler(buff -> ws.write(buff)); // echo message
                ws.closeHandler(v -> {
                    wsClosed.flag();
                });
            }));
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withProxyMiddleware(host, backendPort)
            .withBackend(vertx, backendPort, backendHandler)
            .build()
            .start(proxyPort);

        // when
        final WebSocketClient client = vertx.createWebSocketClient();
        final WebSocketConnectOptions options = new WebSocketConnectOptions()
            .setHost(host)
            .setPort(proxyPort)
            .setURI("/ws")
            .setVersion(version);
        client.connect(options, testCtx.succeeding(ws -> {
            ws.write(Buffer.buffer(message));
            ws.handler(buff -> {
                testCtx.verify(() -> assertEquals(message, buff.toString()));
                ws.close();
            });
        }));
    }

    @Test
    public void testWebSocketReject(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String host = "localhost";
        final int proxyPort = TestUtils.findFreePort();
        final int backendPort = TestUtils.findFreePort();

        final Handler<RoutingContext> backendHandler = ctx -> {
            ctx.response().setStatusCode(400).end();
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withProxyMiddleware(host, backendPort)
            .withBackend(vertx, backendPort, backendHandler)
            .build()
            .start(proxyPort);

        final WebSocketClient client = vertx.createWebSocketClient();
        final WebSocketConnectOptions options = new WebSocketConnectOptions()
            .setHost(host)
            .setPort(proxyPort)
            .setURI("/ws");

        // when
        client.connect(options, testCtx.failing(err -> {
            // then
            testCtx.verify(() -> assertTrue(err.getClass() == UpgradeRejectedException.class));
            testCtx.completeNow();
        }));
    }

    @Test
    public void testInboundClose(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String host = "localhost";
        final int proxyPort = TestUtils.findFreePort();
        final int backendPort = TestUtils.findFreePort();

        final Handler<NetSocket> backendHandler = so -> {
            so.handler(buff -> {
                so.close();
            });
        };
        startNetBackend(vertx, backendPort, backendHandler);

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withProxyMiddleware(host, backendPort)
            .build()
            .start(proxyPort);

        final WebSocketClient client = vertx.createWebSocketClient();
        final WebSocketConnectOptions options = new WebSocketConnectOptions()
            .setHost(host)
            .setPort(proxyPort)
            .setURI("/ws");

        // when
        client.connect(options, testCtx.failing(err -> {
            // then
            testCtx.verify(() -> assertTrue(err.getClass() == UpgradeRejectedException.class));
            testCtx.completeNow();
        }));
    }

    protected void startNetBackend(Vertx vertx, int port, Handler<NetSocket> handler) throws InterruptedException {
        final VertxTestContext testContext = new VertxTestContext();

        final NetServer backendServer = vertx.createNetServer(new HttpServerOptions().setPort(port).setHost("localhost"));
        backendServer.connectHandler(handler);
        backendServer.listen(testContext.succeeding(s -> testContext.completeNow()));

        if (!testContext.awaitCompletion(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timeout: Server did not start in time.");
        }
    }

    @Test
    public void testWebSocketFirefox(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String host = "localhost";
        final int proxyPort = TestUtils.findFreePort();
        final int backendPort = TestUtils.findFreePort();
        final Checkpoint wsClosed = testCtx.checkpoint();

        final Handler<RoutingContext> backendHandler = ctx -> {
            final Future<ServerWebSocket> fut = ctx.request().toWebSocket();
            fut.onComplete(testCtx.succeeding(ws -> {
                ws.closeHandler(v -> {
                    wsClosed.flag();
                });
            }));
        };

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withProxyMiddleware(host, backendPort)
            .withBackend(vertx, backendPort, backendHandler)
            .build()
            .start(proxyPort);

        final HttpClient httpClient = vertx.createHttpClient();
        final RequestOptions options = new RequestOptions()
            .setHost(host)
            .setPort(proxyPort)
            .setURI("/ws")
            .putHeader("Origin", String.format("http://%s:%d", host, proxyPort))
            .putHeader("Connection", "keep-alive, Upgrade")
            .putHeader("Upgrade", "Websocket")
            .putHeader("Sec-WebSocket-Version", "13")
            .putHeader("Sec-WebSocket-Key", "xy6UoM3l3TcREmAeAhZuYQ==");

        // when
        httpClient.request(options).onComplete(testCtx.succeeding(clientRequest -> {
            clientRequest.connect().onComplete(testCtx.succeeding(response -> {
                // then
                testCtx.verify(() -> assertEquals(101, response.statusCode()));
                response.netSocket().close();
            }));
        }));
    }
}
