package com.inventage.portal.gateway.proxy.middleware.sessionLogoutFromBackchannel;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareFactoryBase;
import com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.customClaimsChecker.JWTClaimOperator;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class BackChannelLogoutMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final List<String> audience = List.of("anAudience");
        final String issuer = "anIssuer";
        final String publicKey = "aPublicKey";
        final String publicKeyAlgorithm = "aPublicKeyAlgorithm";
        final List<String> additionalIssuers = List.of("anotherIssuer");
        final JWTClaimOperator claimOperator = JWTClaimOperator.CONTAINS;
        final String claimPath = "aPath";
        final String claimValue = "aValue";
        final Boolean reconciliationEnabled = true;
        final Integer reconciliationIntervalMs = 1234;

        final JsonObject json = JsonObject.of(
            WithAuthHandlerMiddlewareFactoryBase.AUDIENCE, audience,
            WithAuthHandlerMiddlewareFactoryBase.ISSUER, issuer,
            WithAuthHandlerMiddlewareFactoryBase.PUBLIC_KEYS, JsonArray.of(
                JsonObject.of(
                    WithAuthHandlerMiddlewareFactoryBase.PUBLIC_KEY, publicKey,
                    WithAuthHandlerMiddlewareFactoryBase.PUBLIC_KEY_ALGORITHM, publicKeyAlgorithm)),
            WithAuthHandlerMiddlewareFactoryBase.ADDITIONAL_ISSUERS, additionalIssuers,
            WithAuthHandlerMiddlewareFactoryBase.CLAIMS, JsonArray.of(
                JsonObject.of(
                    WithAuthHandlerMiddlewareFactoryBase.CLAIM_OPERATOR, claimOperator.toString(),
                    WithAuthHandlerMiddlewareFactoryBase.CLAIM_PATH, claimPath,
                    WithAuthHandlerMiddlewareFactoryBase.CLAIM_VALUE, claimValue)),
            WithAuthHandlerMiddlewareFactoryBase.PUBLIC_KEYS_RECONCILIATION, JsonObject.of(
                WithAuthHandlerMiddlewareFactoryBase.RECONCILIATION_ENABLED, reconciliationEnabled,
                WithAuthHandlerMiddlewareFactoryBase.RECONCILIATION_INTERVAL_MS, reconciliationIntervalMs));

        // when
        final ThrowingSupplier<BackChannelLogoutMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), BackChannelLogoutMiddlewareOptions.class);

        // then
        final BackChannelLogoutMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
        assertEquals(audience, options.getAudience());
        assertEquals(issuer, options.getIssuer());

        assertNotNull(options.getPublicKeys());
        assertNotNull(options.getPublicKeys().get(0));
        assertEquals(publicKey, options.getPublicKeys().get(0).getKey());
        assertEquals(publicKeyAlgorithm, options.getPublicKeys().get(0).getAlgorithm());

        assertEquals(additionalIssuers, options.getAdditionalIssuers());

        assertNotNull(options.getClaims());
        assertNotNull(options.getClaims().get(0));
        assertEquals(claimOperator, options.getClaims().get(0).getOperator());
        assertEquals(claimPath, options.getClaims().get(0).getPath());
        assertEquals(claimValue, options.getClaims().get(0).getValue());

        assertNotNull(options.getReconciliation());
        assertEquals(reconciliationEnabled, options.getReconciliation().isEnabled());
        assertEquals(reconciliationIntervalMs.intValue(), options.getReconciliation().getIntervalMs());
    }
}
