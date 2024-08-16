package com.inventage.portal.gateway.proxy.middleware.sessionLogoutFromBackchannel;

import static com.inventage.portal.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddleware.SINGLE_SIGN_ON_SID;
import static com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;
import static com.inventage.portal.gateway.proxy.middleware.sessionLogoutFromBackchannel.BackChannelLogoutMiddleware.SHARED_DATA_KEY_SESSIONS;
import static com.inventage.portal.gateway.proxy.middleware.sessionLogoutFromBackchannel.BackChannelLogoutMiddlewareFactory.DEFAULT_SESSION_BACKCHANNELLOGOUT_PATH;
import static io.vertx.core.http.HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.ext.web.sstore.LocalSessionStore.DEFAULT_SESSION_MAP_NAME;

import com.inventage.portal.gateway.proxy.middleware.BrowserConnected;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.sstore.impl.SharedDataSessionImpl;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class BackChannelLogoutMiddlewareTest {

    final String sid1 = "d452ed42-33ec-4a59-b110-0aa0280101bc"; // contained in logoutToken1
    final String logoutToken1 = "logout_token=eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ1Y0p4dWNXRDFWZnFSNU56QmtKZng2RnNZYmJHeEcxOHk5bVZrazFYYWJZIn0.eyJleHAiOjE3MjMyMDcyNDAsImlhdCI6MTcyMzIwNzEyMCwianRpIjoiNTBiNjQyYTItZWNkOS00Nzc2LTllZjUtYjNmYjdiMWMzMTM5IiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDoyMDAwMC9hdXRoL3JlYWxtcy9wb3J0YWwiLCJhdWQiOiJQb3J0YWwtR2F0ZXdheSIsInN1YiI6ImY6OTI0MDg0ZmUtNTA5MS00MjhhLThlM2YtMWZhNTU0MmM1YjY5OjgzIiwidHlwIjoiTG9nb3V0Iiwic2lkIjoiZDQ1MmVkNDItMzNlYy00YTU5LWIxMTAtMGFhMDI4MDEwMWJjIiwiZXZlbnRzIjp7Imh0dHA6Ly9zY2hlbWFzLm9wZW5pZC5uZXQvZXZlbnQvYmFja2NoYW5uZWwtbG9nb3V0Ijp7fX19.YTGdd85jH-7LwiwkmcYuLBam8ERLCYtxXEpunTZI97XxTydiY23gtfPlXJnpCcnsSz-BtxeQWaoHZdPtZ3Wik2W4oj97Hn-Cqaz2w8FPp1DU-BULC8nFhNAOqLvQurI8lTyAl7n8hGYnbtcviT21tN_wuJkZvutPQJMLzTnrWFU96noLFaFuT_7c-DtRn0SdvauwDwYGLaI4nGudE57Sbsx7hiXdnKp0gSivm_Jx8XPsGF6_s54cdz4tT3erx4XXWrp-ENdEpXIUxF577jSqeYDTH2YYtcRYK-pg7Ojbdj3zBjfnw8SQrj4IKGagLJ24KD8XGkD2kfhHBbULxCVWlg";

    @Test
    public void backChannelLogoutRequest(Vertx vertx, VertxTestContext testCtx) {
        // given
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withBackChannelLogoutMiddleware()
            .build().start();

        // when
        final RequestOptions requestOptions = new RequestOptions().addHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED);
        gateway.incomingRequest(POST, DEFAULT_SESSION_BACKCHANNELLOGOUT_PATH, requestOptions, logoutToken1, (outgoingResponse) -> {
            // then
            assertThat(outgoingResponse)
                .hasStatusCode(200);
            Assertions.assertTrue(vertx.sharedData().getLocalMap(SHARED_DATA_KEY_SESSIONS).containsKey(sid1));
            testCtx.completeNow();
        });
    }

    @Test
    public void backChannelLogoutRequest_withoutLogoutToken(Vertx vertx, VertxTestContext testCtx) {
        // given
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withBackChannelLogoutMiddleware()
            .build().start();

        // when
        final RequestOptions requestOptions = new RequestOptions().addHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED);
        gateway.incomingRequest(POST, DEFAULT_SESSION_BACKCHANNELLOGOUT_PATH, requestOptions, "a=b", (outgoingResponse) -> {
            // then
            assertThat(outgoingResponse)
                .hasStatusCode(200);
            testCtx.completeNow();
        });
    }

    @Test
    public void backChannelLogoutRequest_invalidRequestMethod(Vertx vertx, VertxTestContext testCtx) {
        // given
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withBackChannelLogoutMiddleware()
            .build().start();

        // when
        final RequestOptions requestOptions = new RequestOptions().addHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED);
        gateway.incomingRequest(GET, DEFAULT_SESSION_BACKCHANNELLOGOUT_PATH, requestOptions, "", (outgoingResponse) -> {
            // then
            assertThat(outgoingResponse)
                .hasStatusCode(400);
            testCtx.completeNow();
        });
    }

    @Test
    public void backChannelLogoutRequest_invalidRequestContentType(Vertx vertx, VertxTestContext testCtx) {
        // given
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withBackChannelLogoutMiddleware()
            .build().start();

        // when
        final RequestOptions requestOptions = new RequestOptions().addHeader(HttpHeaders.CONTENT_TYPE, "text/plain");
        gateway.incomingRequest(POST, DEFAULT_SESSION_BACKCHANNELLOGOUT_PATH, requestOptions, "", (outgoingResponse) -> {
            // then
            assertThat(outgoingResponse)
                .hasStatusCode(400);
            testCtx.completeNow();
        });
    }

    @Test
    public void requestWithTerminatedSession(Vertx vertx, VertxTestContext testCtx) {
        // given
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withBackChannelLogoutMiddleware()
            .build().start();
        BrowserConnected browser = gateway.connectBrowser();
        HeadersMultiMap headersMultiMap = new HeadersMultiMap();

        final List<String> sessionId = new ArrayList<>();
        final List<String> cookie = new ArrayList<>();
        // when
        browser.request(GET, "/request1")
            .whenComplete((response, error) -> {
                cookie.add(response.cookies().get(0));
                assertThat(response)
                    .hasStatusCode(200)
                    .hasSetSessionCookie();
                vertx.getOrCreateContext();
                // newly created session
                SharedDataSessionImpl sharedDataSession = getSharedDataSession(vertx);
                sessionId.add(sharedDataSession.id());
                sharedDataSession.put(SINGLE_SIGN_ON_SID, sid1);
            })
            .thenCompose(response -> {
                // back channel logout request as triggered by Keycloak
                HeadersMultiMap headersMultiMap1 = new HeadersMultiMap();
                headersMultiMap1.add(HttpHeaders.CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED);
                return browser.request(POST, DEFAULT_SESSION_BACKCHANNELLOGOUT_PATH, headersMultiMap1, logoutToken1);
            })
            .whenComplete((response, error) -> {
                headersMultiMap.add("cookie", DEFAULT_SESSION_COOKIE_NAME + "=" + sessionId.get(0));
                browser.request(GET, "/request1", headersMultiMap)
                    .whenComplete((response2, error2) -> {
                        // then
                        assertThat(response2)
                            .hasStatusCode(200)
                            .hasSetSessionCookieDifferentThan(cookie.get(0));
                        testCtx.completeNow();
                    });
            });
    }

    private SharedDataSessionImpl getSharedDataSession(Vertx vertx) {
        return vertx.sharedData().getLocalMap(DEFAULT_SESSION_MAP_NAME)
            .values()
            .stream()
            .map(item -> (SharedDataSessionImpl) item)
            .findFirst()
            .orElseThrow();
    }
}
