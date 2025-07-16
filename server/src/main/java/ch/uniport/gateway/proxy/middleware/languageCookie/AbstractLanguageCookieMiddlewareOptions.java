package ch.uniport.gateway.proxy.middleware.languageCookie;

import ch.uniport.gateway.core.config.model.ModelStyle;
import ch.uniport.gateway.proxy.config.model.MiddlewareOptionsModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@ModelStyle
@JsonDeserialize(builder = LanguageCookieMiddlewareOptions.Builder.class)
public abstract class AbstractLanguageCookieMiddlewareOptions implements MiddlewareOptionsModel {

    public static final String DEFAULT_LANGUAGE_COOKIE_NAME = "uniport.language";

    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageCookieMiddlewareOptions.class);

    @Default
    @JsonProperty(LanguageCookieMiddlewareFactory.LANGUAGE_COOKIE_NAME)
    public String getCookieName() {
        logDefault(LOGGER, LanguageCookieMiddlewareFactory.LANGUAGE_COOKIE_NAME, DEFAULT_LANGUAGE_COOKIE_NAME);
        return DEFAULT_LANGUAGE_COOKIE_NAME;
    }

}
