package com.inventage.portal.gateway.core.provider;

import io.vertx.core.Promise;

public interface Provider {

    public void provide(Promise<Void> startPromise);

}
