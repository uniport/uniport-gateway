package com.inventage.portal.gateway.proxy.config;

import static com.inventage.portal.gateway.TestUtils.buildConfiguration;
import static com.inventage.portal.gateway.TestUtils.withMiddlewares;
import static com.inventage.portal.gateway.TestUtils.withRouter;
import static com.inventage.portal.gateway.TestUtils.withRouterEntrypoints;
import static com.inventage.portal.gateway.TestUtils.withRouterRule;
import static com.inventage.portal.gateway.TestUtils.withRouterService;
import static com.inventage.portal.gateway.TestUtils.withRouters;
import static com.inventage.portal.gateway.TestUtils.withServer;
import static com.inventage.portal.gateway.TestUtils.withServers;
import static com.inventage.portal.gateway.TestUtils.withService;
import static com.inventage.portal.gateway.TestUtils.withServices;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.inventage.portal.gateway.proxy.listener.Listener;
import com.inventage.portal.gateway.proxy.provider.Provider;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class ConfigurationWatcherTest {

    @Test
    @DisplayName("simple run test")
    @SuppressWarnings("unchecked")
    void simpleRunTest(TestInfo testInfo, Vertx vertx, VertxTestContext testCtx) {
        String errMsg = String.format("'%s' failed", testInfo.getDisplayName());

        // given
        String configurationAddress = "test-simple-configuration-watcher";
        List<JsonObject> messages = List.of(
            assembleMessage("mock", buildConfiguration(
                withRouters(
                    withRouter("test",
                        withRouterRule("Path('/')"),
                        withRouterService("svc"),
                        withRouterEntrypoints("ep"))),
                withServices(
                    withService("svc",
                        withServers(withServer("host", 1234)))))));

        JsonObject expected = buildConfiguration(
            withRouters(
                withRouter("test@mock",
                    withRouterEntrypoints("ep"),
                    withRouterRule("Path('/')"),
                    withRouterService("svc@mock"))),
            withMiddlewares(),
            withServices(
                withService("svc@mock",
                    withServers(withServer("host", 1234)))));

        int providersThrottleIntervalMs = 1000;
        Provider pvd = new MockProvider(vertx, configurationAddress, messages);
        ConfigurationWatcher watcher = new ConfigurationWatcher(vertx, pvd, configurationAddress, providersThrottleIntervalMs, List.of());

        // when
        watcher.addListener(new Listener() {
            @Override
            public void listen(JsonObject actual) {
                // then
                testCtx.verify(() -> assertEquals(expected, actual, errMsg));
                testCtx.completeNow();
            }
        });

        vertx.deployVerticle(watcher)
            .onFailure(err -> testCtx.failNow(String.format("%s: %s", errMsg, err.getMessage())));
    }

    @Test
    @DisplayName("throttle provider config reload test")
    @SuppressWarnings("unchecked")
    void throttleProviderConfigReloadTest(TestInfo testInfo, Vertx vertx, VertxTestContext testCtx) {
        String errMsg = String.format("'%s' failed", testInfo.getDisplayName());

        String configurationAddress = "test-throttle-configuration-watcher";
        List<JsonObject> messages = new ArrayList<JsonObject>();
        for (int i = 0; i < 5; i++) {
            messages.add(assembleMessage("mock", buildConfiguration(
                withRouters(
                    withRouter(String.format("foo%d", i),
                        withRouterRule("Path('/')"),
                        withRouterService("bar"))),
                withServices(
                    withService("bar",
                        withServers(withServer("host", 1234)))))));
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

        vertx.deployVerticle(watcher)
            .onFailure(err -> testCtx.failNow(String.format("%s: %s", errMsg, err.getMessage())));

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

        vertx.deployVerticle(watcher)
            .onFailure(err -> testCtx.failNow(String.format("%s: %s", errMsg, err.getMessage())));

        vertx.setTimer(2000, timerID -> {
            testCtx.completeNow();
        });
    }

    @Test
    @DisplayName("skip same config for provider test")
    @SuppressWarnings("unchecked")
    void skipSameConfigForProviderTest(TestInfo testInfo, Vertx vertx, VertxTestContext testCtx) {
        String errMsg = String.format("'%s' failed", testInfo.getDisplayName());

        String configurationAddress = "test-throttle-configuration-watcher";
        JsonObject message = assembleMessage("mock", buildConfiguration(
            withRouters(
                withRouter("foo",
                    withRouterRule("Path('/')"),
                    withRouterService("bar"))),
            withServices(
                withService("bar",
                    withServers(withServer("host", 1234))))));
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

        vertx.deployVerticle(watcher)
            .onFailure(err -> testCtx.failNow(String.format("%s: %s", errMsg, err.getMessage())));

        vertx.setTimer(2000, timerID -> {
            testCtx.completeNow();
        });
    }

    @Test
    @DisplayName("publishes config for each provider test")
    @SuppressWarnings("unchecked")
    void publishesConfigForEachProviderTest(TestInfo testInfo, Vertx vertx, VertxTestContext testCtx) {
        String errMsg = String.format("'%s' failed", testInfo.getDisplayName());

        String configurationAddress = "test-throttle-configuration-watcher";
        JsonObject pvdConfig = buildConfiguration(
            withRouters(
                withRouter("foo",
                    withRouterRule("Path('/')"),
                    withRouterService("bar"))),
            withServices(
                withService("bar",
                    withServers(withServer("host", 1234)))));
        List<JsonObject> messages = List.of(
            assembleMessage("mock", pvdConfig),
            assembleMessage("mock2", pvdConfig));
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

        vertx.deployVerticle(watcher)
            .onFailure(err -> testCtx.failNow(String.format("%s: %s", errMsg, err.getMessage())));

        vertx.setTimer(2000, timerID -> {
            JsonObject expected = buildConfiguration(
                withRouters(
                    withRouter("foo@mock",
                        withRouterRule("Path('/')"),
                        withRouterEntrypoints(),
                        withRouterService("bar@mock")),
                    withRouter("foo@mock2",
                        withRouterRule("Path('/')"),
                        withRouterEntrypoints(),
                        withRouterService("bar@mock2"))),
                withMiddlewares(),
                withServices(
                    withService("bar@mock", withServers(withServer("host", 1234))),
                    withService("bar@mock2", withServers(withServer("host", 1234)))));

            testCtx.verify(() -> assertEquals(expected, publishedProviderConfig.get()));
            testCtx.completeNow();
        });
    }

    @Test
    @DisplayName("publish config by provider test")
    @SuppressWarnings("unchecked")
    void publishConfigByProviderTest(TestInfo testInfo, Vertx vertx, VertxTestContext testCtx) {
        String errMsg = String.format("'%s' failed", testInfo.getDisplayName());

        String configurationAddress = "test-throttle-configuration-watcher";
        JsonObject pvdConfig = buildConfiguration(
            withRouters(
                withRouter("foo",
                    withRouterRule("Path('/')"),
                    withRouterService("bar"))),
            withServices(
                withService("bar", withServers(withServer("host", 1234)))));

        // Update the provider configuration published in next dynamic Message which should trigger a new publish.
        JsonObject pvdConfigUpdate = buildConfiguration(
            withRouters(
                withRouter("blub",
                    withRouterRule("Path('/')"),
                    withRouterService("bar"))),
            withServices(
                withService("bar", withServers(withServer("host", 1234)))));

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

        vertx.deployVerticle(watcher)
            .onFailure(err -> testCtx.failNow(String.format("%s: %s", errMsg, err.getMessage())));

        vertx.setTimer(2000, timerID -> {
            testCtx.verify(() -> assertEquals(2, publishedConfigCount.get()));
            testCtx.completeNow();
        });
    }

    private JsonObject assembleMessage(String providerName, JsonObject providerConfig) {
        return new JsonObject()
            .put(Provider.PROVIDER_NAME, providerName)
            .put(Provider.PROVIDER_CONFIGURATION, providerConfig);
    }
}
