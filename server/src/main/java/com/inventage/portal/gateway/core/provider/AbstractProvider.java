package com.inventage.portal.gateway.core.provider;

import io.vertx.core.AbstractVerticle;

public abstract class AbstractProvider extends AbstractVerticle implements Provider {
    public static final String CONFIGURATION = "configuration";
    public static final String CONFIGURATION_ADDRESS = "configurationAddress";
}
