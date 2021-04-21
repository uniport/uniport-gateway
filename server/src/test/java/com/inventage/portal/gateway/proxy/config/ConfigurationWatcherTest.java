package com.inventage.portal.gateway.proxy.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.listener.Listener;
import com.inventage.portal.gateway.proxy.provider.Provider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class ConfigurationWatcherTest {

    @Test
    void simpleTest(Vertx vertx, VertxTestContext testCtx) {
        String name = "simple test";
        String errMsg = String.format("'%s' failed.", name);

        String configurationAddress = "test-simple-configuration-watcher";
        List<JsonObject> messages = List.of(new JsonObject().put(Provider.PROVIDER_NAME, "mock")
                .put(Provider.PROVIDER_CONFIGURATION,
                        TestUtils.buildConfiguration(
                                TestUtils.withRouters(TestUtils.withRouter("test@mock",
                                        TestUtils.withRouterService("svc"),
                                        TestUtils.withRouterEntrypoints("ep"))),
                                TestUtils.withServices(TestUtils.withService("svc", TestUtils
                                        .withServers(TestUtils.withServer("host", 1234)))))));
        Provider pvd = new MockProvider(vertx, configurationAddress, messages);

        ConfigurationWatcher watcher =
                new ConfigurationWatcher(vertx, pvd, configurationAddress, 1, List.of());

        watcher.addListener(new Listener() {
            @Override
            public void listen(JsonObject actual) {
                // TODO build expected
                JsonObject expected = null;

                testCtx.verify(() -> assertEquals(expected, actual, errMsg));
                testCtx.completeNow();
            }
        });

        watcher.start().onSuccess(msg -> System.out.printf("Start succeeded: %s\n", msg))
                .onFailure(err -> System.out.printf("Start failed: %s", err.getMessage()));
    }
}
