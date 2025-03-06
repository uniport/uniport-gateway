package com.inventage.portal.gateway.proxy.middleware.csp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = CSPMiddlewareOptions.Builder.class)
public final class CSPMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(CSPMiddlewareFactory.CSP_DIRECTIVES)
    private List<DirectiveOptions> directives;

    @JsonProperty(CSPMiddlewareFactory.CSP_REPORT_ONLY)
    private Boolean reportOnly;

    @JsonProperty(CSPMiddlewareFactory.CSP_MERGE_STRATEGY)
    private String mergeStrategy;

    public static Builder builder() {
        return new Builder();
    }

    private CSPMiddlewareOptions(Builder builder) {
        if (builder.directives == null) {
            throw new IllegalArgumentException("directives is required");
        }

        this.directives = builder.directives;
        this.reportOnly = builder.reportOnly;
        this.mergeStrategy = builder.mergeStrategy;
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

    @JsonDeserialize(builder = DirectiveOptions.Builder.class)
    public static class DirectiveOptions implements GatewayMiddlewareOptions {

        @JsonProperty(CSPMiddlewareFactory.CSP_DIRECTIVE_NAME)
        private String name;

        @JsonProperty(CSPMiddlewareFactory.CSP_DIRECTIVE_VALUES)
        private List<String> values;

        private DirectiveOptions(Builder builder) {
            if (builder.name == null) {
                throw new IllegalArgumentException("name is required");
            }
            if (builder.values == null) {
                throw new IllegalArgumentException("values are required");
            }
            this.name = builder.name;
            this.values = builder.values;
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

        @JsonPOJOBuilder
        public static final class Builder {

            private String name;
            private List<String> values;

            public Builder withName(String name) {
                this.name = name;
                return this;
            }

            public Builder withValues(List<String> values) {
                this.values = values;
                return this;
            }

            public DirectiveOptions build() {
                return new DirectiveOptions(this);
            }
        }
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private List<DirectiveOptions> directives;
        private Boolean reportOnly;
        private String mergeStrategy;

        public Builder withDirectives(List<DirectiveOptions> directives) {
            this.directives = directives;
            return this;
        }

        public Builder withReportOnly(Boolean reportOnly) {
            this.reportOnly = reportOnly;
            return this;
        }

        public Builder withMergeStrategy(String mergeStrategy) {
            this.mergeStrategy = mergeStrategy;
            return this;
        }

        public CSPMiddlewareOptions build() {
            return new CSPMiddlewareOptions(this);
        }
    }
}
