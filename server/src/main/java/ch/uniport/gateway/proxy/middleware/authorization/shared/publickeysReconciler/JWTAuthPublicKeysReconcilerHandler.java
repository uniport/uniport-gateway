package ch.uniport.gateway.proxy.middleware.authorization.shared.publickeysReconciler;

import ch.uniport.gateway.proxy.middleware.authorization.JWKAccessibleAuthHandler;
import ch.uniport.gateway.proxy.middleware.authorization.PublicKeyOptions;
import ch.uniport.gateway.proxy.middleware.authorization.shared.customClaimsChecker.JWTAuthAdditionalClaimsOptions;
import ch.uniport.gateway.proxy.middleware.authorization.shared.customIssuerChecker.JWTAuthMultipleIssuersOptions;
import io.vertx.core.Future;
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
 * Given a list of public key source, {@code JWTAuthPublicKeysReconcilerHandler}
 * created by {@link create}
 * can fetch the public key.
 * A public key source can be a literal public key OR a URL pointing to a
 * Keycloak realm (or more general an OIDC Provider).
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
                        .setBuffer(publicKeyToPEM(publicKey)));
            }
        });

        return Future.join(futures)
            .map(cf -> {
                LOGGER.info("Successfully fetched JWKs");
                return authOpts;
            });
    }

    /**
     * Fetches the JWKs via the OIDC discovery page as specified by OpenID Connect
     * 1.0
     * See
     * https://openid.net/specs/openid-connect-discovery-1_0-final.html#ProviderConfig
     * 
     * As there are multiple requests issued, the scheme/host/port of the provided
     * URI are parsed.
     * Additionally, the .well-known openid configuration path is appended, if not
     * yet present.
     * 
     * @param vertx
     * @param rawRealmBaseURL
     * @return
     */
    private static Future<List<JsonObject>> fetchJWKsFromDiscoveryURL(Vertx vertx, String rawRealmBaseURL) {
        final URL parsedRealmBaseURL = parseURL(rawRealmBaseURL);
        if (parsedRealmBaseURL == null) {
            final String errMsg = String.format("Malformed discovery URL '%s'", rawRealmBaseURL);
            LOGGER.warn(errMsg);
            return Future.failedFuture(errMsg);
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
        return WebClient.create(vertx)
            .get(iamPort, iamHost, iamDiscoveryPath)
            .as(BodyCodec.jsonObject())
            .send()
            .compose(discoveryResp -> fetchJWKsFromJWKsURL(vertx, iamHost, iamPort, discoveryResp))
            .onFailure(err -> LOGGER.info("Failed to complete discovery from URL '{}://{}:{}{}: {}'", iamProtocol,
                iamHost, iamPort, iamDiscoveryPath, err));
    }

    /**
     * The discovery response is JSON and is specified by the OpenID Connect 1.0
     * See
     * https://openid.net/specs/openid-connect-discovery-1_0-final.html#ProviderMetadata
     * 
     * @param vertx
     * @param iamHost
     * @param iamPort
     * @param discoveryResp
     *            discovery response as JSON
     * @return
     */
    private static Future<List<JsonObject>> fetchJWKsFromJWKsURL(
        Vertx vertx, String iamHost, int iamPort,
        HttpResponse<JsonObject> discoveryResp
    ) {
        final String rawJWKsURI = discoveryResp.body().getString(JWKS_URI_KEY);
        if (rawJWKsURI == null || rawJWKsURI.length() == 0) {
            final String errMsg = "No JWK URI found";
            LOGGER.warn(errMsg);
            return Future.failedFuture(errMsg);
        }

        final URL parsedJWKsURL = parseURL(rawJWKsURI);
        if (parsedJWKsURL == null) {
            final String errMsg = String.format("Malformed JWKs URL '%s'", rawJWKsURI);
            LOGGER.warn(errMsg);
            return Future.failedFuture(errMsg);
        }

        final String iamJWKsPath = parsedJWKsURL.getPath();
        if (iamJWKsPath == null) {
            final String errMsg = "Failed to discover JWKs URI";
            LOGGER.warn(errMsg);
            return Future.failedFuture(errMsg);
        }

        LOGGER.debug("Fetching JWKS from URL '{}'", rawJWKsURI);
        return WebClient.create(vertx)
            .get(iamPort, iamHost, iamJWKsPath)
            .as(BodyCodec.jsonObject())
            .send()
            .compose(jwksResponse -> parseJWKs(jwksResponse))
            .onFailure(err -> LOGGER.info("Failed to complete load JWK from URL '{}'", rawJWKsURI));
    }

    /**
     * The JWK response is JSON and is specified by RFC7517
     * See https://datatracker.ietf.org/doc/html/rfc7517#section-5.1
     * 
     * @param jwksResponse
     *            the JWK response as JSON
     * @return
     */
    private static Future<List<JsonObject>> parseJWKs(HttpResponse<JsonObject> jwksResponse) {
        LOGGER.debug("Received JWKS");
        final JsonArray keys = jwksResponse.body().getJsonArray(JWK_KEYS_KEY);
        if (keys == null || keys.size() == 0) {
            final String errMsg = "No JWK found";
            LOGGER.warn(errMsg);
            return Future.failedFuture(errMsg);
        }

        final List<JsonObject> jwks = new ArrayList<JsonObject>();
        for (int i = 0; i < keys.size(); i++) {
            final JsonObject json = keys.getJsonObject(i);
            LOGGER.debug("Fetched JWK with kid '{}'", json.getString(JWK_KID));
            jwks.add(json);
        }

        LOGGER.debug("Successfully fetched {} JWKS from URL", keys.size());
        return Future.succeededFuture(jwks);
    }

    private static String publicKeyToPEM(String publicKey) {
        return String.join(
            "\n",
            "-----BEGIN PUBLIC KEY-----",
            publicKey,
            "-----END PUBLIC KEY-----");
    }

    private static boolean isURL(String publicKey) {
        return parseURL(publicKey) != null;
    }

    private static URL parseURL(String publicKey) {
        try {
            return new URI(publicKey).toURL();
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            LOGGER.debug("URI is malformed, hence it is has to be a raw public key: '{}'", e.getMessage());
            return null;
        }
    }

}
