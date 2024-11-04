package com.inventage.portal.gateway.proxy.provider.file.JsonDirectoryConfigStore;

import io.vertx.config.spi.ConfigStore;
import io.vertx.config.spi.utils.FileSet;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mostly copied from DirectoryConfigStore with a custom Json merge function
 * We only need to change the merge method in get(), but we cannot extend DirectoryConfigStore and overwrite get(), because the path and filesets members are
 * private
 * https://github.com/vert-x3/vertx-config/blob/4.5.8/vertx-config/src/main/java/io/vertx/config/impl/spi/DirectoryConfigStore.java#L95
 */
public class JsonDirectoryConfigStore implements ConfigStore {

    private final VertxInternal vertx;

    private final File path;
    private final List<FileSet> filesets = new ArrayList<>();

    public JsonDirectoryConfigStore(Vertx vertx, JsonObject configuration) {
        this.vertx = (VertxInternal) vertx;
        final String thePath = configuration.getString("path");
        if (thePath == null) {
            throw new IllegalArgumentException("The `path` configuration is required.");
        }
        this.path = new File(thePath);
        if (this.path.isFile()) {
            throw new IllegalArgumentException("The `path` must not be a file");
        }

        final JsonArray files = configuration.getJsonArray("filesets");
        if (files == null) {
            throw new IllegalArgumentException("The `filesets` element is required.");
        }

        for (Object o : files) {
            final JsonObject json = (JsonObject) o;
            final FileSet set = new FileSet(vertx, this.path, json);
            this.filesets.add(set);
        }
    }

    @Override
    public Future<Buffer> get() {
        return vertx.<List<File>>executeBlocking(
            () -> FileSet.traverse(path).stream()
                .sorted()
                .collect(Collectors.toList()))
            .flatMap(files -> {
                final List<Future<JsonObject>> futures = new ArrayList<>();
                for (FileSet set : filesets) {
                    final Promise<JsonObject> promise = vertx.promise();
                    set.buildConfiguration(files, json -> {
                        if (json.failed()) {
                            promise.fail(json.cause());
                        } else {
                            promise.complete(json.result());
                        }
                    });
                    futures.add(promise.future());
                }
                return Future.all(futures);
            }).map(compositeFuture -> {
                final JsonObject json = new JsonObject();
                compositeFuture.<JsonObject>list().forEach(config -> this.merge(json, config));
                return json.toBuffer();
            });
    }

    // Mostly copied from vertx JsonObject#mergeIn(JsonObject, int)
    // The difference is on how JsonArrays are merged. This implementation creates a deduplicated concatenation of two arrays.
    // https://github.com/eclipse-vertx/vert.x/blob/4.5.8/src/main/java/io/vertx/core/json/JsonObject.java#L1007-L1026
    private JsonObject merge(JsonObject one, JsonObject other) {
        for (Map.Entry<String, Object> e : other.getMap().entrySet()) {
            if (e.getValue() == null) {
                one.getMap().put(e.getKey(), null);
            } else {
                one.getMap().merge(e.getKey(), e.getValue(), (oldVal, newVal) -> {
                    if (oldVal instanceof Map) {
                        oldVal = new JsonObject((Map) oldVal);
                    }
                    if (newVal instanceof Map) {
                        newVal = new JsonObject((Map) newVal);
                    }
                    if (oldVal instanceof JsonObject && newVal instanceof JsonObject) {
                        return this.merge((JsonObject) oldVal, (JsonObject) newVal);
                    }

                    // custom part
                    if (oldVal instanceof List) {
                        oldVal = new JsonArray((List) oldVal);
                    }
                    if (newVal instanceof List) {
                        newVal = new JsonArray((List) newVal);
                    }
                    if (oldVal instanceof JsonArray && newVal instanceof JsonArray) {
                        final List newValues = ((JsonArray) newVal).getList();
                        final List oldValues = ((JsonArray) oldVal).getList();
                        for (int i = 0; i < oldValues.size(); i++) {
                            final Object item = oldValues.get(i);
                            if (!newValues.contains(item)) {
                                newValues.add(item);
                            }
                        }
                        return newVal;
                    }
                    return newVal;
                });
            }
        }
        return one;
    }

    @Override
    public Future<Void> close() {
        return vertx.getOrCreateContext().succeededFuture();
    }
}
