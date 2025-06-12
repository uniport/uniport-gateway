package com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.publickeysReconciler;

import com.inventage.portal.gateway.proxy.middleware.authorization.JWKAccessibleAuthHandler;
import com.inventage.portal.gateway.proxy.middleware.authorization.PublicKeyOptions;
import com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.customClaimsChecker.JWTAuthAdditionalClaimsOptions;
import com.inventage.portal.gateway.proxy.middleware.authorization.bearerOnly.customIssuerChecker.JWTAuthMultipleIssuersOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.handler.AuthenticationHandler;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given a list of public key source, {@code JWTAuthPublicKeysReconcilerHandler} created by {@link create}
 * can fetch the public key.
 * A public key source can be a literal public key OR a URL pointing to a Keycloak realm (or more general an OIDC Provider).
 */
public interface JWTAuthPublicKeysReconcilerHandler extends JWKAccessibleAuthHandler {

    String OIDC_DISCOVERY_PATH = "/.well-known/openid-configuration";
    String JWKS_URI_KEY = "jwks_uri";
    String JWK_KEYS_KEY = "keys";
    String JWK_KID = "kid";

    Logger LOGGER = LoggerFactory.getLogger(JWTAuthPublicKeysReconcilerHandler.class);

    static JWTAuthPublicKeysReconcilerHandler create(
        Vertx vertx,
        JWTAuthOptions jwtAuthOptions,
        JWTAuthMultipleIssuersOptions additionalIssuersOptions,
        JWTAuthAdditionalClaimsOptions additionalClaimsOptions,
        List<PublicKeyOptions> publicKeys,
        boolean reconciliationEnabled,
        long reconciliationIntervalMs
    ) {
        return new JWTAuthPublicKeysReconcilerHandlerImpl(
            vertx,
            jwtAuthOptions,
            additionalIssuersOptions,
            additionalClaimsOptions,
            publicKeys,
            reconciliationEnabled,
            reconciliationIntervalMs);
    }

    Future<AuthenticationHandler> getOrRefreshPublicKeys();

    /**
     * Get the JSON web keys.
     *
     * see https://openid.net/specs/draft-jones-json-web-key-03.html
     * 
     * @return a list of JSON web keys as JSON data structure
     */
    List<JsonObject> getJwks();

    static Future<JWTAuthOptions> fetchPublicKeys(Vertx vertx, List<PublicKeyOptions> publicKeys) {
        final JWTAuthOptions authOpts = new JWTAuthOptions();
        final List<Future<List<JsonObject>>> futures = new LinkedList<>();

        publicKeys.forEach(pk -> {
            final String publicKey = pk.getKey();

            if (isURL(publicKey)) {
                LOGGER.info("Public key provided by URL. Fetching JWKs...");
                futures.add(
                    fetchJWKsFromDiscoveryURL(vertx, publicKey)
                        .onSuccess(jwks -> jwks.forEach(jwk -> authOpts.addJwk(jwk))));
            } else {
                LOGGER.info("Public key provided directly");
                authOpts.addPubSecKey(
                    new PubSecKeyOptions()
                        .setAlgorithm(pk.getAlgorithm())
                        .setBuffer(publickeyToPEM(publicKey)));
            }
        });

        return Future.join(futures)
            .map(cf -> {
                LOGGER.info("Successfully fetched JWKs");
                return authOpts;
            });
    }

    private static boolean isURL(String publicKey) {
        try {
            new URI(publicKey);
        } catch (URISyntaxException e) {
            LOGGER.debug("URI is malformed, hence it is has to be a raw public key: '{}'", e.getMessage());
            return false;
        }
        return true;
    }

    private static Future<List<JsonObject>> fetchJWKsFromDiscoveryURL(Vertx vertx, String rawRealmBaseURL) {
        final Promise<List<JsonObject>> promise = Promise.promise();
        fetchJWKsFromDiscoveryURL(vertx, rawRealmBaseURL, promise);
        return promise.future();
    }

