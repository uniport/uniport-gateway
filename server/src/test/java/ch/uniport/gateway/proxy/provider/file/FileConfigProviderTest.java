package ch.uniport.gateway.proxy.provider.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ch.uniport.gateway.core.config.StaticConfiguration;
import ch.uniport.gateway.proxy.config.DynamicConfiguration;
import ch.uniport.gateway.proxy.provider.Provider;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(VertxExtension.class)
public class FileConfigProviderTest {

    static Stream<Arguments> provideWithoutWatchTestData() {
        // port as int/no variables
        return Stream.of(
            Arguments.of("simple file", "fileConfigProvider/simple_file_01.json", 3, 6),
            Arguments.of("simple directory", "fileConfigProvider/simple_dir_01", 2, 3),
            Arguments.of("merge directories", "fileConfigProvider/merge_dir_01", 3, 3),
            Arguments.of("merge directories with arbitrary folder names", "fileConfigProvider/merge_dir_02", 3, 3));

    }

    @Test
    @DisplayName("no config published when empty/invalid")
    void errorWhenEmptyConfigTest(TestInfo testInfo, Vertx vertx, VertxTestContext testCtx) {
        final Checkpoint fileProviderStarted = testCtx.checkpoint();
        final Checkpoint nothingReceived = testCtx.checkpoint();

        final String path = "fileConfigProvider/invalid_config_file_01.json";
        final String configurationAddress = "file-config-provider-test";
        final boolean watch = false;
        final JsonObject env = null;
        final FileConfigProvider fileProvider = createProvider(vertx, path, configurationAddress, watch, env);
        if (fileProvider == null) {
            testCtx.failNow("Failed to created file provder. File/Directory does not exist.");
        }

        final MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(configurationAddress);
        consumer.handler(message -> {
            testCtx.failNow("Received configuration but should not.");
        });

        vertx.deployVerticle(fileProvider).onComplete(testCtx.succeeding(deploymentId -> {
            fileProviderStarted.flag();
        }));

        vertx.setTimer(1000, timerId -> {
            // give some time to process the configuration
            nothingReceived.flag();
        });
    }

    @Test
    @DisplayName("simple file with variable substitution")
    void variableSubstitutionTest(TestInfo testInfo, Vertx vertx, VertxTestContext testCtx) {
        final String errMsg = String.format("'%s' failed", testInfo.getDisplayName());

        final Checkpoint fileProviderStarted = testCtx.checkpoint();
        final Checkpoint configReceived = testCtx.checkpoint();
        final Checkpoint configValidated = testCtx.checkpoint();

        final int expectedNumRouter = 1;
        final int expectedNumService = 2;
        final int expectedNumServersEach = 1;
        final String expectedHost = "http://testhost.org";
        final int expectedPort = 1234;

        final String path = "fileConfigProvider/variable_substitution_file_01.json";
        final String configurationAddress = "file-config-provider-test";
        final boolean watch = false;
        final JsonObject env = new JsonObject()
            .put("test.host", expectedHost)
            .put("test.port.string", String.format("%s", expectedPort))
            .put("test.port.number", expectedPort);
        final FileConfigProvider fileProvider = createProvider(vertx, path, configurationAddress, watch, env);
        if (fileProvider == null) {
            testCtx.failNow("Failed to created file provder. File/Directory does not exist.");
        }

        final MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(configurationAddress);
        consumer.handler(message -> {
            configReceived.flag();
            final JsonObject config = message.body();
            testCtx.verify(() -> {
                final String pvdName = config.getString(Provider.PROVIDER_NAME);
                assertEquals(StaticConfiguration.PROVIDER_FILE, pvdName, errMsg);
                final JsonObject pvdConfig = config.getJsonObject(Provider.PROVIDER_CONFIGURATION);
                assertNotNull(pvdConfig, errMsg);
                final JsonObject http = pvdConfig.getJsonObject(DynamicConfiguration.HTTP);
                assertNotNull(http, errMsg);
                final JsonArray routers = http.getJsonArray(DynamicConfiguration.ROUTERS);
                assertNotNull(routers, errMsg);
                assertEquals(expectedNumRouter, routers.size(), errMsg);
                final JsonArray services = http.getJsonArray(DynamicConfiguration.SERVICES);
                assertNotNull(services, errMsg);
                assertEquals(expectedNumService, services.size(), errMsg);
                for (int i = 0; i < services.size(); i++) {
                    final JsonObject service = services.getJsonObject(i);
                    assertNotNull(service, errMsg);
                    final JsonArray servers = service.getJsonArray(DynamicConfiguration.SERVICE_SERVERS);
                    assertNotNull(servers, errMsg);
                    assertEquals(expectedNumServersEach, servers.size(), errMsg);
                    for (int j = 0; j < servers.size(); j++) {
                        final JsonObject server = servers.getJsonObject(j);
                        assertNotNull(server, errMsg);
                        final String host = server.getString(DynamicConfiguration.SERVICE_SERVER_HOST);
                        assertEquals(expectedHost, host, errMsg);
                        final int port = server.getInteger(DynamicConfiguration.SERVICE_SERVER_PORT);
                        assertEquals(expectedPort, port, errMsg);
                    }
                }
            });
            configValidated.flag();
        });

        vertx.deployVerticle(fileProvider).onComplete(testCtx.succeeding(deploymentId -> {
            fileProviderStarted.flag();
        }));
    }

