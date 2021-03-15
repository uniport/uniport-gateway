package com.inventage.portal.gateway.core.config;

import io.vertx.core.json.JsonObject;
import org.apache.commons.text.StringSubstitutor;

public class ConfigAdapter {

    /**
     * Replace all variables within the given input string.
     *
     * @param config to get the replacement values from
     * @param input  containing 0-n variables to be replaced
     * @return string with replaced variables
     */
    public static String replaceEnvVariables(JsonObject config, String input) {
        return StringSubstitutor.replace(input, config.getMap());
    }

}
