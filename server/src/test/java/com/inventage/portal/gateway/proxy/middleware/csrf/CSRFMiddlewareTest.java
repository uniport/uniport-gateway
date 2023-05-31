package com.inventage.portal.gateway.proxy.middleware.csrf;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.handler.CSRFHandler;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class CSRFMiddlewareTest {

    @Test
    void receiveCsrfCookieOnGetRequest(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String secret = UUID.randomUUID().toString();
        final String cookieName = CSRFHandler.DEFAULT_COOKIE_NAME;

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withBodyHandlerMiddleware()
            .withCsrfMiddleware(secret, null, null)
            .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final List<String> cookies = httpClientResponse.cookies();
            final Optional<String> csrfCookie = cookies.stream().filter(cookie -> cookie.startsWith(cookieName)).findFirst();

            assertTrue(csrfCookie.isPresent(), "Csrf Cookie needs to be included in the response.");

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
            .withBodyHandlerMiddleware()
            .withCsrfMiddleware(secret, cookieName, headerName)
            .build().start();

        //when
        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            //then
            final List<String> cookies = httpClientResponse.cookies();
            final Optional<String> csrfCookie = cookies.stream().filter(cookie -> cookie.startsWith(cookieName)).findFirst();

            assertTrue(csrfCookie.isPresent(), "Csrf Cookie needs to be included in the response.");

            testCtx.completeNow();
        }));
    }

    @Test
    void validPostRequestWithCSRFTokenInHeader(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String secret = UUID.randomUUID().toString();
        final String cookieName = CSRFHandler.DEFAULT_COOKIE_NAME;
        final String headerName = CSRFHandler.DEFAULT_HEADER_NAME;

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withBodyHandlerMiddleware()
            .withCsrfMiddleware(secret, cookieName, headerName)
            .build().start();

        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            final List<String> cookies = httpClientResponse.cookies();
            final Optional<String> csrfCookie = cookies.stream().filter(cookie -> cookie.startsWith(cookieName)).findFirst();
            final String csrfToken = csrfCookie.get().split("=")[1].split(";")[0];

            final RequestOptions requestOptions = new RequestOptions();
            final HeadersMultiMap headerEntries = new HeadersMultiMap();

            headerEntries.add(headerName, csrfToken);
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
    void validPostRequestWithCSRFTokenInBodyAsFormData(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String secret = UUID.randomUUID().toString();
        final String csrfCookieName = CSRFHandler.DEFAULT_COOKIE_NAME;
        final String csrfResponseName = CSRFHandler.DEFAULT_HEADER_NAME;

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withBodyHandlerMiddleware()
            .withCsrfMiddleware(secret, csrfCookieName, csrfResponseName)
            .build().start();

        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            final List<String> cookies = httpClientResponse.cookies();
            final Optional<String> csrfCookie = cookies.stream().filter(cookie -> cookie.startsWith(csrfCookieName)).findFirst();
            final String csrfToken = csrfCookie.get().split("=")[1].split(";")[0];

            final RequestOptions requestOptions = new RequestOptions();
            final HeadersMultiMap headerEntries = new HeadersMultiMap();
            requestOptions.setHeaders(headerEntries);
            Buffer buffer = Buffer.buffer();

            headerEntries.add("Cookie", csrfCookie.get());
            buffer.appendString(addMultiFormDataToRequest(Map.of(csrfResponseName, csrfToken), headerEntries));

            //when
            gateway.incomingRequest(HttpMethod.POST, "/", requestOptions, buffer, httpClientPostResponse -> {
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
            .withBodyHandlerMiddleware()
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
                assertEquals(HttpStatus.SC_FORBIDDEN, httpClientPostResponse.statusCode());
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
            .withBodyHandlerMiddleware()
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

    @Test
    void invalidPostRequestWithCSRFTokenInBodyAsFormData(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String secret = UUID.randomUUID().toString();
        final String csrfCookieName = CSRFHandler.DEFAULT_COOKIE_NAME;
        final String csrfResponseName = CSRFHandler.DEFAULT_HEADER_NAME;

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withBodyHandlerMiddleware()
            .withCsrfMiddleware(secret, csrfCookieName, csrfResponseName)
            .build().start();

        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {
            final List<String> cookies = httpClientResponse.cookies();
            final Optional<String> csrfCookie = cookies.stream().filter(cookie -> cookie.startsWith(csrfCookieName)).findFirst();
            final String invalidTokenValue = "invalidTokenValue";

            final RequestOptions requestOptions = new RequestOptions();
            final HeadersMultiMap headerEntries = new HeadersMultiMap();
            requestOptions.setHeaders(headerEntries);
            Buffer buffer = Buffer.buffer();

            headerEntries.add("Cookie", csrfCookie.get());
            buffer.appendString(addMultiFormDataToRequest(Map.of(csrfResponseName, invalidTokenValue), headerEntries));

            //when
            gateway.incomingRequest(HttpMethod.POST, "/", requestOptions, buffer, httpClientPostResponse -> {
                //then
                assertEquals(HttpStatus.SC_FORBIDDEN, httpClientPostResponse.statusCode());
                testCtx.completeNow();
            });
        }));
    }

    // Util

    private String addMultiFormDataToRequest(Map<String, String> formData, HeadersMultiMap headerEntries) {
        String boundary = "dLV9Wyq26L_-JQxk6ferf-RT153LhOO";
        String bufferContent = "--" + boundary + "\r\n";
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            bufferContent += "Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n" + entry.getValue() + "\r\n" +
                "--" + boundary + "\r\n";
        }
        headerEntries.add(HttpHeaderNames.CONTENT_TYPE, "multipart/form-data; boundary=" + boundary);
        headerEntries.add(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(bufferContent.length()));
        return bufferContent;
    }

}
