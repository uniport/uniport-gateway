package com.inventage.portal.gateway.proxy.provider;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

public abstract class Provider extends AbstractVerticle {
    public static final String PROVIDER_CONFIGURATION = "configuration";
    public static final String PROVIDER_NAME = "name";

    public abstract void provide(Promise<Void> startPromise);
}
