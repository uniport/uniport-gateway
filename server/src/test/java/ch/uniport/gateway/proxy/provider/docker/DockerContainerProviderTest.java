package ch.uniport.gateway.proxy.provider.docker;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.uniport.gateway.TestUtils;
import ch.uniport.gateway.core.config.StaticConfiguration;
import ch.uniport.gateway.proxy.provider.Provider;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.servicediscovery.Status;
import io.vertx.servicediscovery.spi.ServiceImporter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(VertxExtension.class)
public class DockerContainerProviderTest {

    private static final String TEST_NETWORK = "test-network";

    /*
    further test scenario ideas:
    
    no config publication expected (silently dropped because of its invalidity):
    
    invalid rule
    undefined rule
    invalid HTTP service definition
    one container without port
    one container without port with middleware
    one container with portal.enable false
    one container not healthy
    
    config publication expeceted:
    
    one container no label
    two containers no label
    two containers with same service name no label
    one container with label (not on server)
    one container with labels
    one container with rule label
    one container with rule label and one service
    one container with rule label and two services
    one router, one specified but undefined service -> specified one is assigned, but automatic is created instead
    two containers with same service name and same LB methods
    two containers with two identical middlewares
    two containers with two different middlewares with same name
    three containers with different middlewares with same name
    two containers with two different routers with same name
    three containers with different routers with same name
    two containers with two identical routers
    two containers with two identical router rules and different service names
    one container with bad label
    one container with label port
    one container with label port on two services
    */

    @SuppressWarnings("unchecked")
    static Stream<Arguments> defaultRuleTestData() {
        JsonObject record = buildRecord("simple-container",
            withMetadata(withId("abc123"), withName("simple-container"),
                withLabels(Map.ofEntries(entry("portal.enable", "true"),
                    entry("portal.http.routers.simple-router.service", "simple-service"),
                    entry("portal.http.services.simple-service.servers.port", "8080"))),
                withPorts(List.of(8080)), withNetworks(Map.ofEntries(entry(TEST_NETWORK, "172.0.0.1")))));

        return Stream.of(//
            Arguments.of(//
                "default rule with no variable", //
                "Host('foo.bar')", //
                new ArrayList<JsonObject>() {
                    {
                        add(record);
                    }
                }, //
                TestUtils.buildConfiguration(
                    TestUtils.withRouters(
                        TestUtils.withRouter("simple-router",
                            TestUtils.withRouterRule("Host('foo.bar')"),
                            TestUtils.withRouterService("simple-service"))),
                    TestUtils.withMiddlewares(),
                    TestUtils.withServices(
                        TestUtils.withService("simple-service",
                            TestUtils.withServers(
                                TestUtils.withServer("172.0.0.1", 8080)))))//
            ), //
            Arguments.of(//
                "default rule with service name", //
                "Host('${name}.foo.bar')", //
                new ArrayList<JsonObject>() {
                    {
                        add(record);
                    }
                }, //
                TestUtils.buildConfiguration(
                    TestUtils.withRouters(
                        TestUtils.withRouter("simple-router",
                            TestUtils.withRouterRule("Host('simple-container.foo.bar')"),
                            TestUtils.withRouterService("simple-service"))),
                    TestUtils.withMiddlewares(),
                    TestUtils.withServices(
                        TestUtils.withService("simple-service",
                            TestUtils.withServers(
                                TestUtils.withServer("172.0.0.1", 8080)))))//
            ), //
            Arguments.of(//
                "default rule with label", //
                "Host('${portal.enable}.foo.bar')", //
                new ArrayList<JsonObject>() {
                    {
                        add(record);
                    }
                }, //
                TestUtils.buildConfiguration(
                    TestUtils.withRouters(
                        TestUtils.withRouter("simple-router",
                            TestUtils.withRouterService("simple-service"),
                            TestUtils.withRouterRule("Host('true.foo.bar')"))),
                    TestUtils.withMiddlewares(),
                    TestUtils.withServices(
                        TestUtils.withService("simple-service",
                            TestUtils.withServers(
                                TestUtils.withServer("172.0.0.1", 8080)))))//
            ), //
            Arguments.of(//
                "default rule template", //
                AbstractDockerProviderModel.DEFAULT_RULE_TEMPLATE, //
                new ArrayList<JsonObject>() {
                    {
                        add(record);
                    }
                }, //
                TestUtils.buildConfiguration(
                    TestUtils.withRouters(
                        TestUtils.withRouter("simple-router",
                            TestUtils.withRouterService("simple-service"),
                            TestUtils.withRouterRule("Host('simple-container')"))),
                    TestUtils.withMiddlewares(),
                    TestUtils.withServices(
                        TestUtils.withService("simple-service",
                            TestUtils.withServers(
                                TestUtils.withServer("172.0.0.1", 8080)))))//
            )//
        );//
    }

