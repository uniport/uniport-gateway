package com.inventage.portal.gateway.proxy.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value.Style;
import org.immutables.value.Value.Style.ImplementationVisibility;

@Target({ ElementType.PACKAGE, ElementType.TYPE })
@Retention(RetentionPolicy.CLASS) // Make it class retention for incremental compilation
@Style(get = { "is*", "get*" }, // Detect 'get' and 'is' prefixes in accessor methods
    init = "with*", // Builder initialization methods will have 'set' prefix
    typeAbstract = { "Abstract*" }, // 'Abstract' prefix will be detected and trimmed
    typeImmutable = "*", // No prefix or suffix for generated immutable type
    visibility = ImplementationVisibility.PUBLIC // Generated class will be always public
)
public @interface GatewayStyle {

}
