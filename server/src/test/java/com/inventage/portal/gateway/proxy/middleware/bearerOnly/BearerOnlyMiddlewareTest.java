package com.inventage.portal.gateway.proxy.middleware.bearerOnly;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.httpServer;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import com.inventage.portal.gateway.TestUtils;

import com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker.JWTAuthClaim;
import com.inventage.portal.gateway.proxy.middleware.mock.TestBearerOnlyJWTProvider;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class BearerOnlyMiddlewareTest {

    // necessary for jaeger (OpenTracing)
    static {
        System.setProperty("JAEGER_SERVICE_NAME", "portal-gateway");
    }

    /**
     * Corresponding private key stored in /resources/FOR_DEVELOPMENT_PURPOSE_ONLY-privateKey.pem
     */
    private static final String publicKeyRS256 = "-----BEGIN PUBLIC KEY-----\n" + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuFJ0A754CTB9+mhomn9Z\n" + "1aVCiSliTm7Mow3PkWko7PCRVshrqqJEHNg6fgl4KNH+u0ZBjq4L5AKtTuwhsx2v\n" + "IcJ8aJ3mQNdyxFU02nLaNzOVm+rOwytUPflAnYIgqinmiFpqyQ8vwj/L82F5kN5h\n" + "nB+G2heMXSep4uoq++2ogdyLtRi4CCr2tuFdPMcdvozsafRJjgJrmKkGggoembuI\n" + "N5mvuJ/YySMmE3F+TxXOVbhZqAuH4A2+9l0d1rbjghJnv9xCS8Tc7apusoK0q8jW\n" + "yBHp6p12m1IFkrKSSRiXXCmoMIQO8ZTCzpyqCQEgOXHKvxvSPRWsSa4GZWHzH3hv\n" + "RQIDAQAB\n" + "-----END PUBLIC KEY-----";
    private static final String publicKeyAlgorithm = "RS256";

    private static final JsonObject validPayloadTemplate = new JsonObject("{\n" +
            "  \"typ\": \"Bearer\",\n" +
            "  \"exp\": 1893452400,\n" +
            "  \"iat\": 1627053747,\n" +
            "  \"iss\": \"http://test.issuer:1234/auth/realms/test\",\n" +
            "  \"azp\": \"test-authorized-parties\",\n" +
            "  \"aud\": \"test-audience\",\n" +
            "  \"scope\": \"openid email profile Test\"\n" +
            "}");

    private int port;

    @BeforeEach
    public void setup() {
        port = TestUtils.findFreePort();
    }

    @Test
    public void noBearer(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("test-audience");

        httpServer(vertx, port).withBearerOnlyMiddleware(jwtAuth(vertx, expectedIssuer, expectedAudience), false)
                // when
                .doRequest(testCtx, new RequestOptions(), (resp) -> {
                    // then
                    assertEquals(401, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    @Test
    public void invalidSignature(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("test-audience");

        final String invalidSignatureToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0eXAiOiJCZWFyZXIiLCJleHAiOjE4OTM0NTI0MDAsImlhdCI6MTYyNzA1Mzc0NywiaXNzIjoiaHR0cDovL3Rlc3QuaXNzdWVyOjEyMzQvYXV0aC9yZWFsbXMvdGVzdCIsImF6cCI6InRlc3QtYXV0aG9yaXplZC1wYXJ0aWVzIiwiYXVkIjoidGVzdC1hdWRpZW5jZSIsInNjb3BlIjoib3BlbmlkIGVtYWlsIHByb2ZpbGUgVGVzdCJ9.blub";

        httpServer(vertx, port).withBearerOnlyMiddleware(jwtAuth(vertx, expectedIssuer, expectedAudience), false)
                // when
                .doRequest(testCtx, new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(invalidSignatureToken)), (resp) -> {
                    // then
                    assertEquals(401, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    @Test
    public void issuerMismatch(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("test-audience");

        final JsonObject invalidPayload = new JsonObject(validPayloadTemplate.toString());
        invalidPayload.put("iss", "http://malory.issuer:1234/auth/realms/test");
        final String invalidToken = TestBearerOnlyJWTProvider.signToken(invalidPayload.getMap());

        httpServer(vertx, port).withBearerOnlyMiddleware(jwtAuth(vertx, expectedIssuer, expectedAudience), false)
                // when
                .doRequest(testCtx, new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(invalidToken)), (resp) -> {
                    // then
                    assertEquals(403, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    @Test
    public void audienceMismatch(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final JsonObject invalidPayload = new JsonObject(validPayloadTemplate.toString());
        invalidPayload.put("aud", "malory-audience");
        final String invalidToken = TestBearerOnlyJWTProvider.signToken(invalidPayload.getMap());

        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("test-audience");

        httpServer(vertx, port).withBearerOnlyMiddleware(jwtAuth(vertx, expectedIssuer, expectedAudience), false)
                // when
                .doRequest(testCtx, new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(invalidToken)), (resp) -> {
                    // then
                    assertEquals(403, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    @Test
    public void validWithRSA256_(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String validToken = TestBearerOnlyJWTProvider.signToken(validPayloadTemplate.getMap());

        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("test-audience");

        httpServer(vertx, port).withBearerOnlyMiddleware(jwtAuth(vertx, expectedIssuer, expectedAudience), false)
                // when
                .doRequest(testCtx, new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(validToken)), (resp) -> {
                    // then
                    assertEquals(200, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }


    private JWTAuth jwtAuth(Vertx vertx, String expectedIssuer, List<String> expectedAudience) {
        return JWTAuthClaim.create(vertx,
                new JWTAuthOptions()
                        .addPubSecKey(new PubSecKeyOptions().setAlgorithm(publicKeyAlgorithm).setBuffer(publicKeyRS256))
                        .setJWTOptions(new JWTOptions().setIssuer(expectedIssuer).setAudience(expectedAudience)));
    }

    private String bearer(String value) {
        return "Bearer " + value;
    }
}