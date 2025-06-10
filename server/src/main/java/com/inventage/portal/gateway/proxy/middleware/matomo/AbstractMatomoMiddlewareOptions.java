package com.inventage.portal.gateway.proxy.middleware.matomo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.model.GatewayStyle;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = MatomoMiddlewareOptions.Builder.class)
public abstract class AbstractMatomoMiddlewareOptions implements GatewayMiddlewareOptions {
    private static final Logger LOGGER = LoggerFactory.getLogger(MatomoMiddlewareOptions.class);

    @Default
    @JsonProperty(MatomoMiddlewareFactory.JWT_PATH_USERNAME)
    public String getJWTPathUsername() {
        logDefault(LOGGER, MatomoMiddlewareFactory.JWT_PATH_USERNAME, MatomoMiddlewareFactory.DEFAULT_JWT_PATH_USERNAME);
        return MatomoMiddlewareFactory.DEFAULT_JWT_PATH_USERNAME;
    }

    @Default
    @JsonProperty(MatomoMiddlewareFactory.JWT_PATH_EMAIL)
    public String getJWTPathEMail() {
        logDefault(LOGGER, MatomoMiddlewareFactory.JWT_PATH_EMAIL, MatomoMiddlewareFactory.DEFAULT_JWT_PATH_EMAIL);
        return MatomoMiddlewareFactory.DEFAULT_JWT_PATH_EMAIL;
    }

    @Default
    @JsonProperty(MatomoMiddlewareFactory.JWT_PATH_ROLES)
    public String getJWTPathRoles() {
        logDefault(LOGGER, MatomoMiddlewareFactory.JWT_PATH_ROLES, MatomoMiddlewareFactory.DEFAULT_JWT_PATH_ROLES);
        return MatomoMiddlewareFactory.DEFAULT_JWT_PATH_ROLES;
    }

    @Default
    @JsonProperty(MatomoMiddlewareFactory.JWT_PATH_GROUP)
    public String getJWTPathGroup() {
        logDefault(LOGGER, MatomoMiddlewareFactory.JWT_PATH_GROUP, MatomoMiddlewareFactory.DEFAULT_JWT_PATH_GROUP);
        return MatomoMiddlewareFactory.DEFAULT_JWT_PATH_GROUP;
    }
}
