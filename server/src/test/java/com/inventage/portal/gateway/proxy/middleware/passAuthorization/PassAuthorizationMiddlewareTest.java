package com.inventage.portal.gateway.proxy.middleware.passAuthorization;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicReference;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static io.vertx.core.http.HttpMethod.GET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
public class PassAuthorizationMiddlewareTest {

    private static final String host = "localhost";

    @Test
    public void testNoBearer(Vertx vertx, VertxTestContext testCtx) {

        portalGateway(vertx, host, testCtx)
                .withSessionMiddleware()
                .withPassAuthorizationMiddleware("testScope", new MockJWTAuth(new JsonObject(), "someToken"))
                .build()
                .start()
                .incomingRequest(GET, "/", testCtx, (resp) -> {
                    // then
                    assertEquals(401, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });

    }

    @Test
    public void testWithBearerNotAuthorized(Vertx vertx, VertxTestContext testCtx) {

        portalGateway(vertx, host, testCtx)
                .withSessionMiddleware()
                .withMockOAuth2Middleware("unauthorizedAuthHeader")
                .withPassAuthorizationMiddleware("testScope", new MockJWTAuth(new JsonObject(), "authorizedAuthHeader")).build()
                .start()
                .incomingRequest(GET, "/", testCtx, (resp) -> {
                    // then
                    assertEquals(401, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });

    }

    @Test
    public void testWithBearerAuthorized(Vertx vertx, VertxTestContext testCtx) {
        // given
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();
        final String externalAuthHeader = "externalAuthHeader";
        final String internalAuthHeader = "internalAuthHeader";

        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add(HttpHeaders.AUTHORIZATION, externalAuthHeader);

        // when
        portalGateway(vertx, host, testCtx)
                .withSessionMiddleware()
                .withMockOAuth2Middleware(internalAuthHeader)
                .withPassAuthorizationMiddleware("testScope", new MockJWTAuth(new JsonObject(), internalAuthHeader))
                .withRoutingContextHolder(routingContext)
                .build()
                .start()
                .incomingRequest(GET, "/", new RequestOptions().setHeaders(headers),  testCtx, (resp) -> {
                    // then
                    assertEquals(200, resp.statusCode(), "unexpected status code");
                    assertTrue(routingContext.get().request().headers().contains(HttpHeaders.AUTHORIZATION));
                    assertEquals(routingContext.get().request().getHeader(HttpHeaders.AUTHORIZATION), externalAuthHeader);
                    testCtx.completeNow();
                });

    }
}
