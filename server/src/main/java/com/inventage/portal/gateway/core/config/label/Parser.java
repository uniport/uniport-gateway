package com.inventage.portal.gateway.core.config.label;

import java.util.*;

import io.vertx.core.json.JsonObject;

public class Parser {

    public final static String defaultRootName = "portal";

    public static JsonObject decode(Map<String, Object> labels, String rootName, List<String> filters) {
        JsonObject conf = new JsonObject();

        Node node = decodeToNode(labels, rootName, filters);

        // TODO apply node to conf

        return conf;
    }

    private static Node decodeToNode(Map<String, Object> labels, String rootName, List<String> filters) {
        List<String> sortedKeys = sortKeys(labels, filters);

        if (sortedKeys.isEmpty()) {
            return null;
        }

        Node node = new Node();
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

            decodeToNode(node, parts, (String) labels.get(key));
        }

        return node;
    }

    private static void decodeToNode(Node root, List<String> path, String value) {
        if (root.getName() == null) {
            root.setName(path.get(0));
        }

        // It is a leaf or has children
        if (path.size() > 1) {
            Node node = root.hasChild(path.get(1));
            if (node != null) {
                decodeToNode(node, path.subList(1, path.size()), value);
            } else {
                Node child = new Node();
                child.setName(path.get(1));
                decodeToNode(child, path.subList(1, path.size()), value);
                root.addChild(child);
            }
        } else {
            root.setValue(value);
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
