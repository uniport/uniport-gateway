package com.inventage.portal.gateway.proxy.config.dynamic;

import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.proxy.ProxyMiddlewareFactory;
import com.inventage.portal.gateway.proxy.router.RouterFactory;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Draft;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.OutputFormat;
import io.vertx.json.schema.OutputUnit;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.Validator;
import io.vertx.json.schema.common.dsl.Keywords;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.SchemaType;
import io.vertx.json.schema.common.dsl.Schemas;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * It defines the structure of the dynamic configuration.
 */
public class DynamicConfiguration {
    // keywords used for internal purpose only
    public static final String HTTP = "http";
    // routers
    public static final String ROUTERS = "routers";
    public static final String ROUTER_NAME = "name";
    public static final String ROUTER_ENTRYPOINTS = "entrypoints";
    public static final String ROUTER_MIDDLEWARES = "middlewares";
    public static final String ROUTER_SERVICE = "service";
    public static final String ROUTER_RULE = "rule";
    public static final String ROUTER_PRIORITY = "priority";
    // middlewares
    public static final String MIDDLEWARES = "middlewares";
    public static final String MIDDLEWARE_NAME = "name";
    public static final String MIDDLEWARE_TYPE = "type";
    public static final String MIDDLEWARE_OPTIONS = "options";
    public static final List<String> MIDDLEWARE_TYPES = MiddlewareFactory.Loader.listFactories().stream()
        .map(MiddlewareFactory::provides)
        .toList();
    // services
    public static final String SERVICES = "services";
    public static final String SERVICE_NAME = "name";
    // proxying configuration to keep the sovereignty close to the implementation
    public static final String SERVICE_SERVERS = ProxyMiddlewareFactory.SERVICE_SERVERS;
    public static final String SERVICE_SERVER_PROTOCOL = ProxyMiddlewareFactory.SERVICE_SERVER_PROTOCOL;
    public static final String SERVICE_SERVER_HTTPS_OPTIONS = ProxyMiddlewareFactory.SERVICE_SERVER_HTTPS_OPTIONS;
    public static final String SERVICE_SERVER_HTTPS_OPTIONS_VERIFY_HOSTNAME = ProxyMiddlewareFactory.SERVICE_SERVER_HTTPS_OPTIONS_VERIFY_HOSTNAME;
    public static final String SERVICE_SERVER_HTTPS_OPTIONS_TRUST_ALL = ProxyMiddlewareFactory.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_ALL;
    public static final String SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PATH = ProxyMiddlewareFactory.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PATH;
    public static final String SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PASSWORD = ProxyMiddlewareFactory.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PASSWORD;
    public static final String SERVICE_SERVER_HOST = ProxyMiddlewareFactory.SERVICE_SERVER_HOST;
    public static final String SERVICE_SERVER_PORT = ProxyMiddlewareFactory.SERVICE_SERVER_PORT;

    // schema
    private static final Pattern ENV_VARIABLE_PATTERN = Pattern.compile("^\\$\\{.*\\}$");

