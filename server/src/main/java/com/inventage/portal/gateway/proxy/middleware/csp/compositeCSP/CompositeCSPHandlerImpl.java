package com.inventage.portal.gateway.proxy.middleware.csp.compositeCSP;

import com.google.common.collect.Sets;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CSPHandler;
import io.vertx.ext.web.handler.HttpException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Modified version of the default CSPHandler that allows the stacking of multiple CSPMiddleware. By doing so, the union of CSP policies from
 * all CSPMiddlewares on the same route are enforced.
 *
 * The entire class, with exception to the handle method is copied from the default io.vertx.ext.web.handler.impl.CSPHandlerImpl in vertx-web
 * https://github.com/vert-x3/vertx-web/blob/4.4.4/vertx-web/src/main/java/io/vertx/ext/web/handler/impl/CSPHandlerImpl.java
 */
public class CompositeCSPHandlerImpl implements CSPHandler {

    private static final String CSP_PREVIOUS_POLICY_KEY = "CSP_POLICY";
    private static final List<String> MUST_BE_QUOTED = Arrays.asList(
        "none",
        "self",
        "unsafe-inline",
        "unsafe-eval");

    private final Map<String, String> policy = new LinkedHashMap<>();
    // cache the computed policy
    private String policyString;

    private boolean reportOnly;

    public CompositeCSPHandlerImpl() {
        addDirective("default-src", "self");
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

    private String mergeWithPreviousPolicyString(RoutingContext ctx) {
        final StringBuilder mergedPolicyString = new StringBuilder();
        final String previousPolicy = ctx.get(CSP_PREVIOUS_POLICY_KEY);
        final String currentPolicy = getPolicyString();

        if (previousPolicy == null) {
            ctx.put(CSP_PREVIOUS_POLICY_KEY, currentPolicy);
            return currentPolicy;
        }

        final Map<String, Set<String>> oldPoliciesDirectives = extractCspDirectives(previousPolicy);
        final Map<String, Set<String>> currentPoliciesDirectives = extractCspDirectives(currentPolicy);

        oldPoliciesDirectives.forEach((directive, values) -> {
            if (currentPoliciesDirectives.containsKey(directive)) {
                final Set<String> currentValues = currentPoliciesDirectives.get(directive);
                currentPoliciesDirectives.put(directive, Sets.union(currentValues, values));
            } else {
                currentPoliciesDirectives.put(directive, values);
            }
        });

        currentPoliciesDirectives.forEach((directive, values) -> {
            if (mergedPolicyString.length() > 0) {
                mergedPolicyString.append("; ");
            }

            final String mergedValues = values.stream().reduce((resultValue, newDirectiveValue) -> resultValue + " " + newDirectiveValue).get();
            mergedPolicyString.append(directive)
                .append(' ')
                .append(mergedValues);
        });

        ctx.put(CSP_PREVIOUS_POLICY_KEY, mergedPolicyString.toString());
        return mergedPolicyString.toString();
    }

    @Override
    public void handle(RoutingContext ctx) {

        final String mergedPolicyString = mergeWithPreviousPolicyString(ctx);

        if (reportOnly) {
            // add support for 'report-to'
            if (!policy.containsKey("report-uri") && !policy.containsKey("report-to")) {
                ctx.fail(new HttpException(500, "Please disable CSP reportOnly or add a report-uri/report-to policy."));
            } else {
                ctx.response()
                    .putHeader("Content-Security-Policy-Report-Only", mergedPolicyString);
                ctx.next();
            }
        } else {
            ctx.response()
                .putHeader("Content-Security-Policy", mergedPolicyString);
            ctx.next();
        }
    }
}
