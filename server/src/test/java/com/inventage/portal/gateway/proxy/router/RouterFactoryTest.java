package com.inventage.portal.gateway.proxy.router;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(VertxExtension.class)
@SuppressWarnings("unchecked")
public class RouterFactoryTest {

    static final String host = "localhost";

    private HttpServer proxy;
    private HttpServer server;
    private int proxyPort;
    private int serverPort;
    private RouterFactory routerFactory;
    private Router proxyRouter;

    private static Stream<String> provideStringsForValidParseRule() {
        return Stream.of(
            "Path('/foo')",
            "Path('/foo/bar')",
            "Path('/.bar')",
            "Path('/.foo-bar/baz')",

            "PathPrefix('/foo')",
            "PathPrefix('/foo/bar')",
            "PathPrefix('/.bar')",
            "PathPrefix('/.foo-bar/baz')",

            "Host('/foo')",
            "Host('/foo/bar')",
            "Host('/.bar')",
            "Host('/.foo-bar/baz')");
    }

    private static Stream<String> provideStringsForInvalidParseRule() {
        return Stream.of(
            "Path(\"/foo\")",
            "PathPrefix(\"/foo\")",
            "Host(\"/foo\")",
            "Foo('/foo')");
    }

    @BeforeEach
    public void setup(Vertx vertx) throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);

        proxyPort = TestUtils.findFreePort();
        proxy = vertx.createHttpServer().requestHandler(req -> proxyRouter.handle(req)).listen(proxyPort, ready -> {
            if (ready.failed()) {
                throw new RuntimeException(ready.cause());
            }
            latch.countDown();
        });

        serverPort = TestUtils.findFreePort();
        server = vertx.createHttpServer().requestHandler(req -> req.response().setStatusCode(200).end("ok"))
            .listen(serverPort, ready -> {
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

    @Test
    public void configWithService(Vertx vertx, VertxTestContext testCtx) {
        JsonObject config = TestUtils.buildConfiguration(
            TestUtils.withRouters(TestUtils.withRouter("foo", TestUtils.withRouterService("bar"),
                TestUtils.withRouterRule("Path('/path')"))),
            TestUtils.withServices(
                TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer(host, serverPort)))));

        routerFactory.createRouter(config).onComplete(testCtx.succeeding(router -> {
            proxyRouter = router;
            RequestOptions reqOpts = new RequestOptions().setURI("/path");
            doRequest(vertx, testCtx, reqOpts, HttpResponseStatus.OK.code());
            testCtx.completeNow();
        }));
    }

    @Test
    public void configWithServiceAndMoreComplexPathSyntax(Vertx vertx, VertxTestContext testCtx) {
        JsonObject config = TestUtils.buildConfiguration(
            TestUtils.withRouters(TestUtils.withRouter("foo", TestUtils.withRouterService("bar"),
                TestUtils.withRouterRule("Path('/.well-known/any-path')"))),
            TestUtils.withServices(
                TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer(host, serverPort)))));

        routerFactory.createRouter(config).onComplete(testCtx.succeeding(router -> {
            proxyRouter = router;
            RequestOptions reqOpts = new RequestOptions().setURI("/.well-known/any-path");
            doRequest(vertx, testCtx, reqOpts, HttpResponseStatus.OK.code());
            testCtx.completeNow();
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
            testCtx.completeNow();
        }));
    }

    @Test
    public void configWithRedirectMiddleware(Vertx vertx, VertxTestContext testCtx) {
        JsonObject config = TestUtils.buildConfiguration(
            TestUtils.withRouters(TestUtils.withRouter("foo", TestUtils.withRouterService("bar"),
                TestUtils.withRouterRule("Path('/path')"), TestUtils.withRouterMiddlewares("redirect"))),
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
            testCtx.completeNow();
        }));
    }

    @Test
    public void healthyHealthCheck(Vertx vertx, VertxTestContext testCtx) {
        JsonObject config = TestUtils.buildConfiguration(
            TestUtils.withRouters(TestUtils.withRouter("foo", TestUtils.withRouterService("bar"),
                TestUtils.withRouterRule("Path('/path')"))),
            TestUtils.withServices(
                TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer(host, serverPort)))));

        routerFactory.createRouter(config).onComplete(testCtx.succeeding(router -> {
            proxyRouter = router;
            RequestOptions reqOpts = new RequestOptions().setURI("/health");
            doRequest(vertx, testCtx, reqOpts, HttpResponseStatus.OK.code());
            testCtx.completeNow();
        }));
    }

    @Test
    public void hostRule(Vertx vertx, VertxTestContext testCtx) {
        JsonObject config = TestUtils.buildConfiguration(
            TestUtils.withRouters(TestUtils.withRouter("foo", TestUtils.withRouterService("bar"),
                TestUtils.withRouterRule("Host('localhost')"))),
            TestUtils.withServices(
                TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer(host, serverPort)))));

        routerFactory.createRouter(config).onComplete(testCtx.succeeding(router -> {
            proxyRouter = router;
            RequestOptions reqOpts = new RequestOptions().setURI("/path");
            doRequest(vertx, testCtx, reqOpts, HttpResponseStatus.OK.code());
            reqOpts.setURI("/path/another");
            doRequest(vertx, testCtx, reqOpts, HttpResponseStatus.OK.code());
            testCtx.completeNow();
        }));
    }

    @Test
    public void pathRule(Vertx vertx, VertxTestContext testCtx) {
        JsonObject config = TestUtils.buildConfiguration(
            TestUtils.withRouters(
                TestUtils.withRouter("shortPath", TestUtils.withRouterService("bar"),
                    TestUtils.withRouterRule("Path('/path')")),
                TestUtils.withRouter("longPath", TestUtils.withRouterService("bar"),
                    TestUtils.withRouterRule("Path('/path/long')"))),
            TestUtils.withServices(
                TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer(host, serverPort)))));

        routerFactory.createRouter(config).onComplete(testCtx.succeeding(router -> {
            proxyRouter = router;
            RequestOptions reqOpts = new RequestOptions().setURI("/path");
            doRequest(vertx, testCtx, reqOpts, HttpResponseStatus.OK.code());
            reqOpts.setURI("/path/long");
            doRequest(vertx, testCtx, reqOpts, HttpResponseStatus.OK.code());
            testCtx.completeNow();
        }));
    }

    @Test
    public void pathPrefixRule(Vertx vertx, VertxTestContext testCtx) {
        JsonObject config = TestUtils.buildConfiguration(
            TestUtils.withRouters(TestUtils.withRouter("foo", TestUtils.withRouterService("bar"),
                TestUtils.withRouterRule("PathPrefix('/path')"))),
            TestUtils.withServices(
                TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer(host, serverPort)))));

        routerFactory.createRouter(config).onComplete(testCtx.succeeding(router -> {
            proxyRouter = router;
            RequestOptions reqOpts = new RequestOptions().setURI("/path");
            doRequest(vertx, testCtx, reqOpts, HttpResponseStatus.OK.code());
            reqOpts.setURI("/path/long");
            doRequest(vertx, testCtx, reqOpts, HttpResponseStatus.OK.code());
            testCtx.completeNow();
        }));
    }

    @Test
    public void pathPrefixRuleWithMoreComplexPathPrefixSyntax(Vertx vertx, VertxTestContext testCtx) {
        JsonObject config = TestUtils.buildConfiguration(
            TestUtils.withRouters(TestUtils.withRouter("foo", TestUtils.withRouterService("bar"),
                TestUtils.withRouterRule("PathPrefix('/.well-known/any-path')"))),
            TestUtils.withServices(
                TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer(host, serverPort)))));

        routerFactory.createRouter(config).onComplete(testCtx.succeeding(router -> {
            proxyRouter = router;
            RequestOptions reqOpts = new RequestOptions().setURI("/.well-known/any-path");
            doRequest(vertx, testCtx, reqOpts, HttpResponseStatus.OK.code());
            reqOpts.setURI("/.well-known/any-path/long");
            doRequest(vertx, testCtx, reqOpts, HttpResponseStatus.OK.code());
            testCtx.completeNow();
        }));
    }

    @Test
    public void unknownRule(Vertx vertx, VertxTestContext testCtx) {
        JsonObject config = TestUtils.buildConfiguration(
            TestUtils.withRouters(TestUtils.withRouter("foo", TestUtils.withRouterService("bar"),
                TestUtils.withRouterRule("unknownRule('blub')"))),
            TestUtils.withServices(
                TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer(host, serverPort)))));

        routerFactory.createRouter(config).onComplete(testCtx.failing(router -> {
            testCtx.completeNow();
        }));
    }

    @Test
    public void defaultRoutePriority(Vertx vertx, VertxTestContext testCtx) {
        // routes are per default ordered by length
        JsonObject config = TestUtils
            .buildConfiguration(
                TestUtils
                    .withRouters(
                        TestUtils.withRouter("shortPath", TestUtils.withRouterService("noServer"),
                            TestUtils.withRouterRule("PathPrefix('/path')")),
                        TestUtils.withRouter("longPath", TestUtils.withRouterService("bar"),
                            TestUtils.withRouterRule("PathPrefix('/path/long')"))),
                TestUtils.withServices(
                    TestUtils.withService("bar",
                        TestUtils.withServers(TestUtils.withServer(host, serverPort))),
                    TestUtils.withService("noServer",
                        TestUtils.withServers(TestUtils.withServer("some.host", 1234)))));

        routerFactory.createRouter(config).onComplete(testCtx.succeeding(router -> {
            proxyRouter = router;
            RequestOptions reqOpts = new RequestOptions().setURI("/path/long");
            doRequest(vertx, testCtx, reqOpts, HttpResponseStatus.OK.code());
            testCtx.completeNow();
        }));
    }

    @Test
    public void customRoutePriority(Vertx vertx, VertxTestContext testCtx) {
        JsonObject config = TestUtils
            .buildConfiguration(
                TestUtils.withRouters(TestUtils.withRouter("shortPath", TestUtils.withRouterService("bar"),
                    TestUtils.withRouterRule("PathPrefix('/path')"), TestUtils.withRouterPriority(100)),
                    TestUtils
                        .withRouter("longPath", TestUtils.withRouterService("noServer"),
                            TestUtils.withRouterRule("PathPrefix('/path/long')"))),
                TestUtils.withServices(
                    TestUtils.withService("bar",
                        TestUtils.withServers(TestUtils.withServer(host, serverPort))),
                    TestUtils.withService("noServer",
                        TestUtils.withServers(TestUtils.withServer("some.host", 1234)))));

        routerFactory.createRouter(config).onComplete(testCtx.succeeding(router -> {
            proxyRouter = router;
            RequestOptions reqOpts = new RequestOptions().setURI("/path/long");
            doRequest(vertx, testCtx, reqOpts, HttpResponseStatus.OK.code());
            testCtx.completeNow();
        }));
    }

    @Test
    public void failingMiddlewareCreation(Vertx vertx, VertxTestContext testCtx) {
        JsonObject config = TestUtils.buildConfiguration(
            TestUtils.withRouters(TestUtils.withRouter("foo", TestUtils.withRouterService("bar"),
                TestUtils.withRouterRule("Path('/path')"),
                TestUtils.withRouterMiddlewares("unknownMiddleware"))),
            TestUtils.withMiddlewares(TestUtils.withMiddleware("unknownMiddleware", "unknownMiddleware",
                TestUtils.withMiddlewareOpts(new JsonObject()))),
            TestUtils.withServices(
                TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer(host, serverPort)))));

        routerFactory.createRouter(config).onComplete(testCtx.failing(router -> {
            testCtx.completeNow();
        }));
    }

    @ParameterizedTest
    @MethodSource("provideStringsForValidParseRule")
    public void testValidParseRule(String input) {
        assertNotNull(routerFactory.parseRule(input),
            String.format("%s should be parseable into a routing rule", input));
    }

    @ParameterizedTest
    @MethodSource("provideStringsForInvalidParseRule")
    public void testInvalidParseRule(String input) {
        assertNull(routerFactory.parseRule(input),
            String.format("%s should not be parseable into a routing rule", input));
    }

    private void doRequest(Vertx vertx, VertxTestContext testCtx, RequestOptions reqOpts, int expectedStatusCode) {
        CountDownLatch latch = new CountDownLatch(1);

        reqOpts.setHost(host).setPort(proxyPort).setMethod(HttpMethod.GET);
        vertx.createHttpClient().request(reqOpts).compose(HttpClientRequest::send)
            .onComplete(testCtx.succeeding(resp -> testCtx.verify(() -> {
                assertEquals(expectedStatusCode, resp.statusCode(), "unexpected status code");
                latch.countDown();
            })));

        try {
            latch.await();
        } catch (InterruptedException e) {
            testCtx.failNow(e);
        }
    }
}
