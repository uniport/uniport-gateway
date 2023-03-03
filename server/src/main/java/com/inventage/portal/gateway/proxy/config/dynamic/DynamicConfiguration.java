package com.inventage.portal.gateway.proxy.config.dynamic;

import com.inventage.portal.gateway.proxy.middleware.controlapi.ControlApiMiddleware;
import com.inventage.portal.gateway.proxy.middleware.csrf.CSRFMiddleware;
import com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.ReplacedSessionCookieDetectionMiddleware;
import com.inventage.portal.gateway.proxy.middleware.session.SessionMiddleware;
import com.jayway.jsonpath.internal.Path;
import com.jayway.jsonpath.internal.path.PathCompiler;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Draft;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.OutputUnit;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.ValidationException;
import io.vertx.json.schema.Validator;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

/**
 * It defines the structure of the dynamic configuration.
 */
public class DynamicConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfiguration.class);

    // keywords used for internal purpose only
    public static final String HTTP = "http";

    public static final String ROUTERS = "routers";
    public static final String ROUTER_NAME = "name";
    public static final String ROUTER_ENTRYPOINTS = "entrypoints";
    public static final String ROUTER_MIDDLEWARES = "middlewares";
    public static final String ROUTER_SERVICE = "service";
    public static final String ROUTER_RULE = "rule";
    public static final String ROUTER_PRIORITY = "priority";

    public static final String MIDDLEWARES = "middlewares";
    public static final String MIDDLEWARE_NAME = "name";
    public static final String MIDDLEWARE_TYPE = "type";
    public static final String MIDDLEWARE_OPTIONS = "options";

    public static final String MIDDLEWARE_REPLACE_PATH_REGEX = "replacePathRegex";
    public static final String MIDDLEWARE_REPLACE_PATH_REGEX_REGEX = "regex";
    public static final String MIDDLEWARE_REPLACE_PATH_REGEX_REPLACEMENT = "replacement";

    public static final String MIDDLEWARE_REDIRECT_REGEX = "redirectRegex";
    public static final String MIDDLEWARE_REDIRECT_REGEX_REGEX = "regex";
    public static final String MIDDLEWARE_REDIRECT_REGEX_REPLACEMENT = "replacement";

    public static final String MIDDLEWARE_HEADERS = "headers";
    public static final String MIDDLEWARE_HEADERS_REQUEST = "customRequestHeaders";
    public static final String MIDDLEWARE_HEADERS_RESPONSE = "customResponseHeaders";

    public static final String MIDDLEWARE_CORS = "cors";

    public static final String MIDDLEWARE_CSRF = "csrf";
    public static final String MIDDLEWARE_CSRF_COOKIE = "cookie";
    public static final String MIDDLEWARE_CSRF_COOKIE_NAME = "name";
    public static final String MIDDLEWARE_CSRF_COOKIE_PATH = "path";
    public static final String MIDDLEWARE_CSRF_COOKIE_SECURE = "secure";

    public static final String MIDDLEWARE_CSRF_TIMEOUT_IN_MINUTES = "timeoutInMinute";
    public static final String MIDDLEWARE_CSRF_ORIGIN = "origin";
    public static final String MIDDLEWARE_CSRF_NAG_HTTPS = "nagHttps";
    public static final String MIDDLEWARE_CSRF_HEADER_NAME = "headerName";

    public static final String MIDDLEWARE_CSP = "csp";
    public static final String MIDDLEWARE_CSP_REPORT_ONLY = "reportOnly";
    public static final String MIDDLEWARE_CSP_DIRECTIVES = "policyDirectives";
    public static final String MIDDLEWARE_CSP_DIRECTIVE_NAME = "directive";
    public static final String MIDDLEWARE_CSP_DIRECTIVE_VALUES = "values";

    public static final String MIDDLEWARE_AUTHORIZATION_BEARER = "authorizationBearer";

    public static final String MIDDLEWARE_WITH_AUTH_TOKEN_SESSION_SCOPE = "sessionScope";

    public static final String MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEY = "publicKey";
    public static final String MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEY_ALGORITHM = "publicKeyAlgorithm";
    public static final String MIDDLEWARE_WITH_AUTH_HANDLER_ISSUER = "issuer";
    public static final String MIDDLEWARE_WITH_AUTH_HANDLER_AUDIENCE = "audience";
    public static final String MIDDLEWARE_WITH_AUTH_HANDLER_OPTIONAL = "optional";
    public static final String MIDDLEWARE_WITH_AUTH_HANDLER_CLAIMS = "claims";
    public static final String MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_PATH = "claimPath";
    public static final String MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_VALUE = "value";
    public static final String MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_OPERATOR = "operator";
    public static final String MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_OPERATOR_EQUALS = "EQUALS";
    public static final String MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_OPERATOR_CONTAINS = "CONTAINS";
    public static final String MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_OPERATOR_EQUALS_SUBSTRING_WHITESPACE = "EQUALS_SUBSTRING_WHITESPACE";
    public static final String MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_OPERATOR_CONTAINS_SUBSTRING_WHITESPACE = "CONTAINS_SUBSTRING_WHITESPACE";

    public static final String MIDDLEWARE_PASS_AUTHORIZATION = "passAuthorization";

    public static final String MIDDLEWARE_LANGUAGE_COOKIE = "languageCookie";

    public static final String MIDDLEWARE_RESPONSE_SESSION_COOKIE_REMOVAL = "responseSessionCookieRemoval";
    public static final String MIDDLEWARE_RESPONSE_SESSION_COOKIE_REMOVAL_NAME = "name";

    public static final String MIDDLEWARE_REPLACED_SESSION_COOKIE_DETECTION = "replacedSessionCookieDetection";
    public static final String MIDDLEWARE_REPLACED_SESSION_COOKIE_DETECTION_COOKIE_NAME = "name";
    public static final String MIDDLEWARE_REPLACED_SESSION_COOKIE_DETECTION_WAIT_BEFORE_RETRY_MS = "waitTimeInMillisecond";

    public static final String MIDDLEWARE_SESSION = "session";
    public static final String MIDDLEWARE_SESSION_IDLE_TIMEOUT_IN_MINUTES = "idleTimeoutInMinute";
    public static final String MIDDLEWARE_SESSION_ID_MIN_LENGTH = "idMinimumLength";
    public static final String MIDDLEWARE_SESSION_NAG_HTTPS = "nagHttps";
    public static final String MIDDLEWARE_SESSION_LIFETIME_HEADER = "lifetimeHeader";
    public static final String MIDDLEWARE_SESSION_LIFETIME_COOKIE = "lifetimeCookie";
    public static final String MIDDLEWARE_SESSION_COOKIE = "cookie";
    public static final String MIDDLEWARE_SESSION_COOKIE_NAME = "name";
    public static final String MIDDLEWARE_SESSION_COOKIE_HTTP_ONLY = "httpOnly";
    public static final String MIDDLEWARE_SESSION_COOKIE_SAME_SITE = "sameSite";
    public static final String MIDDLEWARE_SESSION_COOKIE_SECURE = "secure";

    public static final String MIDDLEWARE_REQUEST_RESPONSE_LOGGER = "requestResponseLogger";

    public static final String MIDDLEWARE_BEARER_ONLY = "bearerOnly";

    public static final String MIDDLEWARE_OAUTH2 = "oauth2";
    public static final String MIDDLEWARE_OAUTH2_REGISTRATION = "oauth2registration"; // same props as "oauth2"
    public static final String MIDDLEWARE_OAUTH2_CLIENTID = "clientId";
    public static final String MIDDLEWARE_OAUTH2_CLIENTSECRET = "clientSecret";
    public static final String MIDDLEWARE_OAUTH2_DISCOVERYURL = "discoveryUrl";
    public static final String MIDDLEWARE_OAUTH2_SESSION_SCOPE = "sessionScope";
    public static final String MIDDLEWARE_OAUTH2_SESSION_SCOPE_ID = "id";

    public static final String MIDDLEWARE_OAUTH2_RESPONSE_MODE = "responseMode";

    public static final String MIDDLEWARE_SHOW_SESSION_CONTENT = "_session_";

    public static final String MIDDLEWARE_CHECK_ROUTE = "checkRoute";
    public static final String MIDDLEWARE_CHECK_ROUTE_PATH = "_check-route_";

    public static final String MIDDLEWARE_SESSION_BAG = "sessionBag";
    public static final String MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIES = "whitelistedCookies";
    public static final String MIDDLEWARE_SESSION_BAG_COOKIE_NAME = "cookieName";
    /**
     * @deprecated This field should no longer be used as of version 4.3.0.
     * <p> Use {@link DynamicConfiguration#MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIES } instead</p>
     */
    @Deprecated(since = "4.3.0")
    public static final String MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIES_LEGACY = "whithelistedCookies";

    public static final String MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIE_NAME = "name";
    public static final String MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIE_PATH = "path";

    public static final String MIDDLEWARE_CONTROL_API = "controlApi";
    public static final String MIDDLEWARE_CONTROL_API_ACTION = "action";

    public static final List<String> OIDC_RESPONSE_MODES = List.of("query", "fragment", "form_post");

    public static final List<String> MIDDLEWARE_TYPES = Arrays.asList(MIDDLEWARE_REPLACE_PATH_REGEX,
            MIDDLEWARE_REDIRECT_REGEX, MIDDLEWARE_HEADERS, MIDDLEWARE_AUTHORIZATION_BEARER, MIDDLEWARE_BEARER_ONLY,
            MIDDLEWARE_OAUTH2, MIDDLEWARE_OAUTH2_REGISTRATION, MIDDLEWARE_SHOW_SESSION_CONTENT, MIDDLEWARE_SESSION_BAG,
            MIDDLEWARE_CONTROL_API, MIDDLEWARE_LANGUAGE_COOKIE, MIDDLEWARE_REQUEST_RESPONSE_LOGGER,
            MIDDLEWARE_REPLACED_SESSION_COOKIE_DETECTION, MIDDLEWARE_RESPONSE_SESSION_COOKIE_REMOVAL,
            MIDDLEWARE_SESSION, MIDDLEWARE_CHECK_ROUTE, MIDDLEWARE_CSP, MIDDLEWARE_CSRF, MIDDLEWARE_PASS_AUTHORIZATION);

    public static final String SERVICES = "services";
    public static final String SERVICE_NAME = "name";
    public static final String SERVICE_SERVERS = "servers";
    public static final String SERVICE_SERVER_PROTOCOL = "protocol";
    public static final String SERVICE_SERVER_HOST = "host";
    public static final String SERVICE_SERVER_PORT = "port";

    private static Validator validator;

    private static Validator buildValidator(Vertx vertx) {
        final ObjectSchemaBuilder routerSchema = buildRouterSchema();
        final ObjectSchemaBuilder middlewareSchema = buildMiddlewareSchema();
        final ObjectSchemaBuilder serviceSchema = buildServiceSchema();
        final ObjectSchemaBuilder httpSchema = buildHttpSchema(routerSchema, middlewareSchema, serviceSchema);

        final ObjectSchemaBuilder dynamicConfigBuilder = Schemas.objectSchema().requiredProperty(HTTP, httpSchema)
                .allowAdditionalProperties(false);

        final JsonSchema schema = JsonSchema.of(dynamicConfigBuilder.toJson());
        final JsonSchemaOptions options = new JsonSchemaOptions().setDraft(Draft.DRAFT202012)
                .setBaseUri("https://inventage.com/portal-gateway/dynamic-configuration");
        return Validator.create(schema, options);
    }

    private static ObjectSchemaBuilder buildRouterSchema() {
        final ObjectSchemaBuilder routerSchema = Schemas.objectSchema()
                .requiredProperty(ROUTER_NAME, Schemas.stringSchema())
                .property(ROUTER_ENTRYPOINTS, Schemas.arraySchema().items(Schemas.stringSchema()))
                .property(ROUTER_MIDDLEWARES, Schemas.arraySchema().items(Schemas.stringSchema()))
                .requiredProperty(ROUTER_SERVICE, Schemas.stringSchema()).property(ROUTER_RULE, Schemas.stringSchema())
                .property(ROUTER_PRIORITY, Schemas.intSchema()).allowAdditionalProperties(false);
        return routerSchema;
    }

    private static ObjectSchemaBuilder buildMiddlewareSchema() {
        final ObjectSchemaBuilder middlewareOptionsSchema = Schemas.objectSchema()
                .property(MIDDLEWARE_REPLACE_PATH_REGEX_REGEX, Schemas.stringSchema())
                .property(MIDDLEWARE_REPLACE_PATH_REGEX_REPLACEMENT, Schemas.stringSchema())
                .property(MIDDLEWARE_REDIRECT_REGEX_REGEX, Schemas.stringSchema())
                .property(MIDDLEWARE_REDIRECT_REGEX_REPLACEMENT, Schemas.stringSchema())
                .property(MIDDLEWARE_WITH_AUTH_TOKEN_SESSION_SCOPE, Schemas.stringSchema())
                .property(MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEY, Schemas.stringSchema())
                .property(MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEY_ALGORITHM, Schemas.stringSchema())
                .property(MIDDLEWARE_WITH_AUTH_HANDLER_ISSUER, Schemas.stringSchema())
                .property(MIDDLEWARE_WITH_AUTH_HANDLER_AUDIENCE, Schemas.arraySchema())
                .property(MIDDLEWARE_WITH_AUTH_HANDLER_OPTIONAL, Schemas.stringSchema())
                .property(MIDDLEWARE_WITH_AUTH_HANDLER_CLAIMS, Schemas.arraySchema())
                .property(MIDDLEWARE_OAUTH2_CLIENTID, Schemas.stringSchema())
                .property(MIDDLEWARE_OAUTH2_CLIENTSECRET, Schemas.stringSchema())
                .property(MIDDLEWARE_OAUTH2_DISCOVERYURL, Schemas.stringSchema())
                .property(MIDDLEWARE_OAUTH2_SESSION_SCOPE, Schemas.stringSchema())
                .property(MIDDLEWARE_OAUTH2_RESPONSE_MODE, Schemas.stringSchema())
                .property(MIDDLEWARE_HEADERS_REQUEST, Schemas.objectSchema())
                .property(MIDDLEWARE_HEADERS_RESPONSE, Schemas.objectSchema())
                .property(MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIES, Schemas.arraySchema())
                .property(MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIES_LEGACY, Schemas.arraySchema())
                .property(MIDDLEWARE_CONTROL_API_ACTION, Schemas.stringSchema())
                .property(MIDDLEWARE_RESPONSE_SESSION_COOKIE_REMOVAL_NAME, Schemas.stringSchema())
                .property(MIDDLEWARE_REPLACED_SESSION_COOKIE_DETECTION_COOKIE_NAME, Schemas.stringSchema())
                .property(MIDDLEWARE_REPLACED_SESSION_COOKIE_DETECTION_WAIT_BEFORE_RETRY_MS, Schemas.intSchema())
                .property(MIDDLEWARE_SESSION_IDLE_TIMEOUT_IN_MINUTES, Schemas.intSchema())
                .property(MIDDLEWARE_SESSION_COOKIE, Schemas.objectSchema())
                .property(MIDDLEWARE_SESSION_ID_MIN_LENGTH, Schemas.intSchema())
                .property(MIDDLEWARE_SESSION_NAG_HTTPS, Schemas.booleanSchema())
                .property(MIDDLEWARE_SESSION_LIFETIME_HEADER, Schemas.booleanSchema())
                .property(MIDDLEWARE_SESSION_LIFETIME_COOKIE, Schemas.booleanSchema())
                .property(MIDDLEWARE_SESSION_BAG_COOKIE_NAME, Schemas.stringSchema())
                .property(MIDDLEWARE_CSP_REPORT_ONLY, Schemas.booleanSchema())
                .property(MIDDLEWARE_CSP_DIRECTIVES, Schemas.arraySchema())
                .property(MIDDLEWARE_CSRF_COOKIE, Schemas.objectSchema())
                .property(MIDDLEWARE_CSRF_ORIGIN, Schemas.stringSchema())
                .property(MIDDLEWARE_CSRF_HEADER_NAME, Schemas.stringSchema())
                .property(MIDDLEWARE_CSRF_NAG_HTTPS, Schemas.booleanSchema())
                .property(MIDDLEWARE_CSRF_TIMEOUT_IN_MINUTES, Schemas.intSchema())
                .allowAdditionalProperties(false);

        final ObjectSchemaBuilder middlewareSchema = Schemas.objectSchema()
                .requiredProperty(MIDDLEWARE_NAME, Schemas.stringSchema())
                .requiredProperty(MIDDLEWARE_TYPE, Schemas.stringSchema())
                .property(MIDDLEWARE_OPTIONS, middlewareOptionsSchema).allowAdditionalProperties(false);
        return middlewareSchema;
    }

    private static ObjectSchemaBuilder buildServiceSchema() {
        final ObjectSchemaBuilder serviceSchema = Schemas.objectSchema()
                .requiredProperty(SERVICE_NAME, Schemas.stringSchema())
                .requiredProperty(SERVICE_SERVERS, Schemas.arraySchema()
                        .items(Schemas.objectSchema()
                                .optionalProperty(SERVICE_SERVER_PROTOCOL, Schemas.stringSchema())
                                .requiredProperty(SERVICE_SERVER_HOST, Schemas.stringSchema())
                                .requiredProperty(SERVICE_SERVER_PORT, Schemas.intSchema())
                                .allowAdditionalProperties(false)))
                .allowAdditionalProperties(false);
        return serviceSchema;
    }

    private static ObjectSchemaBuilder buildHttpSchema(ObjectSchemaBuilder routerSchema,
                                                       ObjectSchemaBuilder middlewareSchema, ObjectSchemaBuilder serviceSchema) {
        final ObjectSchemaBuilder httpSchema = Schemas.objectSchema()
                .property(ROUTERS, Schemas.arraySchema().items(routerSchema))
                .property(MIDDLEWARES, Schemas.arraySchema().items(middlewareSchema))
                .property(SERVICES, Schemas.arraySchema().items(serviceSchema)).allowAdditionalProperties(false);
        return httpSchema;
    }

    public static ObjectSchemaBuilder getBuildMiddlewareSchema() {
        return buildMiddlewareSchema();
    }

    public static JsonObject buildDefaultConfiguration() {
        final JsonObject config = new JsonObject();

        final JsonObject http = new JsonObject();

        http.put(DynamicConfiguration.ROUTERS, new JsonArray());
        http.put(DynamicConfiguration.MIDDLEWARES, new JsonArray());
        http.put(DynamicConfiguration.SERVICES, new JsonArray());

        config.put(DynamicConfiguration.HTTP, http);

        return config;
    }

    public static boolean isEmptyConfiguration(JsonObject config) {
        if (config == null) {
            return true;
        }

        final JsonObject httpConfig = config.getJsonObject(DynamicConfiguration.HTTP);
        if (httpConfig == null) {
            return true;
        }

        final JsonArray httpRouters = httpConfig.getJsonArray(DynamicConfiguration.ROUTERS);
        final JsonArray httpMiddlewares = httpConfig.getJsonArray(DynamicConfiguration.MIDDLEWARES);
        final JsonArray httpServices = httpConfig.getJsonArray(DynamicConfiguration.SERVICES);

        return httpRouters == null && httpMiddlewares == null && httpServices == null;
    }

    public static JsonObject merge(Map<String, JsonObject> configurations) {
        final JsonObject mergedConfig = buildDefaultConfiguration();
        if (configurations == null) {
            return mergedConfig;
        }

        final JsonObject mergedHttpConfig = mergedConfig.getJsonObject(DynamicConfiguration.HTTP);
        if (mergedHttpConfig == null) {
            return mergedConfig;
        }

        final Map<String, List<String>> routers = new HashMap<>();
        final Set<String> routersToDelete = new HashSet<>();

        final Map<String, List<String>> middlewares = new HashMap<>();
        final Set<String> middlewaresToDelete = new HashSet<>();

        final Map<String, List<String>> services = new HashMap<>();
        final Set<String> servicesToDelete = new HashSet<>();

        for (String key : configurations.keySet()) {
            final JsonObject conf = configurations.get(key);
            final JsonObject httpConf = conf.getJsonObject(DynamicConfiguration.HTTP);

            if (httpConf != null) {
                final JsonArray rts = httpConf.getJsonArray(DynamicConfiguration.ROUTERS, new JsonArray());
                for (int i = 0; i < rts.size(); i++) {
                    final JsonObject rt = rts.getJsonObject(i);
                    final String rtName = rt.getString(DynamicConfiguration.ROUTER_NAME);
                    if (!routers.containsKey(rtName)) {
                        routers.put(rtName, new ArrayList<>());
                    }
                    routers.get(rtName).add(key);
                    if (!addRouter(mergedHttpConfig, rtName, rt)) {
                        routersToDelete.add(rtName);
                    }
                }

                final JsonArray mws = httpConf.getJsonArray(DynamicConfiguration.MIDDLEWARES, new JsonArray());
                for (int i = 0; i < mws.size(); i++) {
                    final JsonObject mw = mws.getJsonObject(i);
                    final String mwName = mw.getString(DynamicConfiguration.MIDDLEWARE_NAME);
                    if (!middlewares.containsKey(mwName)) {
                        middlewares.put(mwName, new ArrayList<>());
                    }
                    middlewares.get(mwName).add(key);
                    if (!addMiddleware(mergedHttpConfig, mwName, mw)) {
                        middlewaresToDelete.add(mwName);
                    }
                }

                final JsonArray svs = httpConf.getJsonArray(DynamicConfiguration.SERVICES, new JsonArray());
                for (int i = 0; i < svs.size(); i++) {
                    final JsonObject sv = svs.getJsonObject(i);
                    final String svName = sv.getString(DynamicConfiguration.SERVICE_NAME);
                    if (!services.containsKey(svName)) {
                        services.put(svName, new ArrayList<>());
                    }
                    services.get(svName).add(key);
                    if (!addService(mergedHttpConfig, svName, sv)) {
                        servicesToDelete.add(svName);
                    }
                }
            }
        }

        for (String routerName : routersToDelete) {
            LOGGER.warn("Router defined multiple times with different configurations in '{}'",
                    routers.get(routerName));
            mergedHttpConfig.remove(routerName);
        }

        for (String middlewareName : middlewaresToDelete) {
            LOGGER.warn("Middleware defined multiple times with different configurations in '{}'",
                    routers.get(middlewareName));
            mergedConfig.remove(middlewareName);
        }

        for (String serviceName : servicesToDelete) {
            LOGGER.warn("Service defined multiple times with different configurations in '{}'",
                    routers.get(serviceName));
            mergedHttpConfig.remove(serviceName);
        }

        return mergedConfig;
    }

    public static JsonObject getObjByKeyWithValue(JsonArray jsonArr, String key, String value) {
        if (jsonArr == null) {
            return null;
        }
        final int size = jsonArr.size();
        for (int i = 0; i < size; i++) {
            final JsonObject obj;
            try {
                obj = jsonArr.getJsonObject(i);
            }
            catch (ClassCastException e) {
                return null;
            }
            if (obj == null) {
                return null;
            }
            if (obj.containsKey(key) && obj.getString(key).equals(value)) {
                return obj;
            }
        }
        return null;
    }

    /**
     * Validates a JSON object representing a dynamic configuration instance.
     *
     * @param vertx    a Vertx instance
     * @param json     the json object to validate
     * @param complete if set to true, all references to objects need to point to existing objects (e.g. router middlewares and router services)
     * @return a Future that will succeed or fail eventually
     */
    public static Future<Void> validate(Vertx vertx, JsonObject json, boolean complete) {
        if (validator == null) {
            validator = buildValidator(vertx);
        }

        final Promise<Void> validPromise = Promise.promise();
        try {
            final OutputUnit result = validator.validate(json);
            if (!result.getValid()) {
                throw result.toException(json);
            }
        }
        catch (SchemaException | ValidationException e) {
            validPromise.fail(e);
            return validPromise.future();
        }

        final JsonObject httpConfig = json.getJsonObject(HTTP);
        final List<Future> validFutures = Arrays.asList(validateRouters(httpConfig, complete),
                validateMiddlewares(httpConfig), validateServices(httpConfig));

        CompositeFuture.all(validFutures).onSuccess(h -> {
            validPromise.complete();
        }).onFailure(err -> {
            validPromise.fail(err.getMessage());
        });

        return validPromise.future();
    }

    private static Future<Void> validateRouters(JsonObject httpConfig, boolean complete) {
        final JsonArray routers = httpConfig.getJsonArray(ROUTERS);
        if (routers == null || routers.size() == 0) {
            LOGGER.warn("No routers defined");
            return Future.succeededFuture();
        }

        final Set<String> routerNames = new HashSet<>();
        for (int i = 0; i < routers.size(); i++) {
            final JsonObject router = routers.getJsonObject(i);
            final String routerName = router.getString(ROUTER_NAME);
            if (routerNames.contains(routerName)) {
                final String errMsg = String.format("validateRouters: duplicated router name '%s'. Should be unique.",
                        routerName);
                LOGGER.warn(errMsg);
                return Future.failedFuture(errMsg);
            }
            routerNames.add(routerName);
        }

        if (!complete) {
            return Future.succeededFuture();
        }

        // collect used middlewares and services
        final Set<String> routerMiddlewareNames = new HashSet<>();
        final Set<String> routerServiceNames = new HashSet<>();
        for (int i = 0; i < routers.size(); i++) {
            final JsonObject router = routers.getJsonObject(i);

            final JsonArray routerMwNames = router.getJsonArray(ROUTER_MIDDLEWARES);
            if (routerMwNames != null) {
                for (int j = 0; j < routerMwNames.size(); j++) {
                    final String routerMwName = routerMwNames.getString(j);
                    if (routerMwName != null) {
                        routerMiddlewareNames.add(routerMwName);
                    }
                }
            }

            final String routerSvName = router.getString(ROUTER_SERVICE);
            if (routerSvName != null) {
                routerServiceNames.add(routerSvName);
            }
        }

        // check whether alls used middlewares and services are defined
        final JsonArray middlewares = httpConfig.getJsonArray(MIDDLEWARES);
        final JsonArray services = httpConfig.getJsonArray(SERVICES);

        for (String mwName : routerMiddlewareNames) {
            if (getObjByKeyWithValue(middlewares, MIDDLEWARE_NAME, mwName) == null) {
                final String errMsg = String.format("validateRouters: unknown middleware '%s' defined", mwName);
                LOGGER.warn(errMsg);
                return Future.failedFuture(errMsg);
            }
        }

        for (String svName : routerServiceNames) {
            if (getObjByKeyWithValue(services, SERVICE_NAME, svName) == null) {
                final String errMsg = String.format("validateRouters: unknown service '%s' defined", svName);
                LOGGER.warn(errMsg);
                return Future.failedFuture(errMsg);
            }
        }

        return Future.succeededFuture();
    }

    public static Future<Void> validateMiddlewares(JsonObject httpConfig) {
        final JsonArray mws = httpConfig.getJsonArray(MIDDLEWARES);
        if (mws == null || mws.size() == 0) {
            LOGGER.debug("No middlewares defined");
            return Future.succeededFuture();
        }

        final Set<String> mwNames = new HashSet<>();
        for (int i = 0; i < mws.size(); i++) {
            final JsonObject mw = mws.getJsonObject(i);
            final String mwName = mw.getString(MIDDLEWARE_NAME);
            final String mwType = mw.getString(MIDDLEWARE_TYPE);
            final JsonObject mwOptions = mw.getJsonObject(MIDDLEWARE_OPTIONS, new JsonObject());

            if (mwNames.contains(mwName)) {
                final String errMsg = String.format(
                        "validateMiddlewares: duplicated middleware name '%s'. Should be unique.",
                        mwName);
                LOGGER.warn(errMsg);
                return Future.failedFuture(errMsg);
            }
            mwNames.add(mwName);

            switch (mwType) {
                case MIDDLEWARE_AUTHORIZATION_BEARER: {
                    final Future<Void> validationResult = validateWithAuthToken(mwType, mwOptions);
                    if (validationResult != null) {
                        return validationResult;
                    }
                    break;
                }
                case MIDDLEWARE_BEARER_ONLY: {
                    final Future<Void> validationResult = validateWithAuthHandler(mwType, mwOptions);
                    if (validationResult != null) {
                        return validationResult;
                    }
                    break;
                }
                case MIDDLEWARE_HEADERS: {
                    final JsonObject requestHeaders = mwOptions.getJsonObject(MIDDLEWARE_HEADERS_REQUEST);
                    if (requestHeaders != null) {
                        if (requestHeaders.isEmpty()) {
                            return Future.failedFuture(String.format("%s: Empty request headers defined", mwType));
                        }

                        for (Entry<String, Object> entry : requestHeaders) {
                            if (entry.getKey() == null || !(entry.getValue() instanceof String)) {
                                return Future.failedFuture(String
                                        .format("%s: Request header and value can only be of type string", mwType));
                            }
                        }
                    }

                    final JsonObject responseHeaders = mwOptions.getJsonObject(MIDDLEWARE_HEADERS_RESPONSE);
                    if (responseHeaders != null) {
                        if (responseHeaders.isEmpty()) {
                            return Future.failedFuture(String.format("%s: Empty response headers defined", mwType));
                        }

                        for (Entry<String, Object> entry : responseHeaders) {
                            if (entry.getKey() == null || !(entry.getValue() instanceof String)) {
                                return Future.failedFuture(String
                                        .format("%s: Response header and value can only be of type string", mwType));
                            }
                        }
                    }

                    if (requestHeaders == null && responseHeaders == null) {
                        return Future.failedFuture(
                                String.format("%s: at least one response or request header has to be defined", mwType));
                    }

                    break;
                }
                case MIDDLEWARE_OAUTH2:
                case MIDDLEWARE_OAUTH2_REGISTRATION: {
                    final String clientID = mwOptions.getString(MIDDLEWARE_OAUTH2_CLIENTID);
                    if (clientID == null || clientID.length() == 0) {
                        return Future.failedFuture(String.format("%s: No client ID defined", mwType));
                    }

                    final String clientSecret = mwOptions.getString(MIDDLEWARE_OAUTH2_CLIENTSECRET);
                    if (clientSecret == null || clientSecret.length() == 0) {
                        return Future.failedFuture(String.format("%s: No client secret defined", mwType));
                    }

                    final String discoveryUrl = mwOptions.getString(MIDDLEWARE_OAUTH2_DISCOVERYURL);
                    if (discoveryUrl == null || discoveryUrl.length() == 0) {
                        return Future.failedFuture(String.format("%s: No discovery URL defined", mwType));
                    }

                    final String sessionScope = mwOptions.getString(MIDDLEWARE_OAUTH2_SESSION_SCOPE);
                    if (sessionScope == null || sessionScope.length() == 0) {
                        return Future.failedFuture(String.format("%s: No session scope defined", mwType));
                    }

                    final String responseMode = mwOptions.getString(MIDDLEWARE_OAUTH2_RESPONSE_MODE);
                    if (responseMode == null) {
                        LOGGER.debug(String.format("%s: value not specified. Use default value: %s",
                                MIDDLEWARE_OAUTH2_RESPONSE_MODE, OAuth2MiddlewareFactory.OIDC_RESPONSE_MODE_DEFAULT));
                    }
                    else if (!OIDC_RESPONSE_MODES.contains(responseMode)) {
                        return Future.failedFuture(String.format("%s: value '%s' not allowed, must be one on %s",
                                MIDDLEWARE_OAUTH2_RESPONSE_MODE, responseMode, OIDC_RESPONSE_MODES));
                    }

                    break;
                }
                case MIDDLEWARE_REDIRECT_REGEX: {
                    final String regex = mwOptions.getString(MIDDLEWARE_REDIRECT_REGEX_REGEX);
                    if (regex == null || regex.length() == 0) {
                        return Future.failedFuture(String.format("%s: No regex defined", mwType));
                    }

                    final String replacement = mwOptions.getString(MIDDLEWARE_REDIRECT_REGEX_REPLACEMENT);
                    if (replacement == null || replacement.length() == 0) {
                        return Future.failedFuture(String.format("%s: No replacement defined", mwType));
                    }

                    break;
                }
                case MIDDLEWARE_REPLACE_PATH_REGEX: {
                    final String regex = mwOptions.getString(MIDDLEWARE_REPLACE_PATH_REGEX_REGEX);
                    if (regex == null || regex.length() == 0) {
                        return Future.failedFuture(String.format("%s: No regex defined", mwType));
                    }

                    final String replacement = mwOptions.getString(MIDDLEWARE_REPLACE_PATH_REGEX_REPLACEMENT);
                    if (replacement == null || replacement.length() == 0) {
                        return Future.failedFuture(String.format("%s: No replacement defined", mwType));
                    }

                    break;
                }
                case MIDDLEWARE_SHOW_SESSION_CONTENT:
                case MIDDLEWARE_LANGUAGE_COOKIE: {
                    break;
                }
                case MIDDLEWARE_SESSION_BAG: {
                    JsonArray whitelistedCookies = mwOptions.getJsonArray(MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIES);
                    if (whitelistedCookies == null) {
                        whitelistedCookies = mwOptions.getJsonArray(MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIES_LEGACY);
                        if (whitelistedCookies == null) {
                            return Future.failedFuture(String.format("%s: No whitelisted cookies defined.", mwType));
                        }
                    }
                    for (int j = 0; j < whitelistedCookies.size(); j++) {
                        final JsonObject whitelistedCookie = whitelistedCookies.getJsonObject(j);
                        if (!whitelistedCookie.containsKey(MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIE_NAME)
                                || whitelistedCookie.getString(MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIE_NAME)
                                .isEmpty()) {
                            return Future.failedFuture(
                                    String.format("%s: whitelisted cookie name has to contain a value", mwType));
                        }
                        if (!whitelistedCookie.containsKey(MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIE_PATH)
                                || whitelistedCookie.getString(MIDDLEWARE_SESSION_BAG_WHITELISTED_COOKIE_PATH)
                                .isEmpty()) {
                            return Future.failedFuture(
                                    String.format("%s: whitelisted cookie path has to contain a value", mwType));
                        }
                    }
                    break;
                }
                case MIDDLEWARE_CONTROL_API: {
                    final String action = mwOptions.getString(MIDDLEWARE_CONTROL_API_ACTION);
                    if (action == null) {
                        return Future.failedFuture(
                                String.format("%s: No control api action defined", mwType));
                    }

                    if (!Objects.equals(action, ControlApiMiddleware.SESSION_TERMINATE_ACTION) &&
                            !Objects.equals(action, ControlApiMiddleware.SESSION_RESET_ACTION)) {
                        return Future
                                .failedFuture(String.format("%s: Not supported control api action defined.", mwType));
                    }
                    break;
                }
                case MIDDLEWARE_SESSION: {
                    final Integer sessionIdleTimeoutInMinutes = mwOptions
                            .getInteger(MIDDLEWARE_SESSION_IDLE_TIMEOUT_IN_MINUTES);
                    if (sessionIdleTimeoutInMinutes == null) {
                        LOGGER.debug(String.format("%s: Session idle timeout not specified. Use default value: %s",
                                mwType,
                                SessionMiddleware.SESSION_IDLE_TIMEOUT_IN_MINUTE_DEFAULT));
                    }
                    else {
                        if (sessionIdleTimeoutInMinutes <= 0) {
                            return Future.failedFuture(String
                                    .format("%s: Session idle timeout is required to be positive number", mwType));
                        }
                    }
                    final Integer sessionIdMinLength = mwOptions.getInteger(MIDDLEWARE_SESSION_ID_MIN_LENGTH);
                    if (sessionIdMinLength == null) {
                        LOGGER.debug(String.format("%s: Minimum session id length not specified. Use default value: %s",
                                mwType,
                                SessionMiddleware.SESSION_ID_MINIMUM_LENGTH_DEFAULT));
                    }
                    else {
                        if (sessionIdMinLength <= 0) {
                            return Future.failedFuture(String
                                    .format("%s: Minimum session id length is required to be positive number", mwType));
                        }
                    }
                    final Boolean nagHttps = mwOptions.getBoolean(MIDDLEWARE_SESSION_NAG_HTTPS);
                    if (nagHttps == null) {
                        LOGGER.debug(String.format("%s: NagHttps not specified. Use default value: %s", mwType,
                                SessionMiddleware.NAG_HTTPS_DEFAULT));
                    }
                    final Boolean lifetimeHeader = mwOptions.getBoolean(MIDDLEWARE_SESSION_LIFETIME_HEADER);
                    if (lifetimeHeader == null) {
                        LOGGER.debug(String.format("%s: LifetimeHeader not specified. Use default value: %s", mwType,
                                SessionMiddleware.SESSION_LIFETIME_HEADER_DEFAULT));
                    }
                    final Boolean lifetimeCookie = mwOptions.getBoolean(MIDDLEWARE_SESSION_LIFETIME_COOKIE);
                    if (lifetimeCookie == null) {
                        LOGGER.debug(String.format("%s: LifetimeCookie not specified. Use default value: %s", mwType,
                                SessionMiddleware.SESSION_LIFETIME_COOKIE_DEFAULT));
                    }
                    final JsonObject cookie = mwOptions.getJsonObject(MIDDLEWARE_SESSION_COOKIE);
                    if (cookie == null) {
                        LOGGER.debug(String.format("%s: Cookie settings not specified. Use default setting", mwType));
                    }
                    else {
                        final String cookieName = cookie.getString(MIDDLEWARE_SESSION_COOKIE_NAME);
                        if (cookieName == null) {
                            LOGGER.debug(String.format(
                                    "%s: No session cookie name specified to be removed. Use default value: %s", mwType,
                                    SessionMiddleware.COOKIE_NAME_DEFAULT));
                        }
                        final Boolean cookieHttpOnly = cookie.getBoolean(MIDDLEWARE_SESSION_COOKIE_HTTP_ONLY);
                        if (cookieHttpOnly == null) {
                            LOGGER.debug(String.format("%s: Cookie HttpOnly not specified. Use default value: %s",
                                    mwType, SessionMiddleware.COOKIE_HTTP_ONLY_DEFAULT));
                        }
                        final String cookieSameSite = cookie.getString(MIDDLEWARE_SESSION_COOKIE_SAME_SITE);
                        if (cookieSameSite == null) {
                            LOGGER.debug(String.format("%s: Cookie SameSite not specified. Use default value: %s",
                                    mwType, SessionMiddleware.COOKIE_SAME_SITE_DEFAULT));
                        }
                        else {
                            try {
                                CookieSameSite.valueOf(cookieSameSite);
                            }
                            catch (RuntimeException exception) {
                                final List<String> allowedPolicies = new LinkedList<>();
                                for (CookieSameSite value : CookieSameSite.values()) {
                                    allowedPolicies.add(value.toString().toUpperCase());
                                }
                                return Future.failedFuture(
                                        String.format("%s: invalid cookie same site value. Allowed values: %s", mwType,
                                                allowedPolicies));
                            }
                        }
                    }
                    break;
                }
                case MIDDLEWARE_RESPONSE_SESSION_COOKIE_REMOVAL: {
                    final String name = mwOptions.getString(MIDDLEWARE_RESPONSE_SESSION_COOKIE_REMOVAL_NAME);
                    if (name == null) {
                        LOGGER.debug(String.format(
                                "%s: No session cookie name specified to be removed. Use default value: %s",
                                mwType,
                                SessionMiddleware.COOKIE_NAME_DEFAULT));
                    }
                    break;
                }
                case MIDDLEWARE_REPLACED_SESSION_COOKIE_DETECTION: {
                    final Integer waitTimeRetryInMs = mwOptions
                            .getInteger(MIDDLEWARE_REPLACED_SESSION_COOKIE_DETECTION_WAIT_BEFORE_RETRY_MS);
                    if (waitTimeRetryInMs == null) {
                        LOGGER.debug(String.format("%s: No wait time for redirect specified. Use default value: %s",
                                mwType,
                                ReplacedSessionCookieDetectionMiddleware.DEFAULT_WAIT_BEFORE_RETRY_MS));
                    }
                    else {
                        if (waitTimeRetryInMs <= 0) {
                            return Future.failedFuture(
                                    String.format("%s: wait time for retry required to be positive", mwType));
                        }
                    }
                    final String detectionCookieName = mwOptions
                            .getString(MIDDLEWARE_REPLACED_SESSION_COOKIE_DETECTION_COOKIE_NAME);
                    if (detectionCookieName == null) {
                        LOGGER.debug(String.format("%s: No detection cookie name. Use default value: %s",
                                mwType,
                                ReplacedSessionCookieDetectionMiddleware.DEFAULT_DETECTION_COOKIE_NAME));
                    }
                    break;
                }
                case MIDDLEWARE_CSP: {
                    final JsonArray directives = mwOptions.getJsonArray(MIDDLEWARE_CSP_DIRECTIVES);
                    if (directives == null) {
                        return Future.failedFuture(
                                String.format("Directive is not defined as JsonObject, middleware: '%s'", mwType));
                    }
                    else {
                        for (Object directive : directives) {
                            if (directive instanceof JsonObject) {
                                final String directiveName = ((JsonObject) directive)
                                        .getString(MIDDLEWARE_CSP_DIRECTIVE_NAME);
                                if (directiveName == null) {
                                    return Future.failedFuture(
                                            String.format("Directive name is not defined, middleware: '%s'", mwType));
                                }
                                final JsonArray directiveValues = ((JsonObject) directive)
                                        .getJsonArray(MIDDLEWARE_CSP_DIRECTIVE_VALUES);
                                if (directiveValues == null) {
                                    return Future.failedFuture(
                                            String.format("Directive values is not defined, middleware: '%s'", mwType));
                                }
                                for (Object a : directiveValues.getList()) {
                                    if (!(a instanceof String)) {
                                        return Future.failedFuture(
                                                String.format(
                                                        "%s: Directive values is required to be a list of strings.",
                                                        mwType));
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
                case MIDDLEWARE_CSRF: {
                    final Integer timeoutInMinutes = mwOptions
                            .getInteger(MIDDLEWARE_CSRF_TIMEOUT_IN_MINUTES);
                    if (timeoutInMinutes == null) {
                        LOGGER.debug(String.format("%s: csrf token timeout not specified. Use default value: %s",
                                mwType,
                                CSRFMiddleware.DEFAULT_TIMEOUT_IN_MINUTES));
                    }
                    else {
                        if (timeoutInMinutes <= 0) {
                            return Future.failedFuture(String
                                    .format("%s: csrf token timeout is required to be a positive number", mwType));
                        }
                    }
                    final String origin = mwOptions.getString(MIDDLEWARE_CSRF_ORIGIN);
                    if (origin != null && (origin.isEmpty() || origin.isBlank())) {
                        return Future
                                .failedFuture(String.format("%s: if origin is defined it should not be empty or blank!",
                                        mwType));
                    }
                    final Boolean nagHttps = mwOptions.getBoolean(MIDDLEWARE_CSRF_NAG_HTTPS);
                    if (nagHttps == null) {
                        LOGGER.debug(String.format("%s: NagHttps not specified. Use default value: %s", mwType,
                                CSRFMiddleware.DEFAULT_NAG_HTTPS));
                    }
                    final String headerName = mwOptions.getString(MIDDLEWARE_CSRF_HEADER_NAME);
                    if (headerName == null) {
                        LOGGER.debug(String.format("%s: header name not specified. Use default value: %s", mwType,
                                CSRFMiddleware.DEFAULT_HEADER_NAME));
                    }
                    final JsonObject cookie = mwOptions.getJsonObject(MIDDLEWARE_CSRF_COOKIE);
                    if (cookie == null) {
                        LOGGER.debug(String.format("%s: Cookie settings not specified. Use default setting", mwType));
                    }
                    else {
                        final String cookieName = cookie.getString(MIDDLEWARE_CSRF_COOKIE_NAME);
                        if (cookieName == null) {
                            LOGGER.debug(String.format(
                                    "%s: No session cookie name specified to be removed. Use default value: %s", mwType,
                                    SessionMiddleware.COOKIE_NAME_DEFAULT));
                        }
                        final String cookiePath = cookie.getString(MIDDLEWARE_CSRF_COOKIE_PATH);
                        if (cookiePath == null) {
                            LOGGER.debug(String.format(
                                    "%s: No session cookie name specified to be removed. Use default value: %s", mwType,
                                    CSRFMiddleware.DEFAULT_COOKIE_NAME));
                        }
                        final Boolean cookieSecure = cookie.getBoolean(MIDDLEWARE_CSRF_COOKIE_SECURE);
                        if (cookieSecure == null) {
                            LOGGER.debug(String.format(
                                    "%s: No session cookie name specified to be removed. Use default value: %s", mwType,
                                    CSRFMiddleware.DEFAULT_COOKIE_SECURE));
                        }
                    }
                    break;
                }
                case MIDDLEWARE_PASS_AUTHORIZATION: {
                    Future<Void> validationResult = validateWithAuthToken(mwType, mwOptions);
                    if (validationResult != null) {
                        return validationResult;
                    }

                    validationResult = validateWithAuthHandler(mwType, mwOptions);
                    if (validationResult != null) {
                        return validationResult;
                    }
                    break;
                }
                case MIDDLEWARE_REQUEST_RESPONSE_LOGGER: {
                    break;
                }
                case MIDDLEWARE_CHECK_ROUTE: {
                    break;
                }
                default: {
                    return Future.failedFuture(String.format("Unknown middleware: '%s'", mwType));
                }
            }
        }

        return Future.succeededFuture();
    }

    private static Future<Void> validateServices(JsonObject httpConfig) {
        final JsonArray svs = httpConfig.getJsonArray(SERVICES);
        if (svs == null || svs.size() == 0) {
            LOGGER.debug("No services defined");
            return Future.succeededFuture();
        }

        final Set<String> svNames = new HashSet<>();
        for (int i = 0; i < svs.size(); i++) {
            final JsonObject sv = svs.getJsonObject(i);
            final String svName = sv.getString(SERVICE_NAME);
            if (svNames.contains(svName)) {
                final String errMsg = String.format("validateServices: duplicated service name '%s'. Should be unique.",
                        svName);
                LOGGER.warn(errMsg);
                return Future.failedFuture(errMsg);
            }
            svNames.add(svName);

            final JsonArray servers = sv.getJsonArray(SERVICE_SERVERS);
            if (servers == null || servers.size() == 0) {
                final String errMsg = "validateServices: no servers defined";
                LOGGER.debug(errMsg);
                return Future.failedFuture(errMsg);
            }
        }

        return Future.succeededFuture();
    }

    public static Future<Void> validateWithAuthHandler(String mwType, JsonObject mwOptions) {
        final String publicKey = mwOptions.getString(MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEY);
        if (publicKey == null) {
            return Future.failedFuture(String.format("%s: No public key defined", mwType));
        }
        else if (publicKey.length() == 0) {
            return Future.failedFuture(String.format("%s: Empty public key defined", mwType));
        }

        // the public key has to be either a valid URL to fetch it from or base64 encoded
        boolean isBase64;
        try {
            Base64.getDecoder().decode(publicKey);
            isBase64 = true;
        }
        catch (IllegalArgumentException e) {
            isBase64 = false;
        }

        boolean isURL = false;
        if (!isBase64) {
            try {
                new URL(publicKey).toURI();
                isURL = true;
            }
            catch (MalformedURLException | URISyntaxException e) {
                isURL = false;
            }
        }

        if (!isBase64 && !isURL) {
            return Future.failedFuture(String
                    .format("%s: Public key is required to either be base64 encoded or a valid URL",
                            mwType));
        }

        final String publicKeyAlgorithm = mwOptions.getString(MIDDLEWARE_WITH_AUTH_HANDLER_PUBLIC_KEY_ALGORITHM);
        if (publicKeyAlgorithm.length() == 0) {
            return Future.failedFuture(String.format("%s: Invalid public key algorithm", mwType));
        }

        final String issuer = mwOptions.getString(MIDDLEWARE_WITH_AUTH_HANDLER_ISSUER);
        if (issuer != null && issuer.length() == 0) {
            return Future.failedFuture(String.format("%s: Empty issuer defined", mwType));
        }

        final JsonArray audience = mwOptions.getJsonArray(MIDDLEWARE_WITH_AUTH_HANDLER_AUDIENCE);
        if (audience != null) {
            if (audience.size() == 0) {
                return Future.failedFuture(String.format("%s: Empty audience defined.", mwType));
            }
            for (Object a : audience.getList()) {
                if (!(a instanceof String)) {
                    return Future.failedFuture(
                            String.format("%s: Audience is required to be a list of strings.", mwType));
                }
            }
        }
        final JsonArray claims = mwOptions.getJsonArray(MIDDLEWARE_WITH_AUTH_HANDLER_CLAIMS);
        if (claims != null) {
            if (claims.size() == 0) {
                LOGGER.debug("Claims is empty");
            }

            for (Object claim : claims.getList()) {
                if (claim instanceof Map) {
                    claim = new JsonObject((Map<String, Object>) claim);
                }
                if (!(claim instanceof JsonObject)) {
                    return Future.failedFuture("Claim is required to be a JsonObject");
                }
                else {
                    final JsonObject cObj = (JsonObject) claim;
                    if (cObj.size() != 3) {
                        return Future.failedFuture(String.format(
                                "%s: Claim is required to contain exactly 3 entries. Namely: claimPath, operator and value",
                                mwType));
                    }
                    if (!(cObj.containsKey(MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_PATH)
                            && cObj.containsKey(MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_OPERATOR)
                            && cObj.containsKey(MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_VALUE))) {
                        return Future.failedFuture(String.format(
                                "%s: Claim is missing at least 1 key. Required keys: %s, %s, %s", mwType,
                                MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_OPERATOR, MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_PATH,
                                MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_VALUE));
                    }

                    if (cObj.getString(MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_PATH) == null) {
                        return Future.failedFuture(
                                String.format("%s: %s value is required to be a String", mwType,
                                        MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_PATH));
                    }
                    else {
                        final String path = cObj.getString(MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_PATH);
                        try {
                            final Path p = PathCompiler.compile(path);
                            LOGGER.debug(p.toString());
                        }
                        catch (RuntimeException e) {
                            LOGGER.debug(String.format("Invalid claimpath %s", path));
                            return Future
                                    .failedFuture(String.format("%s: Invalid claimpath %s", mwType, path));
                        }
                    }
                    if (cObj.getString(MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_OPERATOR) == null) {
                        return Future.failedFuture(
                                String.format("%s: %s value is required to be a String", mwType,
                                        MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_OPERATOR));
                    }
                    else {
                        final String operator = cObj.getString(MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_OPERATOR);
                        if (!(operator.equals(MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_OPERATOR_EQUALS)
                                || operator.equals(MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_OPERATOR_CONTAINS))) {
                            return Future.failedFuture(String.format(
                                    "%s: %s value is illegal. Actual operator: %s .Allowed operators: %s, %s",
                                    mwType,
                                    MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_OPERATOR, operator,
                                    MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_OPERATOR_EQUALS,
                                    MIDDLEWARE_WITH_AUTH_HANDLER_CLAIM_OPERATOR_CONTAINS));
                        }
                    }

                }
            }
        }
        else {
            LOGGER.debug("No custom claims defined!");
        }
        return null;
    }

    private static Future<Void> validateWithAuthToken(String mwType, JsonObject mwOptions) {
        final String sessionScope = mwOptions.getString(MIDDLEWARE_WITH_AUTH_TOKEN_SESSION_SCOPE);
        if (sessionScope == null || sessionScope.length() == 0) {
            return Future.failedFuture(String.format("%s: No session scope defined", mwType));
        }
        return null;
    }

    private static Boolean addRouter(JsonObject httpConf, String routerName, JsonObject routerToAdd) {
        final JsonArray existingRouters = httpConf.getJsonArray(DynamicConfiguration.ROUTERS);
        final JsonObject existingRouter = getObjByKeyWithValue(existingRouters, DynamicConfiguration.ROUTER_NAME,
                routerName);
        if (existingRouter == null) {
            existingRouters.add(routerToAdd);
            return true;
        }

        return existingRouter.equals(routerToAdd);
    }

    private static Boolean addService(JsonObject httpConf, String serviceName, JsonObject serviceToAdd) {
        final JsonArray existingServices = httpConf.getJsonArray(DynamicConfiguration.SERVICES);
        final JsonObject existingService = getObjByKeyWithValue(existingServices, DynamicConfiguration.SERVICE_NAME,
                serviceName);
        if (existingService == null) {
            existingServices.add(serviceToAdd);
            return true;
        }

        final Map<String, JsonObject> uniqueServers = new HashMap<>();

        final JsonArray existingServers = existingService.getJsonArray(DynamicConfiguration.SERVICE_SERVERS);
        for (int i = 0; i < existingServers.size(); i++) {
            final JsonObject server = existingServers.getJsonObject(i);
            final String url = createURL(server.getString(DynamicConfiguration.SERVICE_SERVER_HOST),
                    server.getString(DynamicConfiguration.SERVICE_SERVER_PORT));
            uniqueServers.put(url, server);
        }

        final JsonArray serversToAdd = serviceToAdd.getJsonArray(DynamicConfiguration.SERVICE_SERVERS);
        for (int i = 0; i < serversToAdd.size(); i++) {
            final JsonObject serverToAdd = serversToAdd.getJsonObject(i);
            final String url = createURL(serverToAdd.getString(DynamicConfiguration.SERVICE_SERVER_HOST),
                    serverToAdd.getString(DynamicConfiguration.SERVICE_SERVER_PORT));
            if (!uniqueServers.containsKey(url)) {
                existingServers.add(serverToAdd);
            }
        }

        return true;
    }

    private static String createURL(String host, String port) {
        return String.format("%s:%s", host, port);
    }

    private static Boolean addMiddleware(JsonObject httpConf, String middlewareName, JsonObject middlewareToAdd) {
        final JsonArray existingMiddlewares = httpConf.getJsonArray(DynamicConfiguration.MIDDLEWARES);
        final JsonObject existingMiddleware = getObjByKeyWithValue(existingMiddlewares,
                DynamicConfiguration.MIDDLEWARE_NAME,
                middlewareName);
        if (existingMiddleware == null) {
            existingMiddlewares.add(middlewareToAdd);
            return true;
        }

        return existingMiddleware.equals(middlewareToAdd);
    }
}
