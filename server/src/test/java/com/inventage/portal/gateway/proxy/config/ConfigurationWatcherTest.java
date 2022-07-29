package com.inventage.portal.gateway.proxy.config;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.listener.Listener;
import com.inventage.portal.gateway.proxy.provider.Provider;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(VertxExtension.class)
public class ConfigurationWatcherTest {

    private JsonObject assembleMessage(String providerName, JsonObject providerConfig) {
        return new JsonObject().put(Provider.PROVIDER_NAME, providerName).put(Provider.PROVIDER_CONFIGURATION,
                providerConfig);
    }

    @Test
    @DisplayName("simple run test")
    void simpleRunTest(TestInfo testInfo, Vertx vertx, VertxTestContext testCtx) {
        String errMsg = String.format("'%s' failed", testInfo.getDisplayName());

        String configurationAddress = "test-simple-configuration-watcher";
        List<JsonObject> messages = List.of(assembleMessage("mock", TestUtils.buildConfiguration(
                TestUtils.withRouters(
                        TestUtils.withRouter("test", TestUtils.withRouterService("svc"), TestUtils.withRouterEntrypoints("ep"))),
                TestUtils
                        .withServices(TestUtils.withService("svc", TestUtils.withServers(TestUtils.withServer("host", 1234)))))));
        Provider pvd = new MockProvider(vertx, configurationAddress, messages);

        int providersThrottleIntervalMs = 1000;
        ConfigurationWatcher watcher = new ConfigurationWatcher(vertx, pvd, configurationAddress,
                providersThrottleIntervalMs, List.of());

        watcher.addListener(new Listener() {
            @Override
            public void listen(JsonObject actual) {
                JsonObject expected = TestUtils.buildConfiguration(
                        TestUtils.withRouters(TestUtils.withRouter("test@mock", TestUtils.withRouterEntrypoints("ep"),
                                TestUtils.withRouterService("svc@mock"))),
                        TestUtils.withMiddlewares(), TestUtils.withServices(
                                TestUtils.withService("svc@mock", TestUtils.withServers(TestUtils.withServer("host", 1234)))));

                testCtx.verify(() -> assertEquals(expected, actual, errMsg));
                testCtx.completeNow();
            }
        });

        vertx.deployVerticle(watcher).onFailure(err -> testCtx.failNow(String.format("%s: %s", errMsg, err.getMessage())));
    }

