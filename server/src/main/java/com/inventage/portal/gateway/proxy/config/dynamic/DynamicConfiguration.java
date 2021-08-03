package com.inventage.portal.gateway.proxy.config.dynamic;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;

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

    public static final String MIDDLEWARE_AUTHORIZATION_BEARER = "authorizationBearer";
    public static final String MIDDLEWARE_AUTHORIZATION_BEARER_SESSION_SCOPE = "sessionScope";

    public static final String MIDDLEWARE_BEARER_ONLY = "bearerOnly";
    public static final String MIDDLEWARE_BEARER_ONLY_PUBLIC_KEY = "publicKey";
    public static final String MIDDLEWARE_BEARER_ONLY_PUBLIC_KEY_FROM_URL = "publicKeyFromUrl";
    public static final String MIDDLEWARE_BEARER_ONLY_PUBLIC_KEY_ALGORITHM = "publicKeyAlgorithm";
    public static final String MIDDLEWARE_BEARER_ONLY_ISSUER = "issuer";
    public static final String MIDDLEWARE_BEARER_ONLY_AUDIENCE = "audience";

    public static final String MIDDLEWARE_OAUTH2 = "oauth2";
    public static final String MIDDLEWARE_OAUTH2_CLIENTID = "clientId";
    public static final String MIDDLEWARE_OAUTH2_CLIENTSECRET = "clientSecret";
    public static final String MIDDLEWARE_OAUTH2_DISCOVERYURL = "discoveryUrl";
    public static final String MIDDLEWARE_OAUTH2_SESSION_SCOPE = "sessionScope";
    public static final String MIDDLEWARE_OAUTH2_SESSION_SCOPE_ID = "id";

    public static final String MIDDLEWARE_SHOW_SESSION_CONTENT = "_session_";

    public static final String MIDDLEWARE_SESSION_BAG = "sessionBag";
    public static final String MIDDLEWARE_SESSION_BAG_WHITHELISTED_COOKIES = "whithelistedCookies";
    public static final String MIDDLEWARE_SESSION_BAG_WHITHELISTED_COOKIE_NAME = "name";
    public static final String MIDDLEWARE_SESSION_BAG_WHITHELISTED_COOKIE_PATH = "path";

    public static final List<String> MIDDLEWARE_TYPES = Arrays.asList(MIDDLEWARE_REPLACE_PATH_REGEX,
            MIDDLEWARE_REDIRECT_REGEX, MIDDLEWARE_HEADERS, MIDDLEWARE_AUTHORIZATION_BEARER, MIDDLEWARE_BEARER_ONLY,
            MIDDLEWARE_OAUTH2, MIDDLEWARE_SHOW_SESSION_CONTENT, MIDDLEWARE_SESSION_BAG);

    public static final String SERVICES = "services";
    public static final String SERVICE_NAME = "name";
    public static final String SERVICE_SERVERS = "servers";
    public static final String SERVICE_SERVER_HOST = "host";
    public static final String SERVICE_SERVER_PORT = "port";

    private static Schema schema;

    private static Schema buildSchema(Vertx vertx) {
        ObjectSchemaBuilder routerSchema = Schemas.objectSchema().requiredProperty(ROUTER_NAME, Schemas.stringSchema())
                .property(ROUTER_ENTRYPOINTS, Schemas.arraySchema().items(Schemas.stringSchema()))
                .property(ROUTER_MIDDLEWARES, Schemas.arraySchema().items(Schemas.stringSchema()))
                .requiredProperty(ROUTER_SERVICE, Schemas.stringSchema()).property(ROUTER_RULE, Schemas.stringSchema())
                .property(ROUTER_PRIORITY, Schemas.intSchema()).allowAdditionalProperties(false);

        ObjectSchemaBuilder middlewareOptionsSchema = Schemas.objectSchema()
                .property(MIDDLEWARE_REPLACE_PATH_REGEX_REGEX, Schemas.stringSchema())
                .property(MIDDLEWARE_REPLACE_PATH_REGEX_REPLACEMENT, Schemas.stringSchema())
                .property(MIDDLEWARE_REDIRECT_REGEX_REGEX, Schemas.stringSchema())
                .property(MIDDLEWARE_REDIRECT_REGEX_REPLACEMENT, Schemas.stringSchema())
                .property(MIDDLEWARE_AUTHORIZATION_BEARER_SESSION_SCOPE, Schemas.stringSchema())
                .property(MIDDLEWARE_BEARER_ONLY_PUBLIC_KEY, Schemas.stringSchema())
                .property(MIDDLEWARE_BEARER_ONLY_PUBLIC_KEY_FROM_URL, Schemas.stringSchema())
                .property(MIDDLEWARE_BEARER_ONLY_PUBLIC_KEY_ALGORITHM, Schemas.stringSchema())
                .property(MIDDLEWARE_BEARER_ONLY_ISSUER, Schemas.stringSchema())
                .property(MIDDLEWARE_BEARER_ONLY_AUDIENCE, Schemas.arraySchema())
                .property(MIDDLEWARE_OAUTH2_CLIENTID, Schemas.stringSchema())
                .property(MIDDLEWARE_OAUTH2_CLIENTSECRET, Schemas.stringSchema())
                .property(MIDDLEWARE_OAUTH2_DISCOVERYURL, Schemas.stringSchema())
                .property(MIDDLEWARE_OAUTH2_SESSION_SCOPE, Schemas.stringSchema())
                .property(MIDDLEWARE_HEADERS_REQUEST, Schemas.objectSchema())
                .property(MIDDLEWARE_HEADERS_RESPONSE, Schemas.objectSchema())
                .property(MIDDLEWARE_SESSION_BAG_WHITHELISTED_COOKIES, Schemas.arraySchema())
                .allowAdditionalProperties(false);

        ObjectSchemaBuilder middlewareSchema = Schemas.objectSchema()
                .requiredProperty(MIDDLEWARE_NAME, Schemas.stringSchema())
                .requiredProperty(MIDDLEWARE_TYPE, Schemas.stringSchema())
                .requiredProperty(MIDDLEWARE_OPTIONS, middlewareOptionsSchema).allowAdditionalProperties(false);

        ObjectSchemaBuilder serviceSchema = Schemas.objectSchema()
                .requiredProperty(SERVICE_NAME, Schemas.stringSchema())
                .requiredProperty(SERVICE_SERVERS, Schemas.arraySchema()
                        .items(Schemas.objectSchema().requiredProperty(SERVICE_SERVER_HOST, Schemas.stringSchema())
                                .requiredProperty(SERVICE_SERVER_PORT, Schemas.intSchema())
                                .allowAdditionalProperties(false)))
                .allowAdditionalProperties(false);

        ObjectSchemaBuilder httpSchema = Schemas.objectSchema()
                .property(ROUTERS, Schemas.arraySchema().items(routerSchema))
                .property(MIDDLEWARES, Schemas.arraySchema().items(middlewareSchema))
                .property(SERVICES, Schemas.arraySchema().items(serviceSchema)).allowAdditionalProperties(false);

        ObjectSchemaBuilder dynamicConfigBuilder = Schemas.objectSchema().requiredProperty(HTTP, httpSchema)
                .allowAdditionalProperties(false);

        SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
        SchemaParser schemaParser = SchemaParser.createDraft201909SchemaParser(schemaRouter);
        return dynamicConfigBuilder.build(schemaParser);
    }

    public static JsonObject buildDefaultConfiguration() {
        JsonObject config = new JsonObject();

        JsonObject http = new JsonObject();

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

        JsonObject httpConfig = config.getJsonObject(DynamicConfiguration.HTTP);
        if (httpConfig == null) {
            return true;
        }

        JsonArray httpRouters = httpConfig.getJsonArray(DynamicConfiguration.ROUTERS);
        JsonArray httpMiddlewares = httpConfig.getJsonArray(DynamicConfiguration.MIDDLEWARES);
        JsonArray httpServices = httpConfig.getJsonArray(DynamicConfiguration.SERVICES);

        Boolean httpEmpty = httpRouters == null && httpMiddlewares == null && httpServices == null;

        return httpEmpty;
    }

    public static JsonObject merge(Map<String, JsonObject> configurations) {
        JsonObject mergedConfig = buildDefaultConfiguration();
        if (configurations == null) {
            return mergedConfig;
        }

        JsonObject mergedHttpConfig = mergedConfig.getJsonObject(DynamicConfiguration.HTTP);
        if (mergedHttpConfig == null) {
            return mergedConfig;
        }

        Map<String, List<String>> routers = new HashMap<>();
        Set<String> routersToDelete = new HashSet<>();

        Map<String, List<String>> middlewares = new HashMap<>();
        Set<String> middlewaresToDelete = new HashSet<>();

        Map<String, List<String>> services = new HashMap<>();
        Set<String> servicesToDelete = new HashSet<>();

        for (String key : configurations.keySet()) {
            JsonObject conf = configurations.get(key);
            JsonObject httpConf = conf.getJsonObject(DynamicConfiguration.HTTP);

            if (httpConf != null) {
                JsonArray rts = httpConf.getJsonArray(DynamicConfiguration.ROUTERS, new JsonArray());
                for (int i = 0; i < rts.size(); i++) {
                    JsonObject rt = rts.getJsonObject(i);
                    String rtName = rt.getString(DynamicConfiguration.ROUTER_NAME);
                    if (!routers.containsKey(rtName)) {
                        routers.put(rtName, new ArrayList<String>());
                    }
                    routers.get(rtName).add(key);
                    if (!addRouter(mergedHttpConfig, rtName, rt)) {
                        routersToDelete.add(rtName);
                    }
                }

                JsonArray mws = httpConf.getJsonArray(DynamicConfiguration.MIDDLEWARES, new JsonArray());
                for (int i = 0; i < mws.size(); i++) {
                    JsonObject mw = mws.getJsonObject(i);
                    String mwName = mw.getString(DynamicConfiguration.MIDDLEWARE_NAME);
                    if (!middlewares.containsKey(mwName)) {
                        middlewares.put(mwName, new ArrayList<String>());
                    }
                    middlewares.get(mwName).add(key);
                    if (!addMiddleware(mergedHttpConfig, mwName, mw)) {
                        middlewaresToDelete.add(mwName);
                    }
                }

                JsonArray svs = httpConf.getJsonArray(DynamicConfiguration.SERVICES, new JsonArray());
                for (int i = 0; i < svs.size(); i++) {
                    JsonObject sv = svs.getJsonObject(i);
                    String svName = sv.getString(DynamicConfiguration.SERVICE_NAME);
                    if (!services.containsKey(svName)) {
                        services.put(svName, new ArrayList<String>());
                    }
                    services.get(svName).add(key);
                    if (!addService(mergedHttpConfig, svName, sv)) {
                        servicesToDelete.add(svName);
                    }
                }
            }
        }

        for (String routerName : routersToDelete) {
            LOGGER.warn("merge: Router defined multiple times with different configurations in '{}'",
                    routers.get(routerName));
            mergedHttpConfig.remove(routerName);
        }

        for (String middlewareName : middlewaresToDelete) {
            LOGGER.warn("merge: Middleware defined multiple times with different configurations in '{}'",
                    routers.get(middlewareName));
            mergedConfig.remove(middlewareName);
        }

        for (String serviceName : servicesToDelete) {
            LOGGER.warn("merge: Service defined multiple times with different configurations in '{}'",
                    routers.get(serviceName));
            mergedHttpConfig.remove(serviceName);
        }

        return mergedConfig;
    }

    public static JsonObject getObjByKeyWithValue(JsonArray jsonArr, String key, String value) {
        if (jsonArr == null) {
            return null;
        }
        int size = jsonArr.size();
        for (int i = 0; i < size; i++) {
            JsonObject obj;
            try {
                obj = jsonArr.getJsonObject(i);
            } catch (ClassCastException e) {
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
        if (schema == null) {
            schema = buildSchema(vertx);
        }

        Promise<Void> validPromise = Promise.promise();
        schema.validateAsync(json).onSuccess(f -> {
            JsonObject httpConfig = json.getJsonObject(HTTP);
            List<Future> validFutures = Arrays.asList(validateRouters(httpConfig, complete),
                    validateMiddlewares(httpConfig), validateServices(httpConfig));

            CompositeFuture.all(validFutures).onSuccess(h -> {
                validPromise.complete();
            }).onFailure(err -> {
                validPromise.fail(err.getMessage());
            });

        }).onFailure(err -> {
            validPromise.fail(err.getMessage());
        });

        return validPromise.future();
    }

    private static Future<Void> validateRouters(JsonObject httpConfig, boolean complete) {
        JsonArray routers = httpConfig.getJsonArray(ROUTERS);
        if (routers == null || routers.size() == 0) {
            LOGGER.warn("validateRouters: no routers defined");
            return Future.succeededFuture();
        }

        Set<String> routerNames = new HashSet<>();
        for (int i = 0; i < routers.size(); i++) {
            JsonObject router = routers.getJsonObject(i);
            String routerName = router.getString(ROUTER_NAME);
            if (routerNames.contains(routerName)) {
                String errMsg = String.format("validateRouters: duplicated router name '%s'. Should be unique.",
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
        Set<String> routerMiddlewareNames = new HashSet<>();
        Set<String> routerServiceNames = new HashSet<>();
        for (int i = 0; i < routers.size(); i++) {
            JsonObject router = routers.getJsonObject(i);

            JsonArray routerMwNames = router.getJsonArray(ROUTER_MIDDLEWARES);
            if (routerMwNames != null) {
                for (int j = 0; j < routerMwNames.size(); j++) {
                    String routerMwName = routerMwNames.getString(j);
                    if (routerMwName != null) {
                        routerMiddlewareNames.add(routerMwName);
                    }
                }
            }

            String routerSvName = router.getString(ROUTER_SERVICE);
            if (routerSvName != null) {
                routerServiceNames.add(routerSvName);
            }
        }

        // check whether alls used middlewares and services are defined
        JsonArray middlewares = httpConfig.getJsonArray(MIDDLEWARES);
        JsonArray services = httpConfig.getJsonArray(SERVICES);

        for (String mwName : routerMiddlewareNames) {
            if (getObjByKeyWithValue(middlewares, MIDDLEWARE_NAME, mwName) == null) {
                String errMsg = String.format("validateRouters: unknown middleware '%s' defined", mwName);
                LOGGER.warn(errMsg);
                return Future.failedFuture(errMsg);
            }
        }

        for (String svName : routerServiceNames) {
            if (getObjByKeyWithValue(services, SERVICE_NAME, svName) == null) {
                String errMsg = String.format("validateRouters: unknown service '%s' defined", svName);
                LOGGER.warn(errMsg);
                return Future.failedFuture(errMsg);
            }
        }

        return Future.succeededFuture();
    }

    private static Future<Void> validateMiddlewares(JsonObject httpConfig) {
        JsonArray mws = httpConfig.getJsonArray(MIDDLEWARES);
        if (mws == null || mws.size() == 0) {
            LOGGER.debug("validateMiddlewares: no middlewares defined");
            return Future.succeededFuture();
        }

        Set<String> mwNames = new HashSet<>();
        for (int i = 0; i < mws.size(); i++) {
            JsonObject mw = mws.getJsonObject(i);
            String mwName = mw.getString(MIDDLEWARE_NAME);
            String mwType = mw.getString(MIDDLEWARE_TYPE);
            JsonObject mwOptions = mw.getJsonObject(MIDDLEWARE_OPTIONS);

            if (mwNames.contains(mwName)) {
                String errMsg = String.format("validateMiddlewares: duplicated middleware name '%s'. Should be unique.",
                        mwName);
                LOGGER.warn(errMsg);
                return Future.failedFuture(errMsg);
            }
            mwNames.add(mwName);

            Boolean valid = true;
            String errMsg = "";
            switch (mwType) {
                case MIDDLEWARE_AUTHORIZATION_BEARER: {
                    String sessionScope = mwOptions.getString(MIDDLEWARE_AUTHORIZATION_BEARER_SESSION_SCOPE);
                    if (sessionScope == null || sessionScope.length() == 0) {
                        valid = false;
                        errMsg = String.format("%s: No session scope defined", mwType);
                    }
                    break;
                }
                case MIDDLEWARE_BEARER_ONLY: {
                    boolean publicKeyProvided = false;

                    String publicKey = mwOptions.getString(MIDDLEWARE_BEARER_ONLY_PUBLIC_KEY);
                    if (publicKey != null) {
                        if (publicKey.length() == 0) {
                            valid = false;
                            errMsg = String.format("%s: Empty public key defined", mwType);
                            break;
                        } else if (publicKey.length() > 0) {
                            try {
                                // public key has to be base64 encoded
                                Base64.getDecoder().decode(publicKey);
                            } catch (IllegalArgumentException e) {
                                valid = false;
                                errMsg = String.format("%s: Public key is required to be base64 encoded", mwType);
                                break;
                            }
                            publicKeyProvided = true;
                        }
                    }

                    String publicKeyFromUrl = mwOptions.getString(MIDDLEWARE_BEARER_ONLY_PUBLIC_KEY_FROM_URL);
                    if (publicKeyFromUrl != null) {
                        if (publicKeyFromUrl.length() == 0) {
                            valid = false;
                            errMsg = String.format("%s: Empty public key URL defined", mwType);
                            break;
                        } else if (publicKeyFromUrl.length() > 0) {
                            try {
                                new URL(publicKeyFromUrl).toURI();
                            } catch (MalformedURLException | URISyntaxException e) {
                                valid = false;
                                errMsg = String.format("%s: Public key URL is required to be a valid URL", mwType);
                                break;
                            }
                            publicKeyProvided = true;
                        }
                    }

                    if (!publicKeyProvided) {
                        valid = false;
                        errMsg = String.format("%s: No public key defined", mwType);
                        break;
                    }

                    String publicKeyAlgorithm = mwOptions.getString(MIDDLEWARE_BEARER_ONLY_PUBLIC_KEY_ALGORITHM);
                    if (publicKeyAlgorithm.length() == 0) {
                        valid = false;
                        errMsg = String.format("%s: Invalid public key algorithm", mwType);
                        break;
                    }

                    String issuer = mwOptions.getString(MIDDLEWARE_BEARER_ONLY_ISSUER);
                    if (issuer == null || issuer.length() == 0) {
                        valid = false;
                        errMsg = String.format("%s: No issuer defined", mwType);
                        break;
                    }

                    JsonArray audience = mwOptions.getJsonArray(MIDDLEWARE_BEARER_ONLY_AUDIENCE);
                    if (audience == null || audience.size() == 0) {
                        valid = false;
                        errMsg = String.format("%s: No audience defined.", mwType);
                        break;
                    }
                    for (Object a : audience.getList()) {
                        if (!(a instanceof String)) {
                            valid = false;
                            errMsg = String.format("%s: Audience is required to be a list of strings.", mwType);
                            break;
                        }
                    }
                    if (!valid) {
                        break;
                    }

                    break;
                }
                case MIDDLEWARE_HEADERS: {
                    JsonObject requestHeaders = mwOptions.getJsonObject(MIDDLEWARE_HEADERS_REQUEST);
                    if (requestHeaders != null) {
                        if (requestHeaders.isEmpty()) {
                            valid = false;
                            errMsg = String.format("%s: Empty request headers defined", mwType);
                            break;
                        }

                        for (Entry<String, Object> entry : requestHeaders) {
                            if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof String)) {
                                valid = false;
                                errMsg = String.format("%s: Request header and value can only be of type string",
                                        mwType);
                                break;
                            }
                        }
                        if (!valid) {
                            break;
                        }
                    }

                    JsonObject responseHeaders = mwOptions.getJsonObject(MIDDLEWARE_HEADERS_RESPONSE);
                    if (responseHeaders != null) {
                        if (responseHeaders.isEmpty()) {
                            valid = false;
                            errMsg = String.format("%s: Empty response headers defined", mwType);
                            break;
                        }

                        for (Entry<String, Object> entry : responseHeaders) {
                            if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof String)) {
                                valid = false;
                                errMsg = String.format("%s: Response header and value can only be of type string",
                                        mwType);
                                break;
                            }
                        }
                        if (!valid) {
                            break;
                        }
                    }

                    if (requestHeaders == null && responseHeaders == null) {
                        valid = false;
                        errMsg = String.format("%s: at least one response or request header has to be defined", mwType);
                        break;
                    }

                    break;
                }
                case MIDDLEWARE_OAUTH2: {
                    String clientID = mwOptions.getString(MIDDLEWARE_OAUTH2_CLIENTID);
                    if (clientID == null || clientID.length() == 0) {
                        valid = false;
                        errMsg = String.format("%s: No client ID defined", mwType);
                        break;
                    }

                    String clientSecret = mwOptions.getString(MIDDLEWARE_OAUTH2_CLIENTSECRET);
                    if (clientSecret == null || clientSecret.length() == 0) {
                        valid = false;
                        errMsg = String.format("%s: No client secret defined", mwType);
                        break;
                    }

                    String discoveryUrl = mwOptions.getString(MIDDLEWARE_OAUTH2_DISCOVERYURL);
                    if (discoveryUrl == null || discoveryUrl.length() == 0) {
                        valid = false;
                        errMsg = String.format("%s: No discovery URL defined", mwType);
                        break;
                    }

                    String sessionScope = mwOptions.getString(MIDDLEWARE_OAUTH2_SESSION_SCOPE);
                    if (sessionScope == null || sessionScope.length() == 0) {
                        valid = false;
                        errMsg = String.format("%s: No session scope defined", mwType);
                        break;
                    }

                    break;
                }
                case MIDDLEWARE_REDIRECT_REGEX: {
                    String regex = mwOptions.getString(MIDDLEWARE_REDIRECT_REGEX_REGEX);
                    if (regex == null || regex.length() == 0) {
                        valid = false;
                        errMsg = String.format("%s: No regex defined", mwType);
                        break;
                    }

                    String replacement = mwOptions.getString(MIDDLEWARE_REDIRECT_REGEX_REPLACEMENT);
                    if (replacement == null || replacement.length() == 0) {
                        valid = false;
                        errMsg = String.format("%s: No replacement defined", mwType);
                        break;
                    }

                    break;
                }
                case MIDDLEWARE_REPLACE_PATH_REGEX: {
                    String regex = mwOptions.getString(MIDDLEWARE_REPLACE_PATH_REGEX_REGEX);
                    if (regex == null || regex.length() == 0) {
                        valid = false;
                        errMsg = String.format("%s: No regex defined", mwType);
                        break;
                    }

                    String replacement = mwOptions.getString(MIDDLEWARE_REPLACE_PATH_REGEX_REPLACEMENT);
                    if (replacement == null || replacement.length() == 0) {
                        valid = false;
                        errMsg = String.format("%s: No replacement defined", mwType);
                        break;
                    }

                    break;
                }
                case MIDDLEWARE_SHOW_SESSION_CONTENT: {
                    break;
                }
                case MIDDLEWARE_SESSION_BAG: {
                    JsonArray whithelistedCookies = mwOptions.getJsonArray(MIDDLEWARE_SESSION_BAG_WHITHELISTED_COOKIES);
                    if (whithelistedCookies == null) {
                        valid = false;
                        errMsg = String.format("%s: No whitelisted cookies defined.", mwType);
                        break;
                    }
                    for (int j = 0; j < whithelistedCookies.size(); j++) {
                        JsonObject whithelistedCookie = whithelistedCookies.getJsonObject(j);
                        if (!whithelistedCookie.containsKey(MIDDLEWARE_SESSION_BAG_WHITHELISTED_COOKIE_NAME)
                                || whithelistedCookie.getString(MIDDLEWARE_SESSION_BAG_WHITHELISTED_COOKIE_NAME)
                                        .isEmpty()) {
                            valid = false;
                            errMsg = String.format("%s: whithelisted cookie name has to contain a value", mwType);
                            break;
                        }
                        if (!whithelistedCookie.containsKey(MIDDLEWARE_SESSION_BAG_WHITHELISTED_COOKIE_PATH)
                                || whithelistedCookie.getString(MIDDLEWARE_SESSION_BAG_WHITHELISTED_COOKIE_PATH)
                                        .isEmpty()) {
                            valid = false;
                            errMsg = String.format("%s: whithelisted cookie path has to contain a value", mwType);
                            break;
                        }
                    }
                    break;
                }
                default: {
                    errMsg = String.format("Unknown middleware: '%s'", mwType);
                    valid = false;
                }
            }

            if (!valid) {
                return Future.failedFuture(errMsg);
            }
        }

        return Future.succeededFuture();
    }

    private static Future<Void> validateServices(JsonObject httpConfig) {
        JsonArray svs = httpConfig.getJsonArray(SERVICES);
        if (svs == null || svs.size() == 0) {
            LOGGER.debug("validateServices: no services defined");
            return Future.succeededFuture();
        }

        Set<String> svNames = new HashSet<>();
        for (int i = 0; i < svs.size(); i++) {
            JsonObject sv = svs.getJsonObject(i);
            String svName = sv.getString(SERVICE_NAME);
            if (svNames.contains(svName)) {
                String errMsg = String.format("validateServices: duplicated service name '%s'. Should be unique.",
                        svName);
                LOGGER.warn(errMsg);
                return Future.failedFuture(errMsg);
            }
            svNames.add(svName);

            JsonArray servers = sv.getJsonArray(SERVICE_SERVERS);
            if (servers == null || servers.size() == 0) {
                String errorMsg = "validateServices: no servers defined";
                LOGGER.debug(errorMsg);
                return Future.failedFuture(errorMsg);
            }
        }

        return Future.succeededFuture();
    }

    private static Boolean addRouter(JsonObject httpConf, String routerName, JsonObject routerToAdd) {
        JsonArray existingRouters = httpConf.getJsonArray(DynamicConfiguration.ROUTERS);
        JsonObject existingRouter = getObjByKeyWithValue(existingRouters, DynamicConfiguration.ROUTER_NAME, routerName);
        if (existingRouter == null) {
            existingRouters.add(routerToAdd);
            return true;
        }

        return existingRouter.equals(routerToAdd);
    }

    private static Boolean addService(JsonObject httpConf, String serviceName, JsonObject serviceToAdd) {
        JsonArray existingServices = httpConf.getJsonArray(DynamicConfiguration.SERVICES);
        JsonObject existingService = getObjByKeyWithValue(existingServices, DynamicConfiguration.SERVICE_NAME,
                serviceName);
        if (existingService == null) {
            existingServices.add(serviceToAdd);
            return true;
        }

        Map<String, JsonObject> uniqueServers = new HashMap<>();

        JsonArray existingServers = existingService.getJsonArray(DynamicConfiguration.SERVICE_SERVERS);
        for (int i = 0; i < existingServers.size(); i++) {
            JsonObject server = existingServers.getJsonObject(i);
            String url = createURL(server.getString(DynamicConfiguration.SERVICE_SERVER_HOST),
                    server.getString(DynamicConfiguration.SERVICE_SERVER_PORT));
            uniqueServers.put(url, server);
        }

        JsonArray serversToAdd = serviceToAdd.getJsonArray(DynamicConfiguration.SERVICE_SERVERS);
        for (int i = 0; i < serversToAdd.size(); i++) {
            JsonObject serverToAdd = serversToAdd.getJsonObject(i);
            String url = createURL(serverToAdd.getString(DynamicConfiguration.SERVICE_SERVER_HOST),
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
        JsonArray existingMiddlewares = httpConf.getJsonArray(DynamicConfiguration.MIDDLEWARES);
        JsonObject existingMiddleware = getObjByKeyWithValue(existingMiddlewares, DynamicConfiguration.MIDDLEWARE_NAME,
                middlewareName);
        if (existingMiddleware == null) {
            existingMiddlewares.add(middlewareToAdd);
            return true;
        }

        return existingMiddleware.equals(middlewareToAdd);
    }
}
