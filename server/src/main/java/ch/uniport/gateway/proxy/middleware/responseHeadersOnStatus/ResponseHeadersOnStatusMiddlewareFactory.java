package ch.uniport.gateway.proxy.middleware.responseHeadersOnStatus;

import ch.uniport.gateway.proxy.middleware.Middleware;
import ch.uniport.gateway.proxy.middleware.MiddlewareFactory;
import ch.uniport.gateway.proxy.middleware.MiddlewareOptionsModel;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.json.schema.common.dsl.Keywords;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link ResponseHeadersOnStatusMiddleware}.
 */
public class ResponseHeadersOnStatusMiddlewareFactory implements MiddlewareFactory {

    public static final String TYPE = "responseHeadersOnStatus";
    public static final String STATUS_CODE = "statusCode";
    public static final String SET_RESPONSE_HEADERS = "setResponseHeaders";
    public static final String REWRITE_RESPONSE_HEADERS = "rewriteResponseHeaders";
    public static final String REGEX = "regex";
    public static final String REPLACEMENT = "replacement";

    private static final int HTTP_STATUS_CODE_MIN = 100;
    private static final int HTTP_STATUS_CODE_MAX = 599;

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseHeadersOnStatusMiddlewareFactory.class);

    @Override
    public String provides() {
        return TYPE;
    }

    @Override
    public ObjectSchemaBuilder optionsSchema() {
        return Schemas.objectSchema()
            .requiredProperty(STATUS_CODE, Schemas.intSchema()
                .with(io.vertx.json.schema.draft7.dsl.Keywords.minimum(HTTP_STATUS_CODE_MIN))
                .with(io.vertx.json.schema.draft7.dsl.Keywords.maximum(HTTP_STATUS_CODE_MAX)))
            .optionalProperty(SET_RESPONSE_HEADERS, Schemas.objectSchema()
                .with(Keywords.minProperties(1))
                .additionalProperties(Schemas.stringSchema()
                    .with(Keywords.minLength(1))))
            .optionalProperty(REWRITE_RESPONSE_HEADERS, Schemas.objectSchema()
                .with(Keywords.minProperties(1))
                .additionalProperties(Schemas.objectSchema()
                    .requiredProperty(REGEX, Schemas.stringSchema()
                        .with(Keywords.minLength(1)))
                    .requiredProperty(REPLACEMENT, Schemas.stringSchema())
                    .allowAdditionalProperties(false)))
            .allowAdditionalProperties(false);
    }

    @Override
    public Future<Void> validate(JsonObject options) {
        final JsonObject setHeaders = options.getJsonObject(SET_RESPONSE_HEADERS);
        final JsonObject rewriteHeaders = options.getJsonObject(REWRITE_RESPONSE_HEADERS);

        if (setHeaders == null && rewriteHeaders == null) {
            return Future.failedFuture(
                String.format("%s: at least one of '%s' or '%s' must be defined",
                    TYPE, SET_RESPONSE_HEADERS, REWRITE_RESPONSE_HEADERS));
        }

        // Pre-compile regex patterns to catch invalid patterns at config load time
        if (rewriteHeaders != null) {
            for (String headerName : rewriteHeaders.fieldNames()) {
                final JsonObject rule = rewriteHeaders.getJsonObject(headerName);
                final String regex = rule.getString(REGEX);
                try {
                    Pattern.compile(regex);
                } catch (PatternSyntaxException e) {
                    return Future.failedFuture(
                        String.format("%s: invalid regex pattern for header '%s': %s",
                            TYPE, headerName, e.getMessage()));
                }
            }
        }

        return Future.succeededFuture();
    }

    @Override
    public Class<ResponseHeadersOnStatusMiddlewareOptions> modelType() {
        return ResponseHeadersOnStatusMiddlewareOptions.class;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, MiddlewareOptionsModel config) {
        final ResponseHeadersOnStatusMiddlewareOptions options = castOptions(config, modelType());

        MultiMap setHeaders = null;
        if (options.getSetResponseHeaders() != null) {
            setHeaders = HeadersMultiMap.httpHeaders().addAll(options.getSetResponseHeaders());
        }

        Map<String, ResponseHeadersOnStatusMiddleware.CompiledRewriteRule> rewriteRules = null;
        if (options.getRewriteResponseHeaders() != null) {
            rewriteRules = new LinkedHashMap<>();
            for (Map.Entry<String, RewriteRule> entry : options.getRewriteResponseHeaders().entrySet()) {
                final Pattern pattern = Pattern.compile(entry.getValue().getRegex());
                rewriteRules.put(entry.getKey(),
                    new ResponseHeadersOnStatusMiddleware.CompiledRewriteRule(pattern, entry.getValue().getReplacement()));
            }
        }

        LOGGER.debug("Created '{}#{}' middleware: statusCode={}, setHeaders={}, rewriteHeaders={}",
            TYPE, name, options.getStatusCode(),
            setHeaders != null ? setHeaders.names() : "none",
            rewriteRules != null ? rewriteRules.keySet() : "none");
        return Future.succeededFuture(
            new ResponseHeadersOnStatusMiddleware(name, options.getStatusCode(), setHeaders, rewriteRules));
    }
}
