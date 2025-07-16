package ch.uniport.gateway.proxy.service;

import static ch.uniport.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;

import ch.uniport.gateway.TestUtils;
import ch.uniport.gateway.proxy.middleware.MiddlewareServer;
import ch.uniport.gateway.proxy.middleware.VertxAssertions;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class MiddlewareChainTest {

    @Test
    void chainedRequestURIModifiers(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
        // given 
        final String requestReplacePathRegex1 = "foo";
        final String requestReplacePathReplacement1 = "blub";

        final String requestReplacePathRegex2 = "blub";
        final String requestReplacePathReplacement2 = "";

        final String expectedPath = "/bar";
        final Checkpoint checkedPath = testContext.checkpoint();

        // then
        final Handler<RoutingContext> incomingPathCheckHandler = ctx -> {
            final String actualPath = ctx.request().path();
            VertxAssertions.assertTrue(testContext, actualPath.equals(expectedPath), String.format("request path sould be set to '%s', got '%s'", expectedPath, actualPath));
            checkedPath.flag();
        };

        final int backendPort = TestUtils.findFreePort();
        final MiddlewareServer gateway = portalGateway(vertx, testContext)
            .withReplacePathRegexMiddleware(requestReplacePathRegex1, requestReplacePathReplacement1)
            .withReplacePathRegexMiddleware(requestReplacePathRegex2, requestReplacePathReplacement2)
            .withProxyMiddleware(backendPort)
            .withBackend(vertx, backendPort, incomingPathCheckHandler)
            .build().start();

        // when 
        gateway.incomingRequest(HttpMethod.GET, "/foobar", response -> {
            testContext.completeNow();
        });
    }

    @Test
    void responseHeaderModifiers(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
        // given 
        final String headerName = "TEST";
        final String expectedHeaderValue = "blub";
        final MultiMap responseHeaders = HeadersMultiMap.headers().add(headerName, expectedHeaderValue);

        final int backendPort = TestUtils.findFreePort();
        final MiddlewareServer gateway = portalGateway(vertx, testContext)
            .withHeaderMiddleware(HeadersMultiMap.httpHeaders(), responseHeaders)
            .withProxyMiddleware(backendPort)
            .withBackend(vertx, backendPort)
            .build().start();

        // when 
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            final MultiMap headers = response.headers();
            final String actualHeaderValue = headers.get(headerName);

            // then
            VertxAssertions.assertTrue(testContext, actualHeaderValue.equals(expectedHeaderValue),
                String.format("'%s' header should be set to '%s', got '%s'", headerName, expectedHeaderValue, actualHeaderValue));
            testContext.completeNow();
        });
    }

    @Test
    void chainedResponseHeaderModifiers(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
        // given 
        final String headerName = "TEST";
        // as they are traversed in reversed order, the header should not be set in the end
        final MultiMap responseHeaders1 = HeadersMultiMap.headers().add(headerName, ""); // remove header 
        final MultiMap responseHeaders2 = HeadersMultiMap.headers().add(headerName, "blub"); // set header 

        final int backendPort = TestUtils.findFreePort();
        final MiddlewareServer gateway = portalGateway(vertx, testContext)
            .withHeaderMiddleware(HeadersMultiMap.httpHeaders(), responseHeaders1)
            .withHeaderMiddleware(HeadersMultiMap.httpHeaders(), responseHeaders2)
            .withProxyMiddleware(backendPort)
            .withBackend(vertx, backendPort)
            .build().start();

        // when 
        gateway.incomingRequest(HttpMethod.GET, "/", response -> {
            final MultiMap headers = response.headers();
            final String testHeader = headers.get(headerName);

            // then
            VertxAssertions.assertNull(testContext, testHeader, String.format("'%s' header should not be set", headerName));
            testContext.completeNow();
        });
    }

}
