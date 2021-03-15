package com.inventage.portal.gateway.core.config.label;

import java.util.*;

import com.inventage.portal.gateway.core.config.dynamic.DynamicConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Parser {

    public static final String DEFAULT_ROOT_NAME = "portal";

    private static final Logger LOGGER = LoggerFactory.getLogger(Parser.class);

    // keywords used in the labels
    private static final String ROUTERS = "routers";
    private static final String MIDDLEWARES = "middlewares";
    private static final String SERVICES = "services";

    private static final List<String> CONTAINS_CUSTOM_NAMES =
            Arrays.asList(ROUTERS, MIDDLEWARES, SERVICES);
    private static final List<String> VALUE_IS_ARRAY_TYPE = Arrays.asList("entrypoints");

    public static JsonObject decode(Map<String, Object> labels, String rootName,
            List<String> filters) {
        return decodeToJson(labels, rootName, filters);
    }

    private static JsonObject decodeToJson(Map<String, Object> labels, String rootName,
            List<String> filters) {
        List<String> sortedKeys = sortKeys(labels, filters);

        if (sortedKeys.isEmpty()) {
            return null;
        }

        JsonObject node = DynamicConfiguration.buildDefaultConfiguration();
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
                            "invalid leading character '[' in field name (bracket is a slice delimiter): "
                                    + v);
                }

                if (v.equals(rootName)) {
                    continue;
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
        String key = path.get(0);
        if (path.size() > 1) {
            if (CONTAINS_CUSTOM_NAMES.contains(key)) {
                JsonArray children = root.getJsonArray(key);
                JsonObject child = DynamicConfiguration.getObjByKeyWithValue(children, getName(key),
                        path.get(1));
                if (child != null) {
                    decodeToJson(child, path.subList(2, path.size()), value);
                } else {
                    JsonObject newChild = new JsonObject();
                    newChild.put(getName(key), path.get(1));
                    decodeToJson(newChild, path.subList(2, path.size()), value);
                    children.add(newChild);
                    root.put(key, children);
                }
            } else {
                if (root.containsKey(key)) {
                    JsonObject child = root.getJsonObject(key);
                    decodeToJson(child, path.subList(1, path.size()), value);
                } else {
                    JsonObject newChild = new JsonObject();
                    decodeToJson(newChild, path.subList(1, path.size()), value);
                    root.put(key, newChild);
                }
            }
        } else {
            if (VALUE_IS_ARRAY_TYPE.contains(key)) {
                JsonArray values = root.getJsonArray(key);
                if (values == null) {
                    values = new JsonArray();
                }
                String[] split = value.split("\\,");
                for (String s : split) {
                    values.add(s.trim());
                }
                root.put(key, values);
            } else {
                if (root.containsKey(key)) {
                    LOGGER.warn(
                            "Found multiple values for the same setting. Overwriting '{}': '{}' with '{}'",
                            key, root.getString(key), value);
                }
                root.put(key, value);
            }
        }
    }

    private static String getName(String key) {
        switch (key) {
            case ROUTERS:
                return DynamicConfiguration.ROUTER_NAME;
            case MIDDLEWARES:
                return DynamicConfiguration.MIDDLEWARE_NAME;
            case SERVICES:
                return DynamicConfiguration.SERVICE_NAME;
            default:
                throw new IllegalArgumentException("Unknown type. Cannot find name: " + key);
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
