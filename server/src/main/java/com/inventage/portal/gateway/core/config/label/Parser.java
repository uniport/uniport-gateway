package com.inventage.portal.gateway.core.config.label;

import java.util.*;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Parser {

    public final static String DEFAULT_ROOT_NAME = "portal";

    public static JsonObject decode(Map<String, Object> labels, String rootName, List<String> filters) {
        return decodeToJson(labels, rootName, filters);
    }

    private static JsonObject decodeToJson(Map<String, Object> labels, String rootName, List<String> filters) {
        List<String> sortedKeys = sortKeys(labels, filters);

        if (sortedKeys.isEmpty()) {
            return null;
        }

        JsonObject node = new JsonObject();
        for (int i = 0; i < sortedKeys.size(); i++) {
            String key = sortedKeys.get(i);

            String[] split = key.split("\\.");

            if (!split[0].equals(rootName)) {
                throw new IllegalArgumentException("invalid label root " + split[0]);
            }

            List<String> parts = new ArrayList<>();
            for (String v : split) {
                if (v.equals("")) {
                    throw new IllegalArgumentException("invalid element " + key);
                }

                if (v.substring(0, 1).equals("[")) {
                    throw new IllegalArgumentException(
                            "invalid leading character '[' in field name (bracket is a slice delimiter): " + v);
                }

                if (v.endsWith("]") && v.substring(0, 1) != "[") {
                    int indexLeft = v.indexOf("[");
                    parts.add(v.substring(0, indexLeft));
                    parts.add(v.substring(indexLeft, v.length()));
                } else {
                    parts.add(v);
                }
            }

            decodeToJson(node, parts, (String) labels.get(key));
        }

        return node;
    }

    private static void decodeToJson(JsonObject root, List<String> path, String value) {
        if (!root.containsKey("name")) {
            root.put("name", path.get(0));
        }

        // It is a leaf or has children
        if (path.size() > 1) {
            if (root.containsKey(path.get(1))) {
                JsonObject node = root.getJsonObject(path.get(1));
                decodeToJson(node, path.subList(1, path.size()), value);
            } else {
                JsonObject child = new JsonObject();
                child.put("name", path.get(1));
                decodeToJson(child, path.subList(1, path.size()), value);

                if (!root.containsKey("children")) {
                    root.put("children", new JsonArray());
                }
                root.getJsonArray("children").add(child);
            }
        } else {
            root.put("value", value);
        }
    }

    private static List<String> sortKeys(Map<String, Object> labels, List<String> filters) {
        List<String> sortedKeys = new ArrayList<>();
        for (String key : labels.keySet()) {
            if (!(labels.get(key) instanceof String)) {
                continue;
            }
            if (filters.isEmpty()) {
                sortedKeys.add(key);
                continue;
            }

            for (String filter : filters) {
                if (key.startsWith(filter)) {
                    sortedKeys.add(key);
                    continue;
                }
            }
        }
        Collections.sort(sortedKeys);
        return sortedKeys;
    }
}
