package com.inventage.portal.gateway.core.config;

import org.apache.commons.text.StringSubstitutor;
import io.vertx.core.json.JsonObject;

public class ConfigAdapter {

    /**
     * Replace all variables within the given input string.
     *
     * @param env to get the replacement values from
     * @param input  containing 0-n variables to be replaced
     * @return string with replaced variables
     */
    public static String replaceEnvVariables(JsonObject env, String input) {
        return StringSubstitutor.replace(input, env.getMap());
    }

}
