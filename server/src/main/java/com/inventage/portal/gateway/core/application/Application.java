package com.inventage.portal.gateway.core.application;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

import java.util.Optional;

/**
 * The portal gateway is made of different application. Every one has to implement this interface.
 */
public interface Application {

    // keys in the portal-gateway.json

    String APPLICATIONS = "applications";
    String NAME = "name";
    String ENTRYPOINT = "entrypoint";
    String PROVIDER = "provider";

    /**
     * @return URL path for which this application will be responsible
     */
    String rootPath();

    /**
     * @return router which will be mounted by the global router for receiving requests
     * @see ApplicationFactory#create(io.vertx.core.json.JsonObject, io.vertx.core.json.JsonObject,
     *      io.vertx.core.Vertx)
     */
    Optional<Router> router();

    /**
     * @return entrypoint this application should be attached to
     */
    String entrypoint();

    /**
     * Deploy the verticles this application is made of.
     *
     * @param vertx running instance
     * @return future for signalling when this application has been deployed
     */
    Future<?> deployOn(Vertx vertx);
}
