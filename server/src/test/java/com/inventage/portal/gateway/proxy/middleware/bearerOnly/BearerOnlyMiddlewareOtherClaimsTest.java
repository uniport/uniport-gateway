package com.inventage.portal.gateway.proxy.middleware.bearerOnly;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker.JWTAuthClaim;
import com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker.JWTClaimOptions;
import com.inventage.portal.gateway.proxy.middleware.mock.TestBearerOnlyJWTProvider;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.httpServer;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A series of tests to check that the implementation of custom claims behaves correctly.
 */
@ExtendWith(VertxExtension.class)
public class BearerOnlyMiddlewareOtherClaimsTest {

    // necessary for jaeger (OpenTracing)
    static {
        System.setProperty("JAEGER_SERVICE_NAME", "portal-gateway");
    }

    /**
     * Corresponding private key stored in /resources/FOR_DEVELOPMENT_PURPOSE_ONLY-privateKey.pem
     */
    private static final String publicKeyRS256 = "-----BEGIN PUBLIC KEY-----\n" + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuFJ0A754CTB9+mhomn9Z\n" + "1aVCiSliTm7Mow3PkWko7PCRVshrqqJEHNg6fgl4KNH+u0ZBjq4L5AKtTuwhsx2v\n" + "IcJ8aJ3mQNdyxFU02nLaNzOVm+rOwytUPflAnYIgqinmiFpqyQ8vwj/L82F5kN5h\n" + "nB+G2heMXSep4uoq++2ogdyLtRi4CCr2tuFdPMcdvozsafRJjgJrmKkGggoembuI\n" + "N5mvuJ/YySMmE3F+TxXOVbhZqAuH4A2+9l0d1rbjghJnv9xCS8Tc7apusoK0q8jW\n" + "yBHp6p12m1IFkrKSSRiXXCmoMIQO8ZTCzpyqCQEgOXHKvxvSPRWsSa4GZWHzH3hv\n" + "RQIDAQAB\n" + "-----END PUBLIC KEY-----";

    private static final String publicKeyAlgorithm = "RS256";


    /**
     * claimPath is defined according to the Jsonpath standard. To know more on how to formulate specific path or queries, please refer to:
     * https://github.com/json-path/JsonPath
     * or
     */
    private static final JsonObject validPayloadTemplate = new JsonObject("{\n" + "  \"typ\": \"Bearer\",\n" + "  \"exp\": 1893452400,\n" + "  \"iat\": 1627053747,\n" + "  \"iss\": \"http://test.issuer:1234/auth/realms/test\",\n" + "  \"azp\": \"test-authorized-parties\",\n" + "  \"aud\": \"Organisation\",\n" + "  \"scope\": \"openid email profile Test\",\n" + "  \"organisation\": \"portal\",\n" + "  \"email-verified\": false,\n" + "  \"acr\": 1,\n" + "  \"http://hasura.io/jwt/claims\": {\n" + "    \"x-hasura-portaluser-id\": \"1234\",\n" + "    \"x-hasura-allowed-roles\": [\n" + "      \"KEYCLOAK\",\n" + "      \"portaluser\"\n" + "    ]\n" + "  },\n" + "  \"resource_access\": {\n" + "    \"Organisation\": {\n" + "      \"roles\": \"TENANT\"\n" + "    }\n" + "  }\n" + "}");
    private int port;

    @BeforeEach
    public void setup() {
        port = TestUtils.findFreePort();
    }