    @Test
    @DisplayName("throttle provider config reload test")
    void throttleProviderConfigReloadTest(TestInfo testInfo, Vertx vertx, VertxTestContext testCtx) {
        String errMsg = String.format("'%s' failed", testInfo.getDisplayName());

        String configurationAddress = "test-throttle-configuration-watcher";
        List<JsonObject> messages = new ArrayList<JsonObject>();
        for (int i = 0; i < 5; i++) {
            messages.add(assembleMessage("mock", TestUtils.buildConfiguration(
                    TestUtils.withRouters(TestUtils.withRouter(String.format("foo%d", i), TestUtils.withRouterService("bar"))),
                    TestUtils
                            .withServices(TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer("host", 1234)))))));
        }
        long waitMs = 1000;
        Provider pvd = new MockProvider(vertx, configurationAddress, messages, waitMs);

        int providersThrottleIntervalMs = 3000;
        ConfigurationWatcher watcher = new ConfigurationWatcher(vertx, pvd, configurationAddress,
                providersThrottleIntervalMs, List.of());

        AtomicInteger publishedConfigCount = new AtomicInteger(0);
        watcher.addListener(new Listener() {
            @Override
            public void listen(JsonObject actual) {
                publishedConfigCount.incrementAndGet();
            }
        });

        vertx.deployVerticle(watcher).onFailure(err -> testCtx.failNow(String.format("%s: %s", errMsg, err.getMessage())));

        // give some time so that the configuration can be processed
        vertx.setTimer(10000, timerID -> {
            // after 500 milliseconds 5 new configs were published
            // with a throttle duration of 3000 milliseconds this means,
            // we should have received 3 new configs
            testCtx.verify(() -> assertEquals(3, publishedConfigCount.get()));
            testCtx.completeNow();
        });
    }

    @Test
    @DisplayName("skip empty configs test")
    void skipEmptyConfigsTest(TestInfo testInfo, Vertx vertx, VertxTestContext testCtx) {
        String errMsg = String.format("'%s' failed", testInfo.getDisplayName());

        String configurationAddress = "test-throttle-configuration-watcher";
        List<JsonObject> messages = List.of(assembleMessage("mock", null));
        long waitMs = 10;

        Provider pvd = new MockProvider(vertx, configurationAddress, messages, waitMs);

        int providersThrottleIntervalMs = 1000;
        ConfigurationWatcher watcher = new ConfigurationWatcher(vertx, pvd, configurationAddress,
                providersThrottleIntervalMs, List.of());

        watcher.addListener(new Listener() {
            @Override
            public void listen(JsonObject actual) {
                testCtx.failNow("An empty configuration was published but it should not");
            }
        });

        vertx.deployVerticle(watcher).onFailure(err -> testCtx.failNow(String.format("%s: %s", errMsg, err.getMessage())));

        vertx.setTimer(2000, timerID -> {
            testCtx.completeNow();
        });
    }

    @Test
    @DisplayName("skip same config for provider test")
    void skipSameConfigForProviderTest(TestInfo testInfo, Vertx vertx, VertxTestContext testCtx) {
        String errMsg = String.format("'%s' failed", testInfo.getDisplayName());

        String configurationAddress = "test-throttle-configuration-watcher";
        JsonObject message = assembleMessage("mock", TestUtils.buildConfiguration(
                TestUtils.withRouters(TestUtils.withRouter("foo", TestUtils.withRouterService("bar"))), TestUtils
                        .withServices(TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer("host", 1234))))));
        List<JsonObject> messages = List.of(message, message);
        long waitMs = 100;

        Provider pvd = new MockProvider(vertx, configurationAddress, messages, waitMs);

        int providersThrottleIntervalMs = 1000;
        ConfigurationWatcher watcher = new ConfigurationWatcher(vertx, pvd, configurationAddress,
                providersThrottleIntervalMs, List.of());

        AtomicBoolean isFirst = new AtomicBoolean(true);
        watcher.addListener(new Listener() {
            @Override
            public void listen(JsonObject actual) {
                if (isFirst.get()) {
                    testCtx.verify(() -> assertNotNull(actual, errMsg));
                } else {
                    testCtx.failNow("The same configuration was published but it should not");
                }
                isFirst.set(false);
            }
        });

        vertx.deployVerticle(watcher).onFailure(err -> testCtx.failNow(String.format("%s: %s", errMsg, err.getMessage())));

        vertx.setTimer(2000, timerID -> {
            testCtx.completeNow();
        });
    }

    @Test
    @DisplayName("publishes config for each provider test")
    void publishesConfigForEachProviderTest(TestInfo testInfo, Vertx vertx, VertxTestContext testCtx) {
        String errMsg = String.format("'%s' failed", testInfo.getDisplayName());

        String configurationAddress = "test-throttle-configuration-watcher";
        JsonObject pvdConfig = TestUtils.buildConfiguration(
                TestUtils.withRouters(TestUtils.withRouter("foo", TestUtils.withRouterService("bar"))), TestUtils
                        .withServices(TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer("host", 1234)))));
        List<JsonObject> messages = List.of(assembleMessage("mock", pvdConfig), assembleMessage("mock2", pvdConfig));
        long waitMs = 10;

        Provider pvd = new MockProvider(vertx, configurationAddress, messages, waitMs);

        int providersThrottleIntervalMs = 1000;
        ConfigurationWatcher watcher = new ConfigurationWatcher(vertx, pvd, configurationAddress,
                providersThrottleIntervalMs, List.of());

        AtomicReference<JsonObject> publishedProviderConfig = new AtomicReference<JsonObject>();
        watcher.addListener(new Listener() {
            @Override
            public void listen(JsonObject actual) {
                publishedProviderConfig.set(actual);
            }
        });

        vertx.deployVerticle(watcher).onFailure(err -> testCtx.failNow(String.format("%s: %s", errMsg, err.getMessage())));

        vertx.setTimer(2000, timerID -> {
            JsonObject expected = TestUtils.buildConfiguration(TestUtils.withRouters(
                            TestUtils.withRouter("foo@mock", TestUtils.withRouterEntrypoints(), TestUtils.withRouterService("bar@mock")),
                            TestUtils
                                    .withRouter("foo@mock2", TestUtils.withRouterEntrypoints(), TestUtils.withRouterService("bar@mock2"))),
                    TestUtils.withMiddlewares(),
                    TestUtils.withServices(
                            TestUtils.withService("bar@mock", TestUtils.withServers(TestUtils.withServer("host", 1234))),
                            TestUtils.withService("bar@mock2", TestUtils.withServers(TestUtils.withServer("host", 1234)))));

            testCtx.verify(() -> assertEquals(expected, publishedProviderConfig.get()));
            testCtx.completeNow();
        });
    }

    @Test
    @DisplayName("publish config by provider test")
    void publishConfigByProviderTest(TestInfo testInfo, Vertx vertx, VertxTestContext testCtx) {
        String errMsg = String.format("'%s' failed", testInfo.getDisplayName());

        String configurationAddress = "test-throttle-configuration-watcher";
        JsonObject pvdConfig = TestUtils.buildConfiguration(
                TestUtils.withRouters(TestUtils.withRouter("foo", TestUtils.withRouterService("bar"))), TestUtils
                        .withServices(TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer("host", 1234)))));

        // Update the provider configuration published in next dynamic Message which should trigger a new publish.
        JsonObject pvdConfigUpdate = TestUtils.buildConfiguration(
                TestUtils.withRouters(TestUtils.withRouter("blub", TestUtils.withRouterService("bar"))), TestUtils
                        .withServices(TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer("host", 1234)))));

        List<JsonObject> messages = List.of(assembleMessage("mock", pvdConfig), assembleMessage("mock", pvdConfigUpdate));
        long waitMs = 100;

        Provider pvd = new MockProvider(vertx, configurationAddress, messages, waitMs);

        int providersThrottleIntervalMs = 1000;
        ConfigurationWatcher watcher = new ConfigurationWatcher(vertx, pvd, configurationAddress,
                providersThrottleIntervalMs, List.of());

        AtomicInteger publishedConfigCount = new AtomicInteger(0);
        watcher.addListener(new Listener() {
            @Override
            public void listen(JsonObject actual) {
                publishedConfigCount.getAndIncrement();
            }
        });

        vertx.deployVerticle(watcher).onFailure(err -> testCtx.failNow(String.format("%s: %s", errMsg, err.getMessage())));

        vertx.setTimer(2000, timerID -> {
            testCtx.verify(() -> assertEquals(2, publishedConfigCount.get()));
            testCtx.completeNow();
        });
    }

}
