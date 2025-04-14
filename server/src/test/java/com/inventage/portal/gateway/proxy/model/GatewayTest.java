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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.headers.HeaderMiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.headers.HeaderMiddlewareOptions;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class GatewayTest {

    @Test
    public void parseJsonTest() {
        // given
        final String aRouterName = "rA";
        final String aRouterRule = "rA";
        final String aRouterEp = "rA";

        final String aMiddlewareName = "mA";
        final String aMiddlewareType = "headers";
        final String aHeaderName = "X-Foo";
        final String aHeaderValue = "bar";
        final JsonObject aMiddlewareOpts = JsonObject.of(
            HeaderMiddlewareFactory.HEADERS_REQUEST, JsonObject.of(
                aHeaderName, aHeaderValue));

        final String aServiceName = "sA";
        final String aHost = "example.com";
        final int aPort = 1234;
        final boolean aVerify = true;
        final boolean aTrust = false;
        final String aPath = "/path";
        final String aPassword = "password";

        @SuppressWarnings("unchecked")
        final JsonObject json = buildConfiguration(
            withRouters(
                withRouter(aRouterName,
                    withRouterRule(aRouterRule),
                    withRouterEntrypoints(aRouterEp),
                    withRouterMiddlewares(aMiddlewareName),
                    withRouterService(aServiceName))),
            withMiddlewares(
                withMiddleware(aMiddlewareName, aMiddlewareType,
                    withMiddlewareOpts(aMiddlewareOpts))),
            withServices(
                withService(aServiceName,
                    withServers(
                        withServer(aHost, aPort,
                            withServerHttpOptions(aVerify, aTrust, aPath, aPassword))))));

        // when
        final JsonObject httpJson = json.getJsonObject(DynamicConfiguration.HTTP);
        final ThrowingSupplier<Gateway> parse = () -> new ObjectMapper().readValue(httpJson.encode(), Gateway.class);

        // then
        final Gateway gateway = assertDoesNotThrow(parse);

        assertNotNull(gateway.getRouters());
        final GatewayRouter router = gateway.getRouters().get(0);
        assertNotNull(router);
        assertEquals(aRouterName, router.getName());

        assertNotNull(router.getEntrypoints());
        assertEquals(aRouterEp, router.getEntrypoints().get(0));
        assertEquals(aRouterRule, router.getRule());

        assertNotNull(router.getMiddlewares());
        assertEquals(aMiddlewareName, router.getMiddlewares().get(0));
        assertEquals(aServiceName, router.getService());

        assertNotNull(gateway.getMiddlewares());
        final GatewayMiddleware middleware = gateway.getMiddlewares().get(0);
        assertNotNull(middleware);
        assertEquals(aMiddlewareName, middleware.getName());
        assertEquals(aMiddlewareType, middleware.getType());

        final GatewayMiddlewareOptions options = middleware.getOptions();
        assertNotNull(options);
        assertTrue(options instanceof HeaderMiddlewareOptions);
        final HeaderMiddlewareOptions headerOptions = (HeaderMiddlewareOptions) options;
        assertNotNull(headerOptions.getRequestHeaders());
        assertTrue(headerOptions.getRequestHeaders().containsKey(aHeaderName));
        assertEquals(aHeaderValue, headerOptions.getRequestHeaders().get(aHeaderName));
        assertNotNull(headerOptions.getResponseHeaders());
        assertTrue(headerOptions.getResponseHeaders().isEmpty());

        assertNotNull(gateway.getServices());
        final GatewayService service = gateway.getServices().get(0);
        assertNotNull(service);
        assertEquals(aServiceName, service.getName());

        assertNotNull(service.getServers());
        final ServerOptions server = service.getServers().get(0);
        assertNotNull(server);
        assertEquals(aHost, server.getHost());
        assertEquals(aPort, server.getPort());

        final HTTPsOptions httpsOptions = server.getHTTPs();
        assertNotNull(httpsOptions);
        assertEquals(aVerify, httpsOptions.verifyHostname());
        assertEquals(aTrust, httpsOptions.trustAll());
        assertEquals(aPath, httpsOptions.getTrustStorePath());
        assertEquals(aPassword, httpsOptions.getTrustStorePassword());
    }

    @Test
    public void allowNullOptions() {
        // given 
        final String aMiddlewareName = "mA";
        final String aMiddlewareType = "responseSessionCookieRemoval"; // allows no options in configuration

        @SuppressWarnings("unchecked")
        final JsonObject json = buildConfiguration(
            withMiddlewares(
                withMiddleware(aMiddlewareName, aMiddlewareType)));

        // when
        final JsonObject httpJson = json.getJsonObject(DynamicConfiguration.HTTP);
        final ThrowingSupplier<Gateway> parse = () -> new ObjectMapper().readValue(httpJson.encode(), Gateway.class);

        // then
        final Gateway gateway = assertDoesNotThrow(parse);

        assertNotNull(gateway.getMiddlewares());
        final GatewayMiddleware middleware = gateway.getMiddlewares().get(0);
        assertNotNull(middleware);
        assertEquals(aMiddlewareName, middleware.getName());
        assertEquals(aMiddlewareType, middleware.getType());

        final GatewayMiddlewareOptions options = middleware.getOptions();
        assertNotNull(options);
    }
}
