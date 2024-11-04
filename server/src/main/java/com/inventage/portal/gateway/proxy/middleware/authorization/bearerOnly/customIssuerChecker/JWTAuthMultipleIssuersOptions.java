package com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.customIssuerChecker;

import io.vertx.core.json.JsonArray;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 */
public class JWTAuthMultipleIssuersOptions {

    private final List<String> additionalIssuers = new ArrayList<>();

    public List<String> getAdditionalIssuers() {
        return new ArrayList<>(additionalIssuers);
    }

    public JWTAuthMultipleIssuersOptions setAdditionalIssuers(JsonArray issuers) {
        Validate.notNull(issuers, "Issuers can not be null");
        if (issuers != null) {
            for (Object issuer : issuers) {
                additionalIssuers.add((String) issuer);
            }
        }
        return this;
    }

    public JWTAuthMultipleIssuersOptions setAdditionalIssuers(List<String> issuers) {
        Validate.notNull(issuers, "Issuers can not be null");
        this.additionalIssuers.addAll(issuers);
        return this;
    }
}
