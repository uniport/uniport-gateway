package com.inventage.portal.gateway.proxy.middleware.authorization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import com.inventage.portal.gateway.proxy.model.GatewayStyle;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value.Check;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

public abstract class WithAuthHandlerMiddlewareOptionsBase implements GatewayMiddlewareOptions {

    @Check
    protected void validate() {
        Preconditions.checkState(!getAudience().isEmpty(), "'getAudience' must have at least one element");
        Preconditions.checkState(!getPublicKeys().isEmpty(), "'getPublicKeys' must have at least one element");
    }

    @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_AUDIENCE)
    public abstract List<String> getAudience();

    @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_ISSUER)
    public abstract String getIssuer();

    @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS)
    public abstract List<PublicKeyOptions> getPublicKeys();

    @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_ADDITIONAL_ISSUERS)
    public abstract List<String> getAdditionalIssuers();

    @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_CLAIMS)
    public abstract List<ClaimOptions> getClaims();

    @Nullable
    @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS_RECONCILIATION)
    public abstract ReconciliationOptions getReconciliation();

    @Immutable
    @GatewayStyle
    @JsonDeserialize(builder = PublicKeyOptions.Builder.class)
    public abstract static class AbstractPublicKeyOptions implements GatewayMiddlewareOptions {

        @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEY)
        public abstract String getKey();

        @Nullable
        @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEY_ALGORITHM)
        public abstract String getAlgorithm();
    }

    @Immutable
    @GatewayStyle
    @JsonDeserialize(builder = ClaimOptions.Builder.class)
    public abstract static class AbstractClaimOptions implements GatewayMiddlewareOptions {

        @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_CLAIM_OPERATOR)
        public abstract String getOperator();

        @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_CLAIM_PATH)
        public abstract String getPath();

        @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_CLAIM_VALUE)
        public abstract Object getValue();
    }

    @Immutable
    @GatewayStyle
    @JsonDeserialize(builder = ReconciliationOptions.Builder.class)
    public abstract static class AbstractReconciliationOptions implements GatewayMiddlewareOptions {

        @Default
        @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS_RECONCILIATION_ENABLED)
        public boolean isEnabled() {
            return WithAuthHandlerMiddlewareFactoryBase.DEFAULT_RECONCILIATION_ENABLED_VALUE;
        }

        @Default
        @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS_RECONCILIATION_INTERVAL_MS)
        public long getIntervalMs() {
            return WithAuthHandlerMiddlewareFactoryBase.DEFAULT_RECONCILIATION_INTERVAL_MS;
        }
    }
}
