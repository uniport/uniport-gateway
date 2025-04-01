package com.inventage.portal.gateway.proxy.middleware.oauth2;

import java.net.URI;

/**
 * Factory for patched {@link OAuth2Middleware}.
 *
 * OAuth2 middleware, which sends the user directly to the self registration.
 */
public class OAuth2RegistrationMiddlewareFactory extends OAuth2MiddlewareFactory {

    // schema
    public static final String TYPE = "oauth2registration";
    // same props as "oauth2"

    private static final String AUTH_ENDPOINT = "/protocol/openid-connect/auth";
    private static final String REGISTRATION_ENDPOINT = "/protocol/openid-connect/registrations";

    @Override
    public String provides() {
        return TYPE;
    }

    @Override
    public Class<? extends AbstractOAuth2MiddlewareOptionsBase> modelType() {
        return OAuth2RegistrationMiddlewareOptions.class;
    }

    /**
     * Replaces the {@link #AUTH_ENDPOINT} substring with {@link #REGISTRATION_ENDPOINT}.
     *
     * @param publicUrl
     *            the base url
     * @param keycloakAuthorizationEndpoint
     *            auth endpoint containing the patch to be patched.
     * @return the patched URL.
     */
    @Override
    protected String patchPath(String publicUrl, URI keycloakAuthorizationEndpoint) {
        final String registrationPath = keycloakAuthorizationEndpoint.getPath().replace(AUTH_ENDPOINT, REGISTRATION_ENDPOINT);
        return String.format("%s%s", publicUrl, registrationPath);
    }

}
