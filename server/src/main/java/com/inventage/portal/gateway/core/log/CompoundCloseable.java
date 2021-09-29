package com.inventage.portal.gateway.core.log;

import org.slf4j.MDC;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

public class CompoundCloseable implements Closeable {

    private List<MDC.MDCCloseable> closeables = new ArrayList<>();

    private CompoundCloseable() {
    }

    @Override
    public void close() {
        closeables.forEach(mdcCloseable -> {
            mdcCloseable.close();
        });
    }

    public static CompoundCloseable create(String key, String value) {
        CompoundCloseable instance = new CompoundCloseable();
        if (value != null) {
            instance.add(key, value);
        }
        return instance;
    }

    public CompoundCloseable add(String key, String value) {
        if (value != null) {
            closeables.add(MDC.putCloseable(key, value));
        }
        return this;
    }

}
