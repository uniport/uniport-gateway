package com.inventage.portal.gateway.proxy.listener;

import io.vertx.core.json.JsonObject;

/**
 * To receive announcements about dynamic configuration changes one has to implement this interface.
 */
public interface Listener {
    void listen(JsonObject config);
}
