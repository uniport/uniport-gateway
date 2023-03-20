package com.inventage.portal.gateway.core.entrypoint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.ProxyApplication;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.router.RouterFactory;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
        proxy = vertx.createHttpServer().requestHandler(req -> {
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

        routerFactory = new RouterFactory(vertx, "http", host, String.format("%d", proxyPort));
    }

    @AfterEach
    public void tearDown() {
        proxy.close();
        server.close();
    }

    /**
     * Testing entryMiddleware: is the entryMiddleware traversed for each request at a entrypoint?
     * We send two request on two different routes, we expect that the entry redirect middleware is traversed.
     * The redirect middleware sets a key-value pair in the response header.
     */
    @Test
    void entryRedirectMiddlewareRunsOnAllRoutes(Vertx vertx, VertxTestContext testCtx) {
        //given
        String entryPointIdentifier = "http" + proxyPort;
        String expectedRedirect = "/to/some/page";

        Map<String, JsonObject> configuration = oneEntryRedirectMiddlewareTwoRoutesConfiguration(entryPointIdentifier,
            expectedRedirect);
        JsonObject dynamicConfig = configuration.get("dynamic");
        JsonObject staticConfig = configuration.get("static");
        JsonObject entryMiddlewareConfig = configuration.get("entryMiddleware");

        Entrypoint entrypoint = new Entrypoint(vertx, entryPointIdentifier, proxyPort,
            new JsonArray().add(entryMiddlewareConfig));
        ProxyApplication proxyApplication = new ProxyApplication("proxy", entryPointIdentifier, staticConfig, vertx);
        entrypoint.mount(proxyApplication);

        routerFactory.createRouter(dynamicConfig).onComplete(testCtx.succeeding(router -> {
            entrypoint.router().route("/*").subRouter(router);
            proxyRouter = entrypoint.router();

            RequestOptions optA = new RequestOptions().setURI("/pathA");
            RequestOptions optB = new RequestOptions().setURI("/pathB");

            //when
            doRequest(vertx, testCtx, optA, responsePathA -> {
                //then
                assertEquals(HttpResponseStatus.FOUND.code(), responsePathA.statusCode(), "unexpected status code");
                assertEquals(expectedRedirect, responsePathA.headers().get("location"));
            });
            doRequest(vertx, testCtx, optB, responsePathB -> {
                //then
                assertEquals(HttpResponseStatus.FOUND.code(), responsePathB.statusCode(), "unexpected status code");
                assertEquals(expectedRedirect, responsePathB.headers().get("location"));
            });

            testCtx.completeNow();
        }));
    }

    private void doRequest(Vertx vertx, VertxTestContext testCtx, RequestOptions reqOpts,
        Consumer<HttpClientResponse> assertionHandler) {
        CountDownLatch latch = new CountDownLatch(1);

        reqOpts.setHost(host).setPort(proxyPort).setMethod(HttpMethod.GET);
        vertx.createHttpClient().request(reqOpts).compose(HttpClientRequest::send)
            .onComplete(testCtx.succeeding(resp -> testCtx.verify(() -> {
                assertionHandler.accept(resp);
                latch.countDown();
            })));

        try {
            latch.await();
        } catch (InterruptedException e) {
            testCtx.failNow(e);
        }
    }

    private Map<String, JsonObject> oneEntryRedirectMiddlewareTwoRoutesConfiguration(String entryPointIdentifier,
        String redirect) {
        JsonObject dynamicConfig = TestUtils.buildConfiguration(
            TestUtils.withRouters(TestUtils.withRouter("foo", TestUtils.withRouterService("bar"),
                TestUtils.withRouterRule("Path('/pathA')"), TestUtils.withRouterMiddlewares()),
                TestUtils.withRouter("foo2", TestUtils.withRouterService("bar"),
                    TestUtils.withRouterRule("Path('/pathB')"), TestUtils.withRouterMiddlewares())),
            TestUtils.withServices(
                TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer(host, serverPort)))));
        JsonObject staticConfig = TestUtils.buildStaticConfiguration(
            TestUtils.withEntrypoints(TestUtils.withEntrypoint(entryPointIdentifier, proxyPort),
                TestUtils.withApplication("proxy", entryPointIdentifier, "ProxyApplication",
                    TestUtils.withRequestSelector("/"))));

        JsonObject entryMiddlewareConfig = TestUtils.buildStaticConfiguration(
            TestUtils.withMiddleware("redirect", "redirectRegex",
                TestUtils.withMiddlewareOpts(
                    new JsonObject().put(DynamicConfiguration.MIDDLEWARE_REDIRECT_REGEX_REGEX, "/.*").put(
                        DynamicConfiguration.MIDDLEWARE_REDIRECT_REGEX_REPLACEMENT, redirect))));

        return Map.of("static", staticConfig, "dynamic", dynamicConfig, "entryMiddleware", entryMiddlewareConfig);
    }

}
