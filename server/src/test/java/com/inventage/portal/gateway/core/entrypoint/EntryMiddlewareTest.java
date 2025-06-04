package com.inventage.portal.gateway.core.entrypoint;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.VertxAssertions;
import com.inventage.portal.gateway.proxy.middleware.redirectRegex.RedirectRegexMiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.redirectRegex.RedirectRegexMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.Gateway;
import com.inventage.portal.gateway.proxy.model.GatewayMiddleware;
import com.inventage.portal.gateway.proxy.model.GatewayRouter;
import com.inventage.portal.gateway.proxy.model.GatewayService;
import com.inventage.portal.gateway.proxy.model.ServerOptions;
import com.inventage.portal.gateway.proxy.router.PublicProtoHostPort;
import com.inventage.portal.gateway.proxy.router.RouterFactory;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.web.Router;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.List;
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

        routerFactory = new RouterFactory(vertx, PublicProtoHostPort.of("http", HOST, String.format("%d", proxyPort)), ENTRYPOINT_PREFIX + proxyPort);
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

        final GatewayMiddleware entryMiddlewareConfig = GatewayMiddleware.builder()
            .withName("redirect")
            .withType(RedirectRegexMiddlewareFactory.TYPE)
            .withOptions(RedirectRegexMiddlewareOptions.builder()
                .withRegex("/.*")
                .withReplacement(expectedRedirect)
                .build())
            .build();

        final Gateway dynamicConfig = Gateway.builder()
            .withRouters(List.of(
                GatewayRouter.builder()
                    .withName("foo")
                    .withService("bar")
                    .withRule("Path('/pathA')")
                    .withEntrypoints(List.of(entryPointIdentifier))
                    .build(),
                GatewayRouter.builder()
                    .withName("foo2")
                    .withService("bar")
                    .withRule("Path('/pathB')")
                    .withEntrypoints(List.of(entryPointIdentifier))
                    .build()))
            .withServices(List.of(
                GatewayService.builder()
                    .withName("bar")
                    .withServers(List.of(
                        ServerOptions.builder()
                            .withHost(HOST)
                            .withPort(serverPort)
                            .build()))
                    .build()))
            .build();

        final Entrypoint entrypoint = new Entrypoint(vertx, entryPointIdentifier, proxyPort, List.of(entryMiddlewareConfig));

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

}
