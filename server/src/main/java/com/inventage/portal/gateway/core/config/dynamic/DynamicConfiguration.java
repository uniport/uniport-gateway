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
    public static final String ROUTER_SERVICE = "service";
    public static final String ROUTER_RULE = "rule";
    public static final String MIDDLEWARES = "middlewares";
    public static final String MIDDLEWARE_NAME = "name";
    public static final String MIDDLEWARE_TYPE = "type";
    public static final String SERVICES = "services";
    public static final String SERVICE_NAME = "name";
    public static final String SERVICE_SERVERS = "servers";
    public static final String SERVICE_SERVER_URL = "url";

    private static Schema schema;

    private static Schema buildSchema(Vertx vertx) {
        ObjectSchemaBuilder routerSchema = Schemas.objectSchema().requiredProperty(ROUTER_NAME, Schemas.stringSchema())
                .property(ROUTER_ENTRYPOINTS, Schemas.arraySchema().items(Schemas.stringSchema()))
                .property(MIDDLEWARES, Schemas.arraySchema().items(Schemas.stringSchema()))
                .requiredProperty(ROUTER_SERVICE, Schemas.stringSchema()).property(ROUTER_RULE, Schemas.stringSchema());

        ObjectSchemaBuilder middlewareSchema = Schemas.objectSchema()
                .requiredProperty(MIDDLEWARE_NAME, Schemas.stringSchema())
                .requiredProperty(MIDDLEWARE_TYPE, Schemas.objectSchema());

        ObjectSchemaBuilder serviceSchema = Schemas.objectSchema()
                .requiredProperty(SERVICE_NAME, Schemas.stringSchema())
                .requiredProperty(SERVICE_SERVERS, Schemas.arraySchema()
                        .items(Schemas.objectSchema().requiredProperty(SERVICE_SERVER_URL, Schemas.stringSchema())));

        ObjectSchemaBuilder httpSchema = Schemas.objectSchema()
                .property(ROUTERS, Schemas.arraySchema().items(routerSchema))
                .property(MIDDLEWARES, Schemas.arraySchema().items(middlewareSchema))
                .property(SERVICES, Schemas.arraySchema().items(serviceSchema));

        ObjectSchemaBuilder dynamicConfigBuilder = Schemas.objectSchema().property(HTTP, httpSchema);

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

            JsonArray rs = httpConf.getJsonArray(DynamicConfiguration.ROUTERS);
            for (int i = 0; i < rs.size(); i++) {
                JsonObject r = rs.getJsonObject(i);
                String rName = r.getString(DynamicConfiguration.ROUTER_NAME);
                if (!routers.containsKey(rName)) {
                    routers.put(rName, new ArrayList<String>());
                }
                routers.get(rName).add(key);
                if (!addRouter(mergedHttpConfig, rName, r)) {
                    routersToDelete.add(rName);
                }
            }

            JsonArray ms = httpConf.getJsonArray(DynamicConfiguration.MIDDLEWARES);
            for (int i = 0; i < ms.size(); i++) {
                JsonObject m = ms.getJsonObject(i);
                String mName = m.getString(DynamicConfiguration.MIDDLEWARE_NAME);
                if (!middlewares.containsKey(mName)) {
                    middlewares.put(mName, new ArrayList<String>());
                }
                middlewares.get(mName).add(key);
                if (!addMiddleware(mergedHttpConfig, mName, m)) {
                    middlewaresToDelete.add(mName);
                }
            }

            JsonArray ss = httpConf.getJsonArray(DynamicConfiguration.SERVICES);
            for (int i = 0; i < ss.size(); i++) {
                JsonObject s = ss.getJsonObject(i);
                String sName = s.getString(DynamicConfiguration.SERVICE_NAME);
                if (!services.containsKey(sName)) {
                    services.put(sName, new ArrayList<String>());
                }
                services.get(sName).add(key);
                if (!addService(mergedHttpConfig, sName, s)) {
                    servicesToDelete.add(sName);
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
            uniqueServers.put(server.getString(DynamicConfiguration.SERVICE_SERVER_URL), server);
        }

        JsonArray serversToAdd = serviceToAdd.getJsonArray(DynamicConfiguration.SERVICE_SERVERS);
        for (int i = 0; i < serversToAdd.size(); i++) {
            JsonObject serverToAdd = serversToAdd.getJsonObject(i);
            String url = serverToAdd.getString(DynamicConfiguration.SERVICE_SERVER_URL);
            if (!uniqueServers.containsKey(url)) {
                existingServers.add(serverToAdd);
            }
        }

        return true;
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