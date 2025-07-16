package ch.uniport.gateway;

import ch.uniport.gateway.core.config.StaticConfiguration;
import ch.uniport.gateway.proxy.config.DynamicConfiguration;
import ch.uniport.gateway.proxy.config.model.DynamicModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;

public final class TestUtils {

    private TestUtils() {
    }

    /**
     * Returns a free port number on localhost.
     * <p>
     * Heavily inspired from org.eclipse.jdt.launching.SocketUtil (to avoid a
     * dependency to JDT just because of this).
     * Slightly improved with close() missing in JDT. And throws exception instead
     * of returning -1.
     *
     * @return a free port number on localhost
     * @throws IllegalStateException
     *             if unable to find a free port
     */
    public static int findFreePort() {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            final int port = socket.getLocalPort();
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore IOException on close()
            }
            return port;
        } catch (IOException e) {
            // do nothing
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }
        throw new IllegalStateException("Could not find a free TCP/IP port to start embedded Jetty HTTP Server on");
    }

    public static DynamicModel toModel(JsonObject config) {
        final JsonObject httpJson = config.getJsonObject(DynamicConfiguration.HTTP);
        final ObjectMapper codec = new ObjectMapper();
        DynamicModel gateway = null;
        try {
            gateway = codec.readValue(httpJson.encode(), DynamicModel.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return gateway;
    }

    // buildConfiguration is a helper to create a configuration.
    public static JsonObject buildConfiguration(Handler<JsonObject>... dynamicConfigBuilders) {
        final JsonObject conf = new JsonObject();
        final JsonObject httpConf = new JsonObject();
        conf.put(DynamicConfiguration.HTTP, httpConf);

        for (Handler<JsonObject> build : dynamicConfigBuilders) {
            build.handle(httpConf);
        }
        return conf;
    }

    public static Handler<JsonObject> withRouters(Handler<JsonObject>... opts) {
        return conf -> {
            final JsonArray routers = new JsonArray();
            conf.put(DynamicConfiguration.ROUTERS, routers);
            for (Handler<JsonObject> opt : opts) {
                final JsonObject r = new JsonObject();
                opt.handle(r);
                routers.add(r);
            }
        };
    }

    public static Handler<JsonObject> withRouter(String routerName, Handler<JsonObject>... opts) {
        return router -> {
            router.put(DynamicConfiguration.ROUTER_NAME, routerName);
            for (Handler<JsonObject> opt : opts) {
                opt.handle(router);
            }
        };
    }

    public static Handler<JsonObject> withRouterMiddlewares(String... middlewareNames) {
        return router -> {
            router.put(DynamicConfiguration.ROUTER_MIDDLEWARES, new JsonArray(Arrays.asList(middlewareNames)));
        };
    }

    public static Handler<JsonObject> withRouterService(String serviceName) {
        return router -> {
            router.put(DynamicConfiguration.ROUTER_SERVICE, serviceName);
        };
    }

    public static Handler<JsonObject> withRouterEntrypoints(String... eps) {
        return router -> {
            router.put(DynamicConfiguration.ROUTER_ENTRYPOINTS, new JsonArray(Arrays.asList(eps)));
        };
    }

    public static Handler<JsonObject> withRouterRule(String rule) {
        return router -> {
            router.put(DynamicConfiguration.ROUTER_RULE, rule);
        };
    }

    public static Handler<JsonObject> withRouterPriority(int priority) {
        return router -> {
            router.put(DynamicConfiguration.ROUTER_PRIORITY, priority);
        };
    }

    public static Handler<JsonObject> withMiddlewares(Handler<JsonObject>... opts) {
        return conf -> {
            final JsonArray middlewares = new JsonArray();
            conf.put(DynamicConfiguration.MIDDLEWARES, middlewares);
            for (Handler<JsonObject> opt : opts) {
                final JsonObject m = new JsonObject();
                opt.handle(m);
                middlewares.add(m);
            }
        };
    }

    public static Handler<JsonObject> withMiddleware(
        String middlewareName, String middlewareType,
        Handler<JsonObject>... opts
    ) {
        return middleware -> {
            middleware.put(DynamicConfiguration.MIDDLEWARE_NAME, middlewareName);
            middleware.put(DynamicConfiguration.MIDDLEWARE_TYPE, middlewareType);
            for (Handler<JsonObject> opt : opts) {
                opt.handle(middleware);
            }
        };
    }

    public static Handler<JsonObject> withMiddlewareOpts(JsonObject opts) {
        return middleware -> {
            middleware.put(DynamicConfiguration.MIDDLEWARE_OPTIONS, opts);
        };
    }

    public static Handler<JsonObject> withServices(Handler<JsonObject>... opts) {
        return conf -> {
            final JsonArray services = new JsonArray();
            conf.put(DynamicConfiguration.SERVICES, services);
            for (Handler<JsonObject> opt : opts) {
                final JsonObject s = new JsonObject();
                opt.handle(s);
                services.add(s);
            }
        };
    }

    public static Handler<JsonObject> withService(String serviceName, Handler<JsonObject>... opts) {
        return service -> {
            service.put(DynamicConfiguration.SERVICE_NAME, serviceName);
            for (Handler<JsonObject> opt : opts) {
                opt.handle(service);
            }
        };
    }

    public static Handler<JsonObject> withServers(Handler<JsonObject>... opts) {
        return service -> {
            final JsonArray servers = new JsonArray();
            service.put(DynamicConfiguration.SERVICE_SERVERS, servers);
            for (Handler<JsonObject> opt : opts) {
                final JsonObject server = new JsonObject();
                opt.handle(server);
                servers.add(server);
            }
        };
    }

    public static Handler<JsonObject> withServer(String host, int port, Handler<JsonObject>... opts) {
        return server -> {
            server.put(DynamicConfiguration.SERVICE_SERVER_HOST, host);
            server.put(DynamicConfiguration.SERVICE_SERVER_PORT, port);

            final JsonObject httpOptions = new JsonObject();
            for (Handler<JsonObject> opt : opts) {
                opt.handle(httpOptions);
            }
            if (httpOptions.isEmpty()) {
                return;
            }
            server.put(DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS, httpOptions);
        };
    }

    public static Handler<JsonObject> withServerHttpOptions(
        boolean verifyHostname, boolean trustAll,
        String trustStorePath, String trustStorePassword
    ) {
        return httpOptions -> {
            httpOptions.put(DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS_VERIFY_HOSTNAME, verifyHostname);
            httpOptions.put(DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_ALL, trustAll);
            httpOptions.put(DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PATH, trustStorePath);
            httpOptions.put(DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PASSWORD, trustStorePassword);
        };
    }

    // For static configuration
    public static JsonObject buildStaticConfiguration(Handler<JsonObject>... staticConfiguration) {
        final JsonObject conf = new JsonObject();
        for (Handler<JsonObject> build : staticConfiguration) {
            build.handle(conf);
        }
        return conf;
    }

    public static Handler<JsonObject> withEntrypoints(Handler<JsonObject>... opts) {
        return conf -> {
            final JsonArray entrypoints = new JsonArray();
            conf.put(StaticConfiguration.ENTRYPOINTS, entrypoints);
            for (Handler<JsonObject> opt : opts) {
                final JsonObject entrypoint = new JsonObject();
                opt.handle(entrypoint);
                entrypoints.add(entrypoint);
            }
        };
    }

    public static Handler<JsonObject> withEntrypoint(String name, int port, Handler<JsonObject> entryMiddleware) {
        return conf -> {
            conf.put(StaticConfiguration.ENTRYPOINT_NAME, name);
            conf.put(StaticConfiguration.ENTRYPOINT_PORT, port);
            entryMiddleware.handle(conf);
        };
    }

    public static Handler<JsonObject> withEntrypoint(String name, int port) {
        return conf -> {
            conf.put(StaticConfiguration.ENTRYPOINT_NAME, name);
            conf.put(StaticConfiguration.ENTRYPOINT_PORT, port);
        };
    }

    public static Handler<JsonObject> withProviders(Handler<JsonObject>... opts) {
        return conf -> {
            final JsonArray providers = new JsonArray();
            conf.put(StaticConfiguration.PROVIDERS, providers);
            for (Handler<JsonObject> opt : opts) {
                final JsonObject provider = new JsonObject();
                opt.handle(provider);
                providers.add(provider);
            }
        };
    }

    public static Map<String, String> extractParametersFromHeader(String header) {
        List<NameValuePair> responseParamsList = null;
        try {
            responseParamsList = new URIBuilder(new URI(header), StandardCharsets.UTF_8).getQueryParams();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        if (responseParamsList == null) {
            throw new IllegalStateException("cannot be null");
        }
        final Map<String, String> responseParamsMap = responseParamsList.stream().collect(Collectors.toMap(
            entry -> entry.getName(), entry -> entry.getValue()));

        return responseParamsMap;
    }
}
