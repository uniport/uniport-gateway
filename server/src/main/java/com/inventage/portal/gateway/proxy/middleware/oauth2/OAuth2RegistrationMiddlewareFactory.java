package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import java.net.URI;

/**
 * Factory for patched {@link OAuth2Middleware}.
 *
 * OAuth2 middleware, which sends the user directly to the self registration.
 */
public class OAuth2RegistrationMiddlewareFactory extends OAuth2MiddlewareFactory {

    // schema
    public static final String OAUTH2_REGISTRATION = "oauth2registration";
    // same props as "oauth2"

    private static final String AUTH_ENDPOINT = "/protocol/openid-connect/auth";
    private static final String REGISTRATION_ENDPOINT = "/protocol/openid-connect/registrations";

    @Override
    public String provides() {
        return OAUTH2_REGISTRATION;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return super.optionsSchema();
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        return super.validate(options);
    }

    @Override
    public Class<? extends GatewayMiddlewareOptions> modelType() {
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
