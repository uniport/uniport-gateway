package com.inventage.portal.gateway.proxy.middleware.matomo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatomoMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(MatomoMiddlewareFactory.MATOMO_JWT_PATH_USERNAME)
    private String jwtPathUsername;

    @JsonProperty(MatomoMiddlewareFactory.MATOMO_JWT_PATH_EMAIL)
    private String jwtPathEMail;

    @JsonProperty(MatomoMiddlewareFactory.MATOMO_JWT_PATH_ROLES)
    private String jwtPathRoles;

    @JsonProperty(MatomoMiddlewareFactory.MATOMO_JWT_PATH_GROUP)
    private String jwtPathGroup;

    public MatomoMiddlewareOptions() {
    }

    public String getJWTPathUsername() {
        return jwtPathUsername;
    }

    public String getJWTPathEMail() {
        return jwtPathEMail;
    }

    public String getJWTPathRoles() {
        return jwtPathRoles;
    }

    public String getJWTPathGroup() {
        return jwtPathGroup;
    }

    @Override
    public MatomoMiddlewareOptions clone() {
        try {
            return (MatomoMiddlewareOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
