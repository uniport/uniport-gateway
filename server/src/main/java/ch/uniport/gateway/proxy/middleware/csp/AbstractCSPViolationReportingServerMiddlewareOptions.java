package ch.uniport.gateway.proxy.middleware.csp;

import ch.uniport.gateway.proxy.middleware.MiddlewareOptionsModel;
import ch.uniport.gateway.proxy.middleware.ModelStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

@Immutable
@ModelStyle
@JsonDeserialize(builder = CSPViolationReportingServerMiddlewareOptions.Builder.class)
public abstract class AbstractCSPViolationReportingServerMiddlewareOptions implements MiddlewareOptionsModel {

    // defaults
    public static final Level DEFAULT_LOG_LEVEL = Level.WARN;

    private static final Logger LOGGER = LoggerFactory.getLogger(CSPViolationReportingServerMiddlewareOptions.class);

    @Default
    @JsonProperty(CSPViolationReportingServerMiddlewareFactory.LOG_LEVEL)
    public Level getLogLevel() {
        logDefault(LOGGER, CSPViolationReportingServerMiddlewareFactory.LOG_LEVEL, DEFAULT_LOG_LEVEL.toString());
        return DEFAULT_LOG_LEVEL;
    }

}
