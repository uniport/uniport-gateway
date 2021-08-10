package com.inventage.portal.gateway.proxy.router;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CountDownLatch;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
@SuppressWarnings("unchecked")
public class RouterFactoryTest {
    static final String host = "localhost";
    static final String routerRule = "Path('/path')";

    private HttpServer proxy;
    private HttpServer server;
    private int proxyPort;
    private int serverPort;
    private RouterFactory routerFactory;
    private Router proxyRouter;

    @BeforeEach
    public void setup(Vertx vertx) throws Exception {
        final CountDownLatch proxyLatch = new CountDownLatch(1);
        final CountDownLatch serverLatch = new CountDownLatch(1);

        proxyPort = TestUtils.findFreePort();
        proxy = vertx.createHttpServer().requestHandler(req -> {
            proxyRouter.handle(req);
        }).listen(proxyPort, ready -> {
            if (ready.failed()) {
                throw new RuntimeException(ready.cause());
            }
            proxyLatch.countDown();
        });

        serverPort = TestUtils.findFreePort();
        server = vertx.createHttpServer().requestHandler(req -> {
            req.response().setStatusCode(200).end("ok");
        }).listen(serverPort, ready -> {
            if (ready.failed()) {
                throw new RuntimeException(ready.cause());
            }
            serverLatch.countDown();
        });

        proxyLatch.await();
        serverLatch.await();

        routerFactory = new RouterFactory(vertx, String.format("http://%s", host));
    }

    @AfterEach
    public void tearDown() throws Exception {
        proxy.close();
        server.close();
    }

    @Test
    public void configWithService(Vertx vertx, VertxTestContext testCtx) {
        JsonObject config = TestUtils.buildConfiguration(
                TestUtils.withRouters(TestUtils.withRouter("foo", TestUtils.withRouterService("bar"),
                        TestUtils.withRouterRule(routerRule))),
                TestUtils.withServices(
                        TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer(host, serverPort)))));

        routerFactory.createRouter(config).onComplete(testCtx.succeeding(router -> {
            proxyRouter = router;
            RequestOptions reqOpts = new RequestOptions().setURI("/path");
            doRequest(vertx, testCtx, reqOpts, HttpResponseStatus.OK.code());
        }));
    }

    @Test
    public void configWithEmptyService(Vertx vertx, VertxTestContext testCtx) {
        JsonObject config = TestUtils.buildConfiguration(TestUtils.withRouters(), TestUtils.withMiddlewares(),
                TestUtils.withServices());

        routerFactory.createRouter(config).onComplete(testCtx.succeeding(router -> {
            proxyRouter = router;
            RequestOptions reqOpts = new RequestOptions().setURI("/path");
            doRequest(vertx, testCtx, reqOpts, HttpResponseStatus.NOT_FOUND.code());
        }));
    }

    @Test
    public void configWithRedirectMiddleware(Vertx vertx, VertxTestContext testCtx) {
        JsonObject config = TestUtils.buildConfiguration(
                TestUtils.withRouters(TestUtils.withRouter("foo", TestUtils.withRouterService("bar"),
                        TestUtils.withRouterRule(routerRule), TestUtils.withRouterMiddlewares("redirect"))),
                TestUtils.withMiddlewares(TestUtils.withMiddleware("redirect", "redirectRegex",
                        TestUtils.withMiddlewareOpts(
                                new JsonObject().put(DynamicConfiguration.MIDDLEWARE_REDIRECT_REGEX_REGEX, ".*").put(
                                        DynamicConfiguration.MIDDLEWARE_REDIRECT_REGEX_REPLACEMENT, "/redirect")))),
                TestUtils.withServices(
                        TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer(host, serverPort)))));

        routerFactory.createRouter(config).onComplete(testCtx.succeeding(router -> {
            proxyRouter = router;
            RequestOptions reqOpts = new RequestOptions().setURI("/path");
            doRequest(vertx, testCtx, reqOpts, HttpResponseStatus.FOUND.code());
        }));
    }

    @Test
    public void healthCheck(Vertx vertx, VertxTestContext testCtx) {
        JsonObject config = TestUtils.buildConfiguration(TestUtils.withRouters(), TestUtils.withMiddlewares(),
                TestUtils.withServices());

        routerFactory.createRouter(config).onComplete(testCtx.succeeding(router -> {
            proxyRouter = router;
            RequestOptions reqOpts = new RequestOptions().setURI("/health");
            doRequest(vertx, testCtx, reqOpts, HttpResponseStatus.OK.code());
        }));
    }

    void doRequest(Vertx vertx, VertxTestContext testCtx, RequestOptions reqOpts, int expectedStatusCode) {
        reqOpts.setHost(host).setPort(proxyPort).setMethod(HttpMethod.GET);
        vertx.createHttpClient().request(reqOpts).compose(req -> req.send()).onComplete(testCtx.succeeding(resp -> {
            testCtx.verify(() -> {
                assertEquals(expectedStatusCode, resp.statusCode(), "unexpected status code");
            });
            testCtx.completeNow();
        }));
    }
}
