package com.inventage.portal.gateway.proxy.entryMiddleware;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.core.entrypoint.Entrypoint;
import com.inventage.portal.gateway.proxy.ProxyApplication;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.router.RouterFactory;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(VertxExtension.class)
public class EntryMiddlewareTest {
    static final String host = "localhost";

    private HttpServer proxy;
    private HttpServer server;
    private int proxyPort;
    private int serverPort;
    private RouterFactory routerFactory;
    private Router proxyRouter;

    @BeforeEach
    public void setup(Vertx vertx) throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);

        proxyPort = TestUtils.findFreePort();
        proxy = vertx.createHttpServer().requestHandler(req ->
        {
            proxyRouter.handle(req);
        }).listen(proxyPort, ready -> {
            if (ready.failed()) {
                throw new RuntimeException(ready.cause());
            }
            latch.countDown();
        });

        serverPort = TestUtils.findFreePort();
        server = vertx.createHttpServer().requestHandler(
                req -> {
                    req.response().setStatusCode(200).end("ok");
                }).listen(serverPort, ready -> {
            if (ready.failed()) {
                throw new RuntimeException(ready.cause());
            }
            latch.countDown();
        });

        latch.await();

        routerFactory = new RouterFactory(vertx, String.format("http://%s", host));
    }

    @AfterEach
    public void tearDown() {
        proxy.close();
        server.close();
    }

    @Test
    public void entryMiddlewareRunsOnAllRoutes(Vertx vertx, VertxTestContext testCtx) {
        //given
        JsonObject config = TestUtils.buildConfiguration(
                TestUtils.withRouters(TestUtils.withRouter("foo", TestUtils.withRouterService("bar"),
                                TestUtils.withRouterRule("Path('/pathA')"), TestUtils.withRouterMiddlewares()),
                        TestUtils.withRouter("foo2", TestUtils.withRouterService("bar"),
                                TestUtils.withRouterRule("Path('/pathB')"), TestUtils.withRouterMiddlewares())),
                TestUtils.withServices(
                        TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer(host, serverPort)))));

        String entryPointIdentifier = "http" + proxyPort;

        JsonObject staticConfig = TestUtils.buildStaticConfiguration(
                TestUtils.withEntrypoints(TestUtils.withEntrypoint(entryPointIdentifier, proxyPort),
                        TestUtils.withApplication("proxy", entryPointIdentifier, "ProxyApplication",
                                TestUtils.withRequestSelector("/")))
        );
        String expectedRedirect = "/to/some/page";
        JsonObject middlewareConfig = TestUtils.buildStaticConfiguration(
                TestUtils.withMiddleware("redirect", "redirectRegex",
                        TestUtils.withMiddlewareOpts(
                                new JsonObject().put(DynamicConfiguration.MIDDLEWARE_REDIRECT_REGEX_REGEX, "/.*").put(
                                        DynamicConfiguration.MIDDLEWARE_REDIRECT_REGEX_REPLACEMENT, expectedRedirect)))
        );

        Entrypoint entrypoint = new Entrypoint(vertx, entryPointIdentifier, proxyPort, true, 0, new JsonArray().add(middlewareConfig));
        ProxyApplication proxyApplication = new ProxyApplication("proxy", entryPointIdentifier, staticConfig, vertx);
        entrypoint.mount(proxyApplication);

        routerFactory.createRouter(config).onComplete(testCtx.succeeding(router -> {
            entrypoint.router().mountSubRouter("/", router);
            proxyRouter = entrypoint.router();

            Map<String, String> expectedHeaders = Map.of("location", expectedRedirect);
            RequestOptions optA = new RequestOptions().setURI("/pathA");
            RequestOptions optB = new RequestOptions().setURI("/pathB");

            //when
            //then
            doRequest(vertx, testCtx, optA, HttpResponseStatus.FOUND.code(), expectedHeaders);
            doRequest(vertx, testCtx, optB, HttpResponseStatus.FOUND.code(), expectedHeaders);
            testCtx.completeNow();
        }));
    }

        private void doRequest(Vertx vertx, VertxTestContext testCtx, RequestOptions reqOpts, int expectedStatusCode, Map<String, String> expectedHeaders) {
        CountDownLatch latch = new CountDownLatch(1);

        reqOpts.setHost(host).setPort(proxyPort).setMethod(HttpMethod.GET);
        vertx.createHttpClient().request(reqOpts).compose(HttpClientRequest::send).onComplete(testCtx.succeeding(resp -> testCtx.verify(() -> {
            assertEquals(expectedStatusCode, resp.statusCode(), "unexpected status code");
            expectedHeaders.forEach((headerKey, expectedValue) -> {
                String actualValue = resp.headers().get(headerKey);
                assertEquals(expectedValue, actualValue);
            });
            latch.countDown();
        })));

        try {
            latch.await();
        } catch (InterruptedException e) {
            testCtx.failNow(e);
        }
    }


}
