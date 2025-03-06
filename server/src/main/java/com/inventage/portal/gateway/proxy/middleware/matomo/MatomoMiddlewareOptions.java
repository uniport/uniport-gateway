package com.inventage.portal.gateway.proxy.middleware.matomo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = MatomoMiddlewareOptions.Builder.class)
public final class MatomoMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(MatomoMiddlewareFactory.MATOMO_JWT_PATH_USERNAME)
    private String jwtPathUsername;

    @JsonProperty(MatomoMiddlewareFactory.MATOMO_JWT_PATH_EMAIL)
    private String jwtPathEMail;

    @JsonProperty(MatomoMiddlewareFactory.MATOMO_JWT_PATH_ROLES)
    private String jwtPathRoles;

    @JsonProperty(MatomoMiddlewareFactory.MATOMO_JWT_PATH_GROUP)
    private String jwtPathGroup;

    public static Builder builder() {
        return new Builder();
    }

    private MatomoMiddlewareOptions(Builder builder) {
        this.jwtPathUsername = builder.jwtPathUsername;
        this.jwtPathEMail = builder.jwtPathEMail;
        this.jwtPathRoles = builder.jwtPathRoles;
        this.jwtPathGroup = builder.jwtPathGroup;
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

    @JsonPOJOBuilder
    public static final class Builder {
        private String jwtPathUsername;
        private String jwtPathEMail;
        private String jwtPathRoles;
        private String jwtPathGroup;

        public Builder withJWTPathUsername(String jwtPathUsername) {
            this.jwtPathUsername = jwtPathUsername;
            return this;
        }

        public Builder withJWTPathEMail(String jwtPathEMail) {
            this.jwtPathEMail = jwtPathEMail;
            return this;
        }

        public Builder withJWTPathRoles(String jwtPathRoles) {
            this.jwtPathRoles = jwtPathRoles;
            return this;
        }

        public Builder withJWTPathGroup(String jwtPathGroup) {
            this.jwtPathGroup = jwtPathGroup;
            return this;
        }

        public MatomoMiddlewareOptions build() {
            return new MatomoMiddlewareOptions(this);
        }
    }
}
