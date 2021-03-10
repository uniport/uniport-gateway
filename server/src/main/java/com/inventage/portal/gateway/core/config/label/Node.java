package com.inventage.portal.gateway.core.config.label;

import java.util.Arrays;
import java.util.List;

public class Node {
    private String name;
    // private String fieldName;
    private String value;
    // private Object rawValue;
    // private Boolean disabled;
    // private reflect.Kind kind
    // private reflect.StructTag tag
    private List<Node> children;
    // kind and tag (paerser/node.go:19-20)

    public Node() {
    }

    public Node(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<Node> getChildren() {
        return this.children;
    }

    public void setChildren(List<Node> children) {
        this.children = children;
    }

    public void addChild(Node child) {
        if (this.children == null) {
            this.children = Arrays.asList(child);
        } else {
            this.children.add(child);
        }
    }

    public Node hasChild(String name) {
        if (this.children == null) {
            return null;
        }
        for (Node n : this.children) {
            if (name.equals(n.name)) {
                return n;
            }
        }
        return null;
    }

    public String toString() {
        String str = "";
        str += String.format("name: %s\n", this.name);
        str += String.format("value: %s\n", this.value);
        if (this.children != null) {
            str += "children: \n";
            for (Node child : this.children) {
                str += String.format("\t%s", child.toString());
            }
        }
        return str;
    }
}

// TODO other getters (maybe setters)
