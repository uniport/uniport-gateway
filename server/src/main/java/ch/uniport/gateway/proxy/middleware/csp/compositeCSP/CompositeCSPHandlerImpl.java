package ch.uniport.gateway.proxy.middleware.csp.compositeCSP;

import com.google.common.collect.Sets;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CSPHandler;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modified version of the default CSPHandler that allows the stacking of
 * multiple CSPMiddleware. By doing so, the union of CSP policies from
 * all CSPMiddlewares on the same route are enforced.
 *
 * The entire class, with exception to the handle method is copied from the
 * default io.vertx.ext.web.handler.impl.CSPHandlerImpl in vertx-web
 * https://github.com/vert-x3/vertx-web/blob/4.5.18/vertx-web/src/main/java/io/vertx/ext/web/handler/impl/CSPHandlerImpl.java
 * 
 * The following changes were made:
 * - no default directive
 * - handleResponse added
 */
public class CompositeCSPHandlerImpl implements CompositeCSPHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeCSPHandlerImpl.class);

    private static final String CSP_PREVIOUS_POLICY_KEY = "CSP_POLICY";
    private static final String CSP_PREVIOUS_RESPONSE_POLICY_KEY = "RESPONSE_CSP_POLICY";

    private static final List<String> MUST_BE_QUOTED = Arrays.asList(
        "none",
        "self",
        "unsafe-inline",
        "unsafe-eval");

    private final Map<String, String> policy = new LinkedHashMap<>();
    // cache the computed policy
    private String policyString;

    private boolean reportOnly;

    private final CSPMergeStrategy mergeStrategy;

    /**
     * Modified constructor to configure the merging strategy of multiple CSP
     * policies
     */
    public CompositeCSPHandlerImpl(CSPMergeStrategy mergeStrategy) {
        this.mergeStrategy = mergeStrategy;
    }

    @Override
    public synchronized CSPHandler setDirective(String name, String value) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }

        if (value == null) {
            policy.remove(name);
        }

        if (MUST_BE_QUOTED.contains(value)) {
            // these policies are special, they must be quoted
            value = "'" + value + "'";
        }

        policy.put(name, value);

        // invalidate cache
        policyString = null;
        return this;
    }

    @Override
    public CSPHandler addDirective(String name, String value) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }

        if (value == null) {
            policy.remove(name);
        }

        if (MUST_BE_QUOTED.contains(value)) {
            // these policies are special, they must be quoted
            value = "'" + value + "'";
        }

        final String previous = policy.get(name);
        if (previous == null || "".equals(previous)) {
            policy.put(name, value);
        } else {
            policy.put(name, previous + " " + value);
        }

        // invalidate cache
        policyString = null;
        return this;
    }

    @Override
    public CSPHandler setReportOnly(boolean reportOnly) {
        this.reportOnly = reportOnly;
        return this;
    }

    private String getPolicyString() {
        if (policyString == null) {
            final StringBuilder buffer = new StringBuilder();

            for (Map.Entry<String, String> entry : policy.entrySet()) {
                if (buffer.length() > 0) {
                    buffer.append("; ");
                }
                buffer
                    .append(entry.getKey())
                    .append(' ')
                    .append(entry.getValue());
            }

            policyString = buffer.toString();
        }

        return policyString;
    }

    /*
     * handles requests
     * assemble configured policy and saves it in the routing context
     * merge policies of multiple CSP middleware instances
     */
    @Override
    public void handle(RoutingContext ctx) {
        final String currentPolicy = getPolicyString();
        final String previousPolicy = ctx.get(CSP_PREVIOUS_POLICY_KEY);
        final String effectiveCSPPolicy = unitePolicies(Arrays.asList(previousPolicy, currentPolicy));

        if (effectiveCSPPolicy.length() != 0) {
            ctx.put(CSP_PREVIOUS_POLICY_KEY, effectiveCSPPolicy);
        }
        ctx.next();
    }

    /**
     * handles responses
     * INTERNAL policies are read from the routing context, configured by previous
     * middleware instance
     * EXTERNAL policies are read from Content-Security-Policy and
     * Content-Security-Policy-Report-Only headers and merged (union)
     * if INTERNAL is configured, all EXTERNAL configured policies are ingored
     * if EXTERNAL is configured, all INTERNAL configured policies are ingored
     */
    public void handleResponse(RoutingContext ctx) {
        final String internalCSPPolicy = mergeStrategy == CSPMergeStrategy.EXTERNAL ? ""
            : ctx.get(CSP_PREVIOUS_POLICY_KEY);
        final String externalCSPPolicy = mergeStrategy == CSPMergeStrategy.INTERNAL ? ""
            : unitePolicies(Stream.concat(
                ctx.response().headers().getAll(CSP_HEADER_NAME).stream(),
                ctx.response().headers().getAll(CSP_REPORT_ONLY_HEADER_NAME).stream())
                .toList());

        ctx.response().headers().remove(CSP_HEADER_NAME);
        ctx.response().headers().remove(CSP_REPORT_ONLY_HEADER_NAME);

        String effectiveCSPPolicy = mergePolicies(internalCSPPolicy, externalCSPPolicy, mergeStrategy);

        final String previousResponsePolicy = ctx.get(CSP_PREVIOUS_RESPONSE_POLICY_KEY);
        if (previousResponsePolicy != null) {
            effectiveCSPPolicy = mergePolicies(effectiveCSPPolicy, previousResponsePolicy, CSPMergeStrategy.UNION);
        }
        ctx.put(CSP_PREVIOUS_RESPONSE_POLICY_KEY, effectiveCSPPolicy);
        setPolicy(ctx.response(), effectiveCSPPolicy);
    }

    private String mergePolicies(String internal, String external, CSPMergeStrategy strategy) {
        LOGGER.debug("Merging CSP policies with strategy '{}'", strategy.toString());
        switch (strategy) {
            case UNION:
                return unitePolicies(Arrays.asList(internal, external));
            case INTERNAL:
                return internal;
            case EXTERNAL:
                return external;
            default:
                throw new IllegalStateException(
                    String.format("No support for the following merging strategy: %s", strategy));
        }
    }

    private String unitePolicies(List<String> policies) {
        if (policies == null || policies.size() == 0) {
            return "";
        }

        final Map<String, Set<String>> finalPolicies = extractCspDirectives(policies.get(0));
        for (int i = 1; i < policies.size(); i++) {
            final Map<String, Set<String>> policy = extractCspDirectives(policies.get(i));

            policy.forEach((directive, values) -> {
                if (finalPolicies.containsKey(directive)) {
                    final Set<String> currentValues = finalPolicies.get(directive);
                    finalPolicies.put(directive, Sets.union(currentValues, values));
                } else {
                    finalPolicies.put(directive, values);
                }
            });
        }

        final StringBuilder mergedPolicyString = new StringBuilder();

        finalPolicies.forEach((directive, values) -> {
            if (mergedPolicyString.length() > 0) {
                mergedPolicyString.append("; ");
            }

            final Optional<String> mergedValuesOptional = values.stream()
                .reduce((resultValue, newDirectiveValue) -> resultValue + " " + newDirectiveValue);
            if (mergedValuesOptional.isPresent()) {
                final String mergedValues = mergedValuesOptional.get();
                mergedPolicyString.append(directive)
                    .append(' ')
                    .append(mergedValues);
            }
        });

        return mergedPolicyString.toString();
    }

    // Modified code to support multiple chained CSP Middlewares
    private Map<String, Set<String>> extractCspDirectives(String policyString) {
        final Map<String, Set<String>> directives = new HashMap<>();

        if (policyString == null || policyString.isEmpty()) {
            return directives;
        }

        final String[] policyParts = policyString.split(";");
        for (String part : policyParts) {
            final String[] directiveParts = part.trim().split("\\s+");
            if (directiveParts.length > 0) {
                final String directive = directiveParts[0].toLowerCase();
                final String[] values = Arrays.copyOfRange(directiveParts, 1, directiveParts.length);
                directives.put(directive, new HashSet<>(Arrays.asList(values)));
            }
        }

        return directives;
    }

    private void setPolicy(HttpServerResponse response, String policyString) {
        if (policyString.length() == 0) {
            return;
        }

        if (reportOnly) {
            if (!policy.containsKey("report-uri") && !policy.containsKey("report-to")) {
                throw new IllegalStateException("Please disable CSP reportOnly or add a report-uri/report-to policy.");
            } else {
                response.putHeader(CSP_REPORT_ONLY_HEADER_NAME, policyString);
            }
        } else {
            response.putHeader(CSP_HEADER_NAME, policyString);
        }
    }
}
