package com.inventage.portal.gateway.core.entrypoint;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.ProxyApplication;
import com.inventage.portal.gateway.proxy.middleware.VertxAssertions;
import com.inventage.portal.gateway.proxy.middleware.redirectRegex.RedirectRegexMiddlewareFactory;
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
    static final String HOST = "localhost";
    static final String ENTRYPOINT_PREFIX = "http";

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

        routerFactory = new RouterFactory(vertx, "http", HOST, String.format("%d", proxyPort), ENTRYPOINT_PREFIX + proxyPort);
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
        final String entryPointIdentifier = ENTRYPOINT_PREFIX + proxyPort;
        final String expectedRedirect = "/to/some/page";

        final Map<String, JsonObject> configuration = oneEntryRedirectMiddlewareTwoRoutesConfiguration(entryPointIdentifier,
            expectedRedirect);
        final JsonObject dynamicConfig = configuration.get("dynamic");
        final JsonObject entryMiddlewareConfig = configuration.get("entryMiddleware");

        final Entrypoint entrypoint = new Entrypoint(vertx, entryPointIdentifier, proxyPort,
            new JsonArray().add(entryMiddlewareConfig));
        final ProxyApplication proxyApplication = new ProxyApplication(vertx, "proxy", entryPointIdentifier, 1234, JsonArray.of(), JsonObject.of());
        entrypoint.mount(proxyApplication);

        routerFactory.createRouter(dynamicConfig).onComplete(testCtx.succeeding(router -> {
            entrypoint.router().route("/*").subRouter(router);
            proxyRouter = entrypoint.router();

            final RequestOptions optA = new RequestOptions().setURI("/pathA");
            final RequestOptions optB = new RequestOptions().setURI("/pathB");

            //when
            doRequest(vertx, testCtx, optA, responsePathA -> {
                //then
                VertxAssertions.assertEquals(testCtx, HttpResponseStatus.FOUND.code(), responsePathA.statusCode(), "unexpected status code");
                VertxAssertions.assertEquals(testCtx, expectedRedirect, responsePathA.headers().get("location"));
            });
            doRequest(vertx, testCtx, optB, responsePathB -> {
                //then
                VertxAssertions.assertEquals(testCtx, HttpResponseStatus.FOUND.code(), responsePathB.statusCode(), "unexpected status code");
                VertxAssertions.assertEquals(testCtx, expectedRedirect, responsePathB.headers().get("location"));
            });

            testCtx.completeNow();
        }));
    }

    private void doRequest(
        Vertx vertx, VertxTestContext testCtx, RequestOptions reqOpts,
        Consumer<HttpClientResponse> assertionHandler
    ) {
        final CountDownLatch latch = new CountDownLatch(1);

        reqOpts.setHost(HOST).setPort(proxyPort).setMethod(HttpMethod.GET);
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

    private Map<String, JsonObject> oneEntryRedirectMiddlewareTwoRoutesConfiguration(
        String entryPointIdentifier,
        String redirect
    ) {
        final JsonObject dynamicConfig = TestUtils.buildConfiguration(
            TestUtils.withRouters(TestUtils.withRouter("foo", TestUtils.withRouterService("bar"),
                TestUtils.withRouterRule("Path('/pathA')"), TestUtils.withRouterMiddlewares(), TestUtils.withRouterEntrypoints(entryPointIdentifier)),
                TestUtils.withRouter("foo2", TestUtils.withRouterService("bar"),
                    TestUtils.withRouterRule("Path('/pathB')"), TestUtils.withRouterMiddlewares(), TestUtils.withRouterEntrypoints(entryPointIdentifier))),
            TestUtils.withServices(
                TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer(HOST, serverPort)))));

        final JsonObject entryMiddlewareConfig = TestUtils.buildStaticConfiguration(
            TestUtils.withMiddleware("redirect", "redirectRegex",
                TestUtils.withMiddlewareOpts(
                    new JsonObject()
                        .put(RedirectRegexMiddlewareFactory.REDIRECT_REGEX_REGEX, "/.*")
                        .put(RedirectRegexMiddlewareFactory.REDIRECT_REGEX_REPLACEMENT, redirect))));

        return Map.of("dynamic", dynamicConfig, "entryMiddleware", entryMiddlewareConfig);
    }

}
