package com.inventage.portal.gateway.proxy.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.listener.Listener;
import com.inventage.portal.gateway.proxy.provider.Provider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

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

    ConfigurationWatcher watcher = new ConfigurationWatcher(vertx, pvd, configurationAddress, 1000, List.of());

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

    watcher.start()
        .onFailure(err -> testCtx.verify(() -> assertTrue(false, String.format("%s: %s", errMsg, err.getMessage()))));
  }

  // TODO @Test
  @DisplayName("throttle provider config reload test")
  void throttleProviderConfigReloadTest(TestInfo testInfo, Vertx vertx, VertxTestContext testCtx) {
    String errMsg = String.format("'%s' failed", testInfo.getDisplayName());

    String configurationAddress = "test-throttle-configuration-watcher";
    List<JsonObject> messages = new ArrayList<JsonObject>();
    for (int i = 0; i < 5; i++) {
      messages.add(assembleMessage("mock",
          TestUtils.buildConfiguration(TestUtils.withRouters(TestUtils.withRouter(String.format("foo%d", i))), TestUtils
              .withServices(TestUtils.withService("bar", TestUtils.withServers(TestUtils.withServer("host", 1234)))))));
    }
    long waitMs = 10;
    Provider pvd = new MockProvider(vertx, configurationAddress, messages, waitMs);

    ConfigurationWatcher watcher = new ConfigurationWatcher(vertx, pvd, configurationAddress, 30, List.of());

    AtomicInteger publishedConfigCount = new AtomicInteger(0);
    watcher.addListener(new Listener() {
      @Override
      public void listen(JsonObject actual) {
        publishedConfigCount.incrementAndGet();
      }
    });

    watcher.start()
        .onFailure(err -> testCtx.verify(() -> assertTrue(false, String.format("%s: %s", errMsg, err.getMessage()))));

    testCtx.verify(() -> assertEquals(3, publishedConfigCount.get()));
    testCtx.completeNow();
  }

  // TODO @Test
  @DisplayName("skip empty configs test")
  void skipEmptyConfigsTest(TestInfo testInfo, Vertx vertx, VertxTestContext testCtx) {
    String errMsg = String.format("'%s' failed", testInfo.getDisplayName());

    String configurationAddress = "test-throttle-configuration-watcher";
    List<JsonObject> messages = List.of();
    long waitMs = 10;

    Provider pvd = new MockProvider(vertx, configurationAddress, messages, waitMs);

    ConfigurationWatcher watcher = new ConfigurationWatcher(vertx, pvd, configurationAddress, 1000, List.of());

    watcher.addListener(new Listener() {
      @Override
      public void listen(JsonObject actual) {
        JsonObject expected = null;
        testCtx.verify(() -> assertEquals(expected, actual, errMsg));
        testCtx.completeNow();
      }
    });

    watcher.start()
        .onFailure(err -> testCtx.verify(() -> assertTrue(false, String.format("%s: %s", errMsg, err.getMessage()))));
  }

  // TODO @Test
  @DisplayName("skip same config for provider test")
  void skipSameConfigForProviderTest(TestInfo testInfo, Vertx vertx, VertxTestContext testCtx) {
    String errMsg = String.format("'%s' failed", testInfo.getDisplayName());

    String configurationAddress = "test-throttle-configuration-watcher";
    List<JsonObject> messages = List.of();
    long waitMs = 10;

    Provider pvd = new MockProvider(vertx, configurationAddress, messages, waitMs);

    ConfigurationWatcher watcher = new ConfigurationWatcher(vertx, pvd, configurationAddress, 1000, List.of());

    watcher.addListener(new Listener() {
      @Override
      public void listen(JsonObject actual) {
        JsonObject expected = null;
        testCtx.verify(() -> assertEquals(expected, actual, errMsg));
        testCtx.completeNow();
      }
    });

    watcher.start()
        .onFailure(err -> testCtx.verify(() -> assertTrue(false, String.format("%s: %s", errMsg, err.getMessage()))));
  }

  // TODO @Test
  @DisplayName("does not skip flapping config test")
  void doesNotSkipFlappingConfigTest(TestInfo testInfo, Vertx vertx, VertxTestContext testCtx) {
    String errMsg = String.format("'%s' failed", testInfo.getDisplayName());

    String configurationAddress = "test-throttle-configuration-watcher";
    List<JsonObject> messages = List.of();
    long waitMs = 10;

    Provider pvd = new MockProvider(vertx, configurationAddress, messages, waitMs);

    ConfigurationWatcher watcher = new ConfigurationWatcher(vertx, pvd, configurationAddress, 1000, List.of());

    watcher.addListener(new Listener() {
      @Override
      public void listen(JsonObject actual) {
        JsonObject expected = null;
        testCtx.verify(() -> assertEquals(expected, actual, errMsg));
        testCtx.completeNow();
      }
    });

    watcher.start()
        .onFailure(err -> testCtx.verify(() -> assertTrue(false, String.format("%s: %s", errMsg, err.getMessage()))));
  }

  // TODO @Test
  @DisplayName("publishes config for each provider test")
  void publishesConfigForEachProviderTest(TestInfo testInfo, Vertx vertx, VertxTestContext testCtx) {
    String errMsg = String.format("'%s' failed", testInfo.getDisplayName());

    String configurationAddress = "test-throttle-configuration-watcher";
    List<JsonObject> messages = List.of();
    long waitMs = 10;

    Provider pvd = new MockProvider(vertx, configurationAddress, messages, waitMs);

    ConfigurationWatcher watcher = new ConfigurationWatcher(vertx, pvd, configurationAddress, 1000, List.of());

    watcher.addListener(new Listener() {
      @Override
      public void listen(JsonObject actual) {
        JsonObject expected = null;
        testCtx.verify(() -> assertEquals(expected, actual, errMsg));
        testCtx.completeNow();
      }
    });

    watcher.start()
        .onFailure(err -> testCtx.verify(() -> assertTrue(false, String.format("%s: %s", errMsg, err.getMessage()))));
  }

  // TODO @Test
  @DisplayName("publish config by provider test")
  void publishConfigByProviderTest(TestInfo testInfo, Vertx vertx, VertxTestContext testCtx) {
    String errMsg = String.format("'%s' failed", testInfo.getDisplayName());

    String configurationAddress = "test-throttle-configuration-watcher";
    List<JsonObject> messages = List.of();
    long waitMs = 10;

    Provider pvd = new MockProvider(vertx, configurationAddress, messages, waitMs);

    ConfigurationWatcher watcher = new ConfigurationWatcher(vertx, pvd, configurationAddress, 1000, List.of());

    watcher.addListener(new Listener() {
      @Override
      public void listen(JsonObject actual) {
        JsonObject expected = null;
        testCtx.verify(() -> assertEquals(expected, actual, errMsg));
        testCtx.completeNow();
      }
    });

    watcher.start()
        .onFailure(err -> testCtx.verify(() -> assertTrue(false, String.format("%s: %s", errMsg, err.getMessage()))));
  }

}
