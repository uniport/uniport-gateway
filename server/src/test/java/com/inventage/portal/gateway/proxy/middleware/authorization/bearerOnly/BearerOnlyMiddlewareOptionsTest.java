package com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareFactoryBase;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareOptionsBase;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareOptionsBase.ClaimOptions;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareOptionsBase.PublicKeyOptions;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareOptionsBase.ReconciliationOptions;
import com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.BearerOnlyMiddlewareOptions.Builder;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class BearerOnlyMiddlewareOptionsTest {

    static Stream<Arguments> validOptionsValues() {
        return Stream.of(
            Arguments.of(new Options(List.of("anAudience"), "anIssuer", "aPublicKey", "aPublicKeyAlgorithm", List.of("anotherIssuer"), "anOperator", "aPath", "aValue", true, 1234, true)),
            Arguments.of(new Options(List.of("anAudience"), "anIssuer", "aPublicKey", "aPublicKeyAlgorithm", null, null, null, null, null, null, true))

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

        List<ClaimOptions> claims = null;
        if (given.claimOperator != null || given.claimValue != null || given.claimPath != null) {
            claims = List.of(
                ClaimOptions.builder()
                    .withOperator(given.claimOperator)
                    .withPath(given.claimPath)
                    .withValue(given.claimValue)
                    .build());
        }

        ReconciliationOptions reconciliation = null;
        if (given.reconciliationEnabled != null || given.reconciliationIntervalMs != null) {
            reconciliation = ReconciliationOptions.builder()
                .withEnabled(given.reconciliationEnabled)
                .withIntervalMs(given.reconciliationIntervalMs)
                .build();
        }

        final BearerOnlyMiddlewareOptions actual = BearerOnlyMiddlewareOptions.builder()
            .withAudience(given.audience)
            .withIssuer(given.issuer)
            .withPublicKeys(publicKeys)
            .withAdditionalIssuers(given.additionalIssuers)
            .withClaims(claims)
            .withReconciliation(reconciliation)
            .withOptional(given.optional)
            .build();

        assertCorrectness(actual, given);
    }

    @ParameterizedTest
    @MethodSource("validOptionsValues")
    public void shouldCreateFromJson(Options given) {
        final JsonArray publicKeys = JsonArray.of(
            JsonObject.of(
                WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEY, given.publicKey,
                WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEY_ALGORITHM, given.publicKeyAlgorithm));

        JsonArray claims = null;
        if (given.claimOperator != null || given.claimValue != null || given.claimPath != null) {
            claims = JsonArray.of(
                JsonObject.of(
                    WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_CLAIM_OPERATOR, given.claimOperator,
                    WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_CLAIM_PATH, given.claimPath,
                    WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_CLAIM_VALUE, given.claimValue));
        }

        JsonObject reconciliation = null;
        if (given.reconciliationEnabled != null || given.reconciliationIntervalMs != null) {
            reconciliation = JsonObject.of(
                WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS_RECONCILIATION_ENABLED, given.reconciliationEnabled,
                WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS_RECONCILIATION_INTERVAL_MS, given.reconciliationIntervalMs)

            ;
        }

        final JsonObject json = JsonObject.of(
            WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_AUDIENCE, given.audience,
            WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_ISSUER, given.issuer,
            WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS, publicKeys,
            WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_ADDITIONAL_ISSUERS, given.additionalIssuers,
            WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_CLAIMS, claims,
            WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS_RECONCILIATION, reconciliation,
            BearerOnlyMiddlewareFactory.BEARER_ONLY_OPTIONAL, given.optional);

        // when
        final ThrowingSupplier<BearerOnlyMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), BearerOnlyMiddlewareOptions.class);

        // then
        final BearerOnlyMiddlewareOptions actual = assertDoesNotThrow(parse);
        assertCorrectness(actual, given);
    }

    static void assertCorrectness(BearerOnlyMiddlewareOptions options, Options expected) {
        assertNotNull(options);
        assertEquals(expected.audience, options.getAudience());
        assertEquals(expected.issuer, options.getIssuer());

        assertNotNull(options.getPublicKeys());
        assertNotNull(options.getPublicKeys().get(0));
        assertEquals(expected.publicKey, options.getPublicKeys().get(0).getKey());
        assertEquals(expected.publicKeyAlgorithm, options.getPublicKeys().get(0).getAlgorithm());

        assertEquals(expected.additionalIssuers, options.getAdditionalIssuers());

        if (expected.claimOperator != null || expected.claimValue != null || expected.claimPath != null) {
            assertNotNull(options.getClaims());
            assertNotNull(options.getClaims().get(0));
            assertEquals(expected.claimOperator, options.getClaims().get(0).getOperator());
            assertEquals(expected.claimPath, options.getClaims().get(0).getPath());
            assertEquals(expected.claimValue, options.getClaims().get(0).getValue());
        }

        if (expected.reconciliationEnabled != null || expected.reconciliationIntervalMs != null) {
            assertNotNull(options.getReconciliation());
            assertEquals(expected.reconciliationEnabled, options.getReconciliation().isEnabled());
            assertEquals(expected.reconciliationIntervalMs, options.getReconciliation().getIntervalMs());
        }

        assertEquals(expected.optional, options.isOptional());
    }

    static class Options {
        List<String> audience;
        String issuer;
        String publicKey;
        String publicKeyAlgorithm;
        List<String> additionalIssuers;
        String claimOperator;
        String claimPath;
        String claimValue;
        Boolean reconciliationEnabled;
        Integer reconciliationIntervalMs;
        Boolean optional;

        Options(
            List<String> audience, String issuer, String publicKey, String publicKeyAlgorithm, List<String> additionalIssuers, String claimOperator, String claimPath, String claimValue, Boolean reconciliationEnabled,
            Integer reconciliationIntervalMs, Boolean optional
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
        final PublicKeyOptions publicKey = WithAuthHandlerMiddlewareOptionsBase.PublicKeyOptions.builder()
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
    public void shouldFailIfRequiredPropertiesAreMissing(Builder builder) {
        assertThrows(IllegalArgumentException.class, () -> builder.build());
    }

    @Test
    public void shouldDeepCopy() {
        // given
        final String audience = "anAudience";
        final String issuer = "anIssuer";
        final PublicKeyOptions publicKey = WithAuthHandlerMiddlewareOptionsBase.PublicKeyOptions.builder()
            .withKey("aPublicKey")
            .withAlgorithm("aPublicKeyAlgorithm")
            .build();

        final BearerOnlyMiddlewareOptions options = BearerOnlyMiddlewareOptions.builder()
            .withAudience(Arrays.asList(audience))
            .withIssuer(issuer)
            .withPublicKeys(Arrays.asList(publicKey))
            .build();

        // when
        final BearerOnlyMiddlewareOptions copy = options.clone();
        // then
        assertThrows(UnsupportedOperationException.class, () -> options.getAudience().add("another"));
        assertThrows(UnsupportedOperationException.class, () -> options.getPublicKeys().add(null));
    }
}
