package ch.uniport.gateway.proxy.middleware.authorization.bearerOnly;

import static ch.uniport.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static io.vertx.core.http.HttpMethod.GET;

import ch.uniport.gateway.proxy.middleware.VertxAssertions;
import ch.uniport.gateway.proxy.middleware.authorization.bearerOnly.customClaimsChecker.JWTAuthAdditionalClaimsOptions;
import ch.uniport.gateway.proxy.middleware.authorization.bearerOnly.customClaimsChecker.JWTClaim;
import ch.uniport.gateway.proxy.middleware.authorization.bearerOnly.customClaimsChecker.JWTClaimOperator;
import ch.uniport.gateway.proxy.middleware.authorization.bearerOnly.customIssuerChecker.JWTAuthMultipleIssuersProvider;
import ch.uniport.gateway.proxy.middleware.mock.TestBearerOnlyJWTProvider;
import com.google.common.io.Resources;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * A series of tests to check that the implementation of custom claims behaves correctly.
 */
@ExtendWith(VertxExtension.class)
public class BearerOnlyMiddlewareOtherClaimsTest {

    private static final String HOST = "localhost";
    private static final String PUBLIC_KEY_PATH = "FOR_DEVELOPMENT_PURPOSE_ONLY-publicKey.pem";
    private static final String PUBLIC_KEY_ALGORITHM = "RS256";
    private static final JsonObject VALID_PAYLOAD_TEMPLATE = Json.createObjectBuilder()
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

