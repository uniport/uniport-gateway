package com.inventage.portal.gateway.proxy.config.dynamic;

import java.util.ArrayList;
import java.util.Arrays;
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

    public static final String MIDDLEWARE_OAUTH2 = "oauth2";
    public static final String MIDDLEWARE_OAUTH2_CLIENTID = "clientId";
    public static final String MIDDLEWARE_OAUTH2_CLIENTSECRET = "clientSecret";
    public static final String MIDDLEWARE_OAUTH2_DISCOVERYURL = "discoveryUrl";
    public static final String MIDDLEWARE_OAUTH2_SESSION_SCOPE = "sessionScope";
    public static final String MIDDLEWARE_OAUTH2_SESSION_SCOPE_ID = "id";

    public static final List<String> MIDDLEWARE_TYPES =
            Arrays.asList(MIDDLEWARE_REPLACE_PATH_REGEX, MIDDLEWARE_REDIRECT_REGEX,
                    MIDDLEWARE_HEADERS, MIDDLEWARE_AUTHORIZATION_BEARER, MIDDLEWARE_OAUTH2);

    public static final String SERVICES = "services";
    public static final String SERVICE_NAME = "name";
    public static final String SERVICE_SERVERS = "servers";
    public static final String SERVICE_SERVER_HOST = "host";
    public static final String SERVICE_SERVER_PORT = "port";


    private static Schema schema;

    private static Schema buildSchema(Vertx vertx) {
        LOGGER.trace("buildSchema");

        ObjectSchemaBuilder routerSchema = Schemas.objectSchema()
                .requiredProperty(ROUTER_NAME, Schemas.stringSchema())
                .property(ROUTER_ENTRYPOINTS, Schemas.arraySchema().items(Schemas.stringSchema()))
                .property(ROUTER_MIDDLEWARES, Schemas.arraySchema().items(Schemas.stringSchema()))
                .requiredProperty(ROUTER_SERVICE, Schemas.stringSchema())
                .property(ROUTER_RULE, Schemas.stringSchema()).allowAdditionalProperties(false);

        ObjectSchemaBuilder middlewareOptionsSchema = Schemas.objectSchema()
                .property(MIDDLEWARE_REPLACE_PATH_REGEX_REGEX, Schemas.stringSchema())
                .property(MIDDLEWARE_REPLACE_PATH_REGEX_REPLACEMENT, Schemas.stringSchema())
                .property(MIDDLEWARE_REDIRECT_REGEX_REGEX, Schemas.stringSchema())
                .property(MIDDLEWARE_REDIRECT_REGEX_REPLACEMENT, Schemas.stringSchema())
                .property(MIDDLEWARE_AUTHORIZATION_BEARER_SESSION_SCOPE, Schemas.stringSchema())
                .property(MIDDLEWARE_OAUTH2_CLIENTID, Schemas.stringSchema())
                .property(MIDDLEWARE_OAUTH2_CLIENTSECRET, Schemas.stringSchema())
                .property(MIDDLEWARE_OAUTH2_DISCOVERYURL, Schemas.stringSchema())
                .property(MIDDLEWARE_OAUTH2_SESSION_SCOPE, Schemas.stringSchema())
                .property(MIDDLEWARE_HEADERS_REQUEST, Schemas.objectSchema())
                .property(MIDDLEWARE_HEADERS_RESPONSE, Schemas.objectSchema());

        ObjectSchemaBuilder middlewareSchema =
                Schemas.objectSchema().requiredProperty(MIDDLEWARE_NAME, Schemas.stringSchema())
                        .requiredProperty(MIDDLEWARE_TYPE, Schemas.stringSchema())
                        .property(MIDDLEWARE_OPTIONS, middlewareOptionsSchema)
                        .allowAdditionalProperties(false);

        ObjectSchemaBuilder serviceSchema = Schemas.objectSchema()
                .requiredProperty(SERVICE_NAME, Schemas.stringSchema())
                .requiredProperty(SERVICE_SERVERS,
                        Schemas.arraySchema().items(Schemas.objectSchema()
                                .requiredProperty(SERVICE_SERVER_HOST, Schemas.stringSchema())
                                .requiredProperty(SERVICE_SERVER_PORT, Schemas.intSchema())
                                .allowAdditionalProperties(false)))
                .allowAdditionalProperties(false);

        ObjectSchemaBuilder httpSchema =
                Schemas.objectSchema().property(ROUTERS, Schemas.arraySchema().items(routerSchema))
                        .property(MIDDLEWARES, Schemas.arraySchema().items(middlewareSchema))
                        .property(SERVICES, Schemas.arraySchema().items(serviceSchema))
                        .allowAdditionalProperties(false);

        ObjectSchemaBuilder dynamicConfigBuilder =
                Schemas.objectSchema().requiredProperty(HTTP, httpSchema);

        SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
        SchemaParser schemaParser = SchemaParser.createDraft201909SchemaParser(schemaRouter);
        return dynamicConfigBuilder.build(schemaParser);
    }

    public static JsonObject buildDefaultConfiguration() {
        LOGGER.trace("buildDefaultConfiguration");
        JsonObject config = new JsonObject();

        JsonObject http = new JsonObject();

        http.put(DynamicConfiguration.ROUTERS, new JsonArray());
        http.put(DynamicConfiguration.MIDDLEWARES, new JsonArray());
        http.put(DynamicConfiguration.SERVICES, new JsonArray());

        config.put(DynamicConfiguration.HTTP, http);

        return config;
    }

    public static boolean isEmptyConfiguration(JsonObject config) {
        LOGGER.trace("isEmptyConfiguration");
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

    public static Future<Void> validate(Vertx vertx, JsonObject json) {
        LOGGER.trace("validate");

        if (schema == null) {
            schema = buildSchema(vertx);
        }

        Promise<Void> validPromise = Promise.promise();
        schema.validateAsync(json).onSuccess(f -> {
            JsonObject httpConfig = json.getJsonObject(HTTP);
            List<Future> validFutures =
                    Arrays.asList(validateRouters(httpConfig), validateMiddlewares(httpConfig));

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

    public static Future<Void> validateRouters(JsonObject httpConfig) {
        LOGGER.trace("validateRouters");

        JsonArray routers = httpConfig.getJsonArray(ROUTERS);
        if (routers == null || routers.size() == 0) {
            LOGGER.warn("validateRouters: no routers defined");
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
                String errMsg = "validateRouters: unknown middleware '{}' defined";
                LOGGER.warn(errMsg);
                return Future.failedFuture(errMsg);
            }
        }

        for (String svName : routerServiceNames) {
            if (getObjByKeyWithValue(services, SERVICE_NAME, svName) == null) {
                String errMsg = "validateRouters: unknown service '{}' defined";
                LOGGER.warn(errMsg, svName);
                return Future.failedFuture(errMsg);
            }
        }

        return Future.succeededFuture();
    }

    public static Future<Void> validateMiddlewares(JsonObject httpConfig) {
        LOGGER.trace("validateMiddlewares");

        JsonArray mws = httpConfig.getJsonArray(MIDDLEWARES);
        if (mws == null || mws.size() == 0) {
            LOGGER.debug("validateMiddlewares: no middlewares defined");
            return Future.succeededFuture();
        }

        for (int i = 0; i < mws.size(); i++) {
            JsonObject mw = mws.getJsonObject(i);
            String mwName = mw.getString(MIDDLEWARE_NAME);

            Boolean valid = true;
            String errMsg = "";
            switch (mwName) {
                case MIDDLEWARE_AUTHORIZATION_BEARER: {
                    String sessionScope =
                            mw.getString(MIDDLEWARE_AUTHORIZATION_BEARER_SESSION_SCOPE);
                    if (sessionScope == null || sessionScope.length() == 0) {
                        valid = false;
                        errMsg = "Authorization Bearer: No session scope defined";
                    }
                    break;
                }
                case MIDDLEWARE_HEADERS: {
                    JsonObject requestHeaders = mw.getJsonObject(MIDDLEWARE_HEADERS_REQUEST);
                    if (requestHeaders != null && requestHeaders.isEmpty()) {
                        valid = false;
                        errMsg = "Headers: Empty request headers defined";
                        break;
                    }

                    for (Entry<String, Object> entry : requestHeaders) {
                        if (!(entry.getKey() instanceof String)
                                || !(entry.getValue() instanceof String)) {
                            valid = false;
                            errMsg = "Headers: request header and value can only be of type string";
                            break;
                        }
                    }
                    if (!valid) {
                        break;
                    }

                    JsonObject responseHeaders = mw.getJsonObject(MIDDLEWARE_HEADERS_RESPONSE);
                    if (responseHeaders != null && responseHeaders.isEmpty()) {
                        valid = false;
                        errMsg = "Headers: Empty response headers defined";
                        break;
                    }

                    for (Entry<String, Object> entry : responseHeaders) {
                        if (!(entry.getKey() instanceof String)
                                || !(entry.getValue() instanceof String)) {
                            valid = false;
                            errMsg = "Headers: response header and value can only be of type string";
                            break;
                        }
                    }

                    break;
                }
                case MIDDLEWARE_OAUTH2: {
                    String clientID = mw.getString(MIDDLEWARE_OAUTH2_CLIENTID);
                    if (clientID == null || clientID.length() == 0) {
                        valid = false;
                        errMsg = "OAuth2: No client ID defined";
                        break;
                    }

                    String clientSecret = mw.getString(MIDDLEWARE_OAUTH2_CLIENTSECRET);
                    if (clientSecret == null || clientSecret.length() == 0) {
                        valid = false;
                        errMsg = "OAuth2: No client secret defined";
                        break;
                    }

                    String discoveryUrl = mw.getString(MIDDLEWARE_OAUTH2_DISCOVERYURL);
                    if (discoveryUrl == null || discoveryUrl.length() == 0) {
                        valid = false;
                        errMsg = "OAuth2: No discovery URL defined";
                        break;
                    }

                    String sessionScope = mw.getString(MIDDLEWARE_OAUTH2_SESSION_SCOPE);
                    if (sessionScope == null || sessionScope.length() == 0) {
                        valid = false;
                        errMsg = "OAuth2: No session scope defined";
                        break;
                    }

                    break;
                }
                case MIDDLEWARE_REDIRECT_REGEX: {
                    String regex = mw.getString(MIDDLEWARE_REDIRECT_REGEX_REGEX);
                    if (regex == null || regex.length() == 0) {
                        valid = false;
                        errMsg = "Redirect regex: No regex defined";
                        break;
                    }

                    String replacement = mw.getString(MIDDLEWARE_REDIRECT_REGEX_REPLACEMENT);
                    if (replacement == null || replacement.length() == 0) {
                        valid = false;
                        errMsg = "Redirect regex: No replacement defined";
                        break;
                    }

                    break;
                }
                case MIDDLEWARE_REPLACE_PATH_REGEX: {
                    String regex = mw.getString(MIDDLEWARE_REPLACE_PATH_REGEX_REGEX);
                    if (regex == null || regex.length() == 0) {
                        valid = false;
                        errMsg = "Replace path regex: No regex defined";
                        break;
                    }

                    String replacement = mw.getString(MIDDLEWARE_REPLACE_PATH_REGEX_REPLACEMENT);
                    if (replacement == null || replacement.length() == 0) {
                        valid = false;
                        errMsg = "Replace path regex: No replacement defined";
                        break;
                    }

                    break;
                }
                default: {
                    errMsg = "Unknown middleware";
                    valid = false;
                }
            }

            if (!valid) {
                return Future.failedFuture(errMsg);
            }
        }

        return Future.succeededFuture();
    }

    public static JsonObject merge(Map<String, JsonObject> configurations) {
        LOGGER.trace("merge");
        JsonObject mergedConfig = buildDefaultConfiguration();
        JsonObject mergedHttpConfig = mergedConfig.getJsonObject(DynamicConfiguration.HTTP);

        if (mergedHttpConfig == null) {
            return mergedConfig;
        }

        Map<String, List<String>> routers = new HashMap<>();
        Set<String> routersToDelete = new HashSet<>();

        Map<String, List<String>> services = new HashMap<>();
        Set<String> servicesToDelete = new HashSet<>();

        Map<String, List<String>> middlewares = new HashMap<>();
        Set<String> middlewaresToDelete = new HashSet<>();

        Set<String> keys = configurations.keySet();

        for (String key : keys) {
            JsonObject conf = configurations.get(key);
            JsonObject httpConf = conf.getJsonObject(DynamicConfiguration.HTTP);

            if (httpConf != null) {
                JsonArray rts = httpConf.getJsonArray(DynamicConfiguration.ROUTERS);
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

                JsonArray mws = httpConf.getJsonArray(DynamicConfiguration.MIDDLEWARES);
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

                JsonArray svs = httpConf.getJsonArray(DynamicConfiguration.SERVICES);
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
            LOGGER.warn(
                    "merge: Router defined multiple times with different configurations in '{}'",
                    routers.get(routerName));
            mergedHttpConfig.remove(routerName);
        }

        for (String middlewareName : middlewaresToDelete) {
            LOGGER.warn(
                    "merge: Middleware defined multiple times with different configurations in '{}'",
                    routers.get(middlewareName));
            mergedConfig.remove(middlewareName);
        }

        for (String serviceName : servicesToDelete) {
            LOGGER.warn(
                    "merge: Service defined multiple times with different configurations in '{}'",
                    routers.get(serviceName));
            mergedHttpConfig.remove(serviceName);
        }

        return mergedConfig;
    }

    public static JsonObject getObjByKeyWithValue(JsonArray jsonArr, String key, String value) {
        LOGGER.trace("getObjByKeyWithValue");
        if (jsonArr == null) {
            return null;
        }
        int size = jsonArr.size();
        for (int i = 0; i < size; i++) {
            JsonObject obj = jsonArr.getJsonObject(i);
            if (obj == null) {
                return null;
            }
            if (obj.containsKey(key) && obj.getString(key).equals(value)) {
                return obj;
            }
        }
        return null;
    }

    private static Boolean addRouter(JsonObject httpConf, String routerName,
            JsonObject routerToAdd) {
        LOGGER.trace("addRouter");
        JsonArray existingRouters = httpConf.getJsonArray(DynamicConfiguration.ROUTERS);
        JsonObject existingRouter =
                getObjByKeyWithValue(existingRouters, DynamicConfiguration.ROUTER_NAME, routerName);
        if (existingRouter == null) {
            existingRouters.add(routerToAdd);
            return true;
        }

        return existingRouter.equals(routerToAdd);
    }

    private static Boolean addService(JsonObject httpConf, String serviceName,
            JsonObject serviceToAdd) {
        LOGGER.trace("addService");
        JsonArray existingServices = httpConf.getJsonArray(DynamicConfiguration.SERVICES);
        JsonObject existingService = getObjByKeyWithValue(existingServices,
                DynamicConfiguration.SERVICE_NAME, serviceName);
        if (existingService == null) {
            existingServices.add(serviceToAdd);
            return true;
        }

        Map<String, JsonObject> uniqueServers = new HashMap<>();

        JsonArray existingServers =
                existingService.getJsonArray(DynamicConfiguration.SERVICE_SERVERS);
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
        LOGGER.trace("createURL");
        return String.format("%s:%s", host, port);
    }

    private static Boolean addMiddleware(JsonObject httpConf, String middlewareName,
            JsonObject middlewareToAdd) {
        LOGGER.trace("addMiddleware");
        JsonArray existingMiddlewares = httpConf.getJsonArray(DynamicConfiguration.MIDDLEWARES);
        JsonObject existingMiddleware = getObjByKeyWithValue(existingMiddlewares,
                DynamicConfiguration.MIDDLEWARE_NAME, middlewareName);
        if (existingMiddleware == null) {
            existingMiddlewares.add(middlewareToAdd);
            return true;
        }

        return existingMiddleware.equals(middlewareToAdd);
    }
}
