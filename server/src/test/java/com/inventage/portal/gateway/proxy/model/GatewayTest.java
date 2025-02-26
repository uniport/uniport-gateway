package com.inventage.portal.gateway.proxy.model;

import static com.inventage.portal.gateway.TestUtils.buildConfiguration;
import static com.inventage.portal.gateway.TestUtils.withMiddleware;
import static com.inventage.portal.gateway.TestUtils.withMiddlewareOpts;
import static com.inventage.portal.gateway.TestUtils.withMiddlewares;
import static com.inventage.portal.gateway.TestUtils.withRouter;
import static com.inventage.portal.gateway.TestUtils.withRouterEntrypoints;
import static com.inventage.portal.gateway.TestUtils.withRouterMiddlewares;
import static com.inventage.portal.gateway.TestUtils.withRouterRule;
import static com.inventage.portal.gateway.TestUtils.withRouterService;
import static com.inventage.portal.gateway.TestUtils.withRouters;
import static com.inventage.portal.gateway.TestUtils.withServer;
import static com.inventage.portal.gateway.TestUtils.withServerHttpOptions;
import static com.inventage.portal.gateway.TestUtils.withServers;
import static com.inventage.portal.gateway.TestUtils.withService;
import static com.inventage.portal.gateway.TestUtils.withServices;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GatewayTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayTest.class);

    @SuppressWarnings("unchecked")
    @Test
    public void parseJsonTest() {

        final JsonObject json = buildConfiguration(
            withRouters(
                withRouter("rA",
                    withRouterRule("Path('/')"),
                    withRouterEntrypoints("eA"),
                    withRouterMiddlewares("mwA"),
                    withRouterService("sA"))),
            withMiddlewares(
                withMiddleware("mwA", "headers",
                    withMiddlewareOpts(JsonObject.of(
                        "customRequestHeaders", JsonObject.of(
                            "X-Foo", "bar"))))),
            withServices(
                withService("sA",
                    withServers(
                        withServer("example.com", 1234,
                            withServerHttpOptions(false, true, "/abc", "1234"))))));
        System.out.println(json.encodePrettily());

        final JsonObject httpJson = json.getJsonObject(DynamicConfiguration.HTTP);
        final ObjectMapper codec = new ObjectMapper();
        Gateway gateway = null;
        try {
            gateway = codec.readValue(httpJson.encode(), Gateway.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            assertNull(e);
        }
        System.out.println(gateway.toString());
        assertEquals(httpJson, new JsonObject(gateway.toString()));
    }
}
