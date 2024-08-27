package com.inventage.portal.gateway.proxy.middleware.sessionLogoutFromBackchannel;

import static com.inventage.portal.gateway.proxy.middleware.AuthenticationRedirectRequestAssert.assertThat;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddleware.SSO_SID_TO_INTERNAL_SID_MAP_SESSION_DATA_KEY;
import static com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;
import static io.vertx.core.http.HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.ext.web.sstore.LocalSessionStore.DEFAULT_SESSION_MAP_NAME;

import com.google.common.io.Resources;
import com.inventage.portal.gateway.proxy.middleware.BrowserConnected;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import com.inventage.portal.gateway.proxy.middleware.VertxAssertions;
import com.inventage.portal.gateway.proxy.middleware.mock.TestBearerOnlyJWTProvider;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.sstore.impl.SharedDataSessionImpl;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class BackChannelLogoutMiddlewareTest {

    final String SSO_SID = "d452ed42-33ec-4a59-b110-0aa0280101bc";
    // Token format see: https://openid.net/specs/openid-connect-backchannel-1_0.html#LogoutToken
    final JsonObject VALID_LOGOUT_TOKEN_PAYLOAD_TEMPLATE = Json.createObjectBuilder()
        .add("typ", "Logout")
        .add("exp", 1893452400) // expires at (1.1.2023)
        .add("iat", 1627053747) // issued at (23.7.2021)
        .add("iss", "http://test.issuer:1234/auth/realms/test") // issuer
        .add("aud", "test-audience") // audience
        .add("jti", "50b642a2-ecd9-4776-9ef5-b3fb7b1c3139") // Unique identifier for the token - JWT token ID
        .add("sub", "f:924084fe-5091-428a-8e3f-1fa5542c5b69:83") // subject
        .add("sid", SSO_SID) // sso session id
        .add("events", Json.createObjectBuilder()
            // Claim whose value is a JSON object containing the member name http://schemas.openid.net/event/backchannel-logout. 
            // This declares that the JWT is a Logout Token. 
            // The corresponding member value MUST be a JSON object and SHOULD be the empty JSON object {}. 
            .add("http://schemas.openid.net/event/backchannel-logout", JsonValue.EMPTY_JSON_OBJECT)
            .build())
        .build();

    private static final String PUBLIC_KEY_PATH = "FOR_DEVELOPMENT_PURPOSE_ONLY-publicKey.pem";
    private static final String PUBLIC_KEY_ALGORITHM = "RS256";

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.MINUTES)
    public void backChannelLogoutRequest(Vertx vertx, VertxTestContext testCtx) {
        // given
        final String signedValidLogoutToken = TestBearerOnlyJWTProvider.signToken(VALID_LOGOUT_TOKEN_PAYLOAD_TEMPLATE);

        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withBackChannelLogoutMiddleware("/backchannellogout", jwtAuthOptions())
            .build()
            .start();

        // when
        final List<String> sessionId = new ArrayList<>();
        final List<String> cookie = new ArrayList<>();
        BrowserConnected browser = gateway.connectBrowser();
        browser.request(GET, "/some-path")
            .whenComplete((response, error) -> {
                testCtx.verify(() -> {
                    assertThat(testCtx, response)
                        .hasStatusCode(200)
                        .hasSetSessionCookie();
                });

                // preserve session cookie
                cookie.add(response.cookies().get(0));

                // simulate a login by populating the SID map
                SharedDataSessionImpl sharedDataSession = getSharedDataSession(vertx);
                sessionId.add(sharedDataSession.id());
            })
            .thenCompose(response -> {
                return ssoSIDToInternalSIDMap(vertx)
                    .compose(map -> map.put(SSO_SID, sessionId.get(0)))
                    .toCompletionStage();
            })
            .thenCompose(response -> {
                // back channel logout request as triggered by Keycloak
                final HeadersMultiMap headers = new HeadersMultiMap().add(HttpHeaders.CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED);
                return browser.request(POST, "/backchannellogout", headers, "logout_token=" + signedValidLogoutToken);
            })
            .whenComplete((response, error) -> {
                // assert session destruction
                final HeadersMultiMap headers = new HeadersMultiMap().add("cookie", DEFAULT_SESSION_COOKIE_NAME + "=" + sessionId.get(0));
                browser.request(GET, "/some-path", headers)
                    .whenComplete((response2, error2) -> {
                        // then
                        testCtx.verify(() -> {
                            assertThat(testCtx, response2)
                                .hasStatusCode(200)
                                .hasSetSessionCookieDifferentThan(cookie.get(0));

                            // assert sid mapping has been removed
                            ssoSIDToInternalSIDMap(vertx)
                                .compose(map -> map.get(SSO_SID))
                                .onComplete(testCtx.succeeding(internalSID -> {
                                    VertxAssertions.assertNull(testCtx, internalSID);
                                    testCtx.completeNow();
                                }));
                        });
                    });
            });
    }

    private JWTAuthOptions jwtAuthOptions() {
        String publicKeyRS256 = null;
        try {
            publicKeyRS256 = Resources.toString(Resources.getResource(PUBLIC_KEY_PATH), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new JWTAuthOptions()
            .addPubSecKey(new PubSecKeyOptions()
                .setAlgorithm(PUBLIC_KEY_ALGORITHM)
                .setBuffer(publicKeyRS256));
    }

    private Future<AsyncMap<String, String>> ssoSIDToInternalSIDMap(Vertx vertx) {
        // cant get a localMap, because OAuth2AuthMiddleware uses getAsyncMap
        return vertx.sharedData().getLocalAsyncMap(SSO_SID_TO_INTERNAL_SID_MAP_SESSION_DATA_KEY);
    }

    private SharedDataSessionImpl getSharedDataSession(Vertx vertx) {
        // can get a localMap, because LocalSessionStoreImpl uses getLocalMap (ClusteredSessionStoreImpl uses getAsyncMap)
        return vertx.sharedData().getLocalMap(DEFAULT_SESSION_MAP_NAME)
            .values()
            .stream()
            .map(item -> (SharedDataSessionImpl) item)
            .findFirst()
            .orElseThrow();
    }

    @Test
    public void backChannelLogoutRequest_withoutLogoutToken(Vertx vertx, VertxTestContext testCtx) {
        // given
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withBackChannelLogoutMiddleware("/backchannellogout", jwtAuthOptions())
            .build().start();

        // when
        final RequestOptions requestOptions = new RequestOptions().addHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED);
        gateway.incomingRequest(POST, "/backchannellogout", requestOptions, "a=b", (outgoingResponse) -> {
            // then
            testCtx.verify(() -> assertThat(testCtx, outgoingResponse).hasStatusCode(400));
            testCtx.completeNow();
        });
    }

    @Test
    public void backChannelLogoutRequest_logoutTokenWithoutSubClaim(Vertx vertx, VertxTestContext testCtx) {
        // given
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withBackChannelLogoutMiddleware("/backchannellogout", jwtAuthOptions())
            .build().start();

        final JsonObject logoutTokenWithoutSubClaim = Json.createObjectBuilder(VALID_LOGOUT_TOKEN_PAYLOAD_TEMPLATE)
            .remove("sub")
            .build();
        final String signedLogoutTokenWithoutSubClaim = TestBearerOnlyJWTProvider.signToken(logoutTokenWithoutSubClaim);

        // when
        final RequestOptions requestOptions = new RequestOptions().addHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED);
        gateway.incomingRequest(POST, "/backchannellogout", requestOptions, "logout_token=" + signedLogoutTokenWithoutSubClaim, (outgoingResponse) -> {
            // then
            testCtx.verify(() -> assertThat(testCtx, outgoingResponse).hasStatusCode(400));
            testCtx.completeNow();
        });
    }

    @Test
    public void backChannelLogoutRequest_logoutTokenWithoutSidClaim(Vertx vertx, VertxTestContext testCtx) {
        // given
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withBackChannelLogoutMiddleware("/backchannellogout", jwtAuthOptions())
            .build().start();

        final JsonObject logoutTokenWithoutSidClaim = Json.createObjectBuilder(VALID_LOGOUT_TOKEN_PAYLOAD_TEMPLATE)
            .remove("sid")
            .build();
        final String signedLogoutTokenWithoutSidClaim = TestBearerOnlyJWTProvider.signToken(logoutTokenWithoutSidClaim);

        // when
        final RequestOptions requestOptions = new RequestOptions().addHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED);
        gateway.incomingRequest(POST, "/backchannellogout", requestOptions, "logout_token=" + signedLogoutTokenWithoutSidClaim, (outgoingResponse) -> {
            // then
            testCtx.verify(() -> assertThat(testCtx, outgoingResponse).hasStatusCode(400));
            testCtx.completeNow();
        });
    }

    @Test
    public void backChannelLogoutRequest_logoutTokenWithoutEventsClaim(Vertx vertx, VertxTestContext testCtx) {
        // given
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withBackChannelLogoutMiddleware("/backchannellogout", jwtAuthOptions())
            .build().start();

        final JsonObject logoutTokenWithoutEventsClaim = Json.createObjectBuilder(VALID_LOGOUT_TOKEN_PAYLOAD_TEMPLATE)
            .remove("events")
            .build();
        final String signedLogoutTokenWithoutEventsClaim = TestBearerOnlyJWTProvider.signToken(logoutTokenWithoutEventsClaim);

        // when
        final RequestOptions requestOptions = new RequestOptions().addHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED);
        gateway.incomingRequest(POST, "/backchannellogout", requestOptions, "logout_token=" + signedLogoutTokenWithoutEventsClaim, (outgoingResponse) -> {
            // then
            testCtx.verify(() -> assertThat(testCtx, outgoingResponse).hasStatusCode(400));
            testCtx.completeNow();
        });
    }

    @Test
    public void backChannelLogoutRequest_logoutTokenWithIncorrectEventsClaimValue(Vertx vertx, VertxTestContext testCtx) {
        // given
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withBackChannelLogoutMiddleware("/backchannellogout", jwtAuthOptions())
            .build().start();

        final JsonObject logoutTokenWithIncorrectEventsClaimValue = Json.createObjectBuilder(VALID_LOGOUT_TOKEN_PAYLOAD_TEMPLATE)
            .remove("events")
            .add("events", "test")
            .build();
        final String signedLogoutTokenWithIncorrectEventsClaimValue = TestBearerOnlyJWTProvider.signToken(logoutTokenWithIncorrectEventsClaimValue);

        // when
        final RequestOptions requestOptions = new RequestOptions().addHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED);
        gateway.incomingRequest(POST, "/backchannellogout", requestOptions, "logout_token=" + signedLogoutTokenWithIncorrectEventsClaimValue, (outgoingResponse) -> {
            // then
            testCtx.verify(() -> assertThat(testCtx, outgoingResponse).hasStatusCode(500));
            testCtx.completeNow();
        });
    }

    @Test
    public void backChannelLogoutRequest_logoutTokenWithIncorrectEventsClaim(Vertx vertx, VertxTestContext testCtx) {
        // given
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withBackChannelLogoutMiddleware("/backchannellogout", jwtAuthOptions())
            .build().start();

        final JsonObject logoutTokenWithIncorrectEventsClaim = Json.createObjectBuilder(VALID_LOGOUT_TOKEN_PAYLOAD_TEMPLATE)
            .remove("events")
            .add("events", Json.createObjectBuilder()
                .add("test", JsonValue.EMPTY_JSON_OBJECT)
                .build())
            .build();
        final String signedLogoutTokenWithIncorrectEventsClaim = TestBearerOnlyJWTProvider.signToken(logoutTokenWithIncorrectEventsClaim);

        // when
        final RequestOptions requestOptions = new RequestOptions().addHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED);
        gateway.incomingRequest(POST, "/backchannellogout", requestOptions, "logout_token=" + signedLogoutTokenWithIncorrectEventsClaim, (outgoingResponse) -> {
            // then
            testCtx.verify(() -> assertThat(testCtx, outgoingResponse).hasStatusCode(400));
            testCtx.completeNow();
        });
    }

    @Test
    public void backChannelLogoutRequest_logoutTokenWithNonceClaim(Vertx vertx, VertxTestContext testCtx) {
        // given
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withBackChannelLogoutMiddleware("/backchannellogout", jwtAuthOptions())
            .build().start();

        final JsonObject logoutTokenWithNonceClaim = Json.createObjectBuilder(VALID_LOGOUT_TOKEN_PAYLOAD_TEMPLATE)
            .add("nonce", "123456")
            .build();
        final String signedLogoutTokenWithNonceClaim = TestBearerOnlyJWTProvider.signToken(logoutTokenWithNonceClaim);

        // when
        final RequestOptions requestOptions = new RequestOptions().addHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED);
        gateway.incomingRequest(POST, "/backchannellogout", requestOptions, "logout_token=" + signedLogoutTokenWithNonceClaim, (outgoingResponse) -> {
            // then
            testCtx.verify(() -> assertThat(testCtx, outgoingResponse).hasStatusCode(400));
            testCtx.completeNow();
        });
    }

    @Test
    public void backChannelLogoutRequest_invalidRequestMethod(Vertx vertx, VertxTestContext testCtx) {
        // given
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withBackChannelLogoutMiddleware("/backchannellogout", jwtAuthOptions())
            .build().start();

        // when
        final RequestOptions requestOptions = new RequestOptions().addHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED);
        gateway.incomingRequest(GET, "/backchannellogout", requestOptions, "", (outgoingResponse) -> {
            // then
            testCtx.verify(() -> assertThat(testCtx, outgoingResponse).hasStatusCode(400));
            testCtx.completeNow();
        });
    }

    @Test
    public void backChannelLogoutRequest_invalidRequestContentType(Vertx vertx, VertxTestContext testCtx) {
        // given
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withSessionMiddleware()
            .withBackChannelLogoutMiddleware("/backchannellogout", jwtAuthOptions())
            .build().start();

        // when
        final RequestOptions requestOptions = new RequestOptions().addHeader(HttpHeaders.CONTENT_TYPE, "text/plain");
        gateway.incomingRequest(POST, "/backchannellogout", requestOptions, "", (outgoingResponse) -> {
            // then
            testCtx.verify(() -> assertThat(testCtx, outgoingResponse).hasStatusCode(400));
            testCtx.completeNow();
        });
    }

}
