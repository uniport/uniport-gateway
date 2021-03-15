package com.inventage.portal.gateway.core.provider;

import io.vertx.core.AbstractVerticle;

public abstract class AbstractProvider extends AbstractVerticle implements Provider {
    public static final String PROVIDER_CONFIGURATION = "configuration";
    public static final String PROVIDER_NAME = "name";
}