    private static void fetchJWKsFromDiscoveryURL(
        Vertx vertx, String rawRealmBaseURL,
        Handler<AsyncResult<List<JsonObject>>> handler
    ) {
        final URL parsedRealmBaseURL;
        try {
            parsedRealmBaseURL = new URL(rawRealmBaseURL);
        } catch (MalformedURLException e) {
            LOGGER.warn("Malformed discovery URL '{}'", rawRealmBaseURL);
            handler.handle(Future.failedFuture(e));
            return;
        }

        final String iamProtocol = parsedRealmBaseURL.getProtocol();
        final String iamHost = parsedRealmBaseURL.getHost();
        int port = parsedRealmBaseURL.getPort();
        if (port <= 0) {
            if (iamProtocol.endsWith("s")) {
                port = 443;
            } else {
                port = 80;
            }
        }

        String path = parsedRealmBaseURL.getPath();
        if (path == null) {
            path = "/";
        }

        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        // allow both to avoid confusion:
        // * url with OIDC discovery path already included
        // * url with OIDC discovery path not yet included
        if (!path.endsWith(OIDC_DISCOVERY_PATH)) {
            path = path + OIDC_DISCOVERY_PATH;
        }

        // discover jwks_uri
        final int iamPort = port;
        final String iamDiscoveryPath = path;
        LOGGER.debug("Fetching jwks_uri from URL '{}://{}:{}{}'", iamProtocol, iamHost, iamPort, iamDiscoveryPath);
        WebClient.create(vertx).get(iamPort, iamHost, iamDiscoveryPath).as(BodyCodec.jsonObject()).send()
            .onSuccess(fetchJWKsFromJWKsURL(vertx, iamHost, iamPort, handler))
            .onFailure(err -> {
                LOGGER.info("Failed to complete discovery from URL '{}://{}:{}{}'",
                    iamProtocol, iamHost, iamPort, iamDiscoveryPath);
                handler.handle(Future.failedFuture(err));
            });
    }

    private static Handler<HttpResponse<JsonObject>> fetchJWKsFromJWKsURL(
        Vertx vertx, String iamHost,
        int iamPort, Handler<AsyncResult<List<JsonObject>>> handler
    ) {
        return discoveryResp -> {
            final String rawJWKsURI = discoveryResp.body().getString(JWKS_URI_KEY);
            if (rawJWKsURI == null || rawJWKsURI.length() == 0) {
                final String errMsg = "No JWK URI found";
                LOGGER.warn(errMsg);
                handler.handle(Future.failedFuture(errMsg));
                return;
            }

            final URL parsedJWKsURL;
            try {
                parsedJWKsURL = new URL(rawJWKsURI);
            } catch (MalformedURLException e) {
                LOGGER.warn("Malformed JWKs URL '{}'", rawJWKsURI);
                handler.handle(Future.failedFuture(e));
                return;
            }

            final String iamJWKsPath = parsedJWKsURL.getPath();
            if (iamJWKsPath == null) {
                final String errMsg = "Failed to discover JWKs URI";
                LOGGER.warn(errMsg);
                handler.handle(Future.failedFuture(errMsg));
                return;
            }

            LOGGER.debug("Fetching JWKS from URL '{}'", rawJWKsURI);
            WebClient.create(vertx).get(iamPort, iamHost, iamJWKsPath).as(BodyCodec.jsonObject()).send()
                .onSuccess(parseJWKs(handler))
                .onFailure(err -> {
                    LOGGER.info("Failed to complete load JWK from URL '{}'", rawJWKsURI);
                    handler.handle(Future.failedFuture(err));
                });
        };
    }

    private static Handler<HttpResponse<JsonObject>> parseJWKs(
        Handler<AsyncResult<List<JsonObject>>> handler
    ) {
        return JWKsResp -> {
            LOGGER.debug("Received JWKS");
            final JsonArray keys = JWKsResp.body().getJsonArray(JWK_KEYS_KEY);
            if (keys == null || keys.size() == 0) {
                final String errMsg = "No JWK found";
                LOGGER.warn(errMsg);
                handler.handle(Future.failedFuture(errMsg));
                return;
            }

            final List<JsonObject> jwks = new ArrayList<JsonObject>();
            for (int i = 0; i < keys.size(); i++) {
                final JsonObject json = keys.getJsonObject(i);
                LOGGER.debug("Fetched JWK with kid '{}'", json.getString(JWK_KID));
                jwks.add(json);
            }

            LOGGER.debug("Successfully fetched {} JWKS from URL", keys.size());
            handler.handle(Future.succeededFuture(jwks));
        };
    }

    private static String publickeyToPEM(String publicKey) {
        return String.join(
            "\n",
            "-----BEGIN PUBLIC KEY-----",
            publicKey,
            "-----END PUBLIC KEY-----");
    }

}
