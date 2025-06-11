package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import static com.inventage.portal.gateway.TestUtils.buildConfiguration;
import static com.inventage.portal.gateway.TestUtils.withMiddleware;
import static com.inventage.portal.gateway.TestUtils.withMiddlewareOpts;
import static com.inventage.portal.gateway.TestUtils.withMiddlewares;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static io.vertx.core.http.HttpMethod.GET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.config.model.AbstractServiceModel;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareTestBase;
import com.inventage.portal.gateway.proxy.middleware.VertxAssertions;
import com.inventage.portal.gateway.proxy.service.ReverseProxy;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(VertxExtension.class)
public class SessionBagMiddlewareTest extends MiddlewareTestBase {

    static final String HOST = "localhost";
    static final String SESSION_COOKIE_NAME = "portal-gateway-test.session";

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {
        final JsonObject minimal = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", SessionBagMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        SessionBagMiddlewareFactory.WHITELISTED_COOKIES, JsonArray.of(
                            JsonObject.of(
                                SessionBagMiddlewareFactory.WHITELISTED_COOKIE_NAME, "foo",
                                SessionBagMiddlewareFactory.WHITELISTED_COOKIE_PATH, "/bar")))))));

        final JsonObject simple = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", SessionBagMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        SessionBagMiddlewareFactory.WHITELISTED_COOKIES, JsonArray.of(
                            JsonObject.of(
                                SessionBagMiddlewareFactory.WHITELISTED_COOKIE_NAME, "foo",
                                SessionBagMiddlewareFactory.WHITELISTED_COOKIE_PATH, "/bar"),
                            JsonObject.of(
                                SessionBagMiddlewareFactory.WHITELISTED_COOKIE_NAME, "blub",
                                SessionBagMiddlewareFactory.WHITELISTED_COOKIE_PATH, "/baz")))))));

        final JsonObject missingCookiePath = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", SessionBagMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        SessionBagMiddlewareFactory.WHITELISTED_COOKIES, JsonArray.of(
                            JsonObject.of(
                                SessionBagMiddlewareFactory.WHITELISTED_COOKIE_NAME, "foo")))))));

        final JsonObject missingRequiredProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", SessionBagMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        SessionBagMiddlewareFactory.SESSION_COOKIE_NAME, "blub")))));

        final JsonObject missingOptions = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", SessionBagMiddlewareFactory.TYPE)));

        final JsonObject unknownProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", SessionBagMiddlewareFactory.TYPE,
                    withMiddlewareOpts(
                        JsonObject.of("bar", "blub")))));

        return Stream.of(
            Arguments.of("accept minimal config", minimal, complete, expectedTrue),
            Arguments.of("accept simple config", simple, complete, expectedTrue),
            Arguments.of("reject config with missing cookie path", missingCookiePath, complete, expectedFalse),
            Arguments.of("reject config with no options", missingOptions, complete, expectedFalse),
            Arguments.of("reject config with missing required property", missingRequiredProperty, complete, expectedFalse),
            Arguments.of("reject config with unknown property", unknownProperty, complete, expectedFalse)

        );
    }

    @Test
    void cookiesAreRemovedInResponses(Vertx vertx, VertxTestContext testCtx) {
        final String errMsg = "'test removal of response cookies' failed.";
        final Cookie cookie = Cookie.cookie("blub-cookie", "foobar").setPath("/").setMaxAge(3600);
        final SessionStore sessionStore = LocalSessionStore.create(vertx);
        final AtomicBoolean isFirstReq = new AtomicBoolean(true);
        final AtomicReference<String> sessionId = new AtomicReference<>();

        testHarness(vertx, testCtx, sessionStore, List.of(), ctx -> {
            if (isFirstReq.get()) {
                ctx.response().addCookie(cookie);
                isFirstReq.set(false);
            }
            ctx.response().end();
        }, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, List.of(), resp,
                new ArrayList<Cookie>(Collections.singletonList(cookie)));
        }, null, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, List.of(), resp,
                new ArrayList<Cookie>(Collections.singletonList(cookie)));
        });
    }

    @Test
    void cookiesAreIncludedInFollowUpRequests(Vertx vertx, VertxTestContext testCtx) {
        final String errMsg = "'test cookies are included in follow up requests' failed.";
        final Cookie cookie = Cookie.cookie("blub-cookie", "foobar").setPath("/").setMaxAge(3600);
        final SessionStore sessionStore = LocalSessionStore.create(vertx);
        final AtomicBoolean isFirstReq = new AtomicBoolean(true);
        final AtomicReference<String> sessionId = new AtomicReference<>();

        testHarness(vertx, testCtx, sessionStore, List.of(), ctx -> {
            if (isFirstReq.get()) {
                ctx.response().addCookie(cookie);
                isFirstReq.set(false);
            } else {
                testCtx.verify(() -> {
                    assertFalse(ctx.request().cookies(cookie.getName()).isEmpty(),
                        String.format("%s: Expected cookies to be included in follow up requests but was '%s'",
                            errMsg, ctx.request().cookies()));
                });
            }
            ctx.response().end();
        }, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, List.of(), resp,
                new ArrayList<Cookie>(Collections.singletonList(cookie)));
        }, null, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, List.of(), resp,
                new ArrayList<Cookie>(Collections.singletonList(cookie)));
        });
    }

    @Test
    void laterReturnedCookiesAreSavedToo(Vertx vertx, VertxTestContext testCtx) {
        final String errMsg = "'test cookies included in follow up responses are saved too' failed.";
        final Cookie cookie = Cookie.cookie("blub-cookie", "foobar").setPath("/").setMaxAge(3600);
        final Cookie followUpCookie = Cookie.cookie("moose", "test").setPath("/").setMaxAge(3600);
        final SessionStore sessionStore = LocalSessionStore.create(vertx);
        final AtomicBoolean isFirstReq = new AtomicBoolean(true);
        final AtomicReference<String> sessionId = new AtomicReference<>();

        testHarness(vertx, testCtx, sessionStore, List.of(), ctx -> {
            if (isFirstReq.get()) {
                ctx.response().addCookie(cookie);
                isFirstReq.set(false);
            } else {
                ctx.response().addCookie(followUpCookie);
            }
            ctx.response().end();
        }, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, List.of(), resp,
                new ArrayList<Cookie>(Collections.singletonList(cookie)));
        }, null, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, List.of(), resp,
                new ArrayList<Cookie>(Arrays.asList(cookie, followUpCookie)));
        });
    }

    @Test
    void expiredCookieIsRemoved(Vertx vertx, VertxTestContext testCtx) {
        final String errMsg = "'test expired cookie are removed from session bag' failed.";
        final Cookie cookie = Cookie.cookie("blub", "foobar").setPath("/").setMaxAge(3600);
        final Cookie expiredCookie = Cookie.cookie("blub", "foobar").setPath("/").setMaxAge(0);
        final SessionStore sessionStore = LocalSessionStore.create(vertx);
        final AtomicBoolean isFirstReq = new AtomicBoolean(true);
        final AtomicReference<String> sessionId = new AtomicReference<>();

        testHarness(vertx, testCtx, sessionStore, List.of(), ctx -> {
            if (isFirstReq.get()) {
                ctx.response().addCookie(cookie);
                isFirstReq.set(false);
            } else {
                ctx.response().addCookie(expiredCookie);
            }
            ctx.response().end();
        }, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, List.of(), resp,
                new ArrayList<Cookie>(Collections.singletonList(cookie)));
        }, null, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, List.of(), resp, new ArrayList<Cookie>());
        });
    }

    @Test
    void keycloakMasterRealmCookieIsPassedInResponse(Vertx vertx, VertxTestContext testCtx) {
        final String errMsg = "'test keycloak master realm cookie is passed to user agent' failed";
        final Cookie masterRealmCookie = Cookie.cookie("KEYCLOAK_SESSION", "foobar").setPath("/auth/realms/master/").setMaxAge(3600);
        final Cookie portalRealmCookie = Cookie.cookie("KEYCLOAK_SESSION", "foobar").setPath("/auth/realms/portal/")
            .setMaxAge(3600);
        final List<WhitelistedCookieOptions> whitelistedCookies = List.of(
            WhitelistedCookieOptions.builder()
                .withName("KEYCLOAK_SESSION")
                .withPath("/auth/realms/master/")
                .build(),
            WhitelistedCookieOptions.builder()
                .withName("KEYCLOAK_SESSION")
                .withPath("/auth/realms/portal/")
                .build());
        final SessionStore sessionStore = LocalSessionStore.create(vertx);
        final AtomicBoolean isFirstReq = new AtomicBoolean(true);
        final AtomicReference<String> sessionId = new AtomicReference<>();

        testHarness(vertx, testCtx, sessionStore, whitelistedCookies, ctx -> {
            if (isFirstReq.get()) {
                ctx.response().addCookie(masterRealmCookie);
                ctx.response().addCookie(portalRealmCookie);
                isFirstReq.set(false);
            }
            ctx.response().end();
        }, resp -> {
            testCtx.verify(() -> {
                boolean foundMasterRealmCookie = false;
                for (String respCookie : resp.cookies()) {
                    final Cookie decodedRespCookie = CookieUtil.fromNettyCookie(ClientCookieDecoder.STRICT.decode(respCookie));
                    if (decodedRespCookie.getName().equals(masterRealmCookie.getName())
                        && decodedRespCookie.getPath().equals(masterRealmCookie.getPath())) {
                        foundMasterRealmCookie = true;
                        break;
                    }
                }
                assertTrue(foundMasterRealmCookie,
                    String.format("%s: Expected master realm cookie to be included in response but was '%s'",
                        errMsg, resp.cookies()));
            });
            // masterRealmCookie should not be saved in the session bag
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, whitelistedCookies, resp,
                new ArrayList<Cookie>(List.of()));
        }, null, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, whitelistedCookies, resp,
                new ArrayList<Cookie>(List.of()));
        });
    }

    @Test
    void storedCookieHasPrecedenceOverRequestCookie(Vertx vertx, VertxTestContext testCtx) {
        final String errMsg = "'test stored cookies have precedence over request cookies with the same name' failed";
        final Cookie storedCookie = Cookie.cookie("blub", "foo").setPath("/").setMaxAge(3600);
        final Cookie reqCookie = Cookie.cookie("blub", "bar").setPath("/").setMaxAge(3600);
        final SessionStore sessionStore = LocalSessionStore.create(vertx);
        final AtomicBoolean isFirstReq = new AtomicBoolean(true);
        final AtomicReference<String> sessionId = new AtomicReference<>();

        testHarness(vertx, testCtx, sessionStore, List.of(), ctx -> {
            if (isFirstReq.get()) {
                ctx.response().addCookie(storedCookie);
                isFirstReq.set(false);
            } else {
                testCtx.verify(() -> {
                    for (Cookie cookie : ctx.request().cookies()) {
                        if (cookie.getName().equals("blub")) {
                            final String cookieVal = cookie.getValue();
                            assertEquals("foo", cookieVal, String.format(
                                "%s: Expected cookie value to be '%s' but was '%s'", errMsg, "foo", cookieVal));
                        }
                    }
                });
            }
            ctx.response().end();
        }, resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, List.of(), resp,
                new ArrayList<Cookie>(Collections.singletonList(storedCookie)));
        }, new ArrayList<Cookie>(Collections.singletonList(reqCookie)), resp -> {
            expectedCookies(testCtx, errMsg, sessionStore, sessionId, List.of(), resp,
                new ArrayList<Cookie>(Collections.singletonList(storedCookie)));
        });

    }

    // https://inventage-all.atlassian.net/browse/PORTAL-2431
    @Test
    void propagateIncomingCookieToOutgoingRequest(Vertx vertx, VertxTestContext testCtx) {
        // given
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        final String cookieName = "cookie1";
        final String cookieValue = "value1";
        headers.add(HttpHeaders.COOKIE, cookieName + "=" + cookieValue);
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withRoutingContextHolder(routingContext)
            .withSessionMiddleware()
            .withSessionBagMiddleware(List.of(
                WhitelistedCookieOptions.builder().withName(cookieName).withPath("/").build()))
            .build()
            .start();
        // when
        gateway.incomingRequest(GET, "/secured", new RequestOptions().setHeaders(headers), (resp) -> {
            // then
            VertxAssertions.assertEquals(testCtx, true, routingContext.get().request().headers().contains(HttpHeaders.COOKIE),
                "outgoing request should contain cookie from incoming request");
            testCtx.completeNow();
        });
    }

    void testHarness(
        Vertx vertx, VertxTestContext testCtx, SessionStore sessionStore, List<WhitelistedCookieOptions> whitelistedCookies,
        Handler<RoutingContext> serverReqHandler, Handler<HttpClientResponse> respHandler,
        List<Cookie> followUpReqCookies, Handler<HttpClientResponse> followUpRespHandler
    ) {
        final int port = TestUtils.findFreePort();
        final int servicePort = TestUtils.findFreePort();

        final Checkpoint proxyStarted = testCtx.checkpoint();
        final Checkpoint serverStarted = testCtx.checkpoint();
        final Checkpoint requestServed = testCtx.checkpoint(2);
        final Checkpoint responseServed = testCtx.checkpoint();
        final Checkpoint followUpResponseServed = testCtx.checkpoint();

        // setup server
        final Router serverRouter = Router.router(vertx);
        serverRouter.route().handler(serverReqHandler);
        vertx.createHttpServer().requestHandler(req -> {
            serverRouter.handle(req);
        }).listen(servicePort).onComplete(testCtx.succeeding(s -> {
            serverStarted.flag();

            // setup proxy
            final SessionHandler sessionHandler = SessionHandler.create(sessionStore).setSessionCookieName(SESSION_COOKIE_NAME);
            final SessionBagMiddleware sessionBag = new SessionBagMiddleware("sessionBag", whitelistedCookies, "uniport.session");
            final ReverseProxy proxy = new ReverseProxy(vertx, "proxy",
                HOST, servicePort,
                AbstractServiceModel.DEFAULT_SERVICE_SERVER_PROTOCOL,
                AbstractServiceModel.DEFAULT_SERVICE_SERVER_HTTPS_OPTIONS_TRUST_ALL,
                AbstractServiceModel.DEFAULT_SERVICE_SERVER_HTTPS_OPTIONS_VERIFY_HOSTNAME,
                AbstractServiceModel.DEFAULT_SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PATH,
                AbstractServiceModel.DEFAULT_SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PASSWORD,
                AbstractServiceModel.DEFAULT_SERVICE_VERBOSE);

            final Router proxyRouter = Router.router(vertx);
            proxyRouter.route().handler(sessionHandler).handler(sessionBag).handler(proxy);

            vertx.createHttpServer().requestHandler(req -> {
                proxyRouter.handle(req);
                requestServed.flag();
            }).listen(port).onComplete(testCtx.succeeding(p -> {
                proxyStarted.flag();

                final HttpClient client = vertx.createHttpClient();
                // send first request
                client.request(new RequestOptions().setMethod(HttpMethod.GET).setPort(port).setHost(HOST))
                    .onComplete(testCtx.succeeding(req -> {
                        req.send().onComplete(testCtx.succeeding(resp -> {
                            respHandler.handle(resp);
                            responseServed.flag();

                            // send second request
                            final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
                            for (String respCookie : resp.cookies()) {
                                headers.add(HttpHeaders.COOKIE, respCookie);
                            }
                            if (followUpReqCookies != null) {
                                for (Cookie followUpReqCookie : followUpReqCookies) {
                                    headers.add(HttpHeaders.COOKIE, followUpReqCookie.encode());
                                }
                            }
                            client.request(new RequestOptions().setMethod(HttpMethod.GET).setPort(port)
                                .setHost(HOST).setURI("").setHeaders(headers))
                                .onComplete(testCtx.succeeding(followUpReq -> {
                                    followUpReq.send().onComplete(testCtx.succeeding(followUpResp -> {
                                        followUpRespHandler.handle(followUpResp);
                                        followUpResponseServed.flag();
                                    }));
                                }));
                        }));
                    }));
            }));
        }));
    }

    /**
     * Verifies that
     * a) a session cookie is returned to the user agent
     * b) no other cookies are returned to the user agent but the session cookie
     * c) all expectedSessionBagCookies are present in the session bag
     *
     * @param testCtx
     *            test context
     * @param errMsg
     *            error message
     * @param sessionStore
     *            contains stored cookies
     * @param sessionId
     *            session ID as returned in the session cookie
     * @param resp
     *            response returned to the user agent
     * @param expectedSessionBagCookies
     *            cookie expected to be stored in the session bag
     */
    void expectedCookies(
        VertxTestContext testCtx, String errMsg, SessionStore sessionStore,
        AtomicReference<String> sessionId, List<WhitelistedCookieOptions> whitelistedCookies, HttpClientResponse resp,
        List<Cookie> expectedSessionBagCookies
    ) {
        testCtx.verify(() -> {
            Cookie sessionCookie = null;
            for (String respCookie : resp.cookies()) {
                final Cookie decodedRespCookie = CookieUtil.fromNettyCookie(ClientCookieDecoder.STRICT.decode(respCookie));

                // only session cookie and whitelisted cookies are passed
                if (decodedRespCookie.getName().equals(SESSION_COOKIE_NAME)) {
                    sessionCookie = decodedRespCookie;
                    continue;
                }

                assertTrue(
                    whitelistedCookies.stream().anyMatch(cookie -> cookie.getName().equals(decodedRespCookie.getName()) && cookie.getPath().equals(decodedRespCookie.getPath())),
                    String.format("%s: Not whitelisted cookie was passed to user agent '%s'", errMsg, respCookie));
            }

            // extract session ID to inspect session content i.e. session bag
            if (sessionId.get() == null || sessionId.get().isEmpty()) {
                assertNotNull(sessionCookie, String.format("%s: No session cookie returned.", errMsg));
                sessionId.set(sessionCookie.getValue());
            }

            // check if cookies are stored in session
            sessionStore.get(sessionId.get()).onSuccess(session -> {
                assertNotNull(session, "Expected session to be present.");
                final Set<Cookie> actualSessionBagCookies = session.get(SessionBagMiddleware.SESSION_BAG_COOKIES);
                assertNotNull(actualSessionBagCookies, "Expected session bag to be present");
                final List<String> actualSessionBagCookiesStr = new ArrayList<String>();
                for (Cookie c : actualSessionBagCookies) {
                    actualSessionBagCookiesStr.add(c.encode());
                }
                for (Cookie cookie : expectedSessionBagCookies) {
                    assertTrue(actualSessionBagCookiesStr.contains(cookie.encode()), String
                        .format("%s: Expected cookie '%s' was not present in session", errMsg, cookie.encode()));
                }
            }).onFailure(err -> testCtx.failNow(err));
        });
    }
}
