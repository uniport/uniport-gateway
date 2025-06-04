package com.inventage.portal.gateway.core;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

// https://vertx.io/docs/vertx-junit5/java/#_integration_with_junit_5
@ExtendWith(VertxExtension.class)
public class PortalGatewayVerticleTest {

    @BeforeEach
    void deployVerticle(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(new PortalGatewayVerticle(), testContext.succeeding(id -> testContext.completeNow()));
    }

    @Test
    void verticleDeployed(Vertx vertx, VertxTestContext testContext) {
        testContext.completeNow();
    }
}
