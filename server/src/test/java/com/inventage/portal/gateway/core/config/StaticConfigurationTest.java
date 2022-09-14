package com.inventage.portal.gateway.core.config;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class StaticConfigurationTest {

    @Test
    public void acceptEmptyConfig(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject();
        StaticConfiguration.validate(vertx, json).onComplete(testCtx.succeedingThenComplete());
    }

    @Test
    public void rejectNullEntrypointsApplicationsProviders(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.ENTRYPOINTS, null)
                .put(StaticConfiguration.APPLICATIONS, null).put(StaticConfiguration.PROVIDERS, null);

        StaticConfiguration.validate(vertx, json).onComplete(testCtx.failingThenComplete());
    }

    @Test
    public void acceptEmptyEntrypointsApplicationsProviders(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.ENTRYPOINTS, new JsonArray())
                .put(StaticConfiguration.APPLICATIONS, new JsonArray())
                .put(StaticConfiguration.PROVIDERS, new JsonArray());

        StaticConfiguration.validate(vertx, json).onComplete(testCtx.succeedingThenComplete());
    }

    @Test
    public void rejectEmptyEntrypoint(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.ENTRYPOINTS, new JsonArray().add(new JsonObject()));

        StaticConfiguration.validate(vertx, json).onComplete(testCtx.failingThenComplete());
    }

    @Test
    public void acceptValidEntrypoint(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.ENTRYPOINTS,
                new JsonArray().add(new JsonObject().put(StaticConfiguration.ENTRYPOINT_NAME, "testEntrypoint")
                        .put(StaticConfiguration.ENTRYPOINT_PORT, 1234)));

        StaticConfiguration.validate(vertx, json).onComplete(testCtx.succeedingThenComplete());
    }

    @Test
    public void rejectEntrypointWithMissingValues(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.ENTRYPOINTS,
                new JsonArray().add(new JsonObject().put(StaticConfiguration.ENTRYPOINT_PORT, 1234)));

        StaticConfiguration.validate(vertx, json).onComplete(testCtx.failingThenComplete());
    }

    @Test
    public void rejectEntrypointWithInvalidValues(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.ENTRYPOINTS, new JsonArray().add(new JsonObject()
                .put(StaticConfiguration.ENTRYPOINT_NAME, null).put(StaticConfiguration.ENTRYPOINT_PORT, "port")));

        StaticConfiguration.validate(vertx, json).onComplete(testCtx.failingThenComplete());
    }

    @Test
    public void rejectEntrypointWithUnkownKey(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.ENTRYPOINTS,
                new JsonArray().add(new JsonObject().put("blub", null)));

        StaticConfiguration.validate(vertx, json).onComplete(testCtx.failingThenComplete());
    }

    @Test
    public void acceptValidApplication(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.APPLICATIONS,
                new JsonArray().add(new JsonObject().put(StaticConfiguration.APPLICATION_NAME, "app")
                        .put(StaticConfiguration.APPLICATION_ENTRYPOINT, "PoC")
                        .put(StaticConfiguration.APPLICATION_REQUEST_SELECTOR, new JsonObject()
                                .put(StaticConfiguration.APPLICATION_REQUEST_SELECTOR_URL_PREFIX, "/app"))
                        .put(StaticConfiguration.APPLICATION_PROVIDER, "ProxyApplication")));
        StaticConfiguration.validate(vertx, json).onComplete(testCtx.succeedingThenComplete());
    }

    @Test
    public void rejectEmptyApplication(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.APPLICATIONS, new JsonArray().add(new JsonObject()));
        StaticConfiguration.validate(vertx, json).onComplete(testCtx.failingThenComplete());
    }

    @Test
    public void rejectApplicationWithMissingValues(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.APPLICATIONS,
                new JsonArray().add(new JsonObject().put(StaticConfiguration.APPLICATION_NAME, "app")
                        .put(StaticConfiguration.APPLICATION_ENTRYPOINT, "PoC")));
        StaticConfiguration.validate(vertx, json).onComplete(testCtx.failingThenComplete());
    }

    @Test
    public void rejectApplicationWithInvalidValues(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.APPLICATIONS,
                new JsonArray().add(new JsonObject().put(StaticConfiguration.APPLICATION_NAME, 123)
                        .put(StaticConfiguration.APPLICATION_ENTRYPOINT, true)
                        .put(StaticConfiguration.APPLICATION_REQUEST_SELECTOR, -1)
                        .put(StaticConfiguration.APPLICATION_PROVIDER, 1.0)));
        StaticConfiguration.validate(vertx, json).onComplete(testCtx.failingThenComplete());
    }

    @Test
    public void rejectApplicationWithUnknownKey(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.APPLICATIONS,
                new JsonArray().add(new JsonObject().put(StaticConfiguration.APPLICATION_NAME, "app")
                        .put(StaticConfiguration.APPLICATION_ENTRYPOINT, "PoC")
                        .put(StaticConfiguration.APPLICATION_REQUEST_SELECTOR, "/app")
                        .put(StaticConfiguration.APPLICATION_PROVIDER, "ProxyApplication").put("blub", null)));
        StaticConfiguration.validate(vertx, json).onComplete(testCtx.failingThenComplete());
    }

    @Test
    public void acceptValidFileProvider(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.PROVIDERS,
                new JsonArray()
                        .add(new JsonObject().put(StaticConfiguration.PROVIDER_NAME, StaticConfiguration.PROVIDER_FILE)
                                .put(StaticConfiguration.PROVIDER_FILE_FILENAME, "somefile.json")));
        StaticConfiguration.validate(vertx, json).onComplete(testCtx.succeedingThenComplete());
    }

    @Test
    public void rejectFileProviderWithNoSource(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.PROVIDERS, new JsonArray()
                .add(new JsonObject().put(StaticConfiguration.PROVIDER_NAME, StaticConfiguration.PROVIDER_FILE)));
        StaticConfiguration.validate(vertx, json).onComplete(testCtx.failingThenComplete());
    }

    @Test
    public void acceptValidDockerProvider(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.PROVIDERS, new JsonArray()
                .add(new JsonObject().put(StaticConfiguration.PROVIDER_NAME, StaticConfiguration.PROVIDER_DOCKER)));
        StaticConfiguration.validate(vertx, json).onComplete(testCtx.succeedingThenComplete());
    }

    @Test
    public void acceptValidProviders(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.PROVIDERS, new JsonArray()
                .add(new JsonObject().put(StaticConfiguration.PROVIDER_NAME, StaticConfiguration.PROVIDER_DOCKER))
                .add(new JsonObject().put(StaticConfiguration.PROVIDER_NAME, StaticConfiguration.PROVIDER_FILE)
                        .put(StaticConfiguration.PROVIDER_FILE_FILENAME, "somefile.json")));
        StaticConfiguration.validate(vertx, json).onComplete(testCtx.succeedingThenComplete());
    }

    @Test
    public void rejectEmptyProvider(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.PROVIDERS, new JsonArray().add(new JsonObject()));
        StaticConfiguration.validate(vertx, json).onComplete(testCtx.failingThenComplete());
    }

    @Test
    public void rejectUnkownProvider(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.PROVIDERS,
                new JsonArray().add(new JsonObject().put(StaticConfiguration.PROVIDER_NAME, "blub")));
        StaticConfiguration.validate(vertx, json).onComplete(testCtx.failingThenComplete());
    }

    @Test
    public void rejectProviderWithInvalidName(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.PROVIDERS,
                new JsonArray().add(new JsonObject().put(StaticConfiguration.PROVIDER_NAME, true)));
        StaticConfiguration.validate(vertx, json).onComplete(testCtx.failingThenComplete());
    }

    @Test
    public void rejectProviderWithUnknownKey(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.PROVIDERS,
                new JsonArray().add(new JsonObject().put("cosmos", "blub")));
        StaticConfiguration.validate(vertx, json).onComplete(testCtx.failingThenComplete());
    }

    @Test
    public void acceptApplicationWithPremiddlewares(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.ENTRYPOINTS,
                new JsonArray().add(new JsonObject()
                        .put(StaticConfiguration.ENTRYPOINT_NAME, "testEntrypoint")
                        .put(StaticConfiguration.ENTRYPOINT_PORT, 1234)
                        .put(DynamicConfiguration.MIDDLEWARES, new JsonArray()
                        )));
        StaticConfiguration.validate(vertx, json).onComplete(testCtx.succeedingThenComplete());
    }

    @Test
    public void acceptApplicationWithValidPremiddlewares(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.ENTRYPOINTS,
                new JsonArray().add(new JsonObject()
                        .put(StaticConfiguration.ENTRYPOINT_NAME, "testEntrypoint")
                        .put(StaticConfiguration.ENTRYPOINT_PORT, 1234)
                        .put(DynamicConfiguration.MIDDLEWARES, new JsonArray()
                                .add(new JsonObject()
                                        .put("name", "languagePremiddleware")
                                        .put("type", "languageCookie")
                                        .put("options", new JsonObject()))
                        )));
        StaticConfiguration.validate(vertx, json).onComplete(testCtx.succeedingThenComplete());
    }

    @Test
    public void acceptApplicationWithNonExistingPremiddlewares(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.ENTRYPOINTS,
                new JsonArray().add(new JsonObject()
                        .put(StaticConfiguration.ENTRYPOINT_NAME, "testEntrypoint")
                        .put(StaticConfiguration.ENTRYPOINT_PORT, 1234)
                        .put(DynamicConfiguration.MIDDLEWARES, new JsonArray()
                                .add(new JsonObject()
                                        .put("name", "languagePremiddleware")
                                        .put("type", "nonExistingMiddleware")
                                        .put("options", new JsonObject()))
                        )));
        StaticConfiguration.validate(vertx, json).onComplete(testCtx.failingThenComplete());
    }

    @Test
    public void acceptMultipleEntrypointsWithPremiddlewares(Vertx vertx, VertxTestContext testCtx) {
        JsonObject json = new JsonObject().put(StaticConfiguration.ENTRYPOINTS,
                new JsonArray().add(new JsonObject()
                        .put(StaticConfiguration.ENTRYPOINT_NAME, "testEntrypoint1")
                        .put(StaticConfiguration.ENTRYPOINT_PORT, 1234)
                        .put(DynamicConfiguration.MIDDLEWARES, new JsonArray()
                                .add(new JsonObject()
                                        .put("name", "languagePremiddleware")
                                        .put("type", "languageCookie")
                                        .put("options", new JsonObject()))
                        )).add(new JsonObject()
                        .put(StaticConfiguration.ENTRYPOINT_NAME, "testEntrypoint2")
                        .put(StaticConfiguration.ENTRYPOINT_PORT, 1235)
                        .put(DynamicConfiguration.MIDDLEWARES, new JsonArray()
                                .add(new JsonObject()
                                        .put("name", "languagePremiddleware")
                                        .put("type", "languageCookie")
                                        .put("options", new JsonObject()))
                        )));
        StaticConfiguration.validate(vertx, json).onComplete(testCtx.succeedingThenComplete());
    }

}
