package ch.uniport.gateway.proxy.middleware.authorization.shared.customClaimsChecker;

import ch.uniport.gateway.proxy.middleware.authorization.ClaimOptions;
import org.apache.commons.lang3.Validate;

public class JWTClaim {

    final String path;
    final JWTClaimOperator operator;
    final Object value;

    /**
     * @param claimObject
     *            json Object containing the entries of the claim.
     *            path: In JsonPath syntax
     *            (https://github.com/json-path/JsonPath), which describes
     *            the path to the entry in the payload to be checked.
     *            operator: The operator that defines the rule for the
     *            check.
     *            value: The claim value, that is compared to the payload
     *            entry
     */
    public JWTClaim(ClaimOptions options) {
        this(
            options.getPath(),
            options.getOperator(),
            options.getValue());
    }

    public JWTClaim(String path, JWTClaimOperator operator, Object value) {
        this.path = path;
        this.operator = operator;
        this.value = value;
        validateCheck();
    }

    private void validateCheck() {
        Validate.notNull(path, "Path can not be null");
        Validate.notNull(operator, "Operator can not be null");
        Validate.notNull(value, "Value can not be null");
    }

}
