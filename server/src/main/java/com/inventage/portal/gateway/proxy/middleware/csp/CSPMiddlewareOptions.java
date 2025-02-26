package com.inventage.portal.gateway.proxy.middleware.csp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CSPMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(CSPMiddlewareFactory.CSP_DIRECTIVES)
    private List<DirectiveOptions> directives;

    @JsonProperty(CSPMiddlewareFactory.CSP_REPORT_ONLY)
    private Boolean reportOnly;

    @JsonProperty(CSPMiddlewareFactory.CSP_MERGE_STRATEGY)
    private String mergeStrategy;

    public CSPMiddlewareOptions() {
    }

    public List<DirectiveOptions> getDirectives() {
        return directives == null ? null : directives.stream().map(DirectiveOptions::clone).toList();
    }

    public Boolean isReportOnly() {
        return reportOnly;
    }

    public String getMergeStrategy() {
        return mergeStrategy;
    }

    @Override
    public CSPMiddlewareOptions clone() {
        try {
            final CSPMiddlewareOptions options = (CSPMiddlewareOptions) super.clone();
            options.directives = directives == null ? null : directives.stream().map(DirectiveOptions::clone).toList();
            return options;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public static class DirectiveOptions implements GatewayMiddlewareOptions {

        @JsonProperty(CSPMiddlewareFactory.CSP_DIRECTIVE_NAME)
        private String name;

        @JsonProperty(CSPMiddlewareFactory.CSP_DIRECTIVE_VALUES)
        private List<String> values;

        public DirectiveOptions() {
        }

        public String getName() {
            return name;
        }

        public List<String> getValues() {
            return List.copyOf(values);
        }

        @Override
        public DirectiveOptions clone() {
            try {
                final DirectiveOptions options = (DirectiveOptions) super.clone();
                options.values = values == null ? null : List.copyOf(values);
                return options;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