    @ParameterizedTest
    @MethodSource("provideWithoutWatchTestData")
    void provideWithoutWatchTest(
        String name, String path, int expectedNumRouter, int expectedNumService, Vertx vertx,
        VertxTestContext testCtx
    ) {
        final String errMsg = String.format("'%s' failed", name);

        final Checkpoint fileProviderStarted = testCtx.checkpoint();
        final Checkpoint configReceived = testCtx.checkpoint();
        final Checkpoint configValidated = testCtx.checkpoint();

        final String configurationAddress = "file-config-provider-test";
        final boolean watch = false;
        final JsonObject env = null;
        final FileConfigProvider fileProvider = createProvider(vertx, path, configurationAddress, watch, env);
        if (fileProvider == null) {
            testCtx.failNow("Failed to created file provder. File/Directory does not exist.");
        }

        final MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(configurationAddress);
        consumer.handler(message -> {
            configReceived.flag();
            final JsonObject config = message.body();
            testCtx.verify(() -> {
                final String pvdName = config.getString(Provider.PROVIDER_NAME);
                assertEquals(StaticConfiguration.PROVIDER_FILE, pvdName, errMsg);
                final JsonObject pvdConfig = config.getJsonObject(Provider.PROVIDER_CONFIGURATION);
                assertNotNull(pvdConfig, errMsg);
                final JsonObject http = pvdConfig.getJsonObject(DynamicConfiguration.HTTP);
                assertNotNull(http, errMsg);
                final JsonArray routers = http.getJsonArray(DynamicConfiguration.ROUTERS);
                assertNotNull(routers, errMsg);
                assertEquals(expectedNumRouter, routers.size(), errMsg);
                final JsonArray services = http.getJsonArray(DynamicConfiguration.SERVICES);
                assertNotNull(services, errMsg);
                assertEquals(expectedNumService, services.size(), errMsg);
            });
            configValidated.flag();
        });

        vertx.deployVerticle(fileProvider).onComplete(testCtx.succeeding(deploymentId -> {
            fileProviderStarted.flag();
        }));
    }

    private FileConfigProvider createProvider(
        Vertx vertx, String path, String configurationAddress, boolean watch,
        JsonObject env
    ) {
        final ClassLoader classLoader = getClass().getClassLoader();
        final File file = new File(classLoader.getResource(path).getFile());

        if (!file.exists()) {
            return null;
        } else if (file.isFile()) {
            return new FileConfigProvider(vertx, configurationAddress, file.getAbsolutePath(), "", watch, env);
        } else if (file.isDirectory()) {
            return new FileConfigProvider(vertx, configurationAddress, "", file.getAbsolutePath(), watch, env);
        } else {
            return null;
        }
    }
}
