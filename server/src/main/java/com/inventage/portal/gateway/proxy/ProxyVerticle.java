package com.inventage.portal.gateway.proxy;

import com.inventage.portal.gateway.core.config.PortalGatewayConfigRetriever;
import com.inventage.portal.gateway.proxy.oauth2.OAuth2Configuration;
import com.inventage.portal.gateway.proxy.request.ProxiedHttpServerRequest;
import com.inventage.portal.gateway.proxy.request.header.RequestHeaderMiddleware;
import com.inventage.portal.gateway.proxy.request.header.RequestHeaderMiddlewareProvider;
import com.inventage.portal.gateway.proxy.request.uri.UriMiddlewareProvider;
import com.inventage.portal.gateway.proxy.service.Service;
import com.inventage.portal.gateway.proxy.service.ServiceProvider;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.AllowForwardHeaders;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Function;

/**
 *
 */
public class ProxyVerticle extends AbstractVerticle {

    public static final String SERVICE_PROVIDER = "provider";
    public static final String MIDDLEWARE_HEADERS = "headers";
    public static final String MIDDLEWARE_URIS = "uris";
    public static final String MIDDLEWARE_PROVIDER = "provider";

    private static Logger LOGGER = LoggerFactory.getLogger(ProxyVerticle.class);

    private String name;
    private Optional<JsonObject> middlewareConfig;
    private JsonObject serviceConfig;
    private Router router;
    private Optional<OAuth2Configuration> oAuth2Configuration;
    private RequestHeaderMiddleware headerMiddleware = RequestHeaderMiddleware.IDENTITY;
    private Function<String, String> uriMiddleware = Function.identity();


    private Service service;

    public ProxyVerticle(String proxyName, Optional<JsonObject> middlewareConfig, JsonObject serviceConfig, Router proxyRouter) {
        this(proxyName, middlewareConfig, serviceConfig, proxyRouter, Optional.empty());
    }

    public ProxyVerticle(String proxyName, Optional<JsonObject> middlewareConfig, JsonObject serviceConfig, Router proxyRouter, OAuth2Configuration oAuth2Configuration) {
        this(proxyName, middlewareConfig, serviceConfig, proxyRouter, Optional.of(oAuth2Configuration));
    }

    public ProxyVerticle(String proxyName, Optional<JsonObject> middlewareConfig, JsonObject serviceConfig, Router proxyRouter, Optional<OAuth2Configuration> oAuth2Configuration) {
        this.name = proxyName;
        this.middlewareConfig = middlewareConfig;
        this.serviceConfig = serviceConfig;
        this.router = proxyRouter;
        this.oAuth2Configuration = oAuth2Configuration;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        LOGGER.debug("start: proxy '{}'", name);
        init(startPromise);
    }

    private void init(Promise<Void> startPromise) {
        LOGGER.debug("init: proxy '{}'", name);
        final ConfigRetriever retriever = PortalGatewayConfigRetriever.create(vertx);
        retriever.getConfig(asyncResult -> {
            if (asyncResult.succeeded()) {
                final JsonObject globalConfig = asyncResult.result();
                configureMiddleware();
                service = fromServiceConfig(globalConfig);

                configureOptionalOAuth2().compose(authenticationHandler -> {
                    router.route().handler(this::forward);
                    LOGGER.debug("init: service '{}'", service);
                    return Future.succeededFuture();
                })
                        .onSuccess(s -> startPromise.complete())
                        .onFailure(startPromise::fail);
            }
            else {
                startPromise.fail(asyncResult.cause());
            }
        });
    }

    private void configureMiddleware() {
        if (middlewareConfig.isPresent()) {
            // headers
            final JsonArray headers = middlewareConfig.get().getJsonArray(MIDDLEWARE_HEADERS);
            headers.stream().map(object -> new JsonObject(Json.encode(object)))
                    .forEach(header -> {
                        final RequestHeaderMiddlewareProvider provider = RequestHeaderMiddlewareProvider.Loader.getProvider(header.getString(MIDDLEWARE_PROVIDER));
                        headerMiddleware = headerMiddleware.andThen(provider.create(header));
                    });
            // uris
            final JsonArray uris = middlewareConfig.get().getJsonArray(MIDDLEWARE_URIS);
            uris.stream().map(object -> new JsonObject(Json.encode(object)))
                    .forEach(uri -> {
                        final UriMiddlewareProvider provider = UriMiddlewareProvider.Loader.getProvider(uri.getString(MIDDLEWARE_PROVIDER));
                        uriMiddleware = uriMiddleware.andThen(provider.create(uri));
                    });

        }
    }

    private Service fromServiceConfig(JsonObject globalConfig) {
        return ServiceProvider.Loader.getProvider(serviceConfig.getString(SERVICE_PROVIDER)).create(serviceConfig, globalConfig, vertx);
    }

    private Future<?> configureOptionalOAuth2() {
        final Promise<Object> promise = Promise.promise();
        if (oAuth2Configuration.isPresent()) {
            KeycloakAuth.discover(vertx, oAuth2Options())
                    .onSuccess(oAuth2 -> {
                        router.route().handler(this::prepareUser);
                        oAuth2Configuration.get().callback().handler(this::storeUserForService);
                        // callbackURL (muss aus Sicht des Browsers definiert werden!)
                        final AuthenticationHandler authenticationHandler = OAuth2AuthHandler.create(vertx, oAuth2, "http://localhost:8000" + oAuth2Configuration.get().callback().getPath())
                                .setupCallback(oAuth2Configuration.get().callback());
                        router.route().handler(authenticationHandler);
                        promise.complete(authenticationHandler);
                    })
                    .onFailure(failure -> {
                        final String message = String.format("configureOptionalOAuth2: for proxy '%s' failed with message '%s'", name, failure.getMessage());
                        LOGGER.error(message);
                        promise.fail(message);
                    });
        }
        else {
            promise.complete();
        }
        return promise.future();
    }

    private OAuth2Options oAuth2Options() {
        return new OAuth2Options()
                .setClientID(oAuth2Configuration.orElseThrow().clientId())
                .setClientSecret(oAuth2Configuration.orElseThrow().clientSecret())
                .setSite(oAuth2Configuration.orElseThrow().discoveryUrl());
    }

    // set the user specific for this service
    protected void prepareUser(RoutingContext rc) {
        final User user = rc.user();
        final User service_user = rc.session().get(name);
        rc.setUser(service_user);
        rc.next();
        rc.setUser(user);
    }

    // after the callback handling, we can get and store the user for this service
    protected void storeUserForService(RoutingContext rc) {
        rc.addEndHandler(event -> {
            if (rc.user() != null) {
                rc.session().put(name, rc.user());
            }
        });
        rc.next();
    }

    /**
     * Forward the request to the service.
     *
     * @param rc with the request to be forwarded
     */
    protected void forward(RoutingContext rc) {
        LOGGER.info("forward: request with uri '{}' to service '{}'", rc.request().uri(), service);
        service.proxy().handle(new ProxiedHttpServerRequest(rc, AllowForwardHeaders.ALL)
                .setHeaderMiddleware(headerMiddleware)
                .setUriMiddleware(uriMiddleware)
        );
     }

}
