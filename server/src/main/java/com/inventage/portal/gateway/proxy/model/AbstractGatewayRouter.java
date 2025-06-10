package com.inventage.portal.gateway.proxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventage.portal.gateway.core.model.GatewayStyle;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import java.util.List;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

@Immutable
@GatewayStyle
@JsonDeserialize(builder = GatewayRouter.Builder.class)
public abstract class AbstractGatewayRouter implements Comparable<AbstractGatewayRouter> {

    @JsonProperty(DynamicConfiguration.ROUTER_NAME)
    public abstract String getName();

    @JsonProperty(DynamicConfiguration.ROUTER_RULE)
    public abstract String getRule();

    @Default
    @JsonProperty(DynamicConfiguration.ROUTER_PRIORITY)
    public int getPriority() {
        return 0;
    }

    @JsonProperty(DynamicConfiguration.ROUTER_ENTRYPOINTS)
    public abstract List<String> getEntrypoints();

    @JsonProperty(DynamicConfiguration.ROUTER_MIDDLEWARES)
    public abstract List<String> getMiddlewares();

    @JsonProperty(DynamicConfiguration.ROUTER_SERVICE)
    public abstract String getService();

    /**
     * To avoid path overlap, routes are sorted, by default, in descending order using rules length.
     * The priority is directly equal to the length of the rule, and so the longest length has the
     * highest priority.
     * Additionally, a priority for each router can be defined. This overwrites priority calculates
     * by the length of the rule.
     */
    @Override
    public int compareTo(AbstractGatewayRouter other) {
        final String ruleA = this.getRule();
        final String ruleB = other.getRule();

        int priorityA = ruleA.length();
        int priorityB = ruleB.length();

        if (this.getPriority() > 0) {
            priorityA = this.getPriority();
        }

        if (other.getPriority() > 0) {
            priorityB = other.getPriority();
        }

        return priorityB - priorityA;
    }
}
