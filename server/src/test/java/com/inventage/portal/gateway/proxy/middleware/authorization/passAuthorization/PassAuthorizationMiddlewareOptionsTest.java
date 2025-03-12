package com.inventage.portal.gateway.proxy.middleware.authorization.passAuthorization;

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

public class PassAuthorizationMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final List<String> audience = List.of("anAudience");
        final String issuer = "anIssuer";
        final String publicKey = "aPublicKey";
        final String publicKeyAlgorithm = "aPublicKeyAlgorithm";
        final List<String> additionalIssuers = List.of("anotherIssuer");
        final JWTClaimOperator claimOperator = JWTClaimOperator.EQUALS;
        final String claimPath = "aPath";
        final String claimValue = "aValue";
        final Boolean reconciliationEnabled = true;
        final Integer reconciliationIntervalMs = 1234;
        final String sessionScope = "aSessionScope";

        final JsonObject json = JsonObject.of(
            WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_AUDIENCE, audience,
            WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_ISSUER, issuer,
            WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS, JsonArray.of(
                JsonObject.of(
                    WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEY, publicKey,
                    WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEY_ALGORITHM, publicKeyAlgorithm)),
            WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_ADDITIONAL_ISSUERS, additionalIssuers,
            WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_CLAIMS, JsonArray.of(
                JsonObject.of(
                    WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_CLAIM_OPERATOR, claimOperator,
                    WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_CLAIM_PATH, claimPath,
                    WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_CLAIM_VALUE, claimValue)),
            WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS_RECONCILIATION, JsonObject.of(
                WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS_RECONCILIATION_ENABLED, reconciliationEnabled,
                WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS_RECONCILIATION_INTERVAL_MS, reconciliationIntervalMs),
            PassAuthorizationMiddlewareFactory.PASS_AUTHORIZATION_SESSION_SCOPE, sessionScope);

        // when
        final ThrowingSupplier<PassAuthorizationMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), PassAuthorizationMiddlewareOptions.class);

        // then
        final PassAuthorizationMiddlewareOptions options = assertDoesNotThrow(parse);
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

        assertEquals(sessionScope, options.getSessionScope());
    }
}
