package ch.uniport.gateway.proxy.middleware.authorization.bearerOnly;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.uniport.gateway.proxy.middleware.authorization.ClaimOptions;
import ch.uniport.gateway.proxy.middleware.authorization.PublicKeyOptions;
import ch.uniport.gateway.proxy.middleware.authorization.ReconciliationOptions;
import ch.uniport.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareFactoryBase;
import ch.uniport.gateway.proxy.middleware.authorization.bearerOnly.customClaimsChecker.JWTClaimOperator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class BearerOnlyMiddlewareOptionsTest {

    static Stream<Arguments> validOptionsValues() {
        return Stream.of(
            Arguments.of(new Options(List.of("anAudience"), "anIssuer", "aPublicKey", "aPublicKeyAlgorithm", List.of("anotherIssuer"), JWTClaimOperator.CONTAINS, "aPath", "aValue", true, 1234, true)),
            Arguments.of(new Options(List.of("anAudience"), "anIssuer", "aPublicKey", "aPublicKeyAlgorithm", List.of(), null, null, null, null, null, true))

        );
    }

    @ParameterizedTest
    @MethodSource("validOptionsValues")
    public void shouldCreateFromBuilder(Options given) {
        final List<PublicKeyOptions> publicKeys = List.of(
            PublicKeyOptions.builder()
                .withKey(given.publicKey)
                .withAlgorithm(given.publicKeyAlgorithm)
                .build());

        final BearerOnlyMiddlewareOptions.Builder builder = BearerOnlyMiddlewareOptions.builder()
            .withAudience(given.audience)
            .withIssuer(given.issuer)
            .withPublicKeys(publicKeys)
            .withOptional(given.optional);

        if (given.additionalIssuers != null) {
            builder.withAdditionalIssuers(given.additionalIssuers);
        }

        if (given.claimOperator != null || given.claimValue != null || given.claimPath != null) {
            builder.withClaims(
                List.of(
                    ClaimOptions.builder()
                        .withOperator(given.claimOperator)
                        .withPath(given.claimPath)
                        .withValue(given.claimValue)
                        .build()));
        }

        if (given.reconciliationEnabled != null || given.reconciliationIntervalMs != null) {
            builder.withReconciliation(
                ReconciliationOptions.builder()
                    .withEnabled(given.reconciliationEnabled)
                    .withIntervalMs(given.reconciliationIntervalMs)
                    .build());
        }

        final BearerOnlyMiddlewareOptions actual = builder.build();

        assertCorrectness(actual, given);
    }

    @ParameterizedTest
    @MethodSource("validOptionsValues")
    public void shouldCreateFromJson(Options given) {
        final JsonArray publicKeys = JsonArray.of(
            JsonObject.of(
                WithAuthHandlerMiddlewareFactoryBase.PUBLIC_KEY, given.publicKey,
                WithAuthHandlerMiddlewareFactoryBase.PUBLIC_KEY_ALGORITHM, given.publicKeyAlgorithm));

        final JsonObject json = new JsonObject(JsonObject.of(
            WithAuthHandlerMiddlewareFactoryBase.AUDIENCE, given.audience,
            WithAuthHandlerMiddlewareFactoryBase.ISSUER, given.issuer,
            WithAuthHandlerMiddlewareFactoryBase.PUBLIC_KEYS, publicKeys,
            WithAuthHandlerMiddlewareFactoryBase.ADDITIONAL_ISSUERS, given.additionalIssuers,
            BearerOnlyMiddlewareFactory.OPTIONAL, given.optional).getMap());

        if (given.claimOperator != null || given.claimValue != null || given.claimPath != null) {
            final JsonArray claims = JsonArray.of(
                JsonObject.of(
                    WithAuthHandlerMiddlewareFactoryBase.CLAIM_OPERATOR, given.claimOperator,
                    WithAuthHandlerMiddlewareFactoryBase.CLAIM_PATH, given.claimPath,
                    WithAuthHandlerMiddlewareFactoryBase.CLAIM_VALUE, given.claimValue));
            json.put(WithAuthHandlerMiddlewareFactoryBase.CLAIMS, claims);
        }

        if (given.reconciliationEnabled != null || given.reconciliationIntervalMs != null) {
            final JsonObject reconciliation = JsonObject.of(
                WithAuthHandlerMiddlewareFactoryBase.RECONCILIATION_ENABLED, given.reconciliationEnabled,
                WithAuthHandlerMiddlewareFactoryBase.RECONCILIATION_INTERVAL_MS, given.reconciliationIntervalMs);
            json.put(WithAuthHandlerMiddlewareFactoryBase.PUBLIC_KEYS_RECONCILIATION, reconciliation);
        }

        // when
        final ThrowingSupplier<BearerOnlyMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), BearerOnlyMiddlewareOptions.class);

        // then
        final BearerOnlyMiddlewareOptions actual = assertDoesNotThrow(parse);
        assertCorrectness(actual, given);
    }

    static void assertCorrectness(BearerOnlyMiddlewareOptions options, Options expected) {
        assertNotNull(options, "options");
        assertEquals(expected.audience, options.getAudience(), "audience");
        assertEquals(expected.issuer, options.getIssuer(), "issuer");

        assertNotNull(options.getPublicKeys(), "public keys");
        assertNotNull(options.getPublicKeys().get(0), "public key");
        assertEquals(expected.publicKey, options.getPublicKeys().get(0).getKey(), "key");
        assertEquals(expected.publicKeyAlgorithm, options.getPublicKeys().get(0).getAlgorithm(), "algorithm");

        assertEquals(expected.additionalIssuers, options.getAdditionalIssuers(), "additional issuer");

        if (expected.claimOperator != null || expected.claimValue != null || expected.claimPath != null) {
            assertNotNull(options.getClaims(), "claims");
            assertNotNull(options.getClaims().get(0), "claim");
            assertEquals(expected.claimOperator, options.getClaims().get(0).getOperator(), "oeprator");
            assertEquals(expected.claimPath, options.getClaims().get(0).getPath(), "path");
            assertEquals(expected.claimValue, options.getClaims().get(0).getValue(), "value");
        }

        if (expected.reconciliationEnabled != null || expected.reconciliationIntervalMs != null) {
            assertNotNull(options.getReconciliation(), "reconciliation");
            assertEquals(expected.reconciliationEnabled, options.getReconciliation().isEnabled(), "enabled");
            assertEquals(expected.reconciliationIntervalMs.intValue(), options.getReconciliation().getIntervalMs(), "interval");
        }

        assertEquals(expected.optional, options.isOptional(), "optional");
    }

    static class Options {
        List<String> audience;
        String issuer;
        String publicKey;
        String publicKeyAlgorithm;
        List<String> additionalIssuers;
        JWTClaimOperator claimOperator;
        String claimPath;
        String claimValue;
        Boolean reconciliationEnabled;
        Integer reconciliationIntervalMs;
        boolean optional;

        Options(
            List<String> audience,
            String issuer,
            String publicKey,
            String publicKeyAlgorithm,
            List<String> additionalIssuers,
            JWTClaimOperator claimOperator,
            String claimPath,
            String claimValue,
            Boolean reconciliationEnabled,
            Integer reconciliationIntervalMs,
            boolean optional
        ) {
            this.audience = audience;
            this.issuer = issuer;
            this.publicKey = publicKey;
            this.publicKeyAlgorithm = publicKeyAlgorithm;
            this.additionalIssuers = additionalIssuers;
            this.claimOperator = claimOperator;
            this.claimPath = claimPath;
            this.claimValue = claimValue;
            this.reconciliationEnabled = reconciliationEnabled;
            this.reconciliationIntervalMs = reconciliationIntervalMs;
            this.optional = optional;
        }
    }

    static Stream<Arguments> invalidOptionsValues() {
        final List<String> audience = List.of("anAudience");
        final String issuer = "anIssuer";
        final PublicKeyOptions publicKey = PublicKeyOptions.builder()
            .withKey("aPublicKey")
            .withAlgorithm("aPublicKeyAlgorithm")
            .build();

        return Stream.of(
            Arguments.of(
                BearerOnlyMiddlewareOptions.builder()
                    // missing audience
                    .withIssuer(issuer)
                    .withPublicKeys(List.of(publicKey))),
            Arguments.of(
                BearerOnlyMiddlewareOptions.builder()
                    .withAudience(audience)
                    // missing issuer
                    .withPublicKeys(List.of(publicKey))),
            Arguments.of(
                BearerOnlyMiddlewareOptions.builder()
                    .withAudience(audience)
                    .withIssuer(issuer)
            // missing public key
            )

        );
    }

    @ParameterizedTest
    @MethodSource("invalidOptionsValues")
    public void shouldFailIfRequiredPropertiesAreMissing(BearerOnlyMiddlewareOptions.Builder builder) {
        assertThrows(IllegalStateException.class, () -> builder.build());
    }
}
