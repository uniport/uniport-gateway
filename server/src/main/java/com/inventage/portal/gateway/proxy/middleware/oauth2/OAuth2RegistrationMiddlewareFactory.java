package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import java.net.URI;

/**
 * OAuth2 middleware, which sends the user directly to the self registration.
 */
public class OAuth2RegistrationMiddlewareFactory extends OAuth2MiddlewareFactory {

    private static final String AUTH_ENDPOINT = "/protocol/openid-connect/auth";
    private static final String REGISTRATION_ENDPOINT = "/protocol/openid-connect/registrations";

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_OAUTH2_REGISTRATION;
    }

    protected String authorizationPath(String publicUrl, URI keycloakAuthorizationEndpoint) {
        final String registrationPath = keycloakAuthorizationEndpoint.getPath().replace(AUTH_ENDPOINT, REGISTRATION_ENDPOINT);
        return String.format("%s%s", publicUrl, registrationPath);
    }

}
