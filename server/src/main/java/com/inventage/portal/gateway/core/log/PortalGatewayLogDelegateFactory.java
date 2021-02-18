package com.inventage.portal.gateway.core.log;

import io.vertx.core.spi.logging.LogDelegate;
import io.vertx.core.spi.logging.LogDelegateFactory;

public class PortalGatewayLogDelegateFactory implements LogDelegateFactory {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public LogDelegate createDelegate(String name) {
        return new PortalGatewayLogDelegate(name);
    }
}
