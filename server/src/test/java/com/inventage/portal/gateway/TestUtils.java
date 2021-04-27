package com.inventage.portal.gateway;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class TestUtils {
    /**
     * Returns a free port number on localhost, or throws exception if unable to find a free port.
     *
     * @return a free port number on localhost, or throws exception  if unable to find a free port
     * @since 3.0
     */
    public static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
        }
        throw new IllegalStateException("Could not find a free TCP/IP port to start embedded Jetty HTTP Server on");
    }

    // buildConfiguration is a helper to create a configuration.
    public static JsonObject buildConfiguration(Handler<JsonObject>... dynamicConfigBuilders) {
        JsonObject conf = new JsonObject();
        JsonObject httpConf = new JsonObject();
        conf.put(DynamicConfiguration.HTTP, httpConf);

        for (Handler<JsonObject> build : dynamicConfigBuilders) {
            build.handle(httpConf);
        }
        return conf;
    }

    public static Handler<JsonObject> withRouters(Handler<JsonObject>... opts) {
        return conf -> {
            JsonArray routers = new JsonArray();
            conf.put(DynamicConfiguration.ROUTERS, routers);
            for (Handler<JsonObject> opt : opts) {
                JsonObject r = new JsonObject();
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

    public static Handler<JsonObject> withMiddlewares(Handler<JsonObject>... opts) {
        return conf -> {
            JsonArray middlewares = new JsonArray();
            conf.put(DynamicConfiguration.MIDDLEWARES, middlewares);
            for (Handler<JsonObject> opt : opts) {
                JsonObject m = new JsonObject();
                opt.handle(m);
                middlewares.add(m);
            }
        };
    }

    public static Handler<JsonObject> withMiddleware(String middlewareName, Handler<JsonObject>... opts) {
        return middleware -> {
            middleware.put(DynamicConfiguration.MIDDLEWARE_NAME, middlewareName);
            for (Handler<JsonObject> opt : opts) {
                opt.handle(middleware);
            }
        };
    }

    public static Handler<JsonObject> withServices(Handler<JsonObject>... opts) {
        return conf -> {
            JsonArray services = new JsonArray();
            conf.put(DynamicConfiguration.SERVICES, services);
            for (Handler<JsonObject> opt : opts) {
                JsonObject s = new JsonObject();
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
            JsonArray servers = new JsonArray();
            service.put(DynamicConfiguration.SERVICE_SERVERS, servers);
            for (Handler<JsonObject> opt : opts) {
                JsonObject server = new JsonObject();
                opt.handle(server);
                servers.add(server);
            }
        };
    }

    public static Handler<JsonObject> withServer(String host, int port, Handler<JsonObject>... opts) {
        return server -> {
            server.put(DynamicConfiguration.SERVICE_SERVER_HOST, host);
            server.put(DynamicConfiguration.SERVICE_SERVER_PORT, port);
            for (Handler<JsonObject> opt : opts) {
                opt.handle(server);
            }
        };
    }

}
