package com.inventage.portal.gateway.proxy.middleware.authorization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class WithAuthHandlerMiddlewareOptionsBase implements GatewayMiddlewareOptions {

    @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_AUDIENCE)
    private List<String> audience;

    @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_ISSUER)
    private String issuer;

    @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS)
    private List<PublicKeyOptions> publicKeys;

    @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_ADDITIONAL_ISSUERS)
    private List<String> additionalIssuers;

    @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_CLAIMS)
    private List<ClaimOptions> claims;

    @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS_RECONCILIATION)
    private ReconciliationOptions reconciliation;

    public WithAuthHandlerMiddlewareOptionsBase() {
    }

    public List<String> getAudience() {
        return audience == null ? null : List.copyOf(audience);
    }

    public String getIssuer() {
        return issuer;
    }

    public List<PublicKeyOptions> getPublicKeys() {
        return publicKeys == null ? null : publicKeys.stream().map(PublicKeyOptions::clone).toList();
    }

    public List<String> getAdditionalIssuers() {
        return additionalIssuers == null ? null : List.copyOf(additionalIssuers);
    }

    public List<ClaimOptions> getClaims() {
        return claims == null ? null : claims.stream().map(ClaimOptions::clone).toList();
    }

    public ReconciliationOptions getReconciliation() {
        return reconciliation == null ? null : reconciliation.clone();
    }

    @Override
    public WithAuthHandlerMiddlewareOptionsBase clone() {
        try {
            final WithAuthHandlerMiddlewareOptionsBase options = (WithAuthHandlerMiddlewareOptionsBase) super.clone();
            options.audience = audience == null ? null : List.copyOf(audience);
            options.additionalIssuers = additionalIssuers == null ? null : List.copyOf(additionalIssuers);
            options.publicKeys = publicKeys == null ? null : publicKeys.stream().map(PublicKeyOptions::clone).toList();
            options.claims = claims == null ? null : claims.stream().map(ClaimOptions::clone).toList();
            options.reconciliation = reconciliation == null ? null : reconciliation.clone();
            return options;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public static final class PublicKeyOptions implements GatewayMiddlewareOptions {

        @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEY)
        private String key;

        @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEY_ALGORITHM)
        private String algorithm;

        private PublicKeyOptions() {
        }

        public String getKey() {
            return key;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        @Override
        public PublicKeyOptions clone() {
            try {
                return (PublicKeyOptions) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static final class ClaimOptions implements GatewayMiddlewareOptions {

        @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_CLAIM_OPERATOR)
        private String operator;

        @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_CLAIM_PATH)
        private String path;

        @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_CLAIM_VALUE)
        private Object value;

        private ClaimOptions() {
        }

        public String getOperator() {
            return operator;
        }

        public String getPath() {
            return path;
        }

        public Object getValue() {
            return value;
        }

        @Override
        public ClaimOptions clone() {
            try {
                return (ClaimOptions) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static final class ReconciliationOptions implements GatewayMiddlewareOptions {

        @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS_RECONCILIATION_ENABLED)
        private Boolean enabled;

        @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS_RECONCILIATION_INTERVAL_MS)
        private Integer intervalMs;

        private ReconciliationOptions() {
        }

        public Boolean isEnabled() {
            return Boolean.valueOf(enabled);
        }

        public Integer getIntervalMs() {
            return Integer.valueOf(intervalMs);
        }

        @Override
        public ReconciliationOptions clone() {
            try {
                final ReconciliationOptions options = (ReconciliationOptions) super.clone();
                options.enabled = Boolean.valueOf(enabled);
                options.intervalMs = Integer.valueOf(intervalMs);
                return options;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
