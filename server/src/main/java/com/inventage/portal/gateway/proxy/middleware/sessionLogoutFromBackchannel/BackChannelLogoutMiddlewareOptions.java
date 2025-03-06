package com.inventage.portal.gateway.proxy.middleware.sessionLogoutFromBackchannel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.middleware.authorization.WithAuthHandlerMiddlewareOptionsBase;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = BackChannelLogoutMiddlewareOptions.Builder.class)
public final class BackChannelLogoutMiddlewareOptions extends WithAuthHandlerMiddlewareOptionsBase {

    public static Builder builder() {
        return new Builder();
    }

    private BackChannelLogoutMiddlewareOptions(Builder builder) {
        super(builder);
    }

    @Override
    public BackChannelLogoutMiddlewareOptions clone() {
        return (BackChannelLogoutMiddlewareOptions) super.clone();
    }

    @JsonPOJOBuilder
    public static final class Builder extends BaseBuilder<Builder> {
        public BackChannelLogoutMiddlewareOptions build() {
            return new BackChannelLogoutMiddlewareOptions(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
