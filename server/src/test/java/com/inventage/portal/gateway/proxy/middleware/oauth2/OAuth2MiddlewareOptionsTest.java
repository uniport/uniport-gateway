package com.inventage.portal.gateway.proxy.middleware.oauth2;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class OAuth2MiddlewareOptionsTest {

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
        final String additionalScope = "aAdditionalScope";
        final String additionalParameterName = "aAdditionalParameterName";
        final String additionalParameterValue = "aAdditionalParameterValue";
        final String passthroughParameter = "aPassthroughParameter";

        final JsonObject json = JsonObject.of(
            OAuth2MiddlewareFactory.OAUTH2_CLIENT_ID, clientID,
            OAuth2MiddlewareFactory.OAUTH2_CLIENT_SECRET, clientSecret,
            OAuth2MiddlewareFactory.OAUTH2_DISCOVERY_URL, discoveryURL,
            OAuth2MiddlewareFactory.OAUTH2_SESSION_SCOPE, sessionScope,
            OAuth2MiddlewareFactory.OAUTH2_RESPONSE_MODE, responseMode,
            OAuth2MiddlewareFactory.OAUTH2_PROXY_AUTHENTICATION_FLOW, proxyAuthenticationFlow,
            OAuth2MiddlewareFactory.OAUTH2_PUBLIC_URL, publicURL,
            OAuth2MiddlewareFactory.OAUTH2_ADDITIONAL_SCOPES, List.of(additionalScope),
            OAuth2MiddlewareFactory.OAUTH2_ADDITIONAL_PARAMETERS, Map.of(
                additionalParameterName, additionalParameterValue),
            OAuth2MiddlewareFactory.OAUTH2_PASSTHROUGH_PARAMETERS, List.of(passthroughParameter));

        // when
        final ThrowingSupplier<OAuth2MiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), OAuth2MiddlewareOptions.class);

        // then
        final OAuth2MiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);
        assertEquals(clientID, options.getClientId());
        assertEquals(clientSecret, options.getClientSecret());
        assertEquals(discoveryURL, options.getDiscoveryURL());
        assertEquals(sessionScope, options.getSessionScope());
        assertEquals(responseMode, options.getResponseMode());
        assertEquals(proxyAuthenticationFlow, options.proxyAuthenticationFlow());
        assertEquals(publicURL, options.getPublicURL());

        assertNotNull(options.getAdditionalScopes());
        assertEquals(additionalScope, options.getAdditionalScopes().get(0));

        assertNotNull(options.getAdditionalAuthRequestParameters());
        assertTrue(options.getAdditionalAuthRequestParameters().containsKey(additionalParameterName));
        assertEquals(additionalParameterValue, options.getAdditionalAuthRequestParameters().get(additionalParameterName));

        assertNotNull(options.getPassthroughParameters());
        assertEquals(passthroughParameter, options.getPassthroughParameters().get(0));
    }
}
