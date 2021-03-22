package com.inventage.portal.gateway.core.config.dynamic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.Schemas;

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
    public static final String MIDDLEWARE_OPTIONS = "options";

    public static final String MIDDLEWARE_REPLACE_PATH_REGEX = "replacePathRegex";
    public static final String MIDDLEWARE_REPLACE_PATH_REGEX_REGEX = "regex";
    public static final String MIDDLEWARE_REPLACE_PATH_REGEX_REPLACEMENT = "replacement";

    public static final String MIDDLEWARE_REDIRECT_PATH = "redirectPath";
    public static final String MIDDLEWARE_REDIRECT_PATH_DESTINATION = "destination";

    public static final String MIDDLEWARE_HEADERS = "headers";
    public static final String MIDDLEWARE_HEADERS_HEADER = "header";
    public static final String MIDDLEWARE_HEADERS_VALUE = "value";
    public static final String MIDDLEWARE_HEADERS_REQUEST = "customRequestHeaders";
    public static final String MIDDLEWARE_HEADERS_RESPONSE = "customResponseHeaders";

    public static final String MIDDLEWARE_AUTHORIZATION_BEARER = "authorizationBearer";

    public static final String MIDDLEWARE_OAUTH2 = "oauth2";
    public static final String MIDDLEWARE_OAUTH2_CLIENTID = "clientId";
    public static final String MIDDLEWARE_OAUTH2_CLIENTSECRET = "clientSecret";
    public static final String MIDDLEWARE_OAUTH2_DISCOVERYURL = "discoveryUrl";

    public static final String SERVICES = "services";
    public static final String SERVICE_NAME = "name";
    public static final String SERVICE_SERVERS = "servers";
    public static final String SERVICE_SERVER_HOST = "host";
    public static final String SERVICE_SERVER_PORT = "port";


    private static Schema schema;

    private static Schema buildSchema(Vertx vertx) {
        // TODO consider setting "additionalProperties": false

        ObjectSchemaBuilder routerSchema = Schemas.objectSchema()
                .requiredProperty(ROUTER_NAME, Schemas.stringSchema())
                .property(ROUTER_ENTRYPOINTS, Schemas.arraySchema().items(Schemas.stringSchema()))
                .property(ROUTER_MIDDLEWARES, Schemas.arraySchema().items(Schemas.stringSchema()))
                .requiredProperty(ROUTER_SERVICE, Schemas.stringSchema())
                .property(ROUTER_RULE, Schemas.stringSchema());

        ObjectSchemaBuilder replacePathMiddlewareSchema = Schemas.objectSchema()
                .requiredProperty(MIDDLEWARE_REPLACE_PATH_REGEX_REGEX, Schemas.stringSchema())
                .requiredProperty(MIDDLEWARE_REPLACE_PATH_REGEX_REPLACEMENT,
                        Schemas.stringSchema());

        ObjectSchemaBuilder redirectPathMiddlewareSchema = Schemas.objectSchema()
                .requiredProperty(MIDDLEWARE_REDIRECT_PATH_DESTINATION, Schemas.stringSchema());

        ObjectSchemaBuilder headersMiddlewareSchema = Schemas.objectSchema().property(
                MIDDLEWARE_HEADERS_REQUEST,
                Schemas.arraySchema().items(Schemas.objectSchema()
                        .requiredProperty(MIDDLEWARE_HEADERS_HEADER, Schemas.stringSchema())
                        .requiredProperty(MIDDLEWARE_HEADERS_VALUE, Schemas.stringSchema())))
                .property(MIDDLEWARE_HEADERS_RESPONSE, Schemas.arraySchema()
                        .items(Schemas.objectSchema()
                                .requiredProperty(MIDDLEWARE_HEADERS_HEADER, Schemas.stringSchema())
                                .requiredProperty(MIDDLEWARE_HEADERS_VALUE,
                                        Schemas.stringSchema())));

        ObjectSchemaBuilder authorizationBearerMiddlewareSchema = Schemas.objectSchema();

        ObjectSchemaBuilder oauth2MiddlewareSchema = Schemas.objectSchema()
                .requiredProperty(MIDDLEWARE_OAUTH2_CLIENTID, Schemas.stringSchema())
                .requiredProperty(MIDDLEWARE_OAUTH2_CLIENTSECRET, Schemas.stringSchema())
                .requiredProperty(MIDDLEWARE_OAUTH2_DISCOVERYURL, Schemas.stringSchema());

        ObjectSchemaBuilder middlewareSchema =
                Schemas.objectSchema().requiredProperty(MIDDLEWARE_NAME, Schemas.stringSchema())
                        .property(MIDDLEWARE_REPLACE_PATH_REGEX, replacePathMiddlewareSchema)
                        .property(MIDDLEWARE_REDIRECT_PATH, redirectPathMiddlewareSchema)
                        .property(MIDDLEWARE_HEADERS, headersMiddlewareSchema)
                        .property(MIDDLEWARE_AUTHORIZATION_BEARER,
                                authorizationBearerMiddlewareSchema)
                        .property(MIDDLEWARE_OAUTH2, oauth2MiddlewareSchema);

        ObjectSchemaBuilder serviceSchema = Schemas.objectSchema()
                .requiredProperty(SERVICE_NAME, Schemas.stringSchema())
                .requiredProperty(SERVICE_SERVERS,
                        Schemas.arraySchema().items(Schemas.objectSchema()
                                .requiredProperty(SERVICE_SERVER_HOST, Schemas.stringSchema())
                                .requiredProperty(SERVICE_SERVER_PORT, Schemas.intSchema())));

        ObjectSchemaBuilder httpSchema =
                Schemas.objectSchema().property(ROUTERS, Schemas.arraySchema().items(routerSchema))
                        .property(MIDDLEWARES, Schemas.arraySchema().items(middlewareSchema))
                        .property(SERVICES, Schemas.arraySchema().items(serviceSchema));

        ObjectSchemaBuilder dynamicConfigBuilder =
                Schemas.objectSchema().property(HTTP, httpSchema);

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

    public static Future<Void> validate(Vertx vertx, JsonObject json) {
        if (schema == null) {
            schema = buildSchema(vertx);
        }
        return schema.validateAsync(json);
    }

    public static JsonObject merge(Map<String, JsonObject> configurations) {
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
            LOGGER.error("Router defined multiple times with different configurations in '{}'",
                    routers.get(routerName));
            mergedHttpConfig.remove(routerName);
        }

        for (String middlewareName : middlewaresToDelete) {
            LOGGER.error("Middleware defined multiple times with different configurations in '{}'",
                    routers.get(middlewareName));
            mergedConfig.remove(middlewareName);
        }

        for (String serviceName : servicesToDelete) {
            LOGGER.error("Service defined multiple times with different configurations in '{}'",
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
        return String.format("%s:%s", host, port);
    }

    private static Boolean addMiddleware(JsonObject httpConf, String middlewareName,
            JsonObject middlewareToAdd) {
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
