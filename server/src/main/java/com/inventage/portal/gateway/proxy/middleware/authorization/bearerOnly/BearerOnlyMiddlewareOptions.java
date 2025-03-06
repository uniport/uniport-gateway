package com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareOptionsBase;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = BearerOnlyMiddlewareOptions.Builder.class)
public final class BearerOnlyMiddlewareOptions extends WithAuthHandlerMiddlewareOptionsBase {

    @JsonProperty(BearerOnlyMiddlewareFactory.BEARER_ONLY_OPTIONAL)
    private Boolean optional;

    public static Builder builder() {
        return new Builder();
    }

    private BearerOnlyMiddlewareOptions(Builder builder) {
        super(builder);
        this.optional = builder.optional;
    }

    public Boolean isOptional() {
        return optional;
    }

    @Override
    public BearerOnlyMiddlewareOptions clone() {
        final BearerOnlyMiddlewareOptions options = (BearerOnlyMiddlewareOptions) super.clone();
        return options;
    }

    @JsonPOJOBuilder
    public static final class Builder extends BaseBuilder<Builder> {

        private Boolean optional;

        public Builder withOptional(Boolean optional) {
            this.optional = optional;
            return self();
        }

        public BearerOnlyMiddlewareOptions build() {
            return new BearerOnlyMiddlewareOptions(this);

        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
