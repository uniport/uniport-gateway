package com.inventage.portal.gateway.proxy.service;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import com.inventage.portal.gateway.proxy.middleware.VertxAssertions;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

@ExtendWith(VertxExtension.class)
public class ReverseProxyTest {

    private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
    private static final String X_FORWARDED_HOST = "X-Forwarded-Host";
    private static final String X_FORWARDED_PORT = "X-Forwarded-Port";

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
        final String host = "localhost";
        final int proxyPort = TestUtils.findFreePort();
        final int backendPort = TestUtils.findFreePort();

        final Handler<RoutingContext> backendHandler = ctx -> {
            VertxAssertions.assertEquals(testCtx, "http", ctx.request().headers().get(X_FORWARDED_PROTO));
            VertxAssertions.assertEquals(testCtx, host + ":" + proxyPort, ctx.request().headers().get(X_FORWARDED_HOST));
            VertxAssertions.assertEquals(testCtx, String.valueOf(proxyPort), ctx.request().headers().get(X_FORWARDED_PORT));

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
                VertxAssertions.assertEquals(testCtx, HttpResponseStatus.OK.code(), response.statusCode());
                testCtx.completeNow();
            });
        });
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

}
