package com.inventage.portal.gateway.core.log;

import io.vertx.core.spi.logging.LogDelegate;
import io.vertx.core.spi.logging.LogDelegateFactory;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLoggerFactory;

public class PortalGatewayLogDelegateFactory implements LogDelegateFactory {

    @Override
    public boolean isAvailable() {
        // SLF might be available on the classpath but without configuration
        ILoggerFactory fact = LoggerFactory.getILoggerFactory();
        return !(fact instanceof NOPLoggerFactory);
    }

    @Override
    public LogDelegate createDelegate(String name) {
        return new PortalGatewayLogDelegate(name);
    }
}
