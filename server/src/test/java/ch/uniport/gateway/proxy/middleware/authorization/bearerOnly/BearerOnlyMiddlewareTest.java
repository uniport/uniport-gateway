package ch.uniport.gateway.proxy.middleware.authorization.bearerOnly;

import static ch.uniport.gateway.TestUtils.buildConfiguration;
import static ch.uniport.gateway.TestUtils.withMiddleware;
import static ch.uniport.gateway.TestUtils.withMiddlewareOpts;
import static ch.uniport.gateway.TestUtils.withMiddlewares;
import static ch.uniport.gateway.proxy.middleware.MiddlewareServerBuilder.uniportGateway;
import static io.vertx.core.http.HttpMethod.GET;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.uniport.gateway.proxy.middleware.KeycloakServer;
import ch.uniport.gateway.proxy.middleware.MiddlewareServer;
import ch.uniport.gateway.proxy.middleware.MiddlewareTestBase;
import ch.uniport.gateway.proxy.middleware.VertxAssertions;
import ch.uniport.gateway.proxy.middleware.authorization.PublicKeyOptions;
import ch.uniport.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareFactoryBase;
import ch.uniport.gateway.proxy.middleware.authorization.shared.customIssuerChecker.JWTAuthMultipleIssuersOptions;
import ch.uniport.gateway.proxy.middleware.authorization.shared.customIssuerChecker.JWTAuthMultipleIssuersProvider;
import ch.uniport.gateway.proxy.middleware.mock.TestBearerOnlyJWTProvider;
import com.google.common.io.Resources;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jakarta.json.Json;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(VertxExtension.class)
public class BearerOnlyMiddlewareTest extends MiddlewareTestBase {

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {
        final JsonObject simple = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", BearerOnlyMiddlewareFactory.TYPE,
                    withMiddlewareOpts(new JsonObject()
                        .put(WithAuthHandlerMiddlewareFactoryBase.PUBLIC_KEYS, JsonArray.of(
                            new JsonObject()
                                .put(WithAuthHandlerMiddlewareFactoryBase.PUBLIC_KEY, "Ymx1Ygo=")
                                .put(WithAuthHandlerMiddlewareFactoryBase.PUBLIC_KEY_ALGORITHM, "RS256")))
                        .put(WithAuthHandlerMiddlewareFactoryBase.ISSUER, "bar")
                        .put(WithAuthHandlerMiddlewareFactoryBase.AUDIENCE, JsonArray.of("blub"))))));

        final JsonObject missingRequiredProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", BearerOnlyMiddlewareFactory.TYPE,
                    withMiddlewareOpts(new JsonObject()
                        .put(WithAuthHandlerMiddlewareFactoryBase.ISSUER, "blub")))));

        final JsonObject invalidPublicKey = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", BearerOnlyMiddlewareFactory.TYPE,
                    withMiddlewareOpts(new JsonObject()
                        .put(WithAuthHandlerMiddlewareFactoryBase.PUBLIC_KEY, "notbase64*oraurl")
                        .put(WithAuthHandlerMiddlewareFactoryBase.PUBLIC_KEY_ALGORITHM, "RS256")
                        .put(WithAuthHandlerMiddlewareFactoryBase.ISSUER, "bar")
                        .put(WithAuthHandlerMiddlewareFactoryBase.AUDIENCE, new JsonArray().add("blub"))))));

        final JsonObject invalidPublicKeyFormat = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", BearerOnlyMiddlewareFactory.TYPE,
                    withMiddlewareOpts(new JsonObject()
                        .put(WithAuthHandlerMiddlewareFactoryBase.PUBLIC_KEY, "Ymx1Ygo=")
                        .put(WithAuthHandlerMiddlewareFactoryBase.PUBLIC_KEY_ALGORITHM, "")
                        .put(WithAuthHandlerMiddlewareFactoryBase.ISSUER, "bar")
                        .put(WithAuthHandlerMiddlewareFactoryBase.AUDIENCE, JsonArray.of("blub"))))));

        final JsonObject invalidAudience = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", BearerOnlyMiddlewareFactory.TYPE,
                    withMiddlewareOpts(new JsonObject()
                        .put(WithAuthHandlerMiddlewareFactoryBase.PUBLIC_KEY, "Ymx1Ygo=")
                        .put(WithAuthHandlerMiddlewareFactoryBase.PUBLIC_KEY_ALGORITHM, "RS256")
                        .put(WithAuthHandlerMiddlewareFactoryBase.ISSUER, "bar")
                        .put(WithAuthHandlerMiddlewareFactoryBase.AUDIENCE, JsonArray.of("valid", 123, true))))));

        return Stream.of(
            Arguments.of("accept simple config", simple, complete, expectedTrue),
            Arguments.of("reject config with missing required property", missingRequiredProperty, complete, expectedFalse),
            Arguments.of("reject config with invalid public key", invalidPublicKey, complete, expectedFalse),
            Arguments.of("reject config with invalid public key format", invalidPublicKeyFormat, complete, expectedFalse),
            Arguments.of("reject config with invalid audience", invalidAudience, complete, expectedFalse)

        );
    }

    private static final String HOST = "localhost";
    private static final String PUBLIC_KEY_PATH = "FOR_DEVELOPMENT_PURPOSE_ONLY-publicKey.pem";
    private static final String PUBLIC_KEY_ALGORITHM = "RS256";
    private static final jakarta.json.JsonObject VALID_PAYLOAD_TEMPLATE = Json.createObjectBuilder()
        .add("typ", "Bearer")
        .add("exp", 1893452400)
        .add("iat", 1627053747)
        .add("iss", "http://test.issuer:1234/auth/realms/test")
        .add("azp", "test-authorized-parties")
        .add("aud", "test-audience")
        .add("scope", "openid email profile Test")
        .build();

    @Test
    public void noBearer(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("test-audience");

        uniportGateway(vertx, HOST, testCtx)
            .withBearerOnlyMiddleware(jwtAuth(vertx, expectedIssuer, expectedAudience), false)
            .build().start()
            // when
            .incomingRequest(GET, "/", (resp) -> {
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

        uniportGateway(vertx, HOST, testCtx)
            .withBearerOnlyMiddleware(jwtAuth(vertx, expectedIssuer, expectedAudience), false)
            .build().start()
            // when
            .incomingRequest(GET, "/",
                new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(invalidSignatureToken)),
                (resp) -> {
                    // then
                    VertxAssertions.assertEquals(testCtx, 401, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    @Test
    public void issuerMismatch(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("test-audience");

        final jakarta.json.JsonObject invalidPayload = Json.createObjectBuilder(VALID_PAYLOAD_TEMPLATE)
            .add("iss", "http://malory.issuer:1234/auth/realms/test")
            .build();

        final String invalidToken = TestBearerOnlyJWTProvider.signToken(invalidPayload);

        uniportGateway(vertx, HOST, testCtx)
            .withBearerOnlyMiddleware(jwtAuth(vertx, expectedIssuer, expectedAudience), false)
            .build().start()
            // when
            .incomingRequest(GET, "/",
                new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(invalidToken)),
                (resp) -> {
                    // then
                    assertEquals(401, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    @Test
    public void validIssuerAdditionalIssuers(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final String additionalIssuer = "http://test.issuer:1234/auth/realms/additional";
        final JsonArray additionalIssuers = JsonArray.of(additionalIssuer, "http://test.issuer:1234/auth/realms/test2");

        final List<String> expectedAudience = List.of("test-audience");

        final jakarta.json.JsonObject validPayload = Json.createObjectBuilder(VALID_PAYLOAD_TEMPLATE)
            .add("iss", additionalIssuer)
            .build();

        final String validToken = TestBearerOnlyJWTProvider.signToken(validPayload);

        uniportGateway(vertx, HOST, testCtx)
            .withBearerOnlyMiddleware(jwtAuthWithAdditionalIssuers(vertx, expectedIssuer, expectedAudience, additionalIssuers), false)
            .build().start()
            // when
            .incomingRequest(GET, "/",
                new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(validToken)),
                (resp) -> {
                    // then
                    assertEquals(200, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    @Test
    public void invalidIssuerAdditionalIssuers(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final JsonArray additionalIssuers = JsonArray.of("http://test.issuer:1234/auth/realms/additional", "http://test.issuer:1234/auth/realms/test2");

        final List<String> expectedAudience = List.of("test-audience");

        final jakarta.json.JsonObject invalidPayload = Json.createObjectBuilder(VALID_PAYLOAD_TEMPLATE)
            .add("iss", "http://malory.issuer:1234/auth/realms/test")
            .build();

        final String invalidToken = TestBearerOnlyJWTProvider.signToken(invalidPayload);

        uniportGateway(vertx, HOST, testCtx)
            .withBearerOnlyMiddleware(jwtAuthWithAdditionalIssuers(vertx, expectedIssuer, expectedAudience, additionalIssuers), false)
            .build().start()
            // when
            .incomingRequest(GET, "/",
                new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(invalidToken)),
                (resp) -> {
                    // then
                    assertEquals(401, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    @Test
    public void audienceMismatch(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final jakarta.json.JsonObject invalidPayload = Json.createObjectBuilder(VALID_PAYLOAD_TEMPLATE)
            .add("aud", "malory-audience")
            .build();
        final String invalidToken = TestBearerOnlyJWTProvider.signToken(invalidPayload);

        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("test-audience");

        uniportGateway(vertx, HOST, testCtx)
            .withBearerOnlyMiddleware(jwtAuth(vertx, expectedIssuer, expectedAudience), false)
            .build().start()
            // when
            .incomingRequest(GET, "/",
                new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(invalidToken)),
                (resp) -> {
                    // then
                    assertEquals(401, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    @Test
    public void validWithRSA256(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String validToken = TestBearerOnlyJWTProvider.signToken(VALID_PAYLOAD_TEMPLATE);

        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("test-audience");

        uniportGateway(vertx, HOST, testCtx)
            .withBearerOnlyMiddleware(jwtAuth(vertx, expectedIssuer, expectedAudience), false)
            .build().start()
            // when
            .incomingRequest(GET, "/",
                new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(validToken)),
                (resp) -> {
                    // then
                    assertEquals(200, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    @Test
    void fetchPublicKeysFromOIDCProvider(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String validToken = TestBearerOnlyJWTProvider.signToken(VALID_PAYLOAD_TEMPLATE);

        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("test-audience");

        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx, "localhost")
            .startWithDiscoveryHandlerWithJWKsURIAndDefaultJWKsURIHandler();

        final List<PublicKeyOptions> publicKeys = List.of(
            PublicKeyOptions.builder()
                .withKey("http://localhost:" + keycloakServer.port() + "/auth/realms/test")
                .build());

        uniportGateway(vertx, testCtx)
            .withBearerOnlyMiddleware(keycloakServer, expectedIssuer, expectedAudience, publicKeys)
            .build().start()
            // when
            .incomingRequest(GET, "/",
                new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(validToken)),
                (resp) -> {
                    // then
                    assertEquals(200, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                    keycloakServer.closeServer();
                });
    }

    @Test
    void fetchPublicKeysFromOIDCProviderWithFirstReqIsAuthorizedAndUnauthorizedAfterPublicKeyRefresh(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String validTokenBeforePublicKeysRefresh = TestBearerOnlyJWTProvider.signToken(VALID_PAYLOAD_TEMPLATE);

        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("test-audience");

        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx, "localhost")
            .startWithDiscoveryHandlerWithJWKsURIAndDefaultJWKsURIHandler();

        final List<PublicKeyOptions> publicKeys = List.of(
            PublicKeyOptions.builder()
                .withKey("http://localhost:" + keycloakServer.port() + "/auth/realms/test")
                .build());

        final long reconciliationIntervalMs = 100;

        final MiddlewareServer gateway = uniportGateway(vertx, testCtx)
            .withBearerOnlyMiddleware(keycloakServer, expectedIssuer, expectedAudience, publicKeys, reconciliationIntervalMs)
            .build().start();

        // when
        gateway.incomingRequest(GET, "/",
            new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(validTokenBeforePublicKeysRefresh)),
            (outgoingResponse1) -> {
                VertxAssertions.assertEquals(testCtx, 200, outgoingResponse1.statusCode(), "unexpected status code");

                keycloakServer.serveInvalidPublicKeys();

                // give the middleware time to refetch the public keys
                vertx.setTimer(2 * reconciliationIntervalMs, (timerID) -> {
                    gateway.incomingRequest(GET, "/", new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(validTokenBeforePublicKeysRefresh)), (outgoingResponse2) -> {
                        // then
                        VertxAssertions.assertEquals(testCtx, 401, outgoingResponse2.statusCode(), "unexpected status code");

                        testCtx.completeNow();
                        keycloakServer.closeServer();
                    });
                });
            });
    }

    @Test
    void fetchPublicKeysFromOIDCProviderWithPublicKeysBeingOutdated(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String token = TestBearerOnlyJWTProvider.signToken(VALID_PAYLOAD_TEMPLATE);

        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("test-audience");

        final KeycloakServer keycloakServer = new KeycloakServer(vertx, testCtx, "localhost")
            .startWithDiscoveryHandlerWithJWKsURIAndDefaultJWKsURIHandler();
        keycloakServer.serveInvalidPublicKeys(); // make the token invalid

        final List<PublicKeyOptions> publicKeys = List.of(
            PublicKeyOptions.builder()
                .withKey("http://localhost:" + keycloakServer.port() + "/auth/realms/test")
                .build());

        final MiddlewareServer gateway = uniportGateway(vertx, testCtx)
            .withBearerOnlyMiddleware(keycloakServer, expectedIssuer, expectedAudience, publicKeys)
            .build().start();

        keycloakServer.serveValidPublicKeys(); // make the token valid

        // when
        gateway.incomingRequest(GET, "/",
            new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(token)),
            (outgoingResponse1) -> {
                assertEquals(200, outgoingResponse1.statusCode(), "unexpected status code");

                testCtx.completeNow();
                keycloakServer.closeServer();
            });
    }

    @Test
    void loadPublicKeyFromConfig(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        // given
        final String validToken = TestBearerOnlyJWTProvider.signToken(VALID_PAYLOAD_TEMPLATE);

        final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
        final List<String> expectedAudience = List.of("test-audience");

        final List<PublicKeyOptions> publicKeys = List.of(
            PublicKeyOptions.builder()
                .withKey(
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuFJ0A754CTB9+mhomn9Z1aVCiSliTm7Mow3PkWko7PCRVshrqqJEHNg6fgl4KNH+u0ZBjq4L5AKtTuwhsx2vIcJ8aJ3mQNdyxFU02nLaNzOVm+rOwytUPflAnYIgqinmiFpqyQ8vwj/L82F5kN5hnB+G2heMXSep4uoq++2ogdyLtRi4CCr2tuFdPMcdvozsafRJjgJrmKkGggoembuIN5mvuJ/YySMmE3F+TxXOVbhZqAuH4A2+9l0d1rbjghJnv9xCS8Tc7apusoK0q8jWyBHp6p12m1IFkrKSSRiXXCmoMIQO8ZTCzpyqCQEgOXHKvxvSPRWsSa4GZWHzH3hvRQIDAQAB")
                .build());

        final BearerOnlyMiddlewareOptions bearerOnlyConfig = BearerOnlyMiddlewareOptions.builder()
            .withPublicKeys(publicKeys)
            .withIssuer(expectedIssuer)
            .withAudience(expectedAudience)
            .build();

        uniportGateway(vertx, testCtx)
            .withBearerOnlyMiddleware(bearerOnlyConfig)
            .build().start()
            // when
            .incomingRequest(GET, "/",
                new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(validToken)),
                (resp) -> {
                    // then
                    assertEquals(200, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });

    }

    private JWTAuth jwtAuth(Vertx vertx, String expectedIssuer, List<String> expectedAudience) {
        return jwtAuthWithAdditionalIssuers(vertx, expectedIssuer, expectedAudience, new JsonArray());
    }

    private JWTAuth jwtAuthWithAdditionalIssuers(Vertx vertx, String expectedIssuer, List<String> expectedAudience, JsonArray additionalIssuers) {
        String publicKeyRS256 = null;
        try {
            publicKeyRS256 = Resources.toString(Resources.getResource(PUBLIC_KEY_PATH), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return JWTAuthMultipleIssuersProvider.create(vertx,
            new JWTAuthOptions()
                .addPubSecKey(new PubSecKeyOptions().setAlgorithm(PUBLIC_KEY_ALGORITHM).setBuffer(publicKeyRS256))
                .setJWTOptions(new JWTOptions().setIssuer(expectedIssuer).setAudience(expectedAudience)),
            new JWTAuthMultipleIssuersOptions().setAdditionalIssuers(additionalIssuers));
    }

    private String bearer(String value) {
        return "Bearer " + value;
    }
}
