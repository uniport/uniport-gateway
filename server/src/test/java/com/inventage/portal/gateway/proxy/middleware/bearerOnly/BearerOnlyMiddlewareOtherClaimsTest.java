package com.inventage.portal.gateway.proxy.middleware.bearerOnly;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static io.vertx.core.http.HttpMethod.GET;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.common.io.Resources;
import com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker.JWTAuthClaim;
import com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker.JWTClaim;
import com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker.JWTClaimOperator;
import com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker.JWTClaimOptions;
import com.inventage.portal.gateway.proxy.middleware.mock.TestBearerOnlyJWTProvider;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * A series of tests to check that the implementation of custom claims behaves correctly.
 */
@ExtendWith(VertxExtension.class)
public class BearerOnlyMiddlewareOtherClaimsTest {

        private static final String host = "localhost";
        private static final String publicKeyPath = "FOR_DEVELOPMENT_PURPOSE_ONLY-publicKey.pem";
        private static final String publicKeyAlgorithm = "RS256";
        private static final JsonObject validPayloadTemplate = Json.createObjectBuilder()
                        .add("typ", "Bearer")
                        .add("exp", 1893452400)
                        .add("iat", 1627053747)
                        .add("iss", "http://test.issuer:1234/auth/realms/test")
                        .add("azp", "test-authorized-parties")
                        .add("aud", "Organisation")
                        .add("scope", "openid email profile Test")
                        .add("organisation", "portal")
                        .add("email-verified", false)
                        .add("acr", 1)
                        .add("http://hasura.io/jwt/claims", Json.createObjectBuilder()
                                        .add("x-hasura-user-id", 1234)
                                        .add("x-hasura-allowed-roles",
                                                        Json.createArrayBuilder(List.of("KEYCLOAK", "portaluser"))
                                                                        .build())
                                        .build())
                        .add("resource_access", Json.createObjectBuilder()
                                        .add("Organisation", Json.createObjectBuilder()
                                                        .add("roles", Json.createArrayBuilder(List.of("TENANT")))
                                                        .build())
                                        .build())
                        .build();

