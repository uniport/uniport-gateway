package ch.uniport.gateway.proxy.middleware.matomo;

import ch.uniport.gateway.proxy.middleware.MiddlewareOptionsModel;
import ch.uniport.gateway.proxy.middleware.ModelStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@ModelStyle
@JsonDeserialize(builder = MatomoMiddlewareOptions.Builder.class)
public abstract class AbstractMatomoMiddlewareOptions implements MiddlewareOptionsModel {

    public static final String DEFAULT_JWT_PATH_USERNAME = "$.preferred_username";
    public static final String DEFAULT_JWT_PATH_EMAIL = "$.email";
    public static final String DEFAULT_JWT_PATH_ROLES = "$.resource_access.Analytics.roles";
    public static final String DEFAULT_JWT_PATH_GROUP = "$.tenant";
    private static final Logger LOGGER = LoggerFactory.getLogger(MatomoMiddlewareOptions.class);

    @Default
    @JsonProperty(MatomoMiddlewareFactory.JWT_PATH_USERNAME)
    public String getJWTPathUsername() {
        logDefault(LOGGER, MatomoMiddlewareFactory.JWT_PATH_USERNAME, DEFAULT_JWT_PATH_USERNAME);
        return DEFAULT_JWT_PATH_USERNAME;
    }

    @Default
    @JsonProperty(MatomoMiddlewareFactory.JWT_PATH_EMAIL)
    public String getJWTPathEMail() {
        logDefault(LOGGER, MatomoMiddlewareFactory.JWT_PATH_EMAIL, DEFAULT_JWT_PATH_EMAIL);
        return DEFAULT_JWT_PATH_EMAIL;
    }

    @Default
    @JsonProperty(MatomoMiddlewareFactory.JWT_PATH_ROLES)
    public String getJWTPathRoles() {
        logDefault(LOGGER, MatomoMiddlewareFactory.JWT_PATH_ROLES, DEFAULT_JWT_PATH_ROLES);
        return DEFAULT_JWT_PATH_ROLES;
    }

    @Default
    @JsonProperty(MatomoMiddlewareFactory.JWT_PATH_GROUP)
    public String getJWTPathGroup() {
        logDefault(LOGGER, MatomoMiddlewareFactory.JWT_PATH_GROUP, DEFAULT_JWT_PATH_GROUP);
        return DEFAULT_JWT_PATH_GROUP;
    }
}
