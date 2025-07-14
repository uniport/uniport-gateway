package com.inventage.portal.gateway.proxy.middleware.oauth2;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class OAuth2RegistrationMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final String clientID = "aClientID";
        final String clientSecret = "aClientSecret";
        final String discoveryURL = "aDiscoveryURL";
        final String sessionScope = "aSessionScope";
        final String responseMode = "aResponseMode";
        final Boolean proxyAuthenticationFlow = true;
        final String publicURL = "aPublicURL";
        final String callbackOrigin = "aCallbackOrigin";
        final String additionalScope = "aAdditionalScope";
        final String additionalParameterName = "aAdditionalParameterName";
        final String additionalParameterValue = "aAdditionalParameterValue";
        final String passthroughParameter = "aPassthroughParameter";

        final JsonObject json = new JsonObject()
            .put(OAuth2MiddlewareFactory.CLIENT_ID, clientID)
            .put(OAuth2MiddlewareFactory.CLIENT_SECRET, clientSecret)
            .put(OAuth2MiddlewareFactory.DISCOVERY_URL, discoveryURL)
            .put(OAuth2MiddlewareFactory.SESSION_SCOPE, sessionScope)
            .put(OAuth2MiddlewareFactory.RESPONSE_MODE, responseMode)
            .put(OAuth2MiddlewareFactory.PROXY_AUTHENTICATION_FLOW, proxyAuthenticationFlow)
            .put(OAuth2MiddlewareFactory.PUBLIC_URL, publicURL)
            .put(OAuth2MiddlewareFactory.CALLBACK_ORIGIN, callbackOrigin)
            .put(OAuth2MiddlewareFactory.ADDITIONAL_SCOPES, List.of(additionalScope))
            .put(OAuth2MiddlewareFactory.ADDITIONAL_PARAMETERS, Map.of(additionalParameterName, additionalParameterValue))
            .put(OAuth2MiddlewareFactory.PASSTHROUGH_PARAMETERS, List.of(passthroughParameter));

        // when
        final ThrowingSupplier<OAuth2RegistrationMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), OAuth2RegistrationMiddlewareOptions.class);

        // then
        final OAuth2RegistrationMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
    }
}
