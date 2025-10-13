package ch.uniport.gateway.proxy.middleware.authorization.shared.customClaimsChecker;

import ch.uniport.gateway.proxy.middleware.authorization.ClaimOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JWTAuthAdditionalClaimsOptions {

    private final List<JWTClaim> additionalClaims;

    public JWTAuthAdditionalClaimsOptions() {
        additionalClaims = new ArrayList<>();
    }

    public JWTAuthAdditionalClaimsOptions(List<JWTClaim> claims) {
        this.additionalClaims = new ArrayList<>(claims);
    }

    public List<JWTClaim> getAdditionalClaims() {
        return new ArrayList<>(additionalClaims);
    }

    public JWTAuthAdditionalClaimsOptions addAdditionalClaims(List<ClaimOptions> claims) {
        Objects.requireNonNull(claims, "Claims can not be null");
        for (ClaimOptions claim : claims) {
            additionalClaims.add(new JWTClaim(claim));
        }
        return this;
    }
}
