package ch.uniport.gateway.proxy.config.label;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.uniport.gateway.proxy.config.DynamicConfiguration;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(VertxExtension.class)
public class ParserTest {

    static Stream<Arguments> filterKeysTestData() {
        Map<String, Object> labels = new HashMap<String, Object>(Map.ofEntries(//
            new AbstractMap.SimpleEntry<String, Object>("testA", "value"), //
            new AbstractMap.SimpleEntry<String, Object>("someB", "value"), //
            new AbstractMap.SimpleEntry<String, Object>("otherC", "value")));

        Map<String, Object> invalidLabelValues = new HashMap<String, Object>(Map.ofEntries(//
            new AbstractMap.SimpleEntry<String, Object>("a", true), //
            new AbstractMap.SimpleEntry<String, Object>("b", 1), //
            new AbstractMap.SimpleEntry<String, Object>("c", List.of())));

        return Stream.of(Arguments.of("labels is null", null, null, List.of()),
            Arguments.of("identity is returned if filter is null", labels, null, new ArrayList<>(labels.keySet())),
            Arguments.of("identity is returned if filter is empty", labels, List.of(),
                new ArrayList<>(labels.keySet())),
            Arguments.of("ignore non string values", invalidLabelValues, List.of(), List.of()),
            Arguments.of("filter keys", labels, List.of("test", "some"), List.of("testA", "someB")));
    }

    static Stream<Arguments> decodeTestData() {
        // the middleware type needs to be one of 'DynamicConfiguration.MIDDLEWARE_TYPES'
        Map<String, Object> labels = new HashMap<String, Object>(Map.ofEntries(//
            new AbstractMap.SimpleEntry<String, Object>("test.http.routers.blub.rule", "someRule"), //
            new AbstractMap.SimpleEntry<String, Object>("test.http.routers.blub.middlewares", "one, two,foo"), //
            new AbstractMap.SimpleEntry<String, Object>("test.http.middlewares.foo.headers.bar", "baz"), //
            new AbstractMap.SimpleEntry<String, Object>("test.http.services.moose.servers.port", "1234")));

        Map<String, Object> labelsWithNoMatch = new HashMap<String, Object>(
            Map.ofEntries(new AbstractMap.SimpleEntry<String, Object>("blub.foo.bar.baz", "moose")));

        Map<String, Object> invalidLabels = new HashMap<String, Object>(
            Map.ofEntries(new AbstractMap.SimpleEntry<String, Object>("blub...baz", "moose")));

        JsonObject expectedDecoding = new JsonObject().//
            put(DynamicConfiguration.HTTP, new JsonObject()//
                .put(DynamicConfiguration.ROUTERS, new JsonArray()//
                    .add(new JsonObject()//
                        .put(DynamicConfiguration.ROUTER_NAME, "blub")//
                        .put(DynamicConfiguration.ROUTER_RULE, "someRule")//
                        .put(DynamicConfiguration.ROUTER_MIDDLEWARES, new JsonArray()//
                            .add("one")//
                            .add("two")//
                            .add("foo"))))
                .put(DynamicConfiguration.MIDDLEWARES, new JsonArray()//
                    .add(new JsonObject()//
                        .put(DynamicConfiguration.MIDDLEWARE_NAME, "foo")//
                        .put(DynamicConfiguration.MIDDLEWARE_TYPE, "headers")//
                        .put(DynamicConfiguration.MIDDLEWARE_OPTIONS, new JsonObject()//
                            .put("bar", "baz"))))//
                .put(DynamicConfiguration.SERVICES, new JsonArray()//
                    .add(new JsonObject()//
                        .put(DynamicConfiguration.SERVICE_NAME, "moose")//
                        .put(DynamicConfiguration.SERVICE_SERVERS, new JsonArray()//
                            .add(new JsonObject()//
                                .put(DynamicConfiguration.SERVICE_SERVER_PORT, 1234))))));

        return Stream.of(Arguments.of("labels is null", null, "", null),
            Arguments.of("labels with no match", labelsWithNoMatch, "nomatch", null),
            Arguments.of("invalid labels", invalidLabels, "blub", null),
            Arguments.of("decode labels", labels, "test", expectedDecoding));
    }

    @ParameterizedTest
    @MethodSource("filterKeysTestData")
    void filterKeysTest(
        String name, Map<String, Object> labels, List<String> filters, List<String> expected,
        Vertx vertx, VertxTestContext testCtx
    ) {
        String errMsg = String.format("'%s' failed. Labels: '%s', Filters: '%s'", name, labels, filters);

        List<String> actual = Parser.filterKeys(labels, filters);
        testCtx.verify(() -> assertEquals(expected, actual, errMsg));
        testCtx.completeNow();
    }

    @ParameterizedTest
    @MethodSource("decodeTestData")
    void decodeTest(
        String name, Map<String, Object> labels, String rootName, JsonObject expected, Vertx vertx,
        VertxTestContext testCtx
    ) {
        List<String> filters = null; // usage of filters is tested in 'filterKeysTest'
        String errMsg = String.format("'%s' failed. Labels: '%s', RootName: '%s', Filters: '%s'", name, labels,
            rootName, filters);

        JsonObject actual = Parser.decode(labels, rootName, filters);
        testCtx.verify(() -> assertEquals(expected, actual, errMsg));
        testCtx.completeNow();
    }
}
