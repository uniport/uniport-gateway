package ch.uniport.gateway.core.config.env;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.text.StringSubstitutor;

/**
 */
public class ConfigAdapter {

    /**
     * Replace all variables within the given input string.
     *
     * @param input
     *            containing n variables to be replaced
     * @param env
     *            to get the replacement values from
     * @return string with replaced variables
     */
    public static String replaceEnvVariables(String input, JsonObject env) {
        return StringSubstitutor.replace(input, env.getMap());
    }

    public static String replaceBooleans(String input) {
        return RegExUtils.replacePattern(input, "\"(true|false)\"", "$1");
    }

}
