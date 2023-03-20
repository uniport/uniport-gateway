package com.inventage.portal.gateway.proxy.config.label;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Labels are parsed to its JSON representation. Labels not starting with one of
 * specified filters
 * are ignored.
 */
public class Parser {

    public static final String DEFAULT_ROOT_NAME = "portal";

    private static final Logger LOGGER = LoggerFactory.getLogger(Parser.class);

    // keywords used in the labels
    private static final String ENTRYPOINTS = "entrypoints";
    private static final String ROUTERS = "routers";
    private static final String MIDDLEWARES = "middlewares";
    private static final String SERVICES = "services";
    private static final String SERVICE_SERVERS = "servers";

    private static final List<String> VALUES_ARE_OBJECTS_WITH_CUSTOM_NAMES = Arrays.asList(ROUTERS, MIDDLEWARES,
        SERVICES);
    private static final List<String> VALUES_ARE_OBJECTS = List.of(SERVICE_SERVERS);
    private static final List<String> VALUES_ARE_SEPERATED_BY_COMMA = Arrays.asList(ENTRYPOINTS, MIDDLEWARES);

    public static List<String> filterKeys(Map<String, Object> labels, List<String> filters) {
        final List<String> filteredKeys = new ArrayList<>();
        if (labels == null) {
            return filteredKeys;
        }
        for (Entry<String, Object> entry : labels.entrySet()) {
            final String key = entry.getKey();
            if (!(labels.get(key) instanceof String)) {
                continue;
            }
            if (filters == null || filters.isEmpty()) {
                filteredKeys.add(key);
                continue;
            }

            for (String filter : filters) {
                if (key.startsWith(filter)) {
                    filteredKeys.add(key);
                }
            }
        }
        return filteredKeys;
    }

    public static JsonObject decode(Map<String, Object> labels, String rootName, List<String> filters) {
        try {
            final JsonObject decodedConf = decodeToJson(labels, rootName, filters);
            return decodedConf;
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Decoding to JSON failed: '{}' '{}'", e.getMessage(),
                labels.toString());
        }
        return null;
    }

    private static JsonObject decodeToJson(Map<String, Object> labels, String rootName, List<String> filters) {
        final List<String> sortedKeys = filterKeys(labels, filters);
        Collections.sort(sortedKeys);

        if (sortedKeys.isEmpty()) {
            LOGGER.info("No matching labels");
            return null;
        }

        final JsonObject node = DynamicConfiguration.buildDefaultConfiguration();
        for (int i = 0; i < sortedKeys.size(); i++) {
            final String key = sortedKeys.get(i);
            final String[] split = key.split("\\.");

            if (!split[0].equals(rootName)) {
                throw new IllegalArgumentException("invalid root label: " + split[0]);
            }

            final List<String> parts = new ArrayList<>();
            for (String v : split) {
                if (v.equals("")) {
                    throw new IllegalArgumentException("invalid element " + key);
                }

                if (v.charAt(0) == '[') {
                    throw new IllegalArgumentException(
                        "invalid leading character '[' in field name (bracket is a slice delimiter): " + v);
                }

                if (v.equals(rootName)) {
                    continue;
                }

                if (v.endsWith("]") && v.charAt(0) != '[') {
                    final int indexLeft = v.indexOf("[");
                    parts.add(v.substring(0, indexLeft));
                    parts.add(v.substring(indexLeft));
                } else {
                    parts.add(v);
                }
            }

            decodeToJson(node, parts, (String) labels.get(key));
        }

        return node;
    }

    private static void decodeToJson(JsonObject root, List<String> path, String value) {
        final String key = path.get(0);
        if (path.size() > 1) {
            if (VALUES_ARE_OBJECTS_WITH_CUSTOM_NAMES.contains(key)) {
                final JsonArray children = root.getJsonArray(key);
                final JsonObject child = DynamicConfiguration.getObjByKeyWithValue(children, getName(key), path.get(1));
                if (child != null) {
                    decodeToJson(child, path.subList(2, path.size()), value);
                } else {
                    final JsonObject newChild = new JsonObject();
                    newChild.put(getName(key), path.get(1));
                    decodeToJson(newChild, path.subList(2, path.size()), value);
                    children.add(newChild);
                    root.put(key, children);
                }
            } else if (VALUES_ARE_OBJECTS.contains(key)) {
                // NOTE: later definitions overwrite previous ones since there is no id
                JsonArray children = root.getJsonArray(key);
                if (children == null) {
                    children = new JsonArray();
                    root.put(key, children);
                }
                final JsonObject child;
                if (children.size() < 1) {
                    child = new JsonObject();
                    children.add(child);
                } else {
                    child = children.getJsonObject(0);
                }
                decodeToJson(child, path.subList(1, path.size()), value);
            } else if (DynamicConfiguration.MIDDLEWARE_TYPES.contains(key)) {
                final JsonObject child;
                if (root.containsKey(DynamicConfiguration.MIDDLEWARE_OPTIONS)) {
                    child = root.getJsonObject(DynamicConfiguration.MIDDLEWARE_OPTIONS);
                } else {
                    child = new JsonObject();
                }
                decodeToJson(child, path.subList(1, path.size()), value);
                root.put(DynamicConfiguration.MIDDLEWARE_TYPE, key);
                root.put(DynamicConfiguration.MIDDLEWARE_OPTIONS, child);
            } else {
                if (root.containsKey(key)) {
                    final JsonObject child = root.getJsonObject(key);
                    decodeToJson(child, path.subList(1, path.size()), value);
                } else {
                    final JsonObject newChild = new JsonObject();
                    decodeToJson(newChild, path.subList(1, path.size()), value);
                    root.put(key, newChild);
                }
            }
        } else {
            if (VALUES_ARE_SEPERATED_BY_COMMA.contains(key)) {
                JsonArray values = root.getJsonArray(key);
                if (values == null) {
                    values = new JsonArray();
                }
                final String[] split = value.split("\\,");
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
                if (isInteger(value)) {
                    root.put(key, Integer.parseInt(value));
                } else if (isBoolean(value)) {
                    root.put(key, Boolean.parseBoolean(value));
                } else {
                    root.put(key, value);
                }
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

    private static boolean isInteger(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            Integer.parseInt(strNum);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    private static boolean isBoolean(String strBool) {
        if (strBool == null) {
            return false;
        }
        return "true".equalsIgnoreCase(strBool) || "false".equalsIgnoreCase(strBool);
    }
}
