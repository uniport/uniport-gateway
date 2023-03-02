package com.inventage.portal.gateway.proxy.middleware.csrf;

import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.handler.CSRFHandler;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
class CSRFMiddlewareTest {

    @Test
    void receiveCsrfCookieOnGetRequest(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String secret = UUID.randomUUID().toString();
        final String cookieName = CSRFHandler.DEFAULT_COOKIE_NAME;

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
                .withCsrfMiddleware(secret, null, null)
                .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final List<String> cookies = httpClientResponse.cookies();
            final Optional<String> csrfCookie = cookies.stream().filter(cookie -> cookie.startsWith(cookieName)).findFirst();

            assertTrue(csrfCookie.isPresent(), String.format("Csrf Cookie needs to be included in the response."));

            testCtx.completeNow();
        }));
    }

    @Test
    void receiveCsrfCookieWithCustomNameOnGetRequest(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String secret = UUID.randomUUID().toString();
        final String cookieName = "Uniport-csrf-token";
        final String headerName = CSRFHandler.DEFAULT_HEADER_NAME;

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
                .withCsrfMiddleware(secret, cookieName, headerName)
                .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final List<String> cookies = httpClientResponse.cookies();
            final Optional<String> csrfCookie = cookies.stream().filter(cookie -> cookie.startsWith(cookieName)).findFirst();

            assertTrue(csrfCookie.isPresent(), String.format("Csrf Cookie needs to be included in the response."));

            testCtx.completeNow();
        }));
    }

    @Test
    void validPostRequestWithCSRFToken(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String secret = UUID.randomUUID().toString();
        final String cookieName = CSRFHandler.DEFAULT_COOKIE_NAME;
        final String headerName = CSRFHandler.DEFAULT_HEADER_NAME;

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
                .withCsrfMiddleware(secret, cookieName, headerName)
                .build().start();

        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            final List<String> cookies = httpClientResponse.cookies();
            final Optional<String> csrfCookie = cookies.stream().filter(cookie -> cookie.startsWith(cookieName)).findFirst();

            final RequestOptions requestOptions = new RequestOptions();
            final HeadersMultiMap headerEntries = new HeadersMultiMap();

            headerEntries.add(headerName, csrfCookie.get().split("=")[1].split(";")[0]);
            headerEntries.add("Cookie", csrfCookie.get());

            requestOptions.setHeaders(headerEntries);

            //when
            gateway.incomingRequest(HttpMethod.POST, "/", requestOptions, httpClientPostResponse -> {
                //then
                assertEquals(HttpStatus.SC_OK, httpClientPostResponse.statusCode());
                testCtx.completeNow();
            });

        }));
    }

    @Test
    void invalidPostRequestWithNoCSRFTokenInHeader(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String secret = UUID.randomUUID().toString();
        final String cookieName = CSRFHandler.DEFAULT_COOKIE_NAME;
        final String headerName = CSRFHandler.DEFAULT_HEADER_NAME;

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
                .withCsrfMiddleware(secret, cookieName, headerName)
                .build().start();

        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            final List<String> cookies = httpClientResponse.cookies();
            final Optional<String> csrfCookie = cookies.stream().filter(cookie -> cookie.startsWith(cookieName)).findFirst();

            final RequestOptions requestOptions = new RequestOptions();
            final HeadersMultiMap headerEntries = new HeadersMultiMap();

            headerEntries.add("Cookie", csrfCookie.get());
            requestOptions.setHeaders(headerEntries);

            //when
            gateway.incomingRequest(HttpMethod.POST, "/", requestOptions, httpClientPostResponse -> {
                //then
                assertNotEquals(HttpStatus.SC_OK, httpClientPostResponse.statusCode());
                testCtx.completeNow();
            });

        }));
    }

    @Test
    void invalidPostRequestWithInvalidCSRFTokenInHeader(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String secret = UUID.randomUUID().toString();
        final String cookieName = CSRFHandler.DEFAULT_COOKIE_NAME;
        final String headerName = CSRFHandler.DEFAULT_HEADER_NAME;

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
                .withCsrfMiddleware(secret, cookieName, headerName)
                .build().start();

        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            final List<String> cookies = httpClientResponse.cookies();
            final Optional<String> csrfCookie = cookies.stream().filter(cookie -> cookie.startsWith(cookieName)).findFirst();

            final RequestOptions requestOptions = new RequestOptions();
            final HeadersMultiMap headerEntries = new HeadersMultiMap();

            headerEntries.add("Cookie", csrfCookie.get());
            headerEntries.add(headerName, "someInvalidString");
            requestOptions.setHeaders(headerEntries);

            //when
            gateway.incomingRequest(HttpMethod.POST, "/", requestOptions, httpClientPostResponse -> {
                //then
                assertEquals(HttpStatus.SC_FORBIDDEN, httpClientPostResponse.statusCode());
                testCtx.completeNow();
            });

        }));
    }


}
