package com.inventage.portal.gateway.proxy.middleware.authorization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WithAuthHandlerMiddlewareOptionsBase implements GatewayMiddlewareOptions {

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

    protected WithAuthHandlerMiddlewareOptionsBase(BaseBuilder<?> builder) {
        if (builder.audience == null) {
            throw new IllegalArgumentException("audience is required");
        }
        if (builder.issuer == null) {
            throw new IllegalArgumentException("issuer is required");
        }
        if (builder.publicKeys == null) {
            throw new IllegalArgumentException("public keys is required");
        }

        this.audience = builder.audience == null ? null : List.copyOf(builder.audience);
        this.issuer = builder.issuer;
        this.publicKeys = builder.publicKeys == null ? null : builder.publicKeys.stream().map(PublicKeyOptions::clone).toList();
        this.additionalIssuers = builder.additionalIssuers == null ? null : List.copyOf(builder.additionalIssuers);
        this.claims = builder.claims == null ? null : builder.claims.stream().map(ClaimOptions::clone).toList();
        this.reconciliation = builder.reconciliation == null ? null : builder.reconciliation.clone();
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

    @JsonDeserialize(builder = PublicKeyOptions.Builder.class)
    public static final class PublicKeyOptions implements GatewayMiddlewareOptions {

        @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEY)
        private String key;

        @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEY_ALGORITHM)
        private String algorithm;

        public static Builder builder() {
            return new Builder();
        }

        private PublicKeyOptions(String key, String algorithm) {
            if (key == null) {
                throw new IllegalArgumentException("key is required");
            }
            this.key = key;
            this.algorithm = algorithm;
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

        @JsonPOJOBuilder
        public static final class Builder {

            private String key;
            private String algorithm;

            @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEY)
            public Builder withKey(String key) {
                this.key = key;
                return this;
            }

            @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEY_ALGORITHM)
            public Builder withAlgorithm(String algorithm) {
                this.algorithm = algorithm;
                return this;
            }

            public PublicKeyOptions build() {
                return new PublicKeyOptions(key, algorithm);
            }
        }
    }

    @JsonDeserialize(builder = ClaimOptions.Builder.class)
    public static final class ClaimOptions implements GatewayMiddlewareOptions {

        @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_CLAIM_OPERATOR)
        private String operator;

        @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_CLAIM_PATH)
        private String path;

        @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_CLAIM_VALUE)
        private Object value;

        public static Builder builder() {
            return new Builder();
        }

        private ClaimOptions(String operator, String path, Object value) {
            if (operator == null) {
                throw new IllegalArgumentException("operator is required");
            }
            if (path == null) {
                throw new IllegalArgumentException("path is required");
            }
            if (value == null) {
                throw new IllegalArgumentException("value is required");
            }
            this.operator = operator;
            this.path = path;
            this.value = value;
        }

        public String getOperator() {
            return operator;
        }

        public String getPath() {
            return path;
        }

        public Object getValue() {
            // this reference is going to be leaked, can we do better?
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

        @JsonPOJOBuilder
        public static final class Builder {

            private String operator;
            private String path;
            private Object value;

            public Builder withOperator(String operator) {
                this.operator = operator;
                return this;
            }

            @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_CLAIM_PATH)
            public Builder withPath(String path) {
                this.path = path;
                return this;
            }

            public Builder withValue(Object value) {
                this.value = value;
                return this;
            }

            public ClaimOptions build() {
                return new ClaimOptions(operator, path, value);
            }
        }
    }

    @JsonDeserialize(builder = ReconciliationOptions.Builder.class)
    public static final class ReconciliationOptions implements GatewayMiddlewareOptions {

        @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS_RECONCILIATION_ENABLED)
        private Boolean enabled;

        @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS_RECONCILIATION_INTERVAL_MS)
        private Integer intervalMs;

        public static Builder builder() {
            return new Builder();
        }

        private ReconciliationOptions(Boolean enabled, Integer intervalMs) {
            this.enabled = enabled;
            this.intervalMs = intervalMs;
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
                return (ReconciliationOptions) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        @JsonPOJOBuilder
        public static final class Builder {

            private Boolean enabled;
            private Integer intervalMs;

            public Builder withEnabled(Boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public Builder withIntervalMs(Integer intervalMs) {
                this.intervalMs = intervalMs;
                return this;
            }

            public ReconciliationOptions build() {
                return new ReconciliationOptions(enabled, intervalMs);
            }
        }
    }

    // Builder containing builders:
    // https://stackoverflow.com/questions/21086417/builder-pattern-and-inheritance
    // https://github.com/rtenhove/eg-builder-inheritance
    protected abstract static class BaseBuilder<T extends BaseBuilder<T>> {

        protected List<String> audience;
        protected String issuer;
        protected List<PublicKeyOptions> publicKeys;
        protected List<String> additionalIssuers;
        protected List<ClaimOptions> claims;
        protected ReconciliationOptions reconciliation;

        public static BaseBuilder<?> builder() {
            return new Builder();
        }

        protected abstract T self();

        public T withAudience(List<String> audience) {
            this.audience = audience;
            return self();
        }

        public T withIssuer(String issuer) {
            this.issuer = issuer;
            return self();
        }

        public T withPublicKeys(List<PublicKeyOptions> publicKeys) {
            this.publicKeys = publicKeys;
            return self();
        }

        public T withAdditionalIssuers(List<String> additionalIssuers) {
            this.additionalIssuers = additionalIssuers;
            return self();
        }

        public T withClaims(List<ClaimOptions> claims) {
            this.claims = claims;
            return self();
        }

        @JsonProperty(WithAuthHandlerMiddlewareFactoryBase.WITH_AUTH_HANDLER_PUBLIC_KEYS_RECONCILIATION)
        public T withReconciliation(ReconciliationOptions reconciliation) {
            this.reconciliation = reconciliation;
            return self();
        }

        public WithAuthHandlerMiddlewareOptionsBase build() {
            return new WithAuthHandlerMiddlewareOptionsBase(this);
        }
    }

    @JsonPOJOBuilder
    public static class Builder extends BaseBuilder<Builder> {
        @Override
        protected Builder self() {
            return this;
        }
    }
}
