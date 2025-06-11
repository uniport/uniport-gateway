package com.inventage.portal.gateway.proxy.middleware.csp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.inventage.portal.gateway.core.config.model.ModelStyle;
import com.inventage.portal.gateway.proxy.config.model.MiddlewareOptionsModel;
import com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP.CSPMergeStrategy;
import java.util.List;
import org.immutables.value.Value.Check;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
@ModelStyle
@JsonDeserialize(builder = CSPMiddlewareOptions.Builder.class)
public abstract class AbstractCSPMiddlewareOptions implements MiddlewareOptionsModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(CSPMiddlewareOptions.class);

    @Check
    protected void validate() {
        Preconditions.checkState(!getDirectives().isEmpty(), "'getDirectives' must have at least one element");
    }

    @JsonProperty(CSPMiddlewareFactory.DIRECTIVES)
    public abstract List<DirectiveOptions> getDirectives();

    @Default
    @JsonProperty(CSPMiddlewareFactory.REPORT_ONLY)
    public boolean isReportOnly() {
        logDefault(LOGGER, CSPMiddlewareFactory.REPORT_ONLY, CSPMiddlewareFactory.DEFAULT_REPORT_ONLY);
        return CSPMiddlewareFactory.DEFAULT_REPORT_ONLY;
    }

    @Default
    @JsonProperty(CSPMiddlewareFactory.MERGE_STRATEGY)
    public CSPMergeStrategy getMergeStrategy() {
        logDefault(LOGGER, CSPMiddlewareFactory.MERGE_STRATEGY, CSPMiddlewareFactory.DEFAULT_MERGE_STRATEGY);
        return CSPMiddlewareFactory.DEFAULT_MERGE_STRATEGY;
    }

    @Immutable
    @ModelStyle
    @JsonDeserialize(builder = DirectiveOptions.Builder.class)
    public abstract static class AbstractDirectiveOptions implements MiddlewareOptionsModel {

        @Check
        protected void validate() {
            Preconditions.checkState(!getValues().isEmpty(), "'getValues' must have at least one element");
        }

        @JsonProperty(CSPMiddlewareFactory.DIRECTIVE_NAME)
        public abstract String getName();

        @JsonProperty(CSPMiddlewareFactory.DIRECTIVE_VALUES)
        public abstract List<String> getValues();
    }
}
