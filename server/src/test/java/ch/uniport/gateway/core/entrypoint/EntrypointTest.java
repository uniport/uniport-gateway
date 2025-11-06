package ch.uniport.gateway.core.entrypoint;

import ch.uniport.gateway.TestUtils;
import ch.uniport.gateway.core.config.model.TlsModel;
import ch.uniport.gateway.proxy.config.model.DynamicModel;
import ch.uniport.gateway.proxy.config.model.MiddlewareModel;
import ch.uniport.gateway.proxy.config.model.RouterModel;
import ch.uniport.gateway.proxy.config.model.ServerOptions;
import ch.uniport.gateway.proxy.config.model.ServiceModel;
import ch.uniport.gateway.proxy.middleware.VertxAssertions;
import ch.uniport.gateway.proxy.middleware.redirectRegex.RedirectRegexMiddlewareFactory;
import ch.uniport.gateway.proxy.middleware.redirectRegex.RedirectRegexMiddlewareOptions;
import ch.uniport.gateway.proxy.router.PublicProtoHostPort;
import ch.uniport.gateway.proxy.router.RouterFactory;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.RequestOptions;
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
public class EntrypointTest {
    static final String LOCALHOST = "localhost";
    static final String ENTRYPOINT_PREFIX = "http";

    private HttpServer server;
    private int proxyPort;
    private int serverPort;
    private RouterFactory routerFactory;

    @BeforeEach
    public void setup(Vertx vertx) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        proxyPort = TestUtils.findFreePort();
        serverPort = TestUtils.findFreePort();
        server = vertx.createHttpServer()
            .requestHandler(
                req -> {
                    req.response().setStatusCode(200).end("ok");
                })
            .listen(serverPort, ready -> {
                if (ready.failed()) {
                    throw new RuntimeException(ready.cause());
                }
                latch.countDown();
            });

        latch.await();