    static Stream<Arguments> dockerContainerTestData() {
        return Stream.of();
    }

    /*
    The following methods are helpers to build the following json structure
    {
      "metadata" : {
        "portal.enable" : "true",
        "portal.http.routers.auth.rule" : "PathPrefix('/auth')",
        "portal.http.services.portal-iam.servers.port" : "8080",
        "docker.names" : [ "/local-portal-portal-iam_local-portal-portal-iam_1" ],
        "docker.name" : "/local-portal-portal-iam_local-portal-portal-iam_1",
        "docker.id" : "7a773e36c8750944d4daa84c998dd73fd86dd24d9c43aa88fcab1a9ee854d925",
        "docker.ports" : [ 8080, 8080, 8443 ],
        "docker.hostPerNetwork" : {
          "defaultNetworkMode" : "portal-database",
          "portal-database" : "192.168.240.10",
          "uniport-gateway" : "192.168.224.10",
          "portal-iam" : "172.18.0.2"
        }
      },
      "name" : "/local-portal-portal-iam_local-portal-portal-iam_1",
      "status" : "UNKNOWN"
    }
    */
    private static JsonObject buildRecord(String name, Handler<JsonObject> metadataHandler) {
        JsonObject record = new JsonObject();

        JsonObject metadata = new JsonObject();
        metadataHandler.handle(metadata);

        record.put("metadata", metadata);
        record.put("name", name);
        record.put("status", Status.UNKNOWN);

        return record;
    }

    private static Handler<JsonObject> withMetadata(Handler<JsonObject>... opts) {
        return metadata -> {
            for (Handler<JsonObject> opt : opts) {
                opt.handle(metadata);
            }
        };
    }

    private static Handler<JsonObject> withId(String id) {
        return metadata -> {
            metadata.put("docker.id", id);
        };
    }

    private static Handler<JsonObject> withName(String name) {
        return metadata -> {
            metadata.put("docker.name", name);
            metadata.put("docker.names", new JsonArray().add(name));
        };
    }

    private static Handler<JsonObject> withLabels(Map<String, String> labels) {
        return metadata -> {
            for (String name : labels.keySet()) {
                String value = labels.get(name);
                metadata.put(name, value);
            }
        };
    }

    private static Handler<JsonObject> withPorts(List<Integer> ports) {
        return metadata -> {
            metadata.put("docker.ports", new JsonArray(ports));
        };
    }

    private static Handler<JsonObject> withNetworks(Map<String, String> networks) {
        return metadata -> {
            JsonObject hostPerNetwork = new JsonObject();
            for (String name : networks.keySet()) {
                String addr = networks.get(name);
                hostPerNetwork.put(name, addr);
            }
            metadata.put("docker.hostPerNetwork", hostPerNetwork);
        };
    }

    @ParameterizedTest
    @MethodSource("defaultRuleTestData")
    void dockerContainerTest(
        String desc, String defaultRule, List<JsonObject> containers, JsonObject expectedConfig,
        Vertx vertx, VertxTestContext testCtx
    ) {
        String errMsg = String.format("'%s' failed", desc);

        Checkpoint dockerProviderStarted = testCtx.checkpoint();
        Checkpoint configValidated = testCtx.checkpoint();

        long scanPeriodMs = 3000L;
        ServiceImporter serviceImporter = new MockServiceImporter(containers, scanPeriodMs);

        String configurationAddress = "test-docker-container-provider";
        boolean exposedByDefault = true;
        boolean watch = false;
        JsonObject serviceImporterConfiguration = new JsonObject();
        DockerContainerProvider dockerProvider = new DockerContainerProvider(vertx, configurationAddress, serviceImporter,
            serviceImporterConfiguration, exposedByDefault, TEST_NETWORK, defaultRule, watch);

        MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(configurationAddress);
        consumer.handler(message -> {
            JsonObject config = message.body();
            testCtx.verify(() -> {
                String pvdName = config.getString(Provider.PROVIDER_NAME);
                assertEquals(StaticConfiguration.PROVIDER_DOCKER, pvdName, errMsg);
                JsonObject pvdConfig = config.getJsonObject(Provider.PROVIDER_CONFIGURATION);
                assertEquals(expectedConfig, pvdConfig, errMsg);
            });
            configValidated.flag();
        });

        vertx.deployVerticle(dockerProvider).onComplete(testCtx.succeeding(deploymentId -> {
            dockerProviderStarted.flag();
        }));
    }
}
