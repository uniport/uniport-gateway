package com.inventage.portal.gateway.proxy.middleware.redirectRegex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import com.inventage.portal.gateway.TestUtils;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class RedirectRegexMiddlewareTest {

    static Stream<Arguments> redirectTestData() {
        // name, regex, replacement, URL, expectedStatusCode, expectedURL
        return Stream.of(
                Arguments.of("simple redirection", "^(/dashboard)$", "$1/", "/dashboard",
                        HttpResponseStatus.FOUND.code(), "/dashboard/"),
                Arguments.of("Don't allow admin or master realm", "^/auth(/|/admin.*|/realms/master.*)?$", "/login", "/auth/admin",
                        HttpResponseStatus.FOUND.code(), "/login"),
                Arguments.of("URL doesn't match regex", "^/blub/(.*)$", "/$1", "/wont/match",
                        HttpResponseStatus.OK.code(), null),
                Arguments.of("only /auth/realms/portal* or /auth/resources*", "^/(?!auth/realms/portal|auth/resources).*", "", "/auth/realms/portal/protocol/",
                        HttpResponseStatus.OK.code(), null),
                Arguments.of("only /auth/realms/portal* or /auth/resources*", "^/(?!auth/realms/portal|auth/resources).*", "", "/auth/resources/",
                        HttpResponseStatus.OK.code(), null),
                Arguments.of("only /auth/realms/portal* or /auth/realms/resources*", "^/(?!auth/realms/portal|auth/resources).*", "/login", "/auth/realms/master/protocol/",
                        HttpResponseStatus.FOUND.code(), "/login"),
                Arguments.of("only /auth/realms/portal* or /auth/realms/resources*", "^/(?!auth/realms/portal|auth/resources).*", "/login", "/auth/admin",
                        HttpResponseStatus.FOUND.code(), "/login"),
                Arguments.of("only /auth/realms/portal* or /auth/realms/resources*", "^/(?!auth/realms/portal|auth/resources).*", "/login", "/auth",
                        HttpResponseStatus.FOUND.code(), "/login"));
    }

    @ParameterizedTest
    @MethodSource("redirectTestData")
    void redirectTest(String name, String regex, String replacement, String URL, int expectedStatusCode,
            String expectedURL, Vertx vertx, VertxTestContext testCtx) {

        String failureMsg = String.format("Failure of '%s' test case", name);

        Checkpoint serverStarted = testCtx.checkpoint();
        Checkpoint requestsServed = testCtx.checkpoint();
        Checkpoint responsesReceived = testCtx.checkpoint();

        RedirectRegexMiddleware redirect = new RedirectRegexMiddleware(regex, replacement);

        int port = TestUtils.findFreePort();
        Router router = Router.router(vertx);
        router.route().handler(redirect).handler(ctx -> ctx.response().end("ok"));
        vertx.createHttpServer().requestHandler(req -> {
            router.handle(req);
            requestsServed.flag();
        }).listen(port).onComplete(testCtx.succeeding(s -> {
            serverStarted.flag();

            vertx.createHttpClient().request(HttpMethod.GET, port, "localhost", URL).compose(req -> req.send())
                    .onComplete(testCtx.succeeding(resp -> testCtx.verify(() -> {
                        assertEquals(expectedStatusCode, resp.statusCode(), failureMsg);
                        assertEquals(expectedURL, resp.headers().get(HttpHeaders.LOCATION), failureMsg);
                        responsesReceived.flag();
                    })));
        }));

    }
}
