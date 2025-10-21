package ch.uniport.gateway.proxy.middleware.authorization;

import ch.uniport.gateway.core.config.model.ModelStyle;
import ch.uniport.gateway.proxy.config.model.MiddlewareOptionsModel;
import ch.uniport.gateway.proxy.middleware.authorization.shared.customClaimsChecker.JWTClaimOperator;
import ch.uniport.gateway.proxy.middleware.csrf.CSRFMiddlewareOptions;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import java.util.List;
import org.immutables.value.Value.Check;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WithAuthHandlerMiddlewareOptionsBase implements MiddlewareOptionsModel {

    public static final boolean DEFAULT_RECONCILIATION_ENABLED_VALUE = true;
    public static final long DEFAULT_RECONCILIATION_INTERVAL_MS = 60_000;
    public static final String DEFAULT_PUBLIC_KEY_ALGORITHM = "RS256";

    private static final Logger LOGGER = LoggerFactory.getLogger(CSRFMiddlewareOptions.class);

    @Check
    protected void validate() {
        Preconditions.checkState(!getAudience().isEmpty(), "'getAudience' must have at least one element");
        Preconditions.checkState(!getPublicKeys().isEmpty(), "'getPublicKeys' must have at least one element");
    }

    @JsonProperty(JWTAuthVerifierMiddlewareFactoryBase.AUDIENCE)
    public abstract List<String> getAudience();

    @JsonProperty(JWTAuthVerifierMiddlewareFactoryBase.ISSUER)
    public abstract String getIssuer();

    @JsonProperty(JWTAuthVerifierMiddlewareFactoryBase.PUBLIC_KEYS)
    public abstract List<PublicKeyOptions> getPublicKeys();

    @JsonProperty(JWTAuthVerifierMiddlewareFactoryBase.ADDITIONAL_ISSUERS)
    public abstract List<String> getAdditionalIssuers();

    @JsonProperty(JWTAuthVerifierMiddlewareFactoryBase.CLAIMS)
    public abstract List<ClaimOptions> getClaims();

    @Default
    @JsonProperty(JWTAuthVerifierMiddlewareFactoryBase.PUBLIC_KEYS_RECONCILIATION)
    public ReconciliationOptions getReconciliation() {
        logDefault(LOGGER, JWTAuthVerifierMiddlewareFactoryBase.PUBLIC_KEYS_RECONCILIATION);
        return ReconciliationOptions.builder().build();
    }

    @Immutable
    @ModelStyle
    @JsonDeserialize(builder = PublicKeyOptions.Builder.class)
    public abstract static class AbstractPublicKeyOptions implements MiddlewareOptionsModel {

        @JsonProperty(JWTAuthVerifierMiddlewareFactoryBase.PUBLIC_KEY)
        public abstract String getKey();

        @Default
        @JsonProperty(JWTAuthVerifierMiddlewareFactoryBase.PUBLIC_KEY_ALGORITHM)
        public String getAlgorithm() {
            logDefault(LOGGER, JWTAuthVerifierMiddlewareFactoryBase.PUBLIC_KEY_ALGORITHM, DEFAULT_PUBLIC_KEY_ALGORITHM);
            return DEFAULT_PUBLIC_KEY_ALGORITHM;
        }
    }

    @Immutable
    @ModelStyle
    @JsonDeserialize(builder = ClaimOptions.Builder.class)
    public abstract static class AbstractClaimOptions implements MiddlewareOptionsModel {

        @JsonProperty(JWTAuthVerifierMiddlewareFactoryBase.CLAIM_OPERATOR)
        public abstract JWTClaimOperator getOperator();

        @JsonProperty(JWTAuthVerifierMiddlewareFactoryBase.CLAIM_PATH)
        public abstract String getPath();

        @JsonProperty(JWTAuthVerifierMiddlewareFactoryBase.CLAIM_VALUE)
        public abstract Object getValue();
    }

    @Immutable
    @ModelStyle
    @JsonDeserialize(builder = ReconciliationOptions.Builder.class)
    public abstract static class AbstractReconciliationOptions implements MiddlewareOptionsModel {

        @Default
        @JsonProperty(JWTAuthVerifierMiddlewareFactoryBase.RECONCILIATION_ENABLED)
        public boolean isEnabled() {
            logDefault(LOGGER, JWTAuthVerifierMiddlewareFactoryBase.RECONCILIATION_ENABLED,
                DEFAULT_RECONCILIATION_ENABLED_VALUE);
            return DEFAULT_RECONCILIATION_ENABLED_VALUE;
        }

        @Default
        @JsonProperty(JWTAuthVerifierMiddlewareFactoryBase.RECONCILIATION_INTERVAL_MS)
        public long getIntervalMs() {
            logDefault(LOGGER, JWTAuthVerifierMiddlewareFactoryBase.RECONCILIATION_INTERVAL_MS,
                DEFAULT_RECONCILIATION_INTERVAL_MS);
            return DEFAULT_RECONCILIATION_INTERVAL_MS;
        }
    }
}
