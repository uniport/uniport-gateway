package com.inventage.portal.gateway.proxy.config.dynamic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.inventage.portal.gateway.TestUtils;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("unchecked")
@ExtendWith(VertxExtension.class)
public class DynamicConfigurationTest {

    static Stream<Arguments> validateTestData() {
        /*
        covered cases:
        - json object: null, empty, wrong type, required keys, optional keys, additional keys
        - json array: null, 0, 1, 2 (valid/valid, valid/invalid)
        */

        // root
        JsonObject nullConfig = null;

        JsonObject emptyConfig = new JsonObject();

        JsonObject unknownConfigKey = new JsonObject().put("blub", true);

        // http
        JsonObject nullHttp = new JsonObject().put(DynamicConfiguration.HTTP, null);

        JsonObject emptyHttp = new JsonObject().put(DynamicConfiguration.HTTP, new JsonObject());

        JsonObject unknownHttpKey = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put("blub", new JsonArray()));

        // http routers
        JsonObject nullHttpRouters = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.ROUTERS, null));

        JsonObject emptyHttpRouters = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.ROUTERS, new JsonArray()));

        JsonObject singleMinimalHttpRouter = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.ROUTERS,
                new JsonArray().add(new JsonObject()
                    .put(DynamicConfiguration.ROUTER_NAME, "foo")
                    .put(DynamicConfiguration.ROUTER_SERVICE, "bar"))));

        JsonObject singleCompleteHttpRouter = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.ROUTERS, new JsonArray().add(new JsonObject()
                .put(DynamicConfiguration.ROUTER_NAME, "foo")
                .put(DynamicConfiguration.ROUTER_SERVICE, "bar")
                .put(DynamicConfiguration.ROUTER_ENTRYPOINTS, new JsonArray())
                .put(DynamicConfiguration.ROUTER_MIDDLEWARES, new JsonArray())
                .put(DynamicConfiguration.ROUTER_RULE, "bla")
                .put(DynamicConfiguration.ROUTER_PRIORITY, 42))));

        JsonObject unkownKeyHttpRouter = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.ROUTERS,
                new JsonArray().add(new JsonObject()
                    .put(DynamicConfiguration.ROUTER_NAME, "foo")
                    .put(DynamicConfiguration.ROUTER_SERVICE, "bar")
                    .put("blub", "baz"))));

        JsonObject doubleMinimalHttpRouters = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.ROUTERS,
                new JsonArray()
                    .add(new JsonObject()
                        .put(DynamicConfiguration.ROUTER_NAME,
                            "foo")
                        .put(DynamicConfiguration.ROUTER_SERVICE,
                            "bar"))
                    .add(new JsonObject()
                        .put(DynamicConfiguration.ROUTER_NAME,
                            "blub")
                        .put(DynamicConfiguration.ROUTER_SERVICE,
                            "testing"))));

        JsonObject duplicatedRouter = new JsonObject().put(DynamicConfiguration.ROUTER_NAME, "foo")
            .put(DynamicConfiguration.ROUTER_SERVICE, "bar");
        JsonObject dublicatedMinimalHttpRouters = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject()
                .put(DynamicConfiguration.ROUTERS, new JsonArray().add(duplicatedRouter)
                    .add(duplicatedRouter)));

        JsonObject minimalAndEmptyHttpRouters = new JsonObject()
            .put(DynamicConfiguration.HTTP,
                new JsonObject().put(DynamicConfiguration.ROUTERS,
                    new JsonArray()
                        .add(new JsonObject().put(
                            DynamicConfiguration.ROUTER_NAME,
                            "foo")
                            .put(DynamicConfiguration.ROUTER_SERVICE,
                                "bar"))
                        .add(new JsonObject())));

        // http middlewares
        JsonObject nullHttpMiddlewares = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.MIDDLEWARES, null));

        JsonObject emptyHttpMiddlewares = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.MIDDLEWARES, new JsonArray()));

        JsonObject unkownHttpMiddleware = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.MIDDLEWARES,
                new JsonArray().add(new JsonObject()
                    .put(DynamicConfiguration.MIDDLEWARE_NAME, "foo")
                    .put(DynamicConfiguration.MIDDLEWARE_TYPE, "blub"))));

        JsonObject requestResponseLoggerHttpMiddleware = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.MIDDLEWARES,
                new JsonArray().add(new JsonObject()
                    .put(DynamicConfiguration.MIDDLEWARE_NAME, "foo")
                    .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                        DynamicConfiguration.MIDDLEWARE_REQUEST_RESPONSE_LOGGER)
                    .put(DynamicConfiguration.MIDDLEWARE_OPTIONS,
                        new JsonObject().put(
                            DynamicConfiguration.MIDDLEWARE_REQUEST_RESPONSE_LOGGER_FILTER_REGEX,
                            ".*/health.*|.*/ready.*")))));

        JsonObject requestResponseLoggerHttpMiddlewareMinimal = new JsonObject().put(
            DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.MIDDLEWARES,
                new JsonArray().add(new JsonObject()
                    .put(DynamicConfiguration.MIDDLEWARE_NAME, "foo")
                    .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                        DynamicConfiguration.MIDDLEWARE_REQUEST_RESPONSE_LOGGER))));

        JsonObject replacePathRegexHttpMiddleware = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.MIDDLEWARES,
                new JsonArray().add(new JsonObject()
                    .put(DynamicConfiguration.MIDDLEWARE_NAME, "foo")
                    .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                        DynamicConfiguration.MIDDLEWARE_REPLACE_PATH_REGEX)
                    .put(DynamicConfiguration.MIDDLEWARE_OPTIONS,
                        new JsonObject().put(
                            DynamicConfiguration.MIDDLEWARE_REPLACE_PATH_REGEX_REGEX,
                            "^$")
                            .put(DynamicConfiguration.MIDDLEWARE_REPLACE_PATH_REGEX_REPLACEMENT,
                                "foobar")))));

        JsonObject replacePathRegexHttpMiddlewareWithMissingOptions = new JsonObject().put(
            DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.MIDDLEWARES,
                new JsonArray().add(new JsonObject()
                    .put(DynamicConfiguration.MIDDLEWARE_NAME, "foo")
                    .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                        DynamicConfiguration.MIDDLEWARE_REPLACE_PATH_REGEX))));

        JsonObject directRegexHttpMiddleware = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(
                DynamicConfiguration.MIDDLEWARES,
                new JsonArray().add(new JsonObject()
                    .put(DynamicConfiguration.MIDDLEWARE_NAME, "foo")
                    .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                        DynamicConfiguration.MIDDLEWARE_REDIRECT_REGEX)
                    .put(DynamicConfiguration.MIDDLEWARE_OPTIONS,
                        new JsonObject().put(
                            DynamicConfiguration.MIDDLEWARE_REDIRECT_REGEX_REGEX,
                            "^$")
                            .put(DynamicConfiguration.MIDDLEWARE_REDIRECT_REGEX_REPLACEMENT,
                                "foorbar")))));

        JsonObject directRegexHttpMiddlewareWithMissingOptions = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.MIDDLEWARES,
                new JsonArray().add(new JsonObject()
                    .put(DynamicConfiguration.MIDDLEWARE_NAME, "foo")
                    .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                        DynamicConfiguration.MIDDLEWARE_REDIRECT_REGEX))));

        JsonObject headersHttpMiddlewareWithMissingOptions = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.MIDDLEWARES,
                new JsonArray().add(new JsonObject()
                    .put(DynamicConfiguration.MIDDLEWARE_NAME, "foo")
                    .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                        DynamicConfiguration.MIDDLEWARE_HEADERS))));

        JsonObject headersHttpMiddleware = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.MIDDLEWARES,
                new JsonArray().add(new JsonObject()
                    .put(DynamicConfiguration.MIDDLEWARE_NAME, "foo")
                    .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                        DynamicConfiguration.MIDDLEWARE_HEADERS)
                    .put(DynamicConfiguration.MIDDLEWARE_OPTIONS,
                        new JsonObject().put(
                            DynamicConfiguration.MIDDLEWARE_HEADERS_REQUEST,
                            new JsonObject().put(
                                "foo",
                                "bar"))))));

        JsonObject authBearerHttpMiddlewareWithMissingOptions = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.MIDDLEWARES,
                new JsonArray().add(new JsonObject()
                    .put(DynamicConfiguration.MIDDLEWARE_NAME, "foo")
                    .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                        DynamicConfiguration.MIDDLEWARE_AUTHORIZATION_BEARER))));

        JsonObject authBearerHttpMiddleware = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(
                DynamicConfiguration.MIDDLEWARES,
                new JsonArray().add(new JsonObject()
                    .put(DynamicConfiguration.MIDDLEWARE_NAME, "foo")
                    .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                        DynamicConfiguration.MIDDLEWARE_AUTHORIZATION_BEARER)
                    .put(DynamicConfiguration.MIDDLEWARE_OPTIONS,
                        new JsonObject()
                            .put(DynamicConfiguration.MIDDLEWARE_AUTHORIZATION_BEARER_SESSION_SCOPE,
                                "blub")))));

        JsonObject bearerOnlyHttpMiddlewareWithMissingOptions = TestUtils
            .buildConfiguration(TestUtils.withMiddlewares(TestUtils.withMiddleware("foo",
                DynamicConfiguration.MIDDLEWARE_BEARER_ONLY,
                TestUtils.withMiddlewareOpts(
                    new JsonObject().put(
                        DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_ISSUER,
                        "blub")))));

        JsonObject bearerOnlyHttpMiddlewareWithInvalidPublicKey = TestUtils.buildConfiguration(
            TestUtils.withMiddlewares(TestUtils.withMiddleware("foo",
                DynamicConfiguration.MIDDLEWARE_BEARER_ONLY,
                TestUtils.withMiddlewareOpts(new JsonObject()
                    .put(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEY,
                        "notbase64*oraurl")
                    .put(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEY_ALGORITHM,
                        "RS256")
                    .put(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_ISSUER,
                        "bar")
                    .put(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_AUDIENCE,
                        new JsonArray().add("blub"))))));

        JsonObject bearerOnlyHttpMiddlewareWithInvalidPublicKeyFormat = TestUtils
            .buildConfiguration(TestUtils
                .withMiddlewares(TestUtils.withMiddleware("foo",
                    DynamicConfiguration.MIDDLEWARE_BEARER_ONLY,
                    TestUtils.withMiddlewareOpts(new JsonObject()
                        .put(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEY,
                            "Ymx1Ygo=")
                        .put(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEY_ALGORITHM,
                            "")
                        .put(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_ISSUER,
                            "bar")
                        .put(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_AUDIENCE,
                            new JsonArray().add(
                                "blub"))))));

        JsonObject bearerOnlyHttpMiddlewareWithInvalidAudience = TestUtils
            .buildConfiguration(TestUtils
                .withMiddlewares(TestUtils.withMiddleware("foo",
                    DynamicConfiguration.MIDDLEWARE_BEARER_ONLY,
                    TestUtils.withMiddlewareOpts(new JsonObject()
                        .put(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEY,
                            "Ymx1Ygo=")
                        .put(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEY_ALGORITHM,
                            "RS256")
                        .put(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_ISSUER,
                            "bar")
                        .put(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_AUDIENCE,
                            new JsonArray().add(
                                "valid")
                                .add(123)
                                .add(true))))));

        JsonObject bearerOnlyHttpMiddleware = TestUtils
            .buildConfiguration(TestUtils
                .withMiddlewares(TestUtils.withMiddleware("foo",
                    DynamicConfiguration.MIDDLEWARE_BEARER_ONLY,
                    TestUtils.withMiddlewareOpts(new JsonObject()
                        .put(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEYS,
                            new JsonArray().add(
                                new JsonObject()
                                    .put(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEY,
                                        "Ymx1Ygo=")
                                    .put(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEY_ALGORITHM,
                                        "RS256")))
                        .put(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_ISSUER,
                            "bar")
                        .put(DynamicConfiguration.MIDDLEWARE_WITH_AUTH_HANDLER_AUDIENCE,
                            new JsonArray().add(
                                "blub"))))));

        JsonObject oauth2PathHttpMiddlewareWithMissingOptions = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.MIDDLEWARES,
                new JsonArray().add(new JsonObject()
                    .put(DynamicConfiguration.MIDDLEWARE_NAME, "foo")
                    .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                        DynamicConfiguration.MIDDLEWARE_OAUTH2))));

        JsonObject oauth2PathHttpMiddleware = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(
                DynamicConfiguration.MIDDLEWARES,
                new JsonArray().add(new JsonObject()
                    .put(DynamicConfiguration.MIDDLEWARE_NAME, "foo")
                    .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                        DynamicConfiguration.MIDDLEWARE_OAUTH2)
                    .put(DynamicConfiguration.MIDDLEWARE_OPTIONS,
                        new JsonObject().put(
                            DynamicConfiguration.MIDDLEWARE_OAUTH2_CLIENTID,
                            "foo")
                            .put(DynamicConfiguration.MIDDLEWARE_OAUTH2_CLIENTSECRET,
                                "bar")
                            .put(DynamicConfiguration.MIDDLEWARE_OAUTH2_DISCOVERYURL,
                                "localhost:1234")
                            .put(DynamicConfiguration.MIDDLEWARE_OAUTH2_SESSION_SCOPE,
                                "blub")))));

        JsonObject sessionBagHttpMiddlewareWithMissingOptions = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.MIDDLEWARES,
                new JsonArray().add(new JsonObject()
                    .put(DynamicConfiguration.MIDDLEWARE_NAME, "foo")
                    .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                        DynamicConfiguration.MIDDLEWARE_SESSION_BAG))));

        JsonObject sessionBagHttpMiddleware = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(
                DynamicConfiguration.MIDDLEWARES,
                new JsonArray().add(new JsonObject()
                    .put(DynamicConfiguration.MIDDLEWARE_NAME, "foo")
                    .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                        DynamicConfiguration.MIDDLEWARE_SESSION_BAG)
                    .put(DynamicConfiguration.MIDDLEWARE_OPTIONS,
                        new JsonObject().put(
                            DynamicConfiguration.MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIES,
                            new JsonArray().add(
                                new JsonObject()
                                    .put(DynamicConfiguration.MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIE_NAME,
                                        "foo")
                                    .put(DynamicConfiguration.MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIE_PATH,
                                        "/bar")))))));

        JsonObject unkownKeyHttpMiddleware = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(
                DynamicConfiguration.MIDDLEWARES,
                new JsonArray().add(new JsonObject()
                    .put(DynamicConfiguration.MIDDLEWARE_NAME, "foo")
                    .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                        DynamicConfiguration.MIDDLEWARE_AUTHORIZATION_BEARER)
                    .put(DynamicConfiguration.MIDDLEWARE_OPTIONS,
                        new JsonObject()
                            .put(DynamicConfiguration.MIDDLEWARE_AUTHORIZATION_BEARER_SESSION_SCOPE,
                                "blub"))
                    .put("blub", true))));

        JsonObject doubleHttpMiddlewares = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.MIDDLEWARES, new JsonArray()
                .add(new JsonObject().put(DynamicConfiguration.MIDDLEWARE_NAME, "foo")
                    .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                        DynamicConfiguration.MIDDLEWARE_HEADERS)
                    .put(DynamicConfiguration.MIDDLEWARE_OPTIONS,
                        new JsonObject().put(
                            DynamicConfiguration.MIDDLEWARE_HEADERS_REQUEST,
                            new JsonObject().put(
                                "foo",
                                "bar"))))
                .add(new JsonObject().put(DynamicConfiguration.MIDDLEWARE_NAME, "bar")
                    .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                        DynamicConfiguration.MIDDLEWARE_AUTHORIZATION_BEARER)
                    .put(DynamicConfiguration.MIDDLEWARE_OPTIONS,
                        new JsonObject().put(
                            DynamicConfiguration.MIDDLEWARE_AUTHORIZATION_BEARER_SESSION_SCOPE,
                            "blub")))));

        JsonObject duplicatedMiddleware = new JsonObject().put(DynamicConfiguration.MIDDLEWARE_NAME, "foo")
            .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                DynamicConfiguration.MIDDLEWARE_AUTHORIZATION_BEARER)
            .put(DynamicConfiguration.MIDDLEWARE_OPTIONS, new JsonObject()
                .put(DynamicConfiguration.MIDDLEWARE_AUTHORIZATION_BEARER_SESSION_SCOPE,
                    "blub"));
        JsonObject duplicatedHttpMiddlewares = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(
                DynamicConfiguration.MIDDLEWARES,
                new JsonArray().add(duplicatedMiddleware).add(duplicatedMiddleware)));

        JsonObject completeAndEmptyHttpMiddlewares = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.MIDDLEWARES, new JsonArray()
                .add(new JsonObject().put(DynamicConfiguration.MIDDLEWARE_NAME, "foo")
                    .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                        DynamicConfiguration.MIDDLEWARE_AUTHORIZATION_BEARER)
                    .put(DynamicConfiguration.MIDDLEWARE_OPTIONS,
                        new JsonObject().put(
                            DynamicConfiguration.MIDDLEWARE_AUTHORIZATION_BEARER_SESSION_SCOPE,
                            "blub")))
                .add(new JsonObject())));

        // http services
        JsonObject nullHttpServices = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.SERVICES, null));

        JsonObject emptyHttpServices = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.SERVICES, new JsonArray()));

        JsonObject singleHttpServiceWithNoServers = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.SERVICES,
                new JsonArray().add(new JsonObject()
                    .put(DynamicConfiguration.SERVICE_NAME, "foo")
                    .put(DynamicConfiguration.SERVICE_SERVERS, null))));

        JsonObject singleHttpServiceWithEmptyServers = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.SERVICES,
                new JsonArray().add(new JsonObject()
                    .put(DynamicConfiguration.SERVICE_NAME, "foo")
                    .put(DynamicConfiguration.SERVICE_SERVERS,
                        new JsonArray()))));

        JsonObject singleHttpServiceWithOneServer = new JsonObject()
            .put(DynamicConfiguration.HTTP,
                new JsonObject().put(DynamicConfiguration.SERVICES,
                    new JsonArray().add(new JsonObject().put(
                        DynamicConfiguration.SERVICE_NAME,
                        "foo").put(
                            DynamicConfiguration.SERVICE_SERVERS,
                            new JsonArray().add(
                                new JsonObject()
                                    .put(DynamicConfiguration.SERVICE_SERVER_HOST,
                                        "localhost")
                                    .put(DynamicConfiguration.SERVICE_SERVER_PORT,
                                        1234))))));

        JsonObject singleHttpServiceWithOneMissingKeyServer = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.SERVICES, new JsonArray().add(new JsonObject()
                .put(DynamicConfiguration.SERVICE_NAME, "foo")
                .put(DynamicConfiguration.SERVICE_SERVERS, new JsonArray()
                    .add(new JsonObject().put(
                        DynamicConfiguration.SERVICE_SERVER_HOST,
                        "localhost"))))));

        JsonObject singleHttpServiceWithOneUnkownKeyServer = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.SERVICES,
                new JsonArray().add(new JsonObject()
                    .put(DynamicConfiguration.SERVICE_NAME, "foo")
                    .put(DynamicConfiguration.SERVICE_SERVERS,
                        new JsonArray().add(new JsonObject()
                            .put(DynamicConfiguration.SERVICE_SERVER_HOST,
                                "localhost")
                            .put(DynamicConfiguration.SERVICE_SERVER_PORT,
                                1234)
                            .put("blub", true))))));

        JsonObject singleHttpServiceWithTwoServers = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.SERVICES, new JsonArray().add(new JsonObject()
                .put(DynamicConfiguration.SERVICE_NAME, "foo")
                .put(DynamicConfiguration.SERVICE_SERVERS, new JsonArray()
                    .add(new JsonObject().put(
                        DynamicConfiguration.SERVICE_SERVER_HOST,
                        "localhost")
                        .put(DynamicConfiguration.SERVICE_SERVER_PORT,
                            1234))
                    .add(new JsonObject().put(
                        DynamicConfiguration.SERVICE_SERVER_HOST,
                        "remotehost")
                        .put(DynamicConfiguration.SERVICE_SERVER_PORT,
                            5678))))));

        JsonObject unkownKeyHttpService = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.SERVICES,
                new JsonArray().add(new JsonObject()
                    .put(DynamicConfiguration.SERVICE_NAME, "foo")
                    .put(DynamicConfiguration.SERVICE_SERVERS,
                        new JsonArray().add(new JsonObject()
                            .put(DynamicConfiguration.SERVICE_SERVER_HOST,
                                "localhost")
                            .put(DynamicConfiguration.SERVICE_SERVER_PORT,
                                1234)))
                    .put("blub", true))));

        JsonObject doubleHttpServices = new JsonObject()
            .put(DynamicConfiguration.HTTP,
                new JsonObject()
                    .put(DynamicConfiguration.SERVICES,
                        new JsonArray()
                            .add(new JsonObject()
                                .put(DynamicConfiguration.SERVICE_NAME,
                                    "foo")
                                .put(
                                    DynamicConfiguration.SERVICE_SERVERS,
                                    new JsonArray().add(
                                        new JsonObject()
                                            .put(DynamicConfiguration.SERVICE_SERVER_HOST,
                                                "localhost")
                                            .put(DynamicConfiguration.SERVICE_SERVER_PORT,
                                                1234))))
                            .add(new JsonObject()
                                .put(DynamicConfiguration.SERVICE_NAME,
                                    "bar")
                                .put(
                                    DynamicConfiguration.SERVICE_SERVERS,
                                    new JsonArray().add(
                                        new JsonObject()
                                            .put(DynamicConfiguration.SERVICE_SERVER_HOST,
                                                "localhost")
                                            .put(DynamicConfiguration.SERVICE_SERVER_PORT,
                                                1234))))));

        JsonObject duplicatedService = new JsonObject().put(DynamicConfiguration.SERVICE_NAME, "foo").put(
            DynamicConfiguration.SERVICE_SERVERS,
            new JsonArray().add(new JsonObject()
                .put(DynamicConfiguration.SERVICE_SERVER_HOST, "localhost")
                .put(DynamicConfiguration.SERVICE_SERVER_PORT, 1234)));
        JsonObject duplicatedHttpServices = new JsonObject().put(DynamicConfiguration.HTTP, new JsonObject()
            .put(DynamicConfiguration.SERVICES,
                new JsonArray().add(duplicatedService).add(duplicatedService)));

        JsonObject completeAndEmptyHttpServices = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.SERVICES,
                new JsonArray()
                    .add(new JsonObject().put(
                        DynamicConfiguration.SERVICE_NAME,
                        "foo").put(
                            DynamicConfiguration.SERVICE_SERVERS,
                            new JsonArray().add(
                                new JsonObject()
                                    .put(DynamicConfiguration.SERVICE_SERVER_HOST,
                                        "localhost")
                                    .put(DynamicConfiguration.SERVICE_SERVER_PORT,
                                        1234))))
                    .add(new JsonObject())));

        JsonObject httpRouterWithMiddlewareAndService = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject()
                .put(DynamicConfiguration.ROUTERS,
                    new JsonArray().add(new JsonObject()
                        .put(DynamicConfiguration.ROUTER_NAME,
                            "routerFoo")
                        .put(DynamicConfiguration.MIDDLEWARES,
                            new JsonArray().add(
                                "middlewareFoo"))
                        .put(DynamicConfiguration.ROUTER_SERVICE,
                            "serviceFoo")))
                .put(DynamicConfiguration.MIDDLEWARES,
                    new JsonArray().add(new JsonObject()
                        .put(DynamicConfiguration.MIDDLEWARE_NAME,
                            "middlewareFoo")
                        .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                            DynamicConfiguration.MIDDLEWARE_AUTHORIZATION_BEARER)
                        .put(DynamicConfiguration.MIDDLEWARE_OPTIONS,
                            new JsonObject().put(
                                DynamicConfiguration.MIDDLEWARE_AUTHORIZATION_BEARER_SESSION_SCOPE,
                                "blub"))))
                .put(DynamicConfiguration.SERVICES,
                    new JsonArray()
                        .add(new JsonObject().put(
                            DynamicConfiguration.SERVICE_NAME,
                            "serviceFoo").put(
                                DynamicConfiguration.SERVICE_SERVERS,
                                new JsonArray().add(
                                    new JsonObject()
                                        .put(DynamicConfiguration.SERVICE_SERVER_HOST,
                                            "localhost")
                                        .put(DynamicConfiguration.SERVICE_SERVER_PORT,
                                            1234))))));

        JsonObject httpRouterWithMissingMiddleware = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject()
                .put(DynamicConfiguration.ROUTERS,
                    new JsonArray().add(new JsonObject()
                        .put(DynamicConfiguration.ROUTER_NAME,
                            "routerFoo")
                        .put(DynamicConfiguration.MIDDLEWARES,
                            new JsonArray().add(
                                "middlewareFoo"))
                        .put(DynamicConfiguration.ROUTER_SERVICE,
                            "serviceFoo")))
                .put(DynamicConfiguration.SERVICES,
                    new JsonArray()
                        .add(new JsonObject().put(
                            DynamicConfiguration.SERVICE_NAME,
                            "serviceFoo").put(
                                DynamicConfiguration.SERVICE_SERVERS,
                                new JsonArray().add(
                                    new JsonObject()
                                        .put(DynamicConfiguration.SERVICE_SERVER_HOST,
                                            "localhost")
                                        .put(DynamicConfiguration.SERVICE_SERVER_PORT,
                                            1234))))));

        JsonObject httpRouterWithMissingService = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject()
                .put(DynamicConfiguration.ROUTERS,
                    new JsonArray().add(new JsonObject()
                        .put(DynamicConfiguration.ROUTER_NAME,
                            "routerFoo")
                        .put(DynamicConfiguration.MIDDLEWARES,
                            new JsonArray().add(
                                "middlewareFoo"))
                        .put(DynamicConfiguration.ROUTER_SERVICE,
                            "serviceFoo")))
                .put(DynamicConfiguration.MIDDLEWARES,
                    new JsonArray().add(new JsonObject()
                        .put(DynamicConfiguration.MIDDLEWARE_NAME,
                            "middlewareFoo")
                        .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                            DynamicConfiguration.MIDDLEWARE_AUTHORIZATION_BEARER)
                        .put(DynamicConfiguration.MIDDLEWARE_OPTIONS,
                            new JsonObject().put(
                                DynamicConfiguration.MIDDLEWARE_AUTHORIZATION_BEARER_SESSION_SCOPE,
                                "blub")))));

        JsonObject controlApiMiddlewareWithSessionTermination = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject()
                .put(DynamicConfiguration.ROUTERS,
                    new JsonArray().add(new JsonObject()
                        .put(DynamicConfiguration.ROUTER_NAME,
                            "routerFoo")
                        .put(DynamicConfiguration.MIDDLEWARES,
                            new JsonArray().add(
                                "middlewareFoo"))
                        .put(DynamicConfiguration.ROUTER_SERVICE,
                            "serviceFoo")))
                .put(DynamicConfiguration.MIDDLEWARES,
                    new JsonArray().add(new JsonObject()
                        .put(DynamicConfiguration.MIDDLEWARE_NAME,
                            "middlewareFoo")
                        .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                            DynamicConfiguration.MIDDLEWARE_CONTROL_API)
                        .put(DynamicConfiguration.MIDDLEWARE_OPTIONS,
                            new JsonObject()
                                .put(DynamicConfiguration.MIDDLEWARE_CONTROL_API_ACTION,
                                    "SESSION_TERMINATE"))))
                .put(DynamicConfiguration.SERVICES,
                    new JsonArray()
                        .add(new JsonObject().put(
                            DynamicConfiguration.SERVICE_NAME,
                            "serviceFoo").put(
                                DynamicConfiguration.SERVICE_SERVERS,
                                new JsonArray().add(
                                    new JsonObject()
                                        .put(DynamicConfiguration.SERVICE_SERVER_HOST,
                                            "localhost")
                                        .put(DynamicConfiguration.SERVICE_SERVER_PORT,
                                            1234))))));

        JsonObject controlApiMiddlewareWithSessionReset = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject()
                .put(DynamicConfiguration.ROUTERS,
                    new JsonArray().add(new JsonObject()
                        .put(DynamicConfiguration.ROUTER_NAME,
                            "routerFoo")
                        .put(DynamicConfiguration.MIDDLEWARES,
                            new JsonArray().add(
                                "middlewareFoo"))
                        .put(DynamicConfiguration.ROUTER_SERVICE,
                            "serviceFoo")))
                .put(DynamicConfiguration.MIDDLEWARES,
                    new JsonArray().add(new JsonObject()
                        .put(DynamicConfiguration.MIDDLEWARE_NAME,
                            "middlewareFoo")
                        .put(DynamicConfiguration.MIDDLEWARE_TYPE,
                            DynamicConfiguration.MIDDLEWARE_CONTROL_API)
                        .put(DynamicConfiguration.MIDDLEWARE_OPTIONS,
                            new JsonObject()
                                .put(DynamicConfiguration.MIDDLEWARE_CONTROL_API_ACTION,
                                    "SESSION_RESET"))))
                .put(DynamicConfiguration.SERVICES,
                    new JsonArray()
                        .add(new JsonObject().put(
                            DynamicConfiguration.SERVICE_NAME,
                            "serviceFoo").put(
                                DynamicConfiguration.SERVICE_SERVERS,
                                new JsonArray().add(
                                    new JsonObject()
                                        .put(DynamicConfiguration.SERVICE_SERVER_HOST,
                                            "localhost")
                                        .put(DynamicConfiguration.SERVICE_SERVER_PORT,
                                            1234))))));

        JsonObject cspMiddlewareWithDefaultSrcSelf = TestUtils
            .buildConfiguration(TestUtils
                .withMiddlewares(TestUtils.withMiddleware("foo",
                    DynamicConfiguration.MIDDLEWARE_CSP,
                    TestUtils.withMiddlewareOpts(new JsonObject().put(
                        DynamicConfiguration.MIDDLEWARE_CSP_DIRECTIVES,
                        new JsonArray().add(
                            new JsonObject()
                                .put(DynamicConfiguration.MIDDLEWARE_CSP_DIRECTIVE_NAME,
                                    "default-src")
                                .put(DynamicConfiguration.MIDDLEWARE_CSP_DIRECTIVE_VALUES,
                                    new JsonArray().add(
                                        "self"))))))));

        JsonObject cspMiddlewareWithInvalidValues = TestUtils
            .buildConfiguration(TestUtils
                .withMiddlewares(TestUtils.withMiddleware("foo",
                    DynamicConfiguration.MIDDLEWARE_CSP,
                    TestUtils.withMiddlewareOpts(new JsonObject().put(
                        DynamicConfiguration.MIDDLEWARE_CSP_DIRECTIVES,
                        new JsonArray().add(new JsonObject()
                            .put(DynamicConfiguration.MIDDLEWARE_CSP_DIRECTIVE_NAME,
                                "foo")
                            .put(DynamicConfiguration.MIDDLEWARE_CSP_DIRECTIVE_VALUES,
                                new JsonArray().add(
                                    "valid")
                                    .add(123)
                                    .add(true))))))));

        final JsonObject cspViolationReportingServerMiddlewareWithLogLevelTrace = TestUtils
            .buildConfiguration(TestUtils
                .withMiddlewares(TestUtils.withMiddleware("foo",
                    DynamicConfiguration.MIDDLEWARE_CSP_VIOLATION_REPORTING_SERVER,
                    TestUtils.withMiddlewareOpts(new JsonObject().put(
                        DynamicConfiguration.MIDDLEWARE_CSP_VIOLATION_REPORTING_SERVER_LOG_LEVEL,
                        "TRACE")))));

        final JsonObject cspViolationReportingServerMiddlewareWithLogLevelWithWeirdCaps = TestUtils
            .buildConfiguration(TestUtils
                .withMiddlewares(TestUtils.withMiddleware("foo",
                    DynamicConfiguration.MIDDLEWARE_CSP_VIOLATION_REPORTING_SERVER,
                    TestUtils.withMiddlewareOpts(new JsonObject().put(
                        DynamicConfiguration.MIDDLEWARE_CSP_VIOLATION_REPORTING_SERVER_LOG_LEVEL,
                        "eRroR")))));

        final JsonObject cspViolationReportingServerMiddlewareWithInvalidValues = TestUtils
            .buildConfiguration(TestUtils
                .withMiddlewares(TestUtils.withMiddleware("foo",
                    DynamicConfiguration.MIDDLEWARE_CSP_VIOLATION_REPORTING_SERVER,
                    TestUtils.withMiddlewareOpts(new JsonObject().put(
                        DynamicConfiguration.MIDDLEWARE_CSP_VIOLATION_REPORTING_SERVER_LOG_LEVEL,
                        "blub")))));

        JsonObject sessionMiddleware = TestUtils.buildConfiguration(TestUtils.withMiddlewares(
            TestUtils.withMiddleware(
                "sessionMiddleware",
                DynamicConfiguration.MIDDLEWARE_SESSION,
                TestUtils.withMiddlewareOpts(
                    new JsonObject()
                        .put(DynamicConfiguration.MIDDLEWARE_SESSION_IDLE_TIMEOUT_IN_MINUTES, 15)
                        .put(DynamicConfiguration.MIDDLEWARE_SESSION_ID_MIN_LENGTH, 32)
                        .put(DynamicConfiguration.MIDDLEWARE_SESSION_NAG_HTTPS, true)
                        .put(DynamicConfiguration.MIDDLEWARE_SESSION_IGNORE_SESSION_TIMEOUT_RESET_FOR_URI, "^/polling/.*")
                        .put(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE, new JsonObject()
                            .put(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_NAME, "uniport.session")
                            .put(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_HTTP_ONLY, true)
                            .put(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_SECURE, false)
                            .put(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_SAME_SITE, "STRICT"))))));

        JsonObject sessionMiddlewareMinimal = TestUtils.buildConfiguration(TestUtils.withMiddlewares(
            TestUtils.withMiddleware(
                "sessionMiddleware",
                DynamicConfiguration.MIDDLEWARE_SESSION,
                TestUtils.withMiddlewareOpts(
                    new JsonObject()
                        .put(DynamicConfiguration.MIDDLEWARE_SESSION_IDLE_TIMEOUT_IN_MINUTES, 15)
                        .put(DynamicConfiguration.MIDDLEWARE_SESSION_ID_MIN_LENGTH, 32)
                        .put(DynamicConfiguration.MIDDLEWARE_SESSION_NAG_HTTPS, true)
                        .put(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE, new JsonObject()
                            .put(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_NAME, "uniport.session")
                            .put(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_HTTP_ONLY, true)
                            .put(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_SECURE, false)
                            .put(DynamicConfiguration.MIDDLEWARE_SESSION_COOKIE_SAME_SITE, "STRICT"))))));

        JsonObject openTelemetryMiddleware = TestUtils.buildConfiguration(TestUtils.withMiddlewares(
            TestUtils.withMiddleware("openTelemetry", DynamicConfiguration.MIDDLEWARE_OPEN_TELEMETRY)));

        JsonObject claimToHeaderMiddleware = TestUtils.buildConfiguration(TestUtils.withMiddlewares(
            TestUtils.withMiddleware("claimToHeader", DynamicConfiguration.MIDDLEWARE_CLAIM_TO_HEADER,
                TestUtils.withMiddlewareOpts(
                    new JsonObject()
                        .put(DynamicConfiguration.MIDDLEWARE_CLAIM_TO_HEADER_PATH, "claimPath")
                        .put(DynamicConfiguration.MIDDLEWARE_CLAIM_TO_HEADER_NAME, "headerName")))));

        // the sole purpose of the following variable are to improve readability
        boolean expectedTrue = true;
        boolean expectedFalse = false;

        // the default used in the following should be "complete" since it is more restrictive
        boolean complete = true;
        boolean incomplete = false;

        return Stream.of(
            // general
            Arguments.of("reject null config", nullConfig, complete, expectedFalse),
            Arguments.of("reject empty config", emptyConfig, complete, expectedFalse),
            Arguments.of("reject config with unknown key", unknownConfigKey, complete,
                expectedFalse),

            // http
            Arguments.of("reject null http config", nullHttp, complete, expectedFalse),
            Arguments.of("accept empty http", emptyHttp, complete, expectedTrue),
            Arguments.of("reject unkown http key", unknownHttpKey, complete, expectedFalse),

            // routers (incomplete)
            Arguments.of("reject null routers", nullHttpRouters, incomplete, expectedFalse),
            Arguments.of("accept empty routers", emptyHttpRouters, incomplete, expectedTrue),
            Arguments.of("accept single minimal router", singleMinimalHttpRouter, incomplete,
                expectedTrue),
            Arguments.of("accept single router with all properties", singleCompleteHttpRouter,
                incomplete,
                expectedTrue),
            Arguments.of("reject single router with unknown key", unkownKeyHttpRouter, incomplete,
                expectedFalse),
            Arguments.of("accept two minimal routers", doubleMinimalHttpRouters, incomplete,
                expectedTrue),
            Arguments.of("reject duplicated router", dublicatedMinimalHttpRouters, incomplete,
                expectedFalse),
            Arguments.of("reject minimal and empty routers", minimalAndEmptyHttpRouters, incomplete,
                expectedFalse),

            // middlewares
            Arguments.of("reject null middlewares", nullHttpMiddlewares, complete, expectedFalse),
            Arguments.of("accept empty middlewares", emptyHttpMiddlewares, complete, expectedTrue),
            Arguments.of("reject unkown middleware", unkownHttpMiddleware, complete, expectedFalse),
            // replace path regex middleware
            Arguments.of("accept replace path middleware", replacePathRegexHttpMiddleware, complete,
                expectedTrue),
            Arguments.of("reject replace path middleware with missing options",
                replacePathRegexHttpMiddlewareWithMissingOptions, complete,
                expectedFalse),
            // request response logger middleware
            Arguments.of("accept request response logger middleware",
                requestResponseLoggerHttpMiddleware, complete, expectedTrue),
            Arguments.of("accept minimal request response logger middleware",
                requestResponseLoggerHttpMiddlewareMinimal, complete, expectedTrue),
            // redirect regex middleware
            Arguments.of("accept redirect regex middleware", directRegexHttpMiddleware, complete,
                expectedTrue),
            Arguments.of("reject redirect regex middleware with missing options",
                directRegexHttpMiddlewareWithMissingOptions, complete, expectedFalse),
            // headers middleware
            Arguments.of("accept headers middleware", headersHttpMiddleware, complete,
                expectedTrue),
            Arguments.of("reject headers middleware with missing options",
                headersHttpMiddlewareWithMissingOptions,
                complete, expectedFalse),
            // authorization bearer middleware
            Arguments.of("accept authorization bearer middleware", authBearerHttpMiddleware,
                complete,
                expectedTrue),
            Arguments.of("reject authorization bearer with missing options",
                authBearerHttpMiddlewareWithMissingOptions, complete, expectedFalse),
            // bearer only middleware
            Arguments.of("accept bearer only middleware", bearerOnlyHttpMiddleware, complete,
                expectedTrue),
            Arguments.of("reject bearer only with missing options",
                bearerOnlyHttpMiddlewareWithMissingOptions,
                complete, expectedFalse),
            Arguments.of("reject bearer with invalid public key",
                bearerOnlyHttpMiddlewareWithInvalidPublicKey,
                complete, expectedFalse),
            Arguments.of("reject bearer with invalid public key format",
                bearerOnlyHttpMiddlewareWithInvalidPublicKeyFormat, complete,
                expectedFalse),
            Arguments.of("reject bearer with invalid audience",
                bearerOnlyHttpMiddlewareWithInvalidAudience,
                complete, expectedFalse),
            // oauth2 middleware
            Arguments.of("accept oAuth2 middleware", oauth2PathHttpMiddleware, complete,
                expectedTrue),
            Arguments.of("reject oAuth2 middleware with missing options",
                oauth2PathHttpMiddlewareWithMissingOptions, complete, expectedFalse),
            // session bag middleware
            Arguments.of("accept session bag middleware", sessionBagHttpMiddleware, complete,
                expectedTrue),
            Arguments.of("reject session bag middleware with missing options",
                sessionBagHttpMiddlewareWithMissingOptions, complete, expectedFalse),

            Arguments.of("reject unkown key middleware", unkownKeyHttpMiddleware, complete,
                expectedFalse),
            Arguments.of("accept two valid middelwares", doubleHttpMiddlewares, complete,
                expectedTrue),
            Arguments.of("reject duplicated middlewares", duplicatedHttpMiddlewares, complete,
                expectedFalse),
            Arguments.of("reject complete and empty middlewares", completeAndEmptyHttpMiddlewares,
                complete,
                expectedFalse),

            // controlapi middleware
            Arguments.of("accept control api with 'SESSION_TERMINATE' action middleware",
                controlApiMiddlewareWithSessionTermination, complete, expectedTrue),
            Arguments.of("accept control api with 'SESSION_RESET' action middleware",
                controlApiMiddlewareWithSessionReset, complete, expectedTrue),

            // content security policy middleware
            Arguments.of("accept csp middleware with default-src=[self]",
                cspMiddlewareWithDefaultSrcSelf,
                complete, expectedTrue),
            Arguments.of("reject csp middleware with invalid values",
                cspMiddlewareWithInvalidValues, complete,
                expectedFalse),

            // content security policy violation reporting server middleware
            Arguments.of("accept csp violation reporting middleware with log level",
                cspViolationReportingServerMiddlewareWithLogLevelTrace,
                complete, expectedTrue),
            Arguments.of("reject csp violation reporting middleware with log level with weird caps",
                cspViolationReportingServerMiddlewareWithLogLevelWithWeirdCaps,
                complete, expectedFalse),
            Arguments.of("reject csp violation reporting middleware with invalid log level",
                cspViolationReportingServerMiddlewareWithInvalidValues,
                complete, expectedFalse),

            // session middleware
            Arguments.of("accept session middleware",
                sessionMiddleware, complete, expectedTrue),
            Arguments.of("accept minimal session middleware",
                sessionMiddlewareMinimal, complete, expectedTrue),

            // openTelemetry middleware
            Arguments.of("accept openTelemetry middleware",
                openTelemetryMiddleware, complete, expectedTrue),

            // claimToHeader middleware
            Arguments.of("accept claimToHeader middleware",
                claimToHeaderMiddleware, complete, expectedTrue),

            // services
            Arguments.of("reject null services", nullHttpServices, complete, expectedFalse),
            Arguments.of("accept empty services", emptyHttpServices, complete, expectedTrue),
            Arguments.of("reject single service with no servers", singleHttpServiceWithNoServers,
                complete,
                expectedFalse),
            Arguments.of("reject single service with empty servers",
                singleHttpServiceWithEmptyServers, complete,
                expectedFalse),
            Arguments.of("accept single service with one valid server",
                singleHttpServiceWithOneServer, complete,
                expectedTrue),
            Arguments.of("reject single service with one server with a missing key",
                singleHttpServiceWithOneMissingKeyServer, complete, expectedFalse),
            Arguments.of("reject single service with one server with a unknown key",
                singleHttpServiceWithOneUnkownKeyServer, complete, expectedFalse),
            Arguments.of("accept single service with two valid servers",
                singleHttpServiceWithTwoServers, complete,
                expectedTrue),
            Arguments.of("reject single service with unkown key", unkownKeyHttpService, complete,
                expectedFalse),
            Arguments.of("accept two valid services", doubleHttpServices, complete, expectedTrue),
            Arguments.of("reject duplicated services", duplicatedHttpServices, complete,
                expectedFalse),
            Arguments.of("reject complete and empty services", completeAndEmptyHttpServices,
                complete,
                expectedFalse),

            // routers (complete)
            Arguments.of("accept http config with router referencing middleware and service",
                httpRouterWithMiddlewareAndService, complete, expectedTrue),
            Arguments.of("reject http config with router referencing missing middleware",
                httpRouterWithMissingMiddleware, complete, expectedFalse),
            Arguments.of("accept http config with router referencing missing service",
                httpRouterWithMissingService,
                complete, expectedFalse));

    }

    static Stream<Arguments> isEmptyConfigurationTestData() {

        JsonObject nullConfig = null;

        JsonObject emptyConfig = new JsonObject();

        JsonObject nullHttp = new JsonObject().put(DynamicConfiguration.HTTP, null);

        JsonObject emptyHttp = new JsonObject().put(DynamicConfiguration.HTTP, new JsonObject());

        JsonObject nullRouter = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.ROUTERS, null));
        JsonObject nullMiddleware = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.SERVICES, null));
        JsonObject nullService = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.MIDDLEWARES, null));
        JsonObject someRouters = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.ROUTERS, new JsonArray()));
        JsonObject someMiddlewares = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.SERVICES, new JsonArray()));
        JsonObject someServices = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.MIDDLEWARES, new JsonArray()));

        return Stream.of(Arguments.of("null config", nullConfig, true),
            Arguments.of("empty config", emptyConfig, true),
            Arguments.of("null http", nullHttp, true), Arguments.of("empty http", emptyHttp, true),
            Arguments.of("null router", nullRouter, true),
            Arguments.of("null middleware", nullMiddleware, true),
            Arguments.of("null service", nullService, true),
            Arguments.of("some router", someRouters, false),
            Arguments.of("some middleware", someMiddlewares, false),
            Arguments.of("some service", someServices, false));
    }

    static Stream<Arguments> mergeTestData() {
        JsonObject defaultConfig = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.ROUTERS, new JsonArray())
                .put(DynamicConfiguration.MIDDLEWARES, new JsonArray())
                .put(DynamicConfiguration.SERVICES, new JsonArray()));

        Map<String, JsonObject> nullConfig = null;
        Map<String, JsonObject> emptyConfig = new HashMap<String, JsonObject>(Map.ofEntries());

        JsonObject emptyRoutersServicesMiddlewares = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.ROUTERS, new JsonArray())
                .put(DynamicConfiguration.MIDDLEWARES, new JsonArray())
                .put(DynamicConfiguration.SERVICES, new JsonArray()));
        Map<String, JsonObject> emptyRoutersMiddlewaresServicesConfigs = new HashMap<String, JsonObject>(
            Map.ofEntries(
                new AbstractMap.SimpleEntry<String, JsonObject>("oneConfig",
                    emptyRoutersServicesMiddlewares),
                new AbstractMap.SimpleEntry<String, JsonObject>("anotherConfig",
                    emptyRoutersServicesMiddlewares)));

        JsonObject someConfig = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject()
                .put(DynamicConfiguration.ROUTERS,
                    new JsonArray()
                        .add(new JsonObject().put(
                            DynamicConfiguration.ROUTER_NAME,
                            "someRouter")))
                .put(DynamicConfiguration.MIDDLEWARES,
                    new JsonArray().add(
                        new JsonObject().put(
                            DynamicConfiguration.MIDDLEWARE_NAME,
                            "someMiddleware")))
                .put(DynamicConfiguration.SERVICES,
                    new JsonArray()
                        .add(new JsonObject().put(
                            DynamicConfiguration.SERVICE_NAME,
                            "someService")
                            .put(DynamicConfiguration.SERVICE_SERVERS,
                                new JsonArray()))));

        Map<String, JsonObject> distinctConfigs = new HashMap<String, JsonObject>(Map.ofEntries(
            new AbstractMap.SimpleEntry<String, JsonObject>("someConfig", someConfig),
            new AbstractMap.SimpleEntry<String, JsonObject>("anotherConfig",
                new JsonObject().put(DynamicConfiguration.HTTP, new JsonObject()
                    .put(DynamicConfiguration.ROUTERS,
                        new JsonArray().add(new JsonObject()
                            .put(DynamicConfiguration.ROUTER_NAME,
                                "anotherRouter")))
                    .put(DynamicConfiguration.MIDDLEWARES,
                        new JsonArray().add(new JsonObject()
                            .put(DynamicConfiguration.MIDDLEWARE_NAME,
                                "anotherMiddleware")))
                    .put(DynamicConfiguration.SERVICES,
                        new JsonArray().add(new JsonObject()
                            .put(DynamicConfiguration.SERVICE_NAME,
                                "anotherService")
                            .put(DynamicConfiguration.SERVICE_SERVERS,
                                new JsonArray())))))));

        JsonObject expectedMergedDistinctConfig = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject()
                .put(DynamicConfiguration.ROUTERS,
                    new JsonArray().add(new JsonObject().put(
                        DynamicConfiguration.ROUTER_NAME,
                        "someRouter"))
                        .add(new JsonObject().put(
                            DynamicConfiguration.ROUTER_NAME,
                            "anotherRouter")))
                .put(DynamicConfiguration.MIDDLEWARES,
                    new JsonArray()
                        .add(new JsonObject().put(
                            DynamicConfiguration.MIDDLEWARE_NAME,
                            "someMiddleware"))
                        .add(new JsonObject().put(
                            DynamicConfiguration.MIDDLEWARE_NAME,
                            "anotherMiddleware")))
                .put(DynamicConfiguration.SERVICES,
                    new JsonArray()
                        .add(new JsonObject().put(
                            DynamicConfiguration.SERVICE_NAME,
                            "someService")
                            .put(DynamicConfiguration.SERVICE_SERVERS,
                                new JsonArray()))
                        .add(new JsonObject().put(
                            DynamicConfiguration.SERVICE_NAME,
                            "anotherService")
                            .put(DynamicConfiguration.SERVICE_SERVERS,
                                new JsonArray()))));

        Map<String, JsonObject> overlappingConfigs = new HashMap<String, JsonObject>(
            Map.ofEntries(new AbstractMap.SimpleEntry<String, JsonObject>("someConfig", someConfig),
                new AbstractMap.SimpleEntry<String, JsonObject>("sameConfig",
                    someConfig)));
        JsonObject mergedOverlappingConfig = someConfig;

        return Stream.of(Arguments.of("null returns an empty configuration", nullConfig, defaultConfig),
            Arguments.of("empty returns an empty configuration", emptyConfig, defaultConfig),
            Arguments.of("empty routers, middlewares and services returns an empty configuration",
                emptyRoutersMiddlewaresServicesConfigs, defaultConfig),
            Arguments.of("distinct configs", distinctConfigs, expectedMergedDistinctConfig),
            Arguments.of("overlapping configs", overlappingConfigs, mergedOverlappingConfig));
    }

    static Stream<Arguments> getObjByKeyWithValueTestData() {
        JsonArray nullArray = null;
        JsonArray emptyArray = new JsonArray();

        String theHolyKey = "someKey";
        String theHolyValue = "someValue";

        JsonObject theHolyObject = new JsonObject().put(theHolyKey, theHolyValue).put("foo", "bar");
        JsonObject theUnwantedObject = new JsonObject().put(theHolyKey, "anotherValue").put("foo", "baz");
        JsonObject theUnwantedObjectJunior = new JsonObject().put(theHolyKey, "wrongValue").put("foo", "baz");

        JsonArray oneObjectWithMatch = new JsonArray().add(theHolyObject);
        JsonArray oneObjectWithoutMatch = new JsonArray().add(theUnwantedObject);
        JsonArray multipleObjectsWithMatch = new JsonArray().add(theHolyObject).add(theUnwantedObject);
        JsonArray multipleObjectsWithoutMatch = new JsonArray().add(theUnwantedObject)
            .add(theUnwantedObjectJunior);
        JsonArray stringArray = new JsonArray().add("blub").add("this").add("that");

        return Stream.of(Arguments.of("null array", nullArray, theHolyKey, theHolyValue, null),
            Arguments.of("empty array", emptyArray, theHolyKey, theHolyValue, null),
            Arguments.of("array with one element and it matches", oneObjectWithMatch, theHolyKey,
                theHolyValue,
                theHolyObject),
            Arguments.of("array with one element and it does not match", oneObjectWithoutMatch,
                theHolyKey,
                theHolyValue, null),
            Arguments.of("array with multiple elements and one matches", multipleObjectsWithMatch,
                theHolyKey,
                theHolyValue, theHolyObject),
            Arguments.of("array with multiple elements and none matches",
                multipleObjectsWithoutMatch, theHolyKey,
                theHolyValue, null),
            Arguments.of("array with multiple elements and no key match",
                multipleObjectsWithoutMatch, "blub",
                "nvm", null),
            Arguments.of("array with string items", stringArray, "hei", "hou", null));
    }

    @ParameterizedTest
    @MethodSource("validateTestData")
    void validateTest(
        String name, JsonObject json, Boolean complete, Boolean expected, Vertx vertx,
        VertxTestContext testCtx
    ) {

        DynamicConfiguration.validate(vertx, json, complete).onComplete(ar -> {
            if (ar.succeeded() && expected || ar.failed() && !expected) {
                testCtx.completeNow();
            } else {
                testCtx.failNow(String.format(
                    "'%s' was expected to have '%s'. Error: '%s', Input: '%s'", name,
                    expected ? "succeeded" : "failed", ar.cause(),
                    json != null ? json.encodePrettily() : null));
            }
        });
    }

    @Test
    void buildDefaultConfigurationTest(Vertx vertx, VertxTestContext testCtx) {
        JsonObject actualConfig = DynamicConfiguration.buildDefaultConfiguration();
        JsonObject expectedConfig = new JsonObject().put(DynamicConfiguration.HTTP,
            new JsonObject().put(DynamicConfiguration.ROUTERS, new JsonArray())
                .put(DynamicConfiguration.MIDDLEWARES, new JsonArray())
                .put(DynamicConfiguration.SERVICES, new JsonArray()));
        testCtx.verify(() -> assertEquals(expectedConfig, actualConfig));
        testCtx.completeNow();
    }

    @ParameterizedTest
    @MethodSource("isEmptyConfigurationTestData")
    void isEmptyConfigurationTest(
        String name, JsonObject json, boolean expected, Vertx vertx,
        VertxTestContext testCtx
    ) {
        String errMsg = String.format("'%s' was expected to have '%s'. Input: '%s'", name,
            expected ? "succeeded" : "failed", json != null ? json.encodePrettily() : json);
        testCtx.verify(() -> assertEquals(expected, DynamicConfiguration.isEmptyConfiguration(json), errMsg));
        testCtx.completeNow();
    }

    @ParameterizedTest
    @MethodSource("mergeTestData")
    void mergeTest(
        String name, Map<String, JsonObject> configurations, JsonObject expected, Vertx vertx,
        VertxTestContext testCtx
    ) {
        String errMsg = String.format("'%s' failed. Input: '%s'", name, configurations);

        Comparator<JsonObject> sortByName = new Comparator<JsonObject>() {

            @Override
            public int compare(JsonObject a, JsonObject b) {
                String nameA = a.getString(DynamicConfiguration.ROUTER_NAME);
                String nameB = b.getString(DynamicConfiguration.ROUTER_NAME);

                return nameA.compareTo(nameB);
            }

        };

        JsonObject actual = DynamicConfiguration.merge(configurations);
        testCtx.verify(() -> {
            assertNotNull(actual, errMsg);

            JsonObject expectedHttp = expected.getJsonObject(DynamicConfiguration.HTTP);
            JsonObject actualHttp = actual.getJsonObject(DynamicConfiguration.HTTP);
            assertNotNull(actualHttp, errMsg);

            JsonArray expectedRouters = expectedHttp.getJsonArray(DynamicConfiguration.ROUTERS);
            JsonArray actualRouters = actualHttp.getJsonArray(DynamicConfiguration.ROUTERS);
            Collections.sort((List<JsonObject>) expectedRouters.getList(), sortByName);
            Collections.sort((List<JsonObject>) actualRouters.getList(), sortByName);
            assertEquals(expectedRouters, actualRouters, errMsg);

            JsonArray expectedMiddlewares = expectedHttp.getJsonArray(DynamicConfiguration.MIDDLEWARES);
            JsonArray actualMiddlewares = actualHttp.getJsonArray(DynamicConfiguration.MIDDLEWARES);
            Collections.sort((List<JsonObject>) expectedMiddlewares.getList(), sortByName);
            Collections.sort((List<JsonObject>) actualMiddlewares.getList(), sortByName);
            assertEquals(expectedMiddlewares, actualMiddlewares, errMsg);

            JsonArray expectedServices = expectedHttp.getJsonArray(DynamicConfiguration.SERVICES);
            JsonArray actualServices = actualHttp.getJsonArray(DynamicConfiguration.SERVICES);
            Collections.sort((List<JsonObject>) expectedServices.getList(), sortByName);
            Collections.sort((List<JsonObject>) actualServices.getList(), sortByName);
            assertEquals(expectedServices, actualServices, errMsg);
        });
        testCtx.completeNow();
    }

    @ParameterizedTest
    @MethodSource("getObjByKeyWithValueTestData")
    void getObjByKeyWithValueTest(
        String name, JsonArray arr, String key, String value, JsonObject expected,
        Vertx vertx, VertxTestContext testCtx
    ) {
        String errMsg = String.format("'%s' failed. Array: '%s', Key: '%s', value: '%s'", name,
            arr != null ? arr.encodePrettily() : arr, key, value);
        testCtx.verify(
            () -> assertEquals(expected, DynamicConfiguration.getObjByKeyWithValue(arr, key, value),
                errMsg));
        testCtx.completeNow();
    }

}
