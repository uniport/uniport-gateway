package com.inventage.portal.gateway.proxy;

import com.inventage.portal.gateway.core.application.Application;
import com.inventage.portal.gateway.proxy.oauth2.OAuth2Configuration;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Application for the proxy feature of the portal gateway.
 * The proxies will be read from the "proxies" configuration key.
 */
public class ProxyApplication implements Application {

    public static final String PROXIES = "proxies";
    public static final String PROXY_NAME = "name";
    public static final String PATH_PREFIX = "pathPrefix";
    public static final String OAUTH2 = "oauth2";
    public static final String OAUTH2_CLIENTID = "clientId";
    public static final String OAUTH2_CLIENTSECRET = "clientSecret";
    public static final String OAUTH2_DISCOVERYURL = "discoveryUrl";
    public static final String OAUTH2_CALLBACK_PREFIX = "/callback/";
    public static final String SERVICES = "services";
    public static final String SERVICE = "service";
    public static final String SERVICE_NAME = "name";
    public static final String MIDDLEWARE = "middleware";

    /**
     * the name of this instance
     */
    private final String name;

    /**
     * the name of the entrypoint this application should be mounted on
     */
    private final String entrypoint;

    private final JsonObject config;

    /**
     * the selection criteria for delegating incoming requests to this application
     */
    private String rootPath = "/";

    /**
     * the router on which the routes for this application will be added
     */
    private final Router router;

    public ProxyApplication(String name, String entrypoint, JsonObject config, Vertx vertx) {
        this.name = name;
        this.entrypoint = entrypoint;
        this.config = config;
        this.router = Router.router(vertx);
    }

    public String toString() {
        return String.format("%s:%s", getClass().getSimpleName(), name);
    }

    @Override
    public Future<?> deployOn(Vertx vertx) {
        return CompositeFuture.join(proxies(router, vertx).stream()
                .map(proxyVerticle -> deployVerticle(proxyVerticle, vertx))
                .collect(Collectors.toList()));
    }

    private Future<String> deployVerticle(ProxyVerticle proxyVerticle, Vertx vertx) {
        return vertx.deployVerticle(proxyVerticle);
    }

    @Override
    public String rootPath() {
        return rootPath;
    }

    @Override
    public Optional<Router> router() {
        return Optional.of(router);
    }

    @Override
    public String entrypoint() {
        return entrypoint;
    }

    private List<ProxyVerticle> proxies(Router router, Vertx vertx) {
        List<ProxyVerticle> result = new ArrayList<>();
        final JsonArray configs = config.getJsonArray(PROXIES);
        configs.stream()
                .map(object -> new JsonObject(Json.encode(object)))
                .forEach(proxy -> {
                    final Router proxyRouter = Router.router(vertx);
                    final Optional<OAuth2Configuration> oAuth2Configuration = oAuth2Configuration(proxy);
                    final ProxyVerticle proxyVerticle = new ProxyVerticle(proxy.getString(PROXY_NAME), middlewareConfig(proxy), serviceConfig(proxy.getString(SERVICE)), proxyRouter, oAuth2Configuration);
                    router.mountSubRouter(proxy.getString(PATH_PREFIX), proxyRouter);
                    result.add(proxyVerticle);
                });
        return result;
    }

    private Optional<JsonObject> middlewareConfig(JsonObject proxy) {
        final JsonObject middleware = proxy.getJsonObject(MIDDLEWARE);
        if (middleware != null) {
            return Optional.of(middleware);
        }
        else {
            return Optional.empty();
        }
    }

    private Optional<OAuth2Configuration> oAuth2Configuration(JsonObject proxy) {
        final JsonObject oauth2 = proxy.getJsonObject(OAUTH2);
        if (oauth2 != null) {
            return Optional.of(
                    new OAuth2Configuration(
                            oauth2.getString(OAUTH2_CLIENTID),
                            oauth2.getString(OAUTH2_CLIENTSECRET),
                            oauth2.getString(OAUTH2_DISCOVERYURL),
                            router.get(OAUTH2_CALLBACK_PREFIX + oauth2.getString(OAUTH2_CLIENTID).toLowerCase()))
            );
        }
        else {
            return Optional.empty();
        }
    }

    private JsonObject serviceConfig(String name) {
        final JsonArray configs = config.getJsonArray(SERVICES);
        return configs.stream().map(object -> new JsonObject(Json.encode(object)))
                .filter(service -> service.getString(SERVICE_NAME).equals(name))
                .findFirst().orElseThrow(() -> { throw new IllegalStateException(String.format("Service '%s' not found!", name)); });
    }
}
