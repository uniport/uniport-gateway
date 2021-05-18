package com.inventage.portal.gateway.proxy.router;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

interface ConfigCreator {
  JsonObject create(int port);
}

@ExtendWith(VertxExtension.class)
public class RouterFactoryTest {
  static final String host = "localhost";
  static final String requestPath = "/path";

  static Stream<Arguments> serverResponseTestData() {
    String routeRule = String.format("Path('%s')", requestPath);

    ConfigCreator configWithService = (int port) -> {
      return TestUtils.buildConfiguration(
          TestUtils.withRouters(
              TestUtils.withRouter("foo", TestUtils.withRouterService("bar"), TestUtils.withRouterRule(routeRule))),
          TestUtils
              .withServices(TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer(host, port)))));
    };

    ConfigCreator configWithEmptyService = (int port) -> {
      return TestUtils.buildConfiguration(TestUtils.withRouters(), TestUtils.withMiddlewares(),
          TestUtils.withServices());
    };

    ConfigCreator configWithRedirectMiddleware = (int port) -> {
      return TestUtils.buildConfiguration(
          TestUtils.withRouters(TestUtils.withRouter("foo", TestUtils.withRouterService("bar"),
              TestUtils.withRouterRule(routeRule), TestUtils.withRouterMiddlewares("redirect"))),
          TestUtils.withMiddlewares(TestUtils.withMiddleware("redirect", "redirectRegex",
              TestUtils
                  .withMiddlewareOpts(new JsonObject().put(DynamicConfiguration.MIDDLEWARE_REDIRECT_REGEX_REGEX, ".*")
                      .put(DynamicConfiguration.MIDDLEWARE_REDIRECT_REGEX_REPLACEMENT, "/redirect")))),
          TestUtils
              .withServices(TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer(host, port)))));
    };

    return Stream.of(/*Arguments.of("Ok", configWithService, HttpResponseStatus.OK.code()),
                     Arguments.of("Empty backend", configWithEmptyService, HttpResponseStatus.NOT_FOUND.code()),*/
        Arguments.of("Redirect middleware", configWithRedirectMiddleware, HttpResponseStatus.FOUND.code()));
  }

  @ParameterizedTest
  @MethodSource("serverResponseTestData")
  void serverResponseTest(String name, ConfigCreator configCreator, int expectedStatusCode, Vertx vertx,
      VertxTestContext testCtx) {
    String errMsg = String.format("'%s' failed", name);
    int proxyPort = TestUtils.findFreePort();
    int serverPort = TestUtils.findFreePort();

    Checkpoint proxyStarted = testCtx.checkpoint();
    Checkpoint serverStarted = testCtx.checkpoint();
    Checkpoint reqProxied = testCtx.checkpoint();
    Checkpoint respReceived = testCtx.checkpoint();

    Promise<Void> serverStartedPromise = Promise.promise();
    Promise<Void> proxyStartedPromise = Promise.promise();

    // start server
    Router r = Router.router(vertx);
    r.route().handler(ctx -> ctx.response().end("ok"));
    vertx.createHttpServer().requestHandler(req -> {
      r.handle(req);
    }).listen(serverPort).onComplete(testCtx.succeeding(s -> {
      serverStarted.flag();
      serverStartedPromise.complete();
    }));

    // start proxy with router created from config
    JsonObject config = configCreator.create(serverPort);
    RouterFactory routerFactory = new RouterFactory(vertx, String.format("http://%s", host));
    routerFactory.createRouter(config).onComplete(testCtx.succeeding(router -> {
      vertx.createHttpServer().requestHandler(req -> {
        router.handle(req);
        reqProxied.flag();
      }).listen(proxyPort).onComplete(testCtx.succeeding(p -> {
        proxyStarted.flag();
        proxyStartedPromise.complete();
      }));
      testCtx.verify(() -> {
      });
    })).onFailure(err -> {
      testCtx.failNow(err);
    });

    // do request when proxy and server are started
    CompositeFuture.all(serverStartedPromise.future(), proxyStartedPromise.future()).onComplete(ar -> {
      vertx.createHttpClient().request(HttpMethod.GET, proxyPort, host, requestPath).compose(req -> req.send())
          .onComplete(testCtx.succeeding(resp -> {
            testCtx.verify(() -> {
              assertEquals(expectedStatusCode, resp.statusCode(), errMsg);
            });
            respReceived.flag();
          }));
    });
  }
}
