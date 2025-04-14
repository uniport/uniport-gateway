package com.inventage.portal.gateway.proxy.config;

import static com.inventage.portal.gateway.TestUtils.buildConfiguration;
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
import com.inventage.portal.gateway.proxy.model.Gateway;
import com.inventage.portal.gateway.proxy.model.GatewayRouter;
import com.inventage.portal.gateway.proxy.model.GatewayService;
import com.inventage.portal.gateway.proxy.model.ServerOptions;
import com.inventage.portal.gateway.proxy.provider.Provider;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;
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

        final Gateway expected = Gateway.builder()
            .withRouters(List.of(
                GatewayRouter.builder()
                    .withName("test@mock")
                    .withEntrypoints(List.of("ep"))
                    .withRule("Path('/')")
                    .withService("svc@mock")
                    .build()))
            .withServices(List.of(
                GatewayService.builder()
                    .withName("svc@mock")
                    .withServers(List.of(
                        ServerOptions.builder()
                            .withHost("host")
                            .withPort(1234)
                            .build()))
                    .build()))
            .build();

        int providersThrottleIntervalMs = 1000;
        Provider pvd = new MockProvider(vertx, configurationAddress, messages);
        ConfigurationWatcher watcher = new ConfigurationWatcher(vertx, pvd, configurationAddress, providersThrottleIntervalMs, List.of());

        // when
        watcher.addListener(new Listener() {
            @Override
            public void listen(Gateway actual) {
                // then
                testCtx.verify(() -> assertEquals(expected, actual, errMsg));
                testCtx.completeNow();
            }
        });

        vertx.deployVerticle(watcher)
            .onFailure(err -> testCtx.failNow(String.format("%s: %s", errMsg, err.getMessage())));

        ((VertxInternal) vertx).addCloseHook(promise -> {
            if (!testCtx.completed()) {
                testCtx.failNow("should not terminate");
            }
            promise.complete();
        });
    }

    @Test
    @DisplayName("should fail fast when the first configuration is invalid")
    @SuppressWarnings("unchecked")
    void shouldFailFastWhenFirstConfigurationIsInvalid(TestInfo testInfo, Vertx vertx, VertxTestContext testCtx) {
        String errMsg = String.format("'%s' failed", testInfo.getDisplayName());

        // given
        String configurationAddress = "test-simple-configuration-watcher";
        List<JsonObject> messages = List.of(
            assembleMessage("mock", buildConfiguration(
                withRouters(
                    withRouter("test",
                        withRouterService("non-existent-svc"))))));

        int providersThrottleIntervalMs = 1000;
        Provider pvd = new MockProvider(vertx, configurationAddress, messages);
        ConfigurationWatcher watcher = new ConfigurationWatcher(vertx, pvd, configurationAddress, providersThrottleIntervalMs, List.of());

        // when
        watcher.addListener(new Listener() {
            @Override
            public void listen(Gateway actual) {
                testCtx.failNow(String.format("%s: %s", errMsg, "no message expected"));
            }
        });

        vertx.deployVerticle(watcher)
            .onFailure(err -> testCtx.failNow(String.format("%s: %s", errMsg, err.getMessage())));

        ((VertxInternal) vertx).addCloseHook(promise -> {
            testCtx.completeNow();
            promise.complete();
        });
    }

    @Test
    @DisplayName("should should ignore invalid configurations after the first valid one")
    @SuppressWarnings("unchecked")
    void shouldIgnoreInvalidConfigsAfterTheFirstOne(TestInfo testInfo, Vertx vertx, VertxTestContext testCtx) {
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
                        withServers(withServer("host", 1234)))))),
            assembleMessage("mock", buildConfiguration(
                withRouters(
                    withRouter("test",
                        withRouterService("non-existent-svc"))))));

        int providersThrottleIntervalMs = 100;
        Provider pvd = new MockProvider(vertx, configurationAddress, messages, 150);
        ConfigurationWatcher watcher = new ConfigurationWatcher(vertx, pvd, configurationAddress, providersThrottleIntervalMs, List.of());

        // when
        final AtomicInteger publishedConfigCount = new AtomicInteger(0);
        watcher.addListener(new Listener() {
            @Override
            public void listen(Gateway actual) {
                publishedConfigCount.incrementAndGet();
            }
        });

        vertx.deployVerticle(watcher)
            .onFailure(err -> testCtx.failNow(String.format("%s: %s", errMsg, err.getMessage())));

        ((VertxInternal) vertx).addCloseHook(promise -> {
            if (!testCtx.completed()) {
                testCtx.failNow("should not terminate");
            }
            promise.complete();
        });

        // give some time so that the configuration can be processed
        vertx.setTimer(500, timerID -> {
            testCtx.verify(() -> assertEquals(1, publishedConfigCount.get()));
            testCtx.completeNow();
        });
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
            public void listen(Gateway actual) {
                publishedConfigCount.incrementAndGet();
            }
        });

        vertx.deployVerticle(watcher)
            .onFailure(err -> testCtx.failNow(String.format("%s: %s", errMsg, err.getMessage())));

        ((VertxInternal) vertx).addCloseHook(promise -> {
            if (!testCtx.completed()) {
                testCtx.failNow("should not terminate");
            }
            promise.complete();
        });

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
            public void listen(Gateway actual) {
                testCtx.failNow("An empty configuration was published but it should not");
            }
        });

        vertx.deployVerticle(watcher)
            .onFailure(err -> testCtx.failNow(String.format("%s: %s", errMsg, err.getMessage())));

        ((VertxInternal) vertx).addCloseHook(promise -> {
            if (!testCtx.completed()) {
                testCtx.failNow("should not terminate");
            }
            promise.complete();
        });

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
            public void listen(Gateway actual) {
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

        ((VertxInternal) vertx).addCloseHook(promise -> {
            if (!testCtx.completed()) {
                testCtx.failNow("should not terminate");
            }
            promise.complete();
        });

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

        AtomicReference<Gateway> publishedProviderConfig = new AtomicReference<Gateway>();
        watcher.addListener(new Listener() {
            @Override
            public void listen(Gateway actual) {
                publishedProviderConfig.set(actual);
            }
        });

        vertx.deployVerticle(watcher)
            .onFailure(err -> testCtx.failNow(String.format("%s: %s", errMsg, err.getMessage())));

        ((VertxInternal) vertx).addCloseHook(promise -> {
            if (!testCtx.completed()) {
                testCtx.failNow("should not terminate");
            }
            promise.complete();
        });

        vertx.setTimer(2000, timerID -> {
            final GatewayRouter.Builder routerBuilder = GatewayRouter.builder()
                .withRule("Path('/')");

            final GatewayService.Builder serviceBuilder = GatewayService.builder()
                .withServers(List.of(
                    ServerOptions.builder()
                        .withHost("host")
                        .withPort(1234)
                        .build()));

            final Gateway expected = Gateway.builder()
                .withRouters(List.of(
                    routerBuilder
                        .withName("foo@mock")
                        .withService("bar@mock")
                        .build(),
                    routerBuilder
                        .withName("foo@mock2")
                        .withService("bar@mock2")
                        .build()))
                .withServices(List.of(
                    serviceBuilder
                        .withName("bar@mock")
                        .build(),
                    serviceBuilder
                        .withName("bar@mock2")
                        .build()))
                .build();

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
            public void listen(Gateway actual) {
                publishedConfigCount.getAndIncrement();
            }
        });

        vertx.deployVerticle(watcher)
            .onFailure(err -> testCtx.failNow(String.format("%s: %s", errMsg, err.getMessage())));

        ((VertxInternal) vertx).addCloseHook(promise -> {
            if (!testCtx.completed()) {
                testCtx.failNow("should not terminate");
            }
            promise.complete();
        });

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
