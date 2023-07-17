package com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP;

import com.google.common.collect.Sets;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CSPHandler;
import io.vertx.ext.web.handler.HttpException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.LoggerFactory;

/**
 * Modified version of the default CSPHandler that allows the stacking of multiple CSPMiddleware. By doing so, the union of CSP policies from
 * all CSPMiddlewares on the same route are enforced.
 *
 * The entire class, with exception to the handle method is copied from the default io.vertx.ext.web.handler.impl.CSPHandlerImpl in vertx-web:4.3.8
 */
public class CompositeCSPHandlerImpl implements CSPHandler {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CompositeCSPHandlerImpl.class);

    private static final String CSP_PREVIOUS_POLICY_KEY = "CSP_POLICY";
    private static final CSPMergeStrategy DEFAULT_CSP_MERGE_STRATEGY = CSPMergeStrategy.UNION;
    private static final List<String> MUST_BE_QUOTED = Arrays.asList(
        "none",
        "self",
        "unsafe-inline",
        "unsafe-eval");

    private final Map<String, String> policy = new LinkedHashMap<>();
    // cache the computed policy
    private String policyString;
    private boolean reportOnly;
    private final CSPMergeStrategy cspMergeStrategy;

    public CompositeCSPHandlerImpl() {
        this(DEFAULT_CSP_MERGE_STRATEGY.toString());
    }

    // Modified constructor to configure the merging strategy of multiple csp policies
    public CompositeCSPHandlerImpl(String mergeStrategy) {
        this.cspMergeStrategy = CSPMergeStrategy.valueOf(mergeStrategy);
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
                directives.put(directive, Set.of(values));
            }
        }

        return directives;
    }

    private String mergePolicies(List<String> policies) {
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
            final String mergedValues = values.stream().reduce((resultValue, newDirectiveValue) -> resultValue + " " + newDirectiveValue).get();
            mergedPolicyString.append(directive)
                .append(' ')
                .append(mergedValues);
        });

        return mergedPolicyString.toString();
    }

    private String computeEffectiveCSPPolicy(RoutingContext ctx) {
        final String currentPolicy = getPolicyString();
        final String previousPolicy = ctx.get(CSP_PREVIOUS_POLICY_KEY);

        if (previousPolicy == null) {
            return currentPolicy;
        }
        return mergePolicies(List.of(previousPolicy, currentPolicy));
    }

    private String mergeIncomingResponsePolicies(String internalCSPPolicy, String externalCSPPolicy) {
        LOGGER.info(cspMergeStrategy.toString());
        if (cspMergeStrategy == CSPMergeStrategy.UNION) {
            final List<String> policies = new LinkedList<>();
            policies.add(internalCSPPolicy);
            policies.add(externalCSPPolicy);
            return mergePolicies(policies);
        } else if (cspMergeStrategy == CSPMergeStrategy.EXTERNAL) {
            return externalCSPPolicy;
        } else if (cspMergeStrategy == CSPMergeStrategy.INTERNAL) {
            return internalCSPPolicy;
        } else {
            throw new IllegalStateException(
                String.format("No support for the following merging strategy: %s", cspMergeStrategy));
        }
    }

    public void handleResponse(RoutingContext ctx, MultiMap headers) {
        final String internalCSPPolicy = ctx.get(CSP_PREVIOUS_POLICY_KEY);
        final String headerCSPKey = (reportOnly) ? "Content-Security-Policy-Report-Only" : "Content-Security-Policy";
        final String externalCSPPolicy = headers.get(headerCSPKey);
        headers.remove(headerCSPKey);
        final String effectiveCSPPolicy = mergeIncomingResponsePolicies(internalCSPPolicy, externalCSPPolicy);
        ctx.response().putHeader(headerCSPKey, effectiveCSPPolicy);
    }

    @Override
    public void handle(RoutingContext ctx) {
        final String effectiveCSPPolicy = computeEffectiveCSPPolicy(ctx);
        ctx.put(CSP_PREVIOUS_POLICY_KEY, effectiveCSPPolicy);

        if (reportOnly) {
            if (!policy.containsKey("report-uri")) {
                ctx.fail(new HttpException(500, "Please disable CSP reportOnly or add a report-uri policy."));
            } else {
                ctx.response()
                    .putHeader("Content-Security-Policy-Report-Only", effectiveCSPPolicy);
                ctx.next();
            }
        } else {
            ctx.response()
                .putHeader("Content-Security-Policy", effectiveCSPPolicy);
            ctx.next();
        }

    }
}