    @Test
    public void validToken(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //given
        final JsonObject claimEqualString = new JsonObject("{\"claimPath\":\"$['organisation']\",\"operator\":\"EQUALS\",\"value\":\"portal\"}");
        final JsonObject claimContainRole = new JsonObject("{\"claimPath\":\"$['resource_access']['Organisation']['roles']\",\"operator\":\"CONTAINS\",\"value\":[\"ADMINISTRATOR\",\"TENANT\"]}");
        final JsonObject claimEqualObject = new JsonObject("{\"claimPath\":\"$['http://hasura.io/jwt/claims']\",\"operator\":\"EQUALS\",\"value\":{\"x-hasura-allowed-roles\":[\"KEYCLOAK\",\"portaluser\"],\"x-hasura-portaluser-id\":\"1234\"}}");
        final JsonObject claimEqualBoolean = new JsonObject("{\"claimPath\":\"$['email-verified']\",\"operator\":\"EQUALS\",\"value\":false}");
        final JsonObject claimContainInteger = new JsonObject("{\"claimPath\":\"$['acr']\",\"operator\":\"CONTAINS\",\"value\":[1,9]}");
        final JsonObject claimContainSubstringWhitespace = new JsonObject("{\"claimPath\":\"$['scope']\",\"operator\":\"CONTAINS_SUBSTRING_WHITESPACE\",\"value\":" + "[\"openid\", \"email\", \"profile\", \"Test\"]}");
        final JsonArray claims = new JsonArray(List.of(claimEqualString, claimContainRole, claimEqualObject, claimEqualBoolean, claimContainInteger, claimContainSubstringWhitespace));

        final String validToken = TestBearerOnlyJWTProvider.signToken(validPayloadTemplate.getMap());

        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("Organisation", "Portal-Gateway");

        httpServer(vertx, port).withBearerOnlyMiddlewareOtherClaims(jwtAuth(vertx, expectedIssuer, expectedAudience, claims), false)
                //when
                .doRequest(testCtx, new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(validToken)), (resp) -> {
                    // then
                    assertEquals(200, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }


    @Test
    public void validTokenContainsWhitespaceScope(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //given
        final JsonObject claimContainSubstringWhitespace = new JsonObject("{\"claimPath\":\"$['scope']\",\"operator\":\"CONTAINS_SUBSTRING_WHITESPACE\",\"value\":" + "[\"openid\", \"email\", \"profile\", \"Test\"]}");
        final JsonArray claims = new JsonArray(List.of(claimContainSubstringWhitespace));

        final String validToken = TestBearerOnlyJWTProvider.signToken(validPayloadTemplate.getMap());

        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("Organisation", "Portal-Gateway");

        httpServer(vertx, port).withBearerOnlyMiddlewareOtherClaims(jwtAuth(vertx, expectedIssuer, expectedAudience, claims), false)
                //when
                .doRequest(testCtx, new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(validToken)), (resp) -> {
                    // then
                    assertEquals(200, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    @Test
    public void pathKeyMismatch(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //given
        final JsonObject claimContainRole = new JsonObject("{\"claimPath\":\"$['resource_access']['Organisation']['roles']\"," + "\"operator\":\"CONTAINS\"," + "\"value\":[\"ADMINISTRATOR\",\"TENANT\"]}");
        final JsonArray claims = new JsonArray(List.of(claimContainRole));

        final JsonObject invalidPayload = new JsonObject(validPayloadTemplate.toString());
        invalidPayload.remove("resource_access");
        final String invalidStringToken = TestBearerOnlyJWTProvider.signToken(invalidPayload.getMap());

        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("Organisation", "Portal-Gateway");

        httpServer(vertx, port).withBearerOnlyMiddlewareOtherClaims(jwtAuth(vertx, expectedIssuer, expectedAudience, claims), false)
                //when
                .doRequest(testCtx, new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(invalidStringToken)), (resp) -> {
                    // then
                    assertEquals(401, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    @Test
    public void booleanValueMismatch(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //given
        final JsonObject claimEqualBoolean = new JsonObject("{\"claimPath\":\"$['email-verified']\"," + "\"operator\":\"EQUALS\"," + "\"value\":false}");
        final JsonArray claims = new JsonArray(List.of(claimEqualBoolean));

        final JsonObject invalidPayload = new JsonObject(validPayloadTemplate.toString());
        invalidPayload.put("email-verified", true);
        final String invalidToken = TestBearerOnlyJWTProvider.signToken(invalidPayload.getMap());

        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("Organisation", "Portal-Gateway");

        httpServer(vertx, port).withBearerOnlyMiddlewareOtherClaims(jwtAuth(vertx, expectedIssuer, expectedAudience, claims), false)
                //when
                .doRequest(testCtx, new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(invalidToken)), (resp) -> {
                    // then
                    assertEquals(401, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    @Test
    public void entryMismatch(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //given
        final JsonObject claimContainRole = new JsonObject("{\"claimPath\":\"$['resource_access']['Organisation']['roles']\"," + "\"operator\":\"CONTAINS\"," + "\"value\":[\"ADMINISTRATOR\",\"TENANT\"]}");

        final JsonArray claims = new JsonArray(List.of(claimContainRole));

        final JsonObject invalidPayload = new JsonObject(validPayloadTemplate.toString());
        invalidPayload.getJsonObject("resource_access").getJsonObject("Organisation").put("roles", "Root");
        final String invalidToken = TestBearerOnlyJWTProvider.signToken(invalidPayload.getMap());

        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("Organisation", "Portal-Gateway");

        httpServer(vertx, port).withBearerOnlyMiddlewareOtherClaims(jwtAuth(vertx, expectedIssuer, expectedAudience, claims), false)
                //when
                .doRequest(testCtx, new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(invalidToken)), (resp) -> {
                    // then
                    assertEquals(401, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }


    private JWTAuth jwtAuth(Vertx vertx, String expectedIssuer, List<String> expectedAudience, JsonArray claims) {
        return JWTAuthClaim.create(vertx, new JWTAuthOptions().addPubSecKey(new PubSecKeyOptions().setAlgorithm(publicKeyAlgorithm).setBuffer(publicKeyRS256)).setJWTOptions(new JWTClaimOptions().setOtherClaims(claims).setIssuer(expectedIssuer).setAudience(expectedAudience)));
    }

    private String bearer(String value) {
        return "Bearer " + value;
    }
}