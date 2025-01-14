package com.inventage.portal.gateway.proxy.router;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.VertxAssertions;
import com.inventage.portal.gateway.proxy.middleware.redirectRegex.RedirectRegexMiddlewareFactory;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(VertxExtension.class)
@SuppressWarnings("unchecked")
public class RouterFactoryTest {

    static final String host = "localhost";
    static final String entrypointName = "test";

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
        vertx.createHttpServer()
            .requestHandler(req -> {
                proxyRouter.handle(req);
            })
            .listen(proxyPort)
            .onComplete(r -> {
                if (r.failed()) {
                    throw new RuntimeException(r.cause());
                }
                proxy = r.result();
                latch.countDown();
            });

        serverPort = TestUtils.findFreePort();
        vertx.createHttpServer()
            .requestHandler(req -> {
                req.response().setStatusCode(200).end("ok");
            })
            .listen(serverPort)
            .onComplete(r -> {
                if (r.failed()) {
                    throw new RuntimeException(r.cause());
                }
                server = r.result();
                latch.countDown();
            });

        latch.await();

        routerFactory = new RouterFactory(vertx, "http", host, String.format("%d", proxyPort), entrypointName);
    }

    @AfterEach
    public void tearDown() {
        proxy.close();
        server.close();
    }

    @Test
    public void configWithService(Vertx vertx, VertxTestContext testCtx) {
        // given
        JsonObject config = TestUtils.buildConfiguration(
            TestUtils.withRouters(
                TestUtils.withRouter("foo",
                    TestUtils.withRouterService("bar"),
                    TestUtils.withRouterRule("Path('/path')"),
                    TestUtils.withRouterEntrypoints(entrypointName))),
            TestUtils.withServices(
                TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer(host, serverPort)))));

        routerFactory.createRouter(config)
            .onComplete(testCtx.succeeding(router -> {
                proxyRouter = router;
                // when
                doRequest(vertx, host, "/path")
                    .onComplete(testCtx.succeeding(resp -> testCtx.verify(() -> {
                        // then
                        assertEquals(HttpResponseStatus.OK.code(), resp.statusCode());
                        testCtx.completeNow();
                    })));
            }));
    }

    @Test
    public void configWithEmptyService(Vertx vertx, VertxTestContext testCtx) {
        // given
        JsonObject config = TestUtils.buildConfiguration(
            TestUtils.withRouters(),
            TestUtils.withMiddlewares(),
            TestUtils.withServices());

        routerFactory.createRouter(config)
            .onComplete(testCtx.succeeding(router -> {
                proxyRouter = router;
                // when
                doRequest(vertx, host, "/path")
                    .onComplete(testCtx.succeeding(resp -> testCtx.verify(() -> {
                        // then
                        assertEquals(HttpResponseStatus.NOT_FOUND.code(), resp.statusCode());
                        testCtx.completeNow();
                    })));
            }));
    }

    @Test
    public void configWithRedirectMiddleware(Vertx vertx, VertxTestContext testCtx) {
        // given
        JsonObject config = TestUtils.buildConfiguration(
            TestUtils.withRouters(
                TestUtils.withRouter("foo",
                    TestUtils.withRouterService("bar"),
                    TestUtils.withRouterRule("Path('/path')"),
                    TestUtils.withRouterMiddlewares("redirect"),
                    TestUtils.withRouterEntrypoints(entrypointName))),
            TestUtils.withMiddlewares(
                TestUtils.withMiddleware("redirect", "redirectRegex",
                    TestUtils.withMiddlewareOpts(new JsonObject()
                        .put(RedirectRegexMiddlewareFactory.MIDDLEWARE_REDIRECT_REGEX_REGEX, ".*")
                        .put(RedirectRegexMiddlewareFactory.MIDDLEWARE_REDIRECT_REGEX_REPLACEMENT, "/redirect")))),
            TestUtils.withServices(
                TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer(host, serverPort)))));

        routerFactory.createRouter(config)
            .onComplete(testCtx.succeeding(router -> {
                proxyRouter = router;
                // when
                doRequest(vertx, host, "/path")
                    .onComplete(testCtx.succeeding(resp -> testCtx.verify(() -> {
                        // then
                        assertEquals(HttpResponseStatus.FOUND.code(), resp.statusCode());
                        testCtx.completeNow();
                    })));
            }));
    }

    @Test
    public void healthyHealthCheck(Vertx vertx, VertxTestContext testCtx) {
        // given
        JsonObject config = TestUtils.buildConfiguration(
            TestUtils.withRouters(
                TestUtils.withRouter("foo",
                    TestUtils.withRouterService("bar"),
                    TestUtils.withRouterRule("Path('/path')"),
                    TestUtils.withRouterEntrypoints(entrypointName))),
            TestUtils.withServices(
                TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer(host, serverPort)))));

        routerFactory.createRouter(config)
            .onComplete(testCtx.succeeding(router -> {
                proxyRouter = router;
                // when
                doRequest(vertx, host, "/health")
                    .onComplete(testCtx.succeeding(resp -> testCtx.verify(() -> {
                        // then
                        assertEquals(HttpResponseStatus.OK.code(), resp.statusCode());
                        testCtx.completeNow();
                    })));
            }));
    }

    private static Stream<Arguments> provideRulesForRouting() {
        return Stream.of(
            Arguments.of("Host('foo')", "foo", "/", HttpResponseStatus.OK.code()),
            Arguments.of("Host('foo')", "foo", "/path", HttpResponseStatus.OK.code()),
            Arguments.of("Host('foo')", "bar", "/", HttpResponseStatus.NOT_FOUND.code()),
            Arguments.of("HostRegex('foo(bar|baz)')", "foobar", "/", HttpResponseStatus.OK.code()),
            Arguments.of("HostRegex('foo(bar|baz)')", "foobaz", "/", HttpResponseStatus.OK.code()),
            Arguments.of("HostRegex('foo(bar|baz)')", "fooblub", "/", HttpResponseStatus.NOT_FOUND.code()),
            Arguments.of("HostRegex('foo(bar|baz)')", "foo", "/", HttpResponseStatus.NOT_FOUND.code()),
            Arguments.of("Path('/foo')", host, "/foo", HttpResponseStatus.OK.code()),
            Arguments.of("Path('/foo')", host, "/bar", HttpResponseStatus.NOT_FOUND.code()),
            Arguments.of("Path('/path')", host, "/path", HttpResponseStatus.OK.code()),
            Arguments.of("Path('/path')", host, "/path/long", HttpResponseStatus.NOT_FOUND.code()),
            Arguments.of("Path('/path')", host, "/path?parameter=value", HttpResponseStatus.OK.code()),
            Arguments.of("PathRegex('/foo/(bar|baz)')", host, "/foo/bar", HttpResponseStatus.OK.code()),
            Arguments.of("PathRegex('/foo/(bar|baz)')", host, "/foo/baz", HttpResponseStatus.OK.code()),
            Arguments.of("PathRegex('/foo/(bar|baz)')", host, "/foo/blub", HttpResponseStatus.NOT_FOUND.code()),
            Arguments.of("PathRegex('/foo/.*')", host, "/foo/blub", HttpResponseStatus.OK.code()),
            Arguments.of("PathRegex('/foo/.*')", host, "/foo", HttpResponseStatus.NOT_FOUND.code()),
            Arguments.of("PathPrefix('/path')", host, "/path", HttpResponseStatus.OK.code()),
            Arguments.of("PathPrefix('/path')", host, "/path/long", HttpResponseStatus.OK.code()),
            Arguments.of("PathPrefix('/.well-known/any-path')", host, "/bar", HttpResponseStatus.NOT_FOUND.code()),
            Arguments.of("PathPrefix('/.well-known/any-path/long')", host, "/bar", HttpResponseStatus.NOT_FOUND.code()),
            Arguments.of("PathPrefix('/path')", host, "/path?parameter=value", HttpResponseStatus.OK.code()),
            Arguments.of("PathPrefixRegex('/foo/(a|b)/')", host, "/foo/ab/", HttpResponseStatus.NOT_FOUND.code()),
            Arguments.of("PathPrefixRegex('/foo/(a|b)/')", host, "/foo/a/", HttpResponseStatus.OK.code()),
            Arguments.of("PathPrefixRegex('/foo/(a|b)/')", host, "/foo/a/bar", HttpResponseStatus.OK.code()),
            Arguments.of("PathPrefixRegex('/foo/(a|b)')", host, "/foo/", HttpResponseStatus.NOT_FOUND.code()),
            Arguments.of("PathPrefixRegex('/foo/(a|b)')", host, "/foo/c", HttpResponseStatus.NOT_FOUND.code()),
            Arguments.of("PathPrefixRegex('/foo/(a|b)')", host, "/foo/a", HttpResponseStatus.OK.code()),
            Arguments.of("PathPrefixRegex('/foo/(a|b)')", host, "/foo/bar", HttpResponseStatus.OK.code()),
            Arguments.of("PathPrefixRegex('/foo/(a|b)')", host, "/foo/bar/baz", HttpResponseStatus.OK.code()),
            Arguments.of("PathPrefixRegex('/foo/(a|b)')", host, "/foo/b/baz", HttpResponseStatus.OK.code()),
            Arguments.of("PathPrefixRegex('/foo/(a|b)')", host, "/foo", HttpResponseStatus.NOT_FOUND.code()) //
        );
    }

    @ParameterizedTest
    @MethodSource("provideRulesForRouting")
    public void testRoutingRules(String rule, String virtualHost, String path, int expectedStatusCode, Vertx vertx, VertxTestContext testCtx) {
        // given
        final String svcName = "bar";
        JsonObject config = TestUtils.buildConfiguration(
            TestUtils.withRouters(
                TestUtils.withRouter("foo",
                    TestUtils.withRouterService(svcName),
                    TestUtils.withRouterRule(rule),
                    TestUtils.withRouterEntrypoints(entrypointName))),
            TestUtils.withServices(
                TestUtils.withService(svcName,
                    TestUtils.withServers(
                        TestUtils.withServer(host, serverPort)))));

        routerFactory.createRouter(config)
            .onComplete(testCtx.succeeding(router -> {
                proxyRouter = router;
                // when
                doRequest(vertx, virtualHost, path)
                    .onComplete(testCtx.succeeding(resp -> testCtx.verify(() -> {
                        assertEquals(expectedStatusCode, resp.statusCode());
                        testCtx.completeNow();
                    })));
            }));
    }

    @Test
    public void defaultRoutePriority(Vertx vertx, VertxTestContext testCtx) {
        // given
        // routes are by default ordered by length of their rule
        JsonObject config = TestUtils.buildConfiguration(
            TestUtils.withRouters(
                TestUtils.withRouter("shortPath",
                    TestUtils.withRouterService("noServer"),
                    TestUtils.withRouterRule("PathPrefix('/path')"),
                    TestUtils.withRouterEntrypoints(entrypointName)),
                TestUtils.withRouter("longPath",
                    TestUtils.withRouterService("bar"),
                    TestUtils.withRouterRule("PathPrefix('/path/long')"),
                    TestUtils.withRouterEntrypoints(entrypointName))),
            TestUtils.withServices(
                TestUtils.withService("bar",
                    TestUtils.withServers(TestUtils.withServer(host, serverPort))),
                TestUtils.withService("noServer",
                    TestUtils.withServers(TestUtils.withServer("some.host", 1234)))));

        routerFactory.createRouter(config).onComplete(testCtx.succeeding(router -> {
            proxyRouter = router;
            // when
            doRequest(vertx, host, "/path/long")
                .onComplete(testCtx.succeeding(resp -> testCtx.verify(() -> {
                    // then
                    assertEquals(HttpResponseStatus.OK.code(), resp.statusCode());
                    testCtx.completeNow();
                })));
        }));
    }

    @Test
    public void customRoutePriority(Vertx vertx, VertxTestContext testCtx) {
        // given
        JsonObject config = TestUtils
            .buildConfiguration(
                TestUtils.withRouters(
                    TestUtils.withRouter("shortPath",
                        TestUtils.withRouterService("bar"),
                        TestUtils.withRouterRule("PathPrefix('/path')"),
                        TestUtils.withRouterPriority(100),
                        TestUtils.withRouterEntrypoints(entrypointName)),
                    TestUtils.withRouter("longPath",
                        TestUtils.withRouterService("noServer"),
                        TestUtils.withRouterRule("PathPrefix('/path/long')"),
                        TestUtils.withRouterEntrypoints(entrypointName))),
                TestUtils.withServices(
                    TestUtils.withService("bar",
                        TestUtils.withServers(TestUtils.withServer(host, serverPort))),
                    TestUtils.withService("noServer",
                        TestUtils.withServers(TestUtils.withServer("some.host", 1234)))));

        routerFactory.createRouter(config).onComplete(testCtx.succeeding(router -> {
            proxyRouter = router;
            // when
            doRequest(vertx, host, "/path/long")
                .onComplete(testCtx.succeeding(resp -> testCtx.verify(() -> {
                    // then
                    assertEquals(HttpResponseStatus.OK.code(), resp.statusCode());
                    testCtx.completeNow();
                })));
        }));
    }

    @Test
    public void failingMiddlewareCreation(Vertx vertx, VertxTestContext testCtx) {
        JsonObject config = TestUtils.buildConfiguration(
            TestUtils.withRouters(TestUtils.withRouter("foo", TestUtils.withRouterService("bar"),
                TestUtils.withRouterRule("Path('/path')"),
                TestUtils.withRouterMiddlewares("unknownMiddleware"), TestUtils.withRouterEntrypoints(entrypointName))),
            TestUtils.withMiddlewares(
                TestUtils.withMiddleware("unknownMiddleware", "unknownMiddleware",
                    TestUtils.withMiddlewareOpts(new JsonObject()))),
            TestUtils.withServices(
                TestUtils.withService("bar",
                    TestUtils.withServers(TestUtils.withServer(host, serverPort)))));

        routerFactory.createRouter(config).onComplete(testCtx.failing(router -> {
            testCtx.completeNow();
        }));
    }

    private static Stream<String> provideStringsForValidRouterRule() {
        return Stream.of(
            "Path('/')",
            "Path('/foo')",
            "Path('/foo/bar')",
            "Path('/.bar')",
            "Path('/.foo-bar/baz')",
            "Path('/_foo-bar/baz')",

            "PathRegex('/foo/[bar|baz]')",
            "PathRegex('/foo/.*/bar')",
            "PathRegex('/foo/\\w+/bar')",
            "PathRegex('/foo/[0-9]{5}/bar')",
            "PathRegex('/foo/.*')",

            "PathPrefix('/foo')",
            "PathPrefix('/foo/bar')",
            "PathPrefix('/.bar')",
            "PathPrefix('/.foo-bar/baz')",
            "PathPrefix('/_foo-bar/baz')",

            "Host('foo')",
            "Host('foo.bar')",
            "Host('foo-bar')",
            "Host('foo_bar')",
            "Host('foo~bar')",
            "Host('foo-bar.baz')",
            "Host('foo.bar.baz')",
            "Host('foo-bar.baz:1234')",
            "Host('1.2.3.4')",
            "Host('1.2.3.4:1234')",
            "Host('[829d:ffe7:8b86:d56d:9a1a:d180:a996:54a3]')",
            "Host('[829d::::9a1a::a996:54a3]')",
            "Host('%de%ad%be%ef')");
    }

    @ParameterizedTest
    @MethodSource("provideStringsForValidRouterRule")
    public void testValidRouterRule(String rule, VertxTestContext testCtx) {
        final JsonObject config = JsonObject.of(DynamicConfiguration.ROUTER_RULE, rule);
        VertxAssertions.assertDoesNotThrow(testCtx,
            () -> RouterFactory.validateRouter(config),
            String.format("%s should be parseable into a routing rule", rule));
        testCtx.completeNow();
    }

    private static Stream<String> provideStringsForInvalidRouterRule() {
        return Stream.of(
            "Path(\"/foo\")",
            "Path('')",
            "Path('foo')",
            "Path('/foo#bar')",
            "Path('/foo?bar')",
            "Path('/foo{bar')",
            "Path('/foo]bar')",
            "Path('/foo^bar')",
            "Path('/föö')",
            "PathRegex(\"/foo\")",
            "PathPrefix(\"/foo\")",
            "Host(\"/foo\")",
            "Host('')",
            "Host('foo#bar')",
            "Host('foo?bar')",
            "Host('foo{bar')",
            "Host('foo^bar')",
            "Host('föö')",
            "HostRegex(\"/foo\")",
            "Foo('/foo')",
            "Bar",
            null);
    }

    @ParameterizedTest
    @MethodSource("provideStringsForInvalidRouterRule")
    public void testInvalidRouterRule(String rule, VertxTestContext testCtx) {
        final JsonObject config = JsonObject.of(DynamicConfiguration.ROUTER_RULE, rule);
        VertxAssertions.assertThrows(testCtx,
            IllegalArgumentException.class,
            () -> RouterFactory.validateRouter(config),
            String.format("%s should not be parseable into a routing rule", rule));
        testCtx.completeNow();
    }

    @Test
    void additionalRoutesShouldNotBeShadowed(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String additionalRoutePath = "/some-route";
        JsonObject config = TestUtils.buildConfiguration(
            TestUtils.withRouters(TestUtils.withRouter("foo",
                TestUtils.withRouterEntrypoints(entrypointName),
                TestUtils.withRouterRule("PathPrefix('/')"), // shadowing /callback/test
                TestUtils.withRouterMiddlewares("middleware"),
                TestUtils.withRouterService("bar"))),
            TestUtils.withMiddlewares(
                TestUtils.withMiddleware("middleware", "additionalRoutes",
                    TestUtils.withMiddlewareOpts(JsonObject.of("path", additionalRoutePath)))),
            TestUtils.withServices(
                TestUtils.withService("bar",
                    TestUtils.withServers(TestUtils.withServer(host, serverPort)))));

        routerFactory.createRouter(config)
            .onComplete(testCtx.succeeding(router -> {
                proxyRouter = router;
                // when
                doRequest(vertx, host, additionalRoutePath)
                    .onComplete(testCtx.succeeding(resp -> testCtx.verify(() -> {
                        // then
                        assertEquals(418, resp.statusCode());
                        testCtx.completeNow();
                    })));
            }));
    }

    private Future<HttpResponse<Buffer>> doRequest(Vertx vertx, String virtualHost, String uri) {
        final HttpClient client = vertx.createHttpClient(
            new HttpClientOptions()
                .setDefaultHost(host)
                .setDefaultPort(proxyPort));
        final WebClient webClient = WebClient.wrap(client,
            new WebClientOptions()
                .setFollowRedirects(false));

        return webClient.request(HttpMethod.GET, uri)
            .virtualHost(virtualHost)
            .send();
    }
}
