package com.inventage.portal.gateway.proxy.middleware.csrf;

import static com.inventage.portal.gateway.TestUtils.buildConfiguration;
import static com.inventage.portal.gateway.TestUtils.withMiddleware;
import static com.inventage.portal.gateway.TestUtils.withMiddlewareOpts;
import static com.inventage.portal.gateway.TestUtils.withMiddlewares;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareTestBase;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.CSRFHandler;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(VertxExtension.class)
class CSRFMiddlewareTest extends MiddlewareTestBase {

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {
        final JsonObject simple = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CSRFMiddlewareFactory.CSRF,
                    withMiddlewareOpts(JsonObject.of(
                        CSRFMiddlewareFactory.CSRF_COOKIE, JsonObject.of(
                            CSRFMiddlewareFactory.CSRF_COOKIE_NAME, "bar",
                            CSRFMiddlewareFactory.CSRF_COOKIE_PATH, "/abc",
                            CSRFMiddlewareFactory.CSRF_COOKIE_SECURE, true),
                        CSRFMiddlewareFactory.CSRF_HEADER_NAME, "X-XSRF-TOKEN",
                        CSRFMiddlewareFactory.CSRF_NAG_HTTPS, true,
                        CSRFMiddlewareFactory.CSRF_ORIGIN, "example.com",
                        CSRFMiddlewareFactory.CSRF_TIMEOUT_IN_MINUTES, 42)))));

        final JsonObject minimal = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CSRFMiddlewareFactory.CSRF)));

        final JsonObject missingOptions = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CSRFMiddlewareFactory.CSRF)));

        final JsonObject unknownProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CSRFMiddlewareFactory.CSRF,
                    withMiddlewareOpts(
                        JsonObject.of("bar", "blub")))));

        final JsonObject invalidTimeout = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", CSRFMiddlewareFactory.CSRF,
                    withMiddlewareOpts(
                        JsonObject.of(CSRFMiddlewareFactory.CSRF_TIMEOUT_IN_MINUTES, -1)))));

        return Stream.of(
            Arguments.of("accept simple config", simple, complete, expectedTrue),
            Arguments.of("accept minimal config", minimal, complete, expectedTrue),
            Arguments.of("accept config with no options", missingOptions, complete, expectedTrue),
            Arguments.of("reject config with unknown property", unknownProperty, complete, expectedFalse),
            Arguments.of("reject config with invalid timeout", invalidTimeout, complete, expectedFalse)

        );
    }

    @Test
    void receiveCsrfCookieOnGetRequest(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String secret = UUID.randomUUID().toString();
        final String cookieName = CSRFHandler.DEFAULT_COOKIE_NAME;

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withBodyHandlerMiddleware()
            .withCsrfMiddleware(secret, CSRFMiddlewareFactory.DEFAULT_COOKIE_NAME, CSRFMiddlewareFactory.DEFAULT_HEADER_NAME)
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
    void validPostRequestWithCSRFTokenInHeaderCaseInsensitive(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String secret = UUID.randomUUID().toString();
        final String cookieName = CSRFHandler.DEFAULT_COOKIE_NAME;
        final String headerName = "X-XSRF-TOKEN";
        final String headerNameInLowercase = headerName.toLowerCase();

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

            headerEntries.add(headerNameInLowercase, csrfToken);
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
    void validPostRequestWithCSRFTokenInHeaderWithUserSession(Vertx vertx, VertxTestContext testCtx) {
        //given
        final String secret = UUID.randomUUID().toString();
        final String cookieName = CSRFHandler.DEFAULT_COOKIE_NAME;
        final String headerName = "X-XSRF-TOKEN";

        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withBodyHandlerMiddleware()
            .withCsrfMiddleware(secret, cookieName, headerName)
            .build().start();

        gateway.incomingRequest(HttpMethod.GET, "/", (httpClientResponse -> {

            final List<String> cookies = httpClientResponse.cookies();
            final Optional<String> csrfCookie = cookies.stream().filter(cookie -> cookie.startsWith(cookieName)).findFirst();
            final Optional<String> sessionCookie = cookies.stream().filter(cookie -> cookie.startsWith(DEFAULT_SESSION_COOKIE_NAME)).findFirst();

            final String csrfToken = csrfCookie.get().split("=")[1].split(";")[0];

            final RequestOptions requestOptions = new RequestOptions();
            final HeadersMultiMap headerEntries = new HeadersMultiMap();

            headerEntries.add(headerName, csrfToken);
            headerEntries.add("Cookie", csrfCookie.get() + ";" + sessionCookie.get());

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
    void validGraphQLPostRequestWithCSRFToken(Vertx vertx, VertxTestContext testCtx) {
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

            headerEntries.add("Cookie", csrfCookie.get());
            headerEntries.add(CSRFHandler.DEFAULT_HEADER_NAME, csrfToken);
            headerEntries.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);

            String payload = "{\"operationName\":\"OrganisationList\",\"variables\":{},\"query\":\"query OrganisationList {\\n  organisations: OrganisationView(order_by: {name: asc}) {\\n    ...OrganisationList\\n    __typename\\n  }\\n}\\n\\nfragment OrganisationList on OrganisationView {\\n  id\\n  name\\n  active\\n  internalIdentifier\\n  createdAt\\n  createdBy\\n  __typename\\n}\"}";
            //when
            gateway.incomingRequest(HttpMethod.POST, "/", requestOptions, payload, httpClientPostResponse -> {
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
