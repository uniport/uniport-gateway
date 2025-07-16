package ch.uniport.gateway.proxy.middleware.log;

import io.reactiverse.contextual.logging.ContextualData;

/**
 *
 */
public final class ContextualDataAdapter {

    private ContextualDataAdapter() {
    }

    public static void put(String key, String value) {
        if (key != null && value != null) {
            ContextualData.put(key, value);
        }
    }
}
