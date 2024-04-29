package com.inventage.portal.gateway.proxy.middleware.log;

import io.reactiverse.contextual.logging.ContextualData;

/**
 *
 */
public class ContextualDataAdapter {

    // CheckStyle: Utility classes should not have a public or default constructor.
    protected ContextualDataAdapter() {
        // prevents calls from subclass
        throw new UnsupportedOperationException();
    }

    /**
    */
    public static void put(String key, String value) {
        if (key != null && value != null) {
            ContextualData.put(key, value);
        }
    }
}