        routerFactory = new RouterFactory(vertx, PublicProtoHostPort.of("http", LOCALHOST, String.format("%d", proxyPort)),
            ENTRYPOINT_PREFIX + proxyPort);
    }

    @AfterEach
    public void tearDown() {
        server.close();
    }

    /**
     * Testing entryMiddleware: is the entryMiddleware traversed for each request at
     * a entrypoint?
     * We send two request on two different routes, we expect that the entry
     * redirect middleware is traversed.
     * The redirect middleware sets a key-value pair in the response header.
     */
    @Test
    void entryRedirectMiddlewareRunsOnAllRoutes(Vertx vertx, VertxTestContext testCtx) {
        // given
        final String entryPointIdentifier = ENTRYPOINT_PREFIX + proxyPort;
        final String expectedRedirect = "/to/some/page";

        final MiddlewareModel entryMiddlewareConfig = MiddlewareModel.builder()
            .withName("redirect")
            .withType(RedirectRegexMiddlewareFactory.TYPE)
            .withOptions(RedirectRegexMiddlewareOptions.builder()
                .withRegex("/.*")
                .withReplacement(expectedRedirect)
                .build())
            .build();

        final DynamicModel dynamicConfig = DynamicModel.builder()
            .withRouters(List.of(
                RouterModel.builder()
                    .withName("foo")
                    .withService("bar")
                    .withRule("Path('/pathA')")
                    .withEntrypoints(List.of(entryPointIdentifier))
                    .build(),
                RouterModel.builder()
                    .withName("foo2")
                    .withService("bar")
                    .withRule("Path('/pathB')")
                    .withEntrypoints(List.of(entryPointIdentifier))
                    .build()))
            .withServices(List.of(
                ServiceModel.builder()
                    .withName("bar")
                    .withServers(List.of(
                        ServerOptions.builder()
                            .withHost(LOCALHOST)
                            .withPort(serverPort)
                            .build()))
                    .build()))
            .build();

        final Entrypoint entrypoint = new Entrypoint(vertx, entryPointIdentifier, proxyPort, null, List.of(entryMiddlewareConfig));
        await(testCtx, entrypoint.listen());

        routerFactory.createRouter(dynamicConfig).onComplete(testCtx.succeeding(router -> {
            entrypoint.router().mountSubRouter("/", router);

            final Checkpoint c = testCtx.checkpoint(2);
            List.of("/pathA", "/pathB").stream()
                .forEach(path -> {
                    final RequestOptions opt = new RequestOptions()
                        .setHost(LOCALHOST)
                        .setURI(path);
                    doRequest(vertx, testCtx, new HttpClientOptions(), opt, resp -> {
                        // then
                        VertxAssertions.assertEquals(testCtx, HttpResponseStatus.FOUND.code(), resp.statusCode(),
                            "unexpected status code");
                        VertxAssertions.assertEquals(testCtx, expectedRedirect, resp.headers().get("location"));

                        c.flag();
                    });
                });
        }));
    }

    @Test
    void shouldWorkWithTLS(Vertx vertx, VertxTestContext testCtx) {
        // given
        final String entryPointIdentifier = ENTRYPOINT_PREFIX + proxyPort;
        final String tlsHost = "local.uniport.ch"; // resolves to localhost

        // generate self-signed certificate
        CertGenerator.PemFiles pemFiles = null;
        try {
            pemFiles = CertGenerator.generateTempPemFiles(tlsHost);
        } catch (Exception e) {
            testCtx.failNow(e);
        }
        VertxAssertions.assertNotNull(testCtx, pemFiles);

        // configure entrypoint
        final TlsModel tls = TlsModel.builder()
            .withCertFile(pemFiles.certFile.getAbsolutePath())
            .withKeyFile(pemFiles.keyFile.getAbsolutePath())
            .build();
        final Entrypoint entrypoint = new Entrypoint(vertx, entryPointIdentifier, proxyPort, tls, null);
        await(testCtx, entrypoint.listen());

        // configure router
        final DynamicModel dynamicConfig = DynamicModel.builder()
            .withRouters(List.of(
                RouterModel.builder()
                    .withName("foo")
                    .withService("bar")
                    .withRule("Path('/')")
                    .withEntrypoints(List.of(entryPointIdentifier))
                    .build()))
            .withServices(List.of(
                ServiceModel.builder()
                    .withName("bar")
                    .withServers(List.of(
                        ServerOptions.builder()
                            .withHost(LOCALHOST)
                            .withPort(serverPort)
                            .build()))
                    .build()))
            .build();

        routerFactory.createRouter(dynamicConfig).onComplete(testCtx.succeeding(router -> {
            entrypoint.router().mountSubRouter("/", router); // glue entrypoint and router together

            final HttpClientOptions clientOpts = new HttpClientOptions()
                .setSsl(true)
                .setTrustAll(true); // self-signed certificate
            final RequestOptions reqOpts = new RequestOptions()
                .setHost(tlsHost)
                .setURI("/");
            // when
            doRequest(vertx, testCtx, clientOpts, reqOpts, resp -> {
                // then
                VertxAssertions.assertEquals(testCtx, HttpResponseStatus.OK.code(), resp.statusCode(), "unexpected status code");

                testCtx.completeNow();
            });
        }));
    }

    private void await(VertxTestContext testCtx, Future<?> fut) {
        final CountDownLatch latch = new CountDownLatch(1);
        fut.onComplete(ar -> latch.countDown());
        try {
            latch.await();
        } catch (InterruptedException e) {
            testCtx.failNow(e);
        }
    }

    private void doRequest(
        Vertx vertx, VertxTestContext testCtx, HttpClientOptions clientOpts, RequestOptions reqOpts,
        Consumer<HttpClientResponse> assertionHandler
    ) {
        reqOpts
            .setPort(proxyPort)
            .setMethod(HttpMethod.GET);
        vertx.createHttpClient(clientOpts)
            .request(reqOpts)
            .compose(HttpClientRequest::send)
            .onComplete(testCtx.succeeding(resp -> assertionHandler.accept(resp)));
    }
}
