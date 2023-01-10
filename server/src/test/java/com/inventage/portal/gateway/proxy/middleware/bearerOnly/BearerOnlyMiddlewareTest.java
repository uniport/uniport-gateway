package com.inventage.portal.gateway.proxy.middleware.bearerOnly;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.common.io.Resources;
import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.mock.TestBearerOnlyJWTProvider;

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

    private static final String host = "localhost";
    private static final String publicKeyPath = "FOR_DEVELOPMENT_PURPOSE_ONLY-publicKey.pem";
    private static final String publicKeyAlgorithm = "RS256";
    private static final JsonObject validPayloadTemplate = Json.createObjectBuilder()
            .add("typ", "Bearer")
            .add("exp", 1893452400)
            .add("iat", 1627053747)
            .add("iss", "http://test.issuer:1234/auth/realms/test")
            .add("azp", "test-authorized-parties")
            .add("aud", "test-audience")
            .add("scope", "openid email profile Test")
            .build();

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

        portalGateway(vertx, host, port)
                .withBearerOnlyMiddleware(jwtAuth(vertx, expectedIssuer, expectedAudience), false)
                .build()
                // when
                .incomingRequest(testCtx, new RequestOptions(), (resp) -> {
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

        portalGateway(vertx, host, port)
                .withBearerOnlyMiddleware(jwtAuth(vertx, expectedIssuer, expectedAudience), false)
                .build()
                // when
                .incomingRequest(testCtx,
                        new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(invalidSignatureToken)),
                        (resp) -> {
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

        final JsonObject invalidPayload = Json.createObjectBuilder(validPayloadTemplate)
                .add("iss", "http://malory.issuer:1234/auth/realms/test")
                .build();

        final String invalidToken = TestBearerOnlyJWTProvider.signToken(invalidPayload);

        portalGateway(vertx, host, port)
                .withBearerOnlyMiddleware(jwtAuth(vertx, expectedIssuer, expectedAudience), false)
                .build()
                // when
                .incomingRequest(testCtx,
                        new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(invalidToken)), (resp) -> {
                            // then
                            assertEquals(401, resp.statusCode(), "unexpected status code");
                            testCtx.completeNow();
                        });
    }

    @Test
    public void audienceMismatch(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final JsonObject invalidPayload = Json.createObjectBuilder(validPayloadTemplate)
                .add("aud", "malory-audience")
                .build();
        final String invalidToken = TestBearerOnlyJWTProvider.signToken(invalidPayload);

        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("test-audience");

        portalGateway(vertx, host, port)
                .withBearerOnlyMiddleware(jwtAuth(vertx, expectedIssuer, expectedAudience), false)
                .build()
                // when
                .incomingRequest(testCtx,
                        new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(invalidToken)), (resp) -> {
                            // then
                            assertEquals(401, resp.statusCode(), "unexpected status code");
                            testCtx.completeNow();
                        });
    }

    @Test
    public void validWithRSA256_(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String validToken = TestBearerOnlyJWTProvider.signToken(validPayloadTemplate);

        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("test-audience");

        portalGateway(vertx, host, port)
                .withBearerOnlyMiddleware(jwtAuth(vertx, expectedIssuer, expectedAudience), false)
                .build()
                // when
                .incomingRequest(testCtx, new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(validToken)),
                        (resp) -> {
                            // then
                            assertEquals(200, resp.statusCode(), "unexpected status code");
                            testCtx.completeNow();
                        });
    }

    private JWTAuth jwtAuth(Vertx vertx, String expectedIssuer, List<String> expectedAudience) {
        String publicKeyRS256 = null;
        try {
            publicKeyRS256 = Resources.toString(Resources.getResource(publicKeyPath),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return JWTAuth.create(vertx,
                new JWTAuthOptions()
                        .addPubSecKey(new PubSecKeyOptions().setAlgorithm(publicKeyAlgorithm).setBuffer(publicKeyRS256))
                        .setJWTOptions(new JWTOptions().setIssuer(expectedIssuer).setAudience(expectedAudience)));
    }

    private String bearer(String value) {
        return "Bearer " + value;
    }
}
