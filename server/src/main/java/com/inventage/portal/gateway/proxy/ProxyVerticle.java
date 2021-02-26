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
import io.vertx.ext.auth.oauth2.impl.OAuth2AuthProviderImpl;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.AllowForwardHeaders;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
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

    private JsonObject proxyConfig;
    private String publicHostname;
    private int entrypointPort;
    private Optional<JsonObject> middlewareConfig;
    private JsonObject serviceConfig;
    private Router router;
    private Optional<OAuth2Configuration> oAuth2Configuration;
    private RequestHeaderMiddleware headerMiddleware = RequestHeaderMiddleware.IDENTITY;
    private Function<String, String> uriMiddleware = Function.identity();


    private Service service;

    public ProxyVerticle(JsonObject proxyConfig, String publicHostname, int entrypointPort, Optional<JsonObject> middlewareConfig, JsonObject serviceConfig, Router proxyRouter) {
        this(proxyConfig, publicHostname, entrypointPort, middlewareConfig, serviceConfig, proxyRouter, Optional.empty());
    }

    public ProxyVerticle(JsonObject proxyConfig, String publicHostname, int entrypointPort, Optional<JsonObject> middlewareConfig, JsonObject serviceConfig, Router proxyRouter, Optional<OAuth2Configuration> oAuth2Configuration) {
        this.proxyConfig = proxyConfig;
        this.publicHostname = publicHostname;
        this.entrypointPort = entrypointPort;
        this.middlewareConfig = middlewareConfig;
        this.serviceConfig = serviceConfig;
        this.router = proxyRouter;
        this.oAuth2Configuration = oAuth2Configuration;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        LOGGER.debug("start: proxy '{}'", proxyConfig.getString(ProxyApplication.PROXY_NAME));
        init(startPromise);
    }

    private void init(Promise<Void> startPromise) {
        LOGGER.debug("init: proxy '{}'", proxyConfig.getString(ProxyApplication.PROXY_NAME));
        final ConfigRetriever retriever = PortalGatewayConfigRetriever.create(vertx);
        retriever.getConfig(asyncResult -> {
            if (asyncResult.succeeded()) {
                final JsonObject globalConfig = asyncResult.result();
                configureMiddleware();
                service = fromServiceConfig(globalConfig);

                configureOptionalOAuth2(globalConfig).compose(authenticationHandler -> {
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
            headerMiddlewareConfig().ifPresent(headers -> headers.stream().map(object -> new JsonObject(Json.encode(object)))
                    .forEach(header -> {
                        final RequestHeaderMiddlewareProvider provider = RequestHeaderMiddlewareProvider.Loader.getProvider(header.getString(MIDDLEWARE_PROVIDER));
                        headerMiddleware = headerMiddleware.andThen(provider.create(header));
                    })
            );
            // uris
            uriMiddlewareConfig().ifPresent(uris -> uris.stream().map(object -> new JsonObject(Json.encode(object)))
                    .forEach(uri -> {
                        final UriMiddlewareProvider provider = UriMiddlewareProvider.Loader.getProvider(uri.getString(MIDDLEWARE_PROVIDER));
                        uriMiddleware = uriMiddleware.andThen(provider.create(uri));
                    })
            );
        }
    }

    private Optional<JsonArray> uriMiddlewareConfig() {
        final JsonArray uris = middlewareConfig.get().getJsonArray(MIDDLEWARE_URIS);
        return uris != null ? Optional.of(uris) : Optional.empty();
    }

    private Optional<JsonArray> headerMiddlewareConfig() {
        final JsonArray headers = middlewareConfig.get().getJsonArray(MIDDLEWARE_HEADERS);
        return headers != null ? Optional.of(headers) : Optional.empty();
    }

    private Service fromServiceConfig(JsonObject globalConfig) {
        return ServiceProvider.Loader.getProvider(serviceConfig.getString(SERVICE_PROVIDER)).create(serviceConfig, globalConfig, vertx);
    }

    private Future<?> configureOptionalOAuth2(JsonObject globalConfig) {
        final Promise<Object> promise = Promise.promise();
        if (oAuth2Configuration.isPresent()) {
            KeycloakAuth.discover(vertx, oAuth2Options())
                    .onSuccess(oAuth2 -> {
                        router.route().handler(this::prepareUser);
                        oAuth2Configuration.get().callback().handler(this::storeUserForService);
                        patchAuthorizationPath(((OAuth2AuthProviderImpl) oAuth2).getConfig(), globalConfig);
                        final AuthenticationHandler authenticationHandler = OAuth2AuthHandler.create(vertx, oAuth2, String.format("http://%s:%s%s", publicHostname, entrypointPort, oAuth2Configuration.get().callback().getPath()))
                                .withScope("openid").setupCallback(oAuth2Configuration.get().callback());
                        router.route().handler(authenticationHandler);
                        promise.complete(authenticationHandler);
                    })
                    .onFailure(failure -> {
                        final String message = String.format("configureOptionalOAuth2: for proxy '%s' failed with message '%s'", proxyConfig.getString(ProxyApplication.PROXY_NAME), failure.getMessage());
                        LOGGER.error(message);
                        promise.fail(message);
                    });
        }
        else {
            promise.complete();
        }
        return promise.future();
    }

    /**
     * Change the authorization path so that the requests go again through the portal gateway
     * @param configToPatch to be changed
     * @return the given config
     */
    private OAuth2Options patchAuthorizationPath(OAuth2Options configToPatch, JsonObject globalConfig) {
        try {
            final URI uri = new URI(configToPatch.getAuthorizationPath());
            final String newAuthorizationPath = String.format("%s://%s:%s%s", "http", publicHostname, entrypointPort, uri.getPath());
            configToPatch.setAuthorizationPath(newAuthorizationPath);
        }
        catch (Exception e) {

        }
        return configToPatch;
    }

    // for doing the OIDC discovery request
    private OAuth2Options oAuth2Options() {
        return new OAuth2Options()
                .setClientID(oAuth2Configuration.orElseThrow().clientId())
                .setClientSecret(oAuth2Configuration.orElseThrow().clientSecret())
                .setSite(oAuth2Configuration.orElseThrow().discoveryUrl());
    }

    // set the user specific for this service
    protected void prepareUser(RoutingContext rc) {
        final User user = rc.user();
        final User service_user = rc.session().get(sessionScopeOrName());
        rc.setUser(service_user);
        rc.next();
        rc.setUser(user);
    }

    // after the callback handling, we can get and store the user for this service
    protected void storeUserForService(RoutingContext rc) {
        rc.addEndHandler(event -> {
            if (rc.user() != null) {
                final JsonObject principal = rc.user().principal();
                rc.session().put(sessionScopeOrName(), rc.user());
            }
        });
        rc.next();
    }

    private String sessionScopeOrName() {
        return proxyConfig.getString("sessionScope") != null ? proxyConfig.getString("sessionScope") : proxyConfig.getString(ProxyApplication.PROXY_NAME);
    }

    /**
     * Forward the request to the service.
     *
     * @param rc with the request to be forwarded
     */
    protected void forward(RoutingContext rc) {
        LOGGER.info("forward: request with uri '{}' to service '{}'", rc.request().uri(), service);
        service.handle(new ProxiedHttpServerRequest(rc, AllowForwardHeaders.ALL)
                .setHeaderMiddleware(headerMiddleware)
                .setUriMiddleware(uriMiddleware)
        );
     }

}
