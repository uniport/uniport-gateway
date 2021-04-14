package com.inventage.portal.gateway.proxy.middleware.redirectRegex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;

public class RedirectRegexMiddlewareTest {
    Vertx vertx = Vertx.vertx();

    @TestFactory
    Stream<DynamicTest> testRedirects() {
        // TODO this should actually fail
        List<TestInstance> testInstances = Arrays.asList(new TestInstance("simple",
                "^(/dashboard)$", "$1/", "/dashboard", 301, "/dashboard/"));

        return testInstances.stream()
                .map(testInstance -> DynamicTest.dynamicTest("Testing " + testInstance.name,
                        () -> testRedirect(testInstance.regex, testInstance.replacement,
                                testInstance.requestURI, testInstance.expectedStatusCode,
                                testInstance.expectedLocation)));
    }

    void testRedirect(String regex, String replacement, String requestURI, int expectedStatusCode,
            String expectedLocation) {
        VertxTestContext testContext = new VertxTestContext();

        Checkpoint serverStarted = testContext.checkpoint();
        Checkpoint requestsServed = testContext.checkpoint();
        Checkpoint responsesReceived = testContext.checkpoint();

        RedirectRegexMiddleware redirect = new RedirectRegexMiddleware(regex, replacement);

        int port = 8888;
        Router router = Router.router(vertx);
        router.route().handler(redirect);
        vertx.createHttpServer().requestHandler(req -> {
            router.handle(req);
            requestsServed.flag();
        }).listen(port).onComplete(testContext.succeeding(httpServer -> serverStarted.flag()));

        HttpClient client = vertx.createHttpClient();

        client.request(HttpMethod.GET, port, "localhost", requestURI).compose(req -> req.send())
                .onSuccess(resp -> {
                    testContext.succeeding(r -> testContext.verify(() -> {
                        assertEquals(expectedStatusCode, resp.statusCode());
                        assertEquals(expectedLocation, resp.headers().get(HttpHeaders.LOCATION));
                    }));
                    responsesReceived.flag();
                });
    }

    private class TestInstance {
        public String name;

        public String regex;
        public String replacement;

        public String requestURI;
        public int expectedStatusCode;
        public String expectedLocation;

        public TestInstance(String name, String regex, String replacement, String requestURI,
                int expectedStatusCode, String expectedLocation) {
            this.name = name;
            this.regex = regex;
            this.replacement = replacement;
            this.requestURI = requestURI;
            this.expectedStatusCode = expectedStatusCode;
            this.expectedLocation = expectedLocation;
        }
    }
}
