package com.inventage.portal.gateway.proxy.middleware.matomo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayStyle;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = MatomoMiddlewareOptions.Builder.class)
public abstract class AbstractMatomoMiddlewareOptions implements GatewayMiddlewareOptions {

    @Default
    @JsonProperty(MatomoMiddlewareFactory.MATOMO_JWT_PATH_USERNAME)
    public String getJWTPathUsername() {
        return MatomoMiddlewareFactory.DEFAULT_JWT_PATH_USERNAME;
    }

    @Default
    @JsonProperty(MatomoMiddlewareFactory.MATOMO_JWT_PATH_EMAIL)
    public String getJWTPathEMail() {
        return MatomoMiddlewareFactory.DEFAULT_JWT_PATH_EMAIL;
    }

    @Default
    @JsonProperty(MatomoMiddlewareFactory.MATOMO_JWT_PATH_ROLES)
    public String getJWTPathRoles() {
        return MatomoMiddlewareFactory.DEFAULT_JWT_PATH_ROLES;
    }

    @Default
    @JsonProperty(MatomoMiddlewareFactory.MATOMO_JWT_PATH_GROUP)
    public String getJWTPathGroup() {
        return MatomoMiddlewareFactory.DEFAULT_JWT_PATH_GROUP;
    }
}
