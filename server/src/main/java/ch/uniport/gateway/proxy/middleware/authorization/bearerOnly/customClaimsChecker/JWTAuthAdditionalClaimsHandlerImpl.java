package ch.uniport.gateway.proxy.middleware.authorization.bearerOnly.customClaimsChecker;

import ch.uniport.gateway.proxy.middleware.authorization.bearerOnly.publickeysReconciler.JWTAuthPublicKeysReconcilerHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.impl.JWTAuthHandlerImpl;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.minidev.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JWTAuthAdditionalClaimsHandlerImpl extends JWTAuthHandlerImpl implements JWTAuthAdditionalClaimsHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JWTAuthAdditionalClaimsHandlerImpl.class);

    private static final int MAX_AUTH_RETRIES = 1;
    private static final String AUTH_RETRIES_KEY = "uniport.jwtauth.auth-retries";

    private final List<JWTClaim> additionalJWTClaims;
    private final JWTAuthPublicKeysReconcilerHandler reconciler;

    public JWTAuthAdditionalClaimsHandlerImpl(
        JWTAuth authProvider, JWTAuthAdditionalClaimsOptions options,
        JWTAuthPublicKeysReconcilerHandler reconciler
    ) {
        super(authProvider, null);

        additionalJWTClaims = options == null ? List.of() : options.getAdditionalClaims();
        this.reconciler = reconciler;
    }

    private static boolean verifyClaim(Object payloadValue, Object claimValue, JWTClaimOperator operator)
        throws JsonProcessingException {

        // We need to convert the dynamic type of the payload to ensure compatibility
        // when using method calls from external libraries.
        payloadValue = convertPayloadType(payloadValue);
        claimValue = convertClaimType(claimValue);

        if (operator == JWTClaimOperator.EQUALS) {
            return verifyClaimEquals(payloadValue, claimValue);
        } else if (operator == JWTClaimOperator.CONTAINS) {
            return verifyClaimContains(payloadValue, claimValue);
        } else if (operator == JWTClaimOperator.EQUALS_SUBSTRING_WHITESPACE) {
            final String[] array = payloadValue.toString().split(" ");
            final JsonArray payloadArray = new JsonArray(Arrays.asList(array));
            return verifyClaimEquals(payloadArray, claimValue);
        } else if (operator == JWTClaimOperator.CONTAINS_SUBSTRING_WHITESPACE) {
            final String[] array = payloadValue.toString().split(" ");
            final JsonArray payloadArray = new JsonArray(Arrays.asList(array));
            return verifyClaimContains(payloadArray, claimValue);
        } else {
            throw new IllegalStateException(
                String.format("No support for the following operator: %s", operator));
        }
    }

    private static boolean verifyClaimEquals(Object payloadValue, Object claimValue) throws JsonProcessingException {
        if ((claimValue instanceof String && payloadValue instanceof String)
            || (claimValue instanceof Number && payloadValue instanceof Number)
            || (claimValue instanceof Boolean && payloadValue instanceof Boolean)) {
            return claimValue.equals(payloadValue);
        }

        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(payloadValue.toString()).equals(mapper.readTree(claimValue.toString()));
    }

    private static boolean verifyClaimContains(Object payloadValue, Object claimValue) throws JsonProcessingException {
        // By definition, we require that contains can only work with an array/list.

        final JsonArray claimArray = new JsonArray(claimValue.toString());

        if (payloadValue instanceof JsonArray) {
            final JsonArray payloadArray = (JsonArray) payloadValue;
            return verifyClaimContainsArray(payloadArray, claimArray);
        } else {
            for (Object claimItem : claimArray) {
                if (verifyClaimEquals(payloadValue, claimItem)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static boolean verifyClaimContainsArray(JsonArray payloadArray, JsonArray claimArray)
        throws JsonProcessingException {
        // Every entry in the payload array must be contained in the claimed array
        boolean found = false;
        for (Object payloadItem : payloadArray) {
            for (Object claimItem : claimArray) {
                if (verifyClaimEquals(payloadItem, claimItem)) {
                    found = true;
                    break;
                }
            }
        }
        // If the entry has been found, the code will terminate before reaching this
        // statement
        return found;
    }

    private static Object convertPayloadType(Object payloadValue) {
        // The JsonPath library converts a Json Object directly to a map data structure.
        // To use external libraries to perform equality check between
        // two JSON objects, we convert the payload to a JsonObject, such that we get
        // the same structure when calling toString
        if (payloadValue instanceof Map) {
            payloadValue = new JsonObject((Map<String, Object>) payloadValue);
        }
        // Potentially payloadValue has type JSONArray, but for our solution we require
        // type JsonArray
        // Provided by JsonPath
        if (payloadValue instanceof JSONArray) {
            payloadValue = new JsonArray(payloadValue.toString());
        }

        return payloadValue;
    }

    private static Object convertClaimType(Object claimValue) {
        if (claimValue instanceof Map) {
            claimValue = new JsonObject((Map<String, Object>) claimValue);
        }
        if (claimValue instanceof List) {
            claimValue = new JsonArray((List<Object>) claimValue);
        }
        return claimValue;
    }

    @Override
    public void postAuthentication(RoutingContext ctx) {
        final User user = ctx.user();
        if (user == null) {
            // bad state
            LOGGER.debug("no user in context");
            ctx.fail(403, new IllegalStateException("no user in the context"));
            return;
        }

        final JsonObject jwt = user.get("accessToken");
        if (jwt == null) {
            LOGGER.debug("invalid JWT: malformed or audience, issuer or signature is invalid");
            ctx.fail(403,
                new IllegalStateException("Invalid JWT: malformed or audience, issuer or signature is invalid"));
            return;
        }

        // Check that all required additional claims are present
        try {
            for (JWTClaim additionalClaim : additionalJWTClaims) {
                LOGGER.debug("Verifying claims. Path: {}, Operator: {}, Claim: {}", additionalClaim.path,
                    additionalClaim.operator, additionalClaim.value);

                // Claims are provided by the dynamic configuration file.
                // We verify that each payload complies with the claims defined in the
                // configuration
                // Throws an exception if the path does not exist in the payload
                final Object payloadValue = JsonPath.read(jwt.encode(), additionalClaim.path);

                // Verify if the value stored in that path complies to the claim.
                if (!verifyClaim(payloadValue, additionalClaim.value, additionalClaim.operator)) {
                    throw new IllegalStateException(String.format(
                        "Invalid JWT token: Claim verification failed. Path: %s, Operator: %s, claim: %s, payload: %s",
                        additionalClaim.path, additionalClaim.operator,
                        additionalClaim.value,
                        payloadValue));
                }
            }
        } catch (RuntimeException | JsonProcessingException e) {
            LOGGER.warn(e.getMessage());
            ctx.fail(403, e);
        }

        super.postAuthentication(ctx);
    }

    /*
     * ProcessException catches a 401 response and retries it until MAX_AUTH_RETRIES
     * is reached.
     * This is intended to work together with JWTAuthPublicKeysReconcilerHandler
     * that is responsible for public key refreshes
     */
    @Override
    protected void processException(RoutingContext ctx, Throwable exception) {
        if (reconciler == null || exception == null || !(exception instanceof HttpException)) {
            super.processException(ctx, exception);
            return;
        }

        int authRetries = ctx.get(AUTH_RETRIES_KEY, 0);

        final int statusCode = ((HttpException) exception).getStatusCode();
        if (statusCode == 401 && authRetries < MAX_AUTH_RETRIES) {
            ctx.put(AUTH_RETRIES_KEY, ++authRetries);
            LOGGER.error(String.format("%d", authRetries));

            reconciler.getOrRefreshPublicKeys()
                .onSuccess(ah -> {
                    LOGGER.debug("Retrying failed authenticated request");
                    ctx.reroute(ctx.request().path());
                })
                .onFailure(err -> {
                    LOGGER.warn("Failed to retry failed authenticated request '{}'", err.getMessage());
                    super.processException(ctx, exception);
                });
        } else {
            super.processException(ctx, exception);
        }
    }
}