        @Test
        public void validToken(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
                //given
                final JWTClaim claimEqualString = new JWTClaim("$['organisation']", JWTClaimOperator.EQUALS, "portal");
                final JWTClaim claimContainRole = new JWTClaim("$['resource_access']['Organisation']['roles']",
                                JWTClaimOperator.CONTAINS,
                                Json.createArrayBuilder(List.of("ADMINISTRATOR", "TENANT"))
                                                .build());
                final JWTClaim claimEqualObject = new JWTClaim("$['http://hasura.io/jwt/claims']",
                                JWTClaimOperator.EQUALS,
                                Json.createObjectBuilder()
                                                .add("x-hasura-allowed-roles",
                                                                Json.createArrayBuilder(
                                                                                List.of("KEYCLOAK", "portaluser")))
                                                .add("x-hasura-user-id", 1234)
                                                .build());
                final JWTClaim claimEqualBoolean = new JWTClaim("$['email-verified']", JWTClaimOperator.EQUALS,
                                Boolean.FALSE);
                final JWTClaim claimContainInteger = new JWTClaim("$['acr']", JWTClaimOperator.CONTAINS,
                                Json.createArrayBuilder(List.of(1, 9)).build());
                final JWTClaim claimContainSubstringWhitespace = new JWTClaim("$['scope']",
                                JWTClaimOperator.CONTAINS_SUBSTRING_WHITESPACE,
                                Json.createArrayBuilder(List.of("openid", "email", "profile", "Test")).build());
                final List<JWTClaim> claims = List.of(claimContainRole, claimEqualString, claimEqualObject,
                                claimEqualBoolean, claimContainInteger, claimContainSubstringWhitespace);

                final String validToken = TestBearerOnlyJWTProvider.signToken(validPayloadTemplate);
                final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
                final List<String> expectedAudience = List.of("Organisation", "Portal-Gateway");

                portalGateway(vertx, host, testCtx)
                                .withBearerOnlyMiddlewareOtherClaims(
                                                jwtAuth(vertx, expectedIssuer, expectedAudience, claims), false)
                                .build().start()
                                //when
                                .incomingRequest(GET, "/", new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION,
                                                bearer(validToken)), testCtx, (resp) -> {
                                                        // then
                                                        assertEquals(200, resp.statusCode(), "unexpected status code");
                                                        testCtx.completeNow();
                                                });
        }

        @Test
        public void validTokenContainsWhitespaceScope(Vertx vertx, VertxTestContext testCtx)
                        throws InterruptedException {
                //given
                final JWTClaim claimContainSubstringWhitespace = new JWTClaim("$['scope']",
                                JWTClaimOperator.CONTAINS_SUBSTRING_WHITESPACE,
                                Json.createArrayBuilder(List.of("openid", "email", "profile", "Test")).build());
                final List<JWTClaim> claims = List.of(claimContainSubstringWhitespace);

                final String validToken = TestBearerOnlyJWTProvider.signToken(validPayloadTemplate);
                final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
                final List<String> expectedAudience = List.of("Organisation", "Portal-Gateway");

                portalGateway(vertx, host, testCtx)
                                .withBearerOnlyMiddlewareOtherClaims(
                                                jwtAuth(vertx, expectedIssuer, expectedAudience, claims), false)
                                .build().start()
                                //when
                                .incomingRequest(GET, "/", new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION,
                                                bearer(validToken)), testCtx, (resp) -> {
                                                        // then
                                                        assertEquals(200, resp.statusCode(), "unexpected status code");
                                                        testCtx.completeNow();
                                                });
        }

        @Test
        public void pathKeyMismatch(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
                //given
                final JWTClaim claimContainRole = new JWTClaim("$['resource_access']['Organisation']['roles']",
                                JWTClaimOperator.CONTAINS,
                                Json.createArrayBuilder(List.of("ADMINISTRATOR", "TENANT"))
                                                .build());
                final List<JWTClaim> claims = List.of(claimContainRole);

                final io.vertx.core.json.JsonObject invalidPayload = new io.vertx.core.json.JsonObject(
                                validPayloadTemplate.toString());
                invalidPayload.remove("resource_access");
                final String invalidStringToken = TestBearerOnlyJWTProvider.signToken(invalidPayload.getMap());

                final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
                final List<String> expectedAudience = List.of("Organisation", "Portal-Gateway");

                portalGateway(vertx, host, testCtx)
                                .withBearerOnlyMiddlewareOtherClaims(
                                                jwtAuth(vertx, expectedIssuer, expectedAudience, claims), false)
                                .build().start()
                                //when
                                .incomingRequest(GET, "/", new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION,
                                                bearer(invalidStringToken)), testCtx, (resp) -> {
                                                        // then
                                                        assertEquals(403, resp.statusCode(), "unexpected status code");
                                                        testCtx.completeNow();
                                                });
        }

        @Test
        public void booleanValueMismatch(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
                //given
                final JWTClaim claimEqualBoolean = new JWTClaim("$['email-verified']", JWTClaimOperator.EQUALS,
                                Boolean.FALSE);
                final List<JWTClaim> claims = List.of(claimEqualBoolean);

                JsonObject invalidPayload = Json.createObjectBuilder(validPayloadTemplate).add("email-verified",
                                true).build();

                final String invalidToken = TestBearerOnlyJWTProvider.signToken(invalidPayload);

                final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
                final List<String> expectedAudience = List.of("Organisation", "Portal-Gateway");

                portalGateway(vertx, host, testCtx)
                                .withBearerOnlyMiddlewareOtherClaims(
                                                jwtAuth(vertx, expectedIssuer, expectedAudience, claims), false)
                                .build().start()
                                //when
                                .incomingRequest(GET, "/", new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION,
                                                bearer(invalidToken)), testCtx, (resp) -> {
                                                        // then
                                                        assertEquals(403, resp.statusCode(), "unexpected status code");
                                                        testCtx.completeNow();
                                                });
        }

        @Test
        public void entryMismatch(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
                //given
                final JWTClaim claimContainRole = new JWTClaim("$['resource_access']['Organisation']['roles']",
                                JWTClaimOperator.CONTAINS,
                                Json.createArrayBuilder(List.of("ADMINISTRATOR", "TENANT"))
                                                .build());
                final List<JWTClaim> claims = List.of(claimContainRole);

                JsonObject invalidPayload = Json.createObjectBuilder(validPayloadTemplate)
                                .add("resource_access", Json.createObjectBuilder()
                                                .add("Organisation", Json.createObjectBuilder()
                                                                .add("roles", "Root")
                                                                .build())
                                                .build())
                                .build();
                final String invalidToken = TestBearerOnlyJWTProvider.signToken(invalidPayload);

                final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
                final List<String> expectedAudience = List.of("Organisation", "Portal-Gateway");

                portalGateway(vertx, host, testCtx)
                                .withBearerOnlyMiddlewareOtherClaims(
                                                jwtAuth(vertx, expectedIssuer, expectedAudience, claims), false)
                                .build().start()
                                //when
                                .incomingRequest(GET, "/", new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION,
                                                bearer(invalidToken)), testCtx, (resp) -> {
                                                        // then
                                                        assertEquals(403, resp.statusCode(), "unexpected status code");
                                                        testCtx.completeNow();
                                                });
        }

        private JWTAuth jwtAuth(Vertx vertx, String expectedIssuer, List<String> expectedAudience,
                        List<JWTClaim> claims) {
                String publicKeyRS256 = null;
                try {
                        publicKeyRS256 = Resources.toString(Resources.getResource(publicKeyPath),
                                        StandardCharsets.UTF_8);
                } catch (IOException e) {
                        e.printStackTrace();
                }
                return JWTAuthClaim.create(vertx, new JWTAuthOptions()
                                .addPubSecKey(new PubSecKeyOptions().setAlgorithm(publicKeyAlgorithm)
                                                .setBuffer(publicKeyRS256))
                                .setJWTOptions(new JWTClaimOptions().setOtherClaims(claims).setIssuer(expectedIssuer)
                                                .setAudience(expectedAudience)));
        }

        private String bearer(String value) {
                return "Bearer " + value;
        }
}