    @Timeout(value = 1, timeUnit = TimeUnit.HOURS)
    @Test
    public void validToken(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //given
        final JWTClaim claimEqualString = new JWTClaim("$['organisation']", JWTClaimOperator.EQUALS, "portal");
        final JWTClaim claimContainRole = new JWTClaim("$['resource_access']['Organisation']['roles']", JWTClaimOperator.CONTAINS, List.of("ADMINISTRATOR", "TENANT"));
        final JWTClaim claimEqualObject = new JWTClaim("$['http://hasura.io/jwt/claims']", JWTClaimOperator.EQUALS, Map.of(
            "x-hasura-allowed-roles", List.of("KEYCLOAK", "portaluser"),
            "x-hasura-user-id", 1234));
        final JWTClaim claimEqualBoolean = new JWTClaim("$['email-verified']", JWTClaimOperator.EQUALS, Boolean.FALSE);
        final JWTClaim claimContainInteger = new JWTClaim("$['acr']", JWTClaimOperator.CONTAINS, List.of(1, 9));
        final JWTClaim claimContainSubstringWhitespace = new JWTClaim("$['scope']", JWTClaimOperator.CONTAINS_SUBSTRING_WHITESPACE, List.of("openid", "email", "profile", "Test"));
        final List<JWTClaim> expectedClaims = List.of(claimContainRole, claimEqualString, claimEqualObject, claimEqualBoolean, claimContainInteger, claimContainSubstringWhitespace);

        final String validToken = TestBearerOnlyJWTProvider.signToken(VALID_PAYLOAD_TEMPLATE);
        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("Organisation", "Uniport-Gateway");

        portalGateway(vertx, HOST, testCtx)
            .withBearerOnlyMiddlewareOtherClaims(
                jwtAuth(vertx, expectedIssuer, expectedAudience),
                jwtAdditionalClaims(expectedClaims), false)
            .build().start()
            //when
            .incomingRequest(GET, "/", new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION,
                bearer(validToken)), (resp) -> {
                    // then
                    VertxAssertions.assertEquals(testCtx, 200, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    @Test
    public void validTokenContainsWhitespaceScope(Vertx vertx, VertxTestContext testCtx)
        throws InterruptedException {
        //given
        final JWTClaim claimContainSubstringWhitespace = new JWTClaim("$['scope']", JWTClaimOperator.CONTAINS_SUBSTRING_WHITESPACE, List.of("openid", "email", "profile", "Test"));
        final List<JWTClaim> expectedClaims = List.of(claimContainSubstringWhitespace);

        final String validToken = TestBearerOnlyJWTProvider.signToken(VALID_PAYLOAD_TEMPLATE);
        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("Organisation", "Uniport-Gateway");

        portalGateway(vertx, HOST, testCtx)
            .withBearerOnlyMiddlewareOtherClaims(
                jwtAuth(vertx, expectedIssuer, expectedAudience),
                jwtAdditionalClaims(expectedClaims), false)
            .build().start()
            //when
            .incomingRequest(GET, "/", new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION,
                bearer(validToken)), (resp) -> {
                    // then
                    VertxAssertions.assertEquals(testCtx, 200, resp.statusCode(), "unexpected status code");
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
        final List<JWTClaim> expectedClaims = List.of(claimContainRole);

        final io.vertx.core.json.JsonObject invalidPayload = new io.vertx.core.json.JsonObject(
            VALID_PAYLOAD_TEMPLATE.toString());
        invalidPayload.remove("resource_access");
        final String invalidStringToken = TestBearerOnlyJWTProvider.signToken(invalidPayload.getMap());

        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("Organisation", "Uniport-Gateway");

        portalGateway(vertx, HOST, testCtx)
            .withBearerOnlyMiddlewareOtherClaims(
                jwtAuth(vertx, expectedIssuer, expectedAudience),
                jwtAdditionalClaims(expectedClaims), false)
            .build().start()
            //when
            .incomingRequest(GET, "/", new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION,
                bearer(invalidStringToken)), (resp) -> {
                    // then
                    VertxAssertions.assertEquals(testCtx, 403, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    @Test
    public void booleanValueMismatch(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //given
        final JWTClaim claimEqualBoolean = new JWTClaim("$['email-verified']", JWTClaimOperator.EQUALS,
            Boolean.FALSE);
        final List<JWTClaim> expectedClaims = List.of(claimEqualBoolean);

        final JsonObject invalidPayload = Json.createObjectBuilder(VALID_PAYLOAD_TEMPLATE).add("email-verified",
            true).build();

        final String invalidToken = TestBearerOnlyJWTProvider.signToken(invalidPayload);

        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("Organisation", "Uniport-Gateway");

        portalGateway(vertx, HOST, testCtx)
            .withBearerOnlyMiddlewareOtherClaims(
                jwtAuth(vertx, expectedIssuer, expectedAudience),
                jwtAdditionalClaims(expectedClaims), false)
            .build().start()
            //when
            .incomingRequest(GET, "/", new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION,
                bearer(invalidToken)), (resp) -> {
                    // then
                    VertxAssertions.assertEquals(testCtx, 403, resp.statusCode(), "unexpected status code");
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
        final List<JWTClaim> expectedClaims = List.of(claimContainRole);

        final JsonObject invalidPayload = Json.createObjectBuilder(VALID_PAYLOAD_TEMPLATE)
            .add("resource_access", Json.createObjectBuilder()
                .add("Organisation", Json.createObjectBuilder()
                    .add("roles", "Root")
                    .build())
                .build())
            .build();
        final String invalidToken = TestBearerOnlyJWTProvider.signToken(invalidPayload);

        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("Organisation", "Uniport-Gateway");

        portalGateway(vertx, HOST, testCtx)
            .withBearerOnlyMiddlewareOtherClaims(
                jwtAuth(vertx, expectedIssuer, expectedAudience),
                jwtAdditionalClaims(expectedClaims), false)
            .build().start()
            //when
            .incomingRequest(GET, "/", new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION,
                bearer(invalidToken)), (resp) -> {
                    // then
                    VertxAssertions.assertEquals(testCtx, 403, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    private JWTAuth jwtAuth(Vertx vertx, String expectedIssuer, List<String> expectedAudience) {
        String publicKeyRS256 = null;
        try {
            publicKeyRS256 = Resources.toString(Resources.getResource(PUBLIC_KEY_PATH),
                StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return JWTAuthMultipleIssuersProvider.create(vertx, new JWTAuthOptions()
            .addPubSecKey(new PubSecKeyOptions().setAlgorithm(PUBLIC_KEY_ALGORITHM)
                .setBuffer(publicKeyRS256))
            .setJWTOptions(new JWTOptions()
                .setIssuer(expectedIssuer)
                .setAudience(expectedAudience)));
    }

    private JWTAuthAdditionalClaimsOptions jwtAdditionalClaims(List<JWTClaim> claims) {
        return new JWTAuthAdditionalClaimsOptions(claims);
    }

    private String bearer(String value) {
        return "Bearer " + value;
    }
}