    private static Validator validator;

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfiguration.class);

    private static Validator buildValidator() {
        final JsonSchema schema = JsonSchema.of(buildSchema().toJson());
        final JsonSchemaOptions options = new JsonSchemaOptions()
            .setDraft(Draft.DRAFT202012)
            .setOutputFormat(OutputFormat.Basic)
            .setBaseUri("https://inventage.com/portal-gateway/dynamic-configuration");
        return Validator.create(schema, options);
    }

    private static ObjectSchemaBuilder buildRouterSchema() {
        final ObjectSchemaBuilder routerSchema = Schemas.objectSchema()
            .requiredProperty(ROUTER_NAME, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .optionalProperty(ROUTER_ENTRYPOINTS, Schemas.arraySchema().items(Schemas.stringSchema()))
            .optionalProperty(ROUTER_MIDDLEWARES, Schemas.arraySchema().items(Schemas.stringSchema()))
            .requiredProperty(ROUTER_SERVICE, Schemas.stringSchema())
            .requiredProperty(ROUTER_RULE, Schemas.stringSchema()
                .with(Keywords.minLength(1)))
            .optionalProperty(ROUTER_PRIORITY, Schemas.intSchema())
            .allowAdditionalProperties(false);
        return routerSchema;
    }

    private static ObjectSchemaBuilder[] buildMiddlewareSchema() {
        final ObjectSchemaBuilder[] middlewareSchemas = MiddlewareFactory.Loader.listFactories()
            .stream()
            .map(factory -> {
                final ObjectSchemaBuilder optionsSchema = factory.optionsSchema();
                final boolean optionsHasAtLeastOneRequiredProperty = optionsSchema.getProperties()
                    .keySet()
                    .stream()
                    .anyMatch(keyword -> optionsSchema.isPropertyRequired(keyword));

                final ObjectSchemaBuilder middlewareSchema = Schemas.objectSchema()
                    .requiredProperty(MIDDLEWARE_NAME, Schemas.stringSchema())
                    .requiredProperty(MIDDLEWARE_TYPE, Schemas.constSchema(factory.provides()))
                    .allowAdditionalProperties(false);

                if (optionsHasAtLeastOneRequiredProperty) {
                    middlewareSchema.requiredProperty(MIDDLEWARE_OPTIONS, optionsSchema);
                } else {
                    middlewareSchema.optionalProperty(MIDDLEWARE_OPTIONS, optionsSchema);
                }

                return middlewareSchema;
            })
            .toArray(ObjectSchemaBuilder[]::new);
        return middlewareSchemas;
    }

    private static ObjectSchemaBuilder buildServiceSchema() {
        final ObjectSchemaBuilder serviceSchema = Schemas.objectSchema()
            .requiredProperty(SERVICE_NAME, Schemas.stringSchema())
            .requiredProperty(SERVICE_SERVERS, Schemas.arraySchema()
                .items(Schemas.objectSchema()
                    .optionalProperty(SERVICE_SERVER_PROTOCOL, Schemas.stringSchema())
                    .optionalProperty(SERVICE_SERVER_HTTPS_OPTIONS, Schemas.objectSchema())
                    .requiredProperty(SERVICE_SERVER_HOST, Schemas.stringSchema())
                    .requiredProperty(SERVICE_SERVER_PORT, Schemas.anyOf(
                        Schemas.intSchema(),
                        Schemas.schema()
                            .with(Keywords.type(SchemaType.STRING))
                            .with(Keywords.pattern(ENV_VARIABLE_PATTERN))))
                    .allowAdditionalProperties(false)))
            .allowAdditionalProperties(false);
        return serviceSchema;
    }

    private static ObjectSchemaBuilder buildHttpSchema(
        ObjectSchemaBuilder routerSchema,
        ObjectSchemaBuilder[] middlewareSchema,
        ObjectSchemaBuilder serviceSchema
    ) {
        final ObjectSchemaBuilder httpSchema = Schemas.objectSchema()
            .optionalProperty(ROUTERS, Schemas.arraySchema()
                .items(routerSchema))
            .optionalProperty(MIDDLEWARES, Schemas.arraySchema()
                .items(Schemas.oneOf(middlewareSchema)))
            .optionalProperty(SERVICES, Schemas.arraySchema()
                .items(serviceSchema))
            .allowAdditionalProperties(false);
        return httpSchema;
    }

    public static ObjectSchemaBuilder[] getBuildMiddlewareSchema() {
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

        for (Entry<String, JsonObject> entry : configurations.entrySet()) {
            final String key = entry.getKey();
            final JsonObject conf = entry.getValue();
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

    public static ObjectSchemaBuilder buildSchema() {
        final ObjectSchemaBuilder routerSchema = buildRouterSchema();
        final ObjectSchemaBuilder[] middlewareSchema = buildMiddlewareSchema();
        final ObjectSchemaBuilder serviceSchema = buildServiceSchema();
        final ObjectSchemaBuilder httpSchema = buildHttpSchema(routerSchema, middlewareSchema, serviceSchema);

        final ObjectSchemaBuilder dynamicConfigBuilder = Schemas.objectSchema()
            .requiredProperty(HTTP, httpSchema)
            .allowAdditionalProperties(false);

        return dynamicConfigBuilder;
    }

    /**
     * Validates a JSON object representing a dynamic configuration instance.
     * The JSON object is expected to be complete i.e. all references are present in it.
     * 
     * This is a all or nothing validation.
     * 
     * @param vertx
     * @param json
     * @return Future that fails, if any reference cannot be resolved or any part is invalid.
     */
    public static Future<Void> validate(Vertx vertx, JsonObject json) {
        return validate(vertx, json, true, false).mapEmpty();
    }

    /**
     * Validates a JSON object representing a dynamic configuration instance.
     * The JSON object may contain references that are not present <==> {@code requireCompleteConfig == false}.
     * 
     * @param vertx
     * @param json
     * @param requireCompleteConfig
     * @return Future that fails, if any part is invalid.
     */
    public static Future<Void> validate(Vertx vertx, JsonObject json, boolean requireCompleteConfig) {
        return validate(vertx, json, requireCompleteConfig, false).mapEmpty();
    }

    /**
     * Validates a JSON object representing a dynamic configuration instance.
     *
     * @param vertx
     *            The Vertx instance
     * @param json
     *            The JSON object to validate
     * @param requireCompleteConfig
     *            If set, all references (to middlewares and services) need to be present
     * @param omitInvalidRouters
     *            If set, routers with invalid configuration are omitted from the final result
     *            A valid router requires all of its referenced dependencies to be valid as well.
     * @return Future that contains the subset of router that are valid.
     */
    public static Future<JsonArray> validate(Vertx vertx, JsonObject json, boolean requireCompleteConfig, boolean omitInvalidRouters) {
        if (validator == null) {
            validator = buildValidator();
        }

        final OutputUnit result;
        try {
            result = validator.validate(json);
        } catch (SchemaException e) {
            return Future.failedFuture(e);
        }
        if (result.getValid() == null || !result.getValid()) {
            // the error message for items/oneOf validation failure is unusable, so we try being more helpful
            if (result.getErrors() != null) {
                for (final OutputUnit error : result.getErrors()) {
                    final String keyword = error.getKeywordLocation();
                    final String instance = error.getInstanceLocation();
                    final String message = error.getError();

                    if (keyword.endsWith("items/oneOf")) {
                        LOGGER.warn("{} at '{}'", message, instance);
                    }
                }
            }

            return Future.failedFuture(result.toJson().encodePrettily());
        }

        // validate possible dependencies first
        final JsonObject httpConfig = json.getJsonObject(HTTP);
        final Future<JsonArray> validMiddlewares = validateMiddlewares(httpConfig.getJsonArray(MIDDLEWARES), omitInvalidRouters);
        final Future<JsonArray> validServices = validateServices(httpConfig.getJsonArray(SERVICES), omitInvalidRouters);

        return Future.all(List.of(validMiddlewares, validServices))
            .compose(
                cf -> {
                    final JsonArray routers = httpConfig.getJsonArray(ROUTERS);
                    return validateRouters(routers, validMiddlewares.result(), validServices.result(), requireCompleteConfig, omitInvalidRouters);
                },
                err -> {
                    if (validMiddlewares.failed()) {
                        LOGGER.warn("invalid middleware configuration: {}", validMiddlewares.cause().getMessage());
                    }
                    if (validServices.failed()) {
                        LOGGER.warn("invalid service configuration: {}", validServices.cause().getMessage());
                    }
                    return Future.failedFuture("invalid configuration");
                });
    }

    private static Future<JsonArray> validateRouters(
        JsonArray routers, JsonArray validMiddlewares, JsonArray validServices,
        boolean requireFullValidation, boolean discardInvalid
    ) {
        if (routers == null || routers.size() == 0) {
            LOGGER.info("No routers defined");
            return Future.succeededFuture(JsonArray.of());
        }

        final JsonArray validRouters = filterValidRouters(routers);
        if (!discardInvalid && validRouters.size() != routers.size()) {
            return Future.failedFuture("at least one router configuration is invalid");
        }

        if (!requireFullValidation) {
            return Future.succeededFuture(validRouters);
        }

        final Set<String> middlewareNames = collectMiddlewareNames(validMiddlewares);
        final Set<String> serviceNames = collectServiceNames(validServices);
        final JsonArray validRoutersExtended = filterRoutersWithValidReferences(validRouters, middlewareNames, serviceNames);
        if (!discardInvalid && validRoutersExtended.size() != validRouters.size()) {
            return Future.failedFuture("at least one router configuration is invalid");
        }

        return Future.succeededFuture(validRoutersExtended);
    }

    /**
     * Filters routers, which a unique name and a valid rule.
     * 
     * @param routers
     *            to validate
     * @return the filtered routers
     * 
     */
    private static JsonArray filterValidRouters(JsonArray routers) {
        final JsonArray validRouters = new JsonArray();
        final Set<String> names = new HashSet<>();
        for (int i = 0; i < routers.size(); i++) {
            final JsonObject router = routers.getJsonObject(i);
            final String name = router.getString(ROUTER_NAME);
            if (names.contains(name)) {
                final String errMsg = String.format("duplicated router name '%s'. Should be unique.", name);
                LOGGER.warn("ignoring invalid router '{}': {}", name, errMsg);
                continue;
            }
            names.add(name);

            try {
                RouterFactory.validateRouter(router);
            } catch (IllegalArgumentException e) {
                final String errMsg = e.getMessage();
                LOGGER.warn("ignoring invalid router '{}': {}", name, errMsg);
                continue;
            }

            validRouters.add(router);
        }
        return validRouters;
    }

    private static Set<String> collectMiddlewareNames(JsonArray middlewares) {
        if (middlewares == null) {
            return Set.of();
        }

        final Set<String> names = new HashSet<>();
        for (int i = 0; i < middlewares.size(); i++) {
            final JsonObject middleware = middlewares.getJsonObject(i);
            final String mwName = middleware.getString(MIDDLEWARE_NAME);
            if (mwName != null) {
                names.add(mwName);
            }
        }
        return names;
    }

    private static Set<String> collectServiceNames(JsonArray services) {
        if (services == null) {
            return Set.of();
        }

        final Set<String> names = new HashSet<>();
        for (int i = 0; i < services.size(); i++) {
            final JsonObject service = services.getJsonObject(i);
            final String svName = service.getString(SERVICE_NAME);
            if (svName != null) {
                names.add(svName);
            }
        }
        return names;
    }

    /**
     * Filters routers, which reference only middlewares and services that exist.
     * 
     * @param routers
     *            to validate
     * @param middlewareNames
     *            set of names of existing middleware
     * @param serviceNames
     *            set of names of existing services
     * @return the filtered routers
     */
    private static JsonArray filterRoutersWithValidReferences(JsonArray routers, Set<String> middlewareNames, Set<String> serviceNames) {
        final JsonArray validRouters = new JsonArray();

        for (int i = 0; i < routers.size(); i++) {
            final JsonObject router = routers.getJsonObject(i);
            final String name = router.getString(ROUTER_NAME);

            final JsonArray routerMwNames = router.getJsonArray(ROUTER_MIDDLEWARES, JsonArray.of());
            for (int j = 0; j < routerMwNames.size(); j++) {
                final String routerMwName = routerMwNames.getString(j);
                if (!middlewareNames.contains(routerMwName)) {
                    final String errMsg = String.format("unknown middleware '%s' defined", routerMwNames);
                    LOGGER.warn("ignoring invalid router '{}': {}", name, errMsg);
                    continue;
                }
            }

            final String routerSvcName = router.getString(ROUTER_SERVICE);
            if (!serviceNames.contains(routerSvcName)) {
                final String errMsg = String.format("unknown service '%s' defined", routerSvcName);
                LOGGER.warn("ignoring invalid router '{}': {}", name, errMsg);
                continue;
            }

            validRouters.add(router);
        }

        return validRouters;
    }

    public static Future<JsonArray> validateMiddlewares(JsonArray middlewares, boolean discardInvalid) {
        if (middlewares == null || middlewares.size() == 0) {
            LOGGER.debug("No middlewares defined");
            return Future.succeededFuture(JsonArray.of());
        }

        return filterValidMiddlewares(middlewares)
            .compose(validMiddlewares -> {
                if (!discardInvalid && validMiddlewares.size() != middlewares.size()) {
                    return Future.failedFuture("at least one middleware configuration is invalid");
                }
                return Future.succeededFuture(validMiddlewares);
            });
    }

    private static Future<JsonArray> filterValidMiddlewares(JsonArray middlewares) {
        final JsonArray validMiddlewares = new JsonArray();
        final List<Future<Void>> futs = new LinkedList<>();
        final Set<String> names = new HashSet<>();
        for (int i = 0; i < middlewares.size(); i++) {
            final JsonObject middleware = middlewares.getJsonObject(i);
            final String name = middleware.getString(MIDDLEWARE_NAME);
            if (names.contains(name)) {
                final String errMsg = String.format("duplicated middleware name '%s'. Should be unique.", name);
                LOGGER.info("ignoring invalid middleware'{}': {}", name, errMsg);
                continue;
            }
            names.add(name);

            final String type = middleware.getString(MIDDLEWARE_TYPE);
            final JsonObject options = middleware.getJsonObject(MIDDLEWARE_OPTIONS, new JsonObject());
            futs.add(validateMiddlewareOptions(type, options)
                .onSuccess(v -> validMiddlewares.add(middleware))
                .onFailure(err -> LOGGER.warn("ignoring invalid middleware '{}': {}", name, err.getMessage())));
        }

        final Promise<JsonArray> p = Promise.promise();
        Future.join(futs)
            .onComplete(cf -> {
                p.complete(validMiddlewares);
            });

        return p.future();
    }

    private static Future<Void> validateMiddlewareOptions(String mwType, JsonObject mwOptions) {
        final Optional<MiddlewareFactory> middlewareFactory = MiddlewareFactory.Loader.getFactory(mwType);
        if (middlewareFactory.isEmpty()) {
            final String errMsg = String.format("Unknown middleware '%s'", mwType);
            LOGGER.warn("{}", errMsg);
            return Future.failedFuture(errMsg);
        }

        return middlewareFactory.get()
            .validate(mwOptions)
            .recover(err -> Future.failedFuture(String.format("%s: %s", mwType, err)));
    }

    private static Future<JsonArray> validateServices(JsonArray services, boolean discardInvalid) {
        if (services == null || services.size() == 0) {
            LOGGER.debug("No services defined");
            return Future.succeededFuture(JsonArray.of());
        }

        final JsonArray validServices = filterValidServices(services);
        if (!discardInvalid && validServices.size() != services.size()) {
            return Future.failedFuture("at least one service configuration is invalid");
        }

        return Future.succeededFuture(validServices);
    }

    private static JsonArray filterValidServices(JsonArray services) {
        final JsonArray validServices = new JsonArray();
        final Set<String> names = new HashSet<>();
        for (int i = 0; i < services.size(); i++) {
            final JsonObject service = services.getJsonObject(i);
            final String name = service.getString(SERVICE_NAME);
            if (names.contains(name)) {
                final String errMsg = String.format("duplicated service name '%s'. Should be unique.", name);
                LOGGER.warn("ignoring invalid service '{}': {}", name, errMsg);
                continue;
            }
            names.add(name);

            try {
                validateService(service);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("ignoring invalid service '{}': {}", name, e.getMessage());
                continue;
            }
            validServices.add(service);
        }
        return validServices;
    }

    private static void validateService(JsonObject service) {
        final JsonArray servers = service.getJsonArray(SERVICE_SERVERS);
        if (servers == null || servers.size() == 0) {
            throw new IllegalArgumentException("no servers defined");
        }
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
