package com.inventage.portal.gateway.core.entrypoint;

import java.util.Optional;

import com.inventage.portal.gateway.core.application.Application;
import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.core.log.RequestResponseLogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

/**
 * Entry point for the portal gateway.
 */
public class Entrypoint {

    private static Logger LOGGER = LoggerFactory.getLogger(Entrypoint.class);

    public static final String SESSION_COOKIE_NAME = "inventage-portal-gateway.session";
    public static final boolean SESSION_COOKIE_HTTP_ONLY = true;
    public static final boolean SESSION_COOKIE_SECURE = false;
    public static final CookieSameSite SESSION_COOKIE_SAME_SITE = CookieSameSite.STRICT;
    public static final int SESSION_COOKIE_MIN_LENGTH = 32;

    private final Vertx vertx;
    private final String name;
    private final int port;
    private final boolean sessionDisabled;
    private Router router;
    private boolean enabled;
    private Tls tls;

    public Entrypoint(Vertx vertx, String name, int port, boolean sessionDisabled) {
        this.vertx = vertx;
        this.name = name;
        this.port = port;
        this.sessionDisabled = sessionDisabled;
        this.enabled = true;
    }

    public String name() {
        return name;
    }

    public int port() {
        return port;
    }

    public Router router() {
        if (router != null) {
            return router;
        }
        router = Router.router(vertx);
        router.route().handler(RequestResponseLogger.create());

        if (this.sessionDisabled) {
            LOGGER.info("router: session managament is disabled");
            return router;
        }
        LOGGER.info(
                "router: session managament is enabled with\n" + "session cookie name: '{}'\n"
                        + "session cookie http only: '{}'\n" + "session cookie secure: '{}'\n"
                        + "session cookie same site: '{}'\n" + "session cookie min length: '{}'",
                SESSION_COOKIE_NAME, SESSION_COOKIE_HTTP_ONLY, SESSION_COOKIE_SECURE, SESSION_COOKIE_SAME_SITE,
                SESSION_COOKIE_MIN_LENGTH);
        // https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html
        router.route()
                .handler(SessionHandler.create(LocalSessionStore.create(vertx))
                        .setSessionCookieName(SESSION_COOKIE_NAME)
                        .setCookieHttpOnlyFlag(SESSION_COOKIE_HTTP_ONLY)
                        .setCookieSecureFlag(SESSION_COOKIE_SECURE)
                        .setCookieSameSite(SESSION_COOKIE_SAME_SITE)
                        .setMinLength(SESSION_COOKIE_MIN_LENGTH)
                        .setNagHttps(true));

        return router;
    }

    public void mount(Application application) {
        final Optional<Router> optionApplicationRouter = application.router();
        optionApplicationRouter.ifPresent(applicationRouter -> {
            if (name.equals(application.entrypoint())) {
                if (enabled()) {
                    router().mountSubRouter(application.rootPath(), applicationRouter);
                    LOGGER.info("mount: application '{}' for '{}' at endpoint '{}'", application,
                            application.rootPath(), name);
                } else {
                    LOGGER.warn("mount: disabled endpoint '{}' can not mount application '{}' for '{}'", name,
                            application, application.rootPath());
                }
            }
        });
    }

    public boolean isTls() {
        return tls != null;
    }

    public JksOptions jksOptions() {
        if (isTls()) {
            return tls.jksOptions();
        }
        return new JksOptions();
    }

    public boolean enabled() {
        return enabled && port > 0;
    }

    public void disable() {
        enabled = false;
    }

    class Tls {

        public JksOptions jksOptions() {
            return null;
            // final String keyStorePath = config.getString(CONFIG_PREFIX +
            // CONFIG_HTTPS_KEY_STORE_PATH);
            // if (keyStorePath == null || keyStorePath.isEmpty()) {
            // throw new IllegalStateException("When using https the path to the key store must be
            // configured by variable: '" +
            // CONFIG_PREFIX + "https-key-store-path'. To disable the https port configuration use
            // '-1' as port.");
            // }
            // return new JksOptions()
            // .setPath(keyStorePath)
            // .setPassword(config.getString(CONFIG_PREFIX + CONFIG_HTTPS_KEY_STORE_PASSWORD));
        }
    }

    public static JsonObject entrypointConfigByName(String name, JsonObject globalConfig) {
        final JsonArray configs = globalConfig.getJsonArray(StaticConfiguration.ENTRYPOINTS);
        return configs.stream().map(object -> new JsonObject(Json.encode(object)))
                .filter(entrypoint -> entrypoint.getString(StaticConfiguration.ENTRYPOINT_NAME).equals(name))
                .findFirst().orElseThrow(() -> {
                    throw new IllegalStateException(String.format("Entrypoint '%s' not found!", name));
                });
    }

}
