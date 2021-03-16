package com.inventage.portal.gateway.proxy;

import io.vertx.core.json.JsonObject;

public interface Listener {
    public void listen(JsonObject config);
}
