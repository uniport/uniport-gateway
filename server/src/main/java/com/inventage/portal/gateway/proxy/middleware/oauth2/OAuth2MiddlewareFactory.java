package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.middleware.Middleware;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareFactory;
import com.inventage.portal.gateway.proxy.middleware.oauth2.relyingParty.RelyingPartyHandler;
import com.inventage.portal.gateway.proxy.router.RouterFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.impl.OAuth2AuthProviderImpl;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configures keycloak as the OAuth2 provider. It patches the authorization path to ensure all
 * follow-up requests are routed through this application as well, if configured.
 */
public class OAuth2MiddlewareFactory implements MiddlewareFactory {

    public static final String OIDC_RESPONSE_MODE = "response_mode";
    public static final String OIDC_RESPONSE_MODE_FORM_POST = "form_post";
    public static final String OIDC_RESPONSE_MODE_DEFAULT = OIDC_RESPONSE_MODE_FORM_POST;

    private static final String OAUTH2_CALLBACK_PREFIX = "/callback/";
    private static final int OAUTH2_PKCE_VERIFIER_LENGTH = 64;
    private static final String OIDC_SCOPE = "openid";
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2MiddlewareFactory.class);

    @Override
    public String provides() {
        return DynamicConfiguration.MIDDLEWARE_OAUTH2;
    }

    @Override
    public Future<Middleware> create(Vertx vertx, String name, Router router, JsonObject middlewareConfig) {
        final boolean proxyAuthenticationFlow = middlewareConfig.getBoolean(DynamicConfiguration.MIDDLEWARE_OAUTH2_PROXY_AUTHENTICATION_FLOW, true);
        final String sessionScope = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_SESSION_SCOPE);
        final String responseMode = middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_RESPONSE_MODE, OIDC_RESPONSE_MODE_DEFAULT);

        final String callbackPath = OAUTH2_CALLBACK_PREFIX + sessionScope.toLowerCase();
        final List<Route> callbacks = mountCallbackRoutes(router, callbackPath, responseMode);

        return KeycloakAuth.discover(vertx, oAuth2Options(middlewareConfig))
            .onFailure(err -> LOGGER.error("Failed to create OAuth2 Middleware due to failing Keycloak discovery '{}'", err.getMessage()))
            .compose(createMiddleware(
                vertx,
                name,
                getPublicURL(middlewareConfig),
                proxyAuthenticationFlow,
                callbacks,
                callbackPath,
                sessionScope,
                getAdditionalScopes(middlewareConfig),
                getAdditionalAuthRequestParams(middlewareConfig, responseMode),
                getPassthroughParameters(middlewareConfig)))
            .onSuccess(m -> LOGGER.debug("Created middleware '{}' successfully", DynamicConfiguration.MIDDLEWARE_OAUTH2))
            .onFailure(err -> LOGGER.warn("Failed to create OAuth2 Middleware '{}'", err.getMessage()));
    }

    protected String patchPath(String publicUrl, URI keycloakUrl) {
        return String.format("%s%s", publicUrl, keycloakUrl.getPath());
    }

    /**
     * Additional request parameters for the OAuth 2.0 Authentication Request to the Authorization Server.
     *
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest">AuthRequest</a>
     */
    private JsonObject getAdditionalAuthRequestParams(JsonObject middlewareConfig, String responseMode) {
        JsonObject additionalParameters = middlewareConfig.getJsonObject(DynamicConfiguration.MIDDLEWARE_OAUTH2_ADDITIONAL_PARAMETERS);
        if (additionalParameters == null) {
            additionalParameters = JsonObject.of();
        }

        return new JsonObject(additionalParameters.getMap())
            .put(OIDC_RESPONSE_MODE, responseMode);
    }

    @SuppressWarnings("unchecked")
    private List<String> getAdditionalScopes(JsonObject middlewareConfig) {
        final JsonArray additionalScopes = middlewareConfig.getJsonArray(DynamicConfiguration.MIDDLEWARE_OAUTH2_ADDITIONAL_SCOPES);
        if (additionalScopes == null) {
            return List.of();
        }
        return additionalScopes.getList();
    }

    @SuppressWarnings("unchecked")
    private List<String> getPassthroughParameters(JsonObject middlewareConfig) {
        final JsonArray passthroughParameters = middlewareConfig.getJsonArray(DynamicConfiguration.MIDDLEWARE_OAUTH2_PASSTHROUGH_PARAMETERS);
        if (passthroughParameters == null) {
            return List.of();
        }
        return passthroughParameters.getList();
    }

    /**
     *
     * the protocol, hostname or port can be different from what the portal-gateway knows, therefore
     * in RouterFactory.createMiddleware the publicUrl configuration is added to this middleware
     * configuration
     */
    private String getPublicURL(JsonObject middlewareConfig) {
        if (middlewareConfig.containsKey(DynamicConfiguration.MIDDLEWARE_OAUTH2_PUBLIC_URL)) {
            return middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_PUBLIC_URL);
        }

        String publicUrl = String.format("%s://%s",
            getValueByKeyOrFail(middlewareConfig, RouterFactory.PUBLIC_PROTOCOL_KEY),
            getValueByKeyOrFail(middlewareConfig, RouterFactory.PUBLIC_HOSTNAME_KEY));

        // only include port if it is not already fixed by the protocol
        final String publicPort = getValueByKeyOrFail(middlewareConfig, RouterFactory.PUBLIC_PORT_KEY);
        if (!publicPort.equals("80") && !publicPort.equals("443")) {
            publicUrl = String.format("%s:%s", publicUrl, publicPort);
        }
        return publicUrl;
    }

    private String getValueByKeyOrFail(JsonObject config, String key) {
        final String value = config.getString(key);
        if (value == null) {
            throw new IllegalArgumentException("missing key in config: '" + key + "'");
        }
        return value;
    }

    private List<Route> mountCallbackRoutes(Router router, String callbackPath, String responseMode) {
        final List<Route> callbacks = new LinkedList<>();

        // PORTAL-2004: We always need a GET callback. In the case of FormPost, we need the GET callback for requests with an Accept header that does not allow text/html
        callbacks.add(router
            .get(callbackPath)
            .setName("callback GET")
            .handler(BodyHandler.create()));

        if (isFormPost(responseMode)) {
            // PORTAL-513: Forces the OIDC Provider to send the authorization code in the body
            callbacks.add(router
                .post(callbackPath)
                .setName("callback POST")
                .handler(BodyHandler.create()));
        }

        return callbacks;
    }

    private boolean isFormPost(String responseMode) {
        return OIDC_RESPONSE_MODE_FORM_POST.equals(responseMode);
    }

    private OAuth2Options oAuth2Options(JsonObject middlewareConfig) {
        return new OAuth2Options()
            .setClientId(middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_CLIENTID))
            .setClientSecret(middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_CLIENTSECRET))
            .setSite(middlewareConfig.getString(DynamicConfiguration.MIDDLEWARE_OAUTH2_DISCOVERYURL))
            .setValidateIssuer(false);
    }

    private Function<OAuth2Auth, Future<Middleware>> createMiddleware(
        Vertx vertx,
        String name,
        String publicUrl,
        boolean proxyAuthenticationFlow,
        List<Route> callbacks,
        String callbackPath,
        String sessionScope,
        List<String> additionalScopes,
        JsonObject additionalAuthRequestParams,
        List<String> passthroughParameters
    ) {
        return authProvider -> {
            LOGGER.debug("Successfully completed Keycloak discovery");

            if (proxyAuthenticationFlow) {
                try {
                    patchPublicKeycloakURIs(authProvider, publicUrl);
                } catch (URISyntaxException err) {
                    LOGGER.warn("Failed to create OAuth2 Middleware due to failed authorization path patching: '{}'",
                        err.getMessage());
                    return Future.failedFuture("Failed to patch authorization path: '" + err.getMessage() + "'");
                }
            }

            for (Route callback : callbacks) {
                OAuth2AuthMiddleware.registerCallbackHandlers(vertx, callback, sessionScope, authProvider);
            }
            final String callbackURL = String.format("%s%s", publicUrl, callbackPath);

            // PORTAL-1184 we are using a patched OAuth2AuthHandlerImpl class as OAuth2AuthHandler implementation.
            final OAuth2AuthHandler authHandler = new RelyingPartyHandler(vertx, authProvider, callbackURL)
                .setupCallbacks(callbacks)
                .passthroughParameters(passthroughParameters)
                .extraParams(additionalAuthRequestParams)
                .withScopes(scopes(sessionScope, additionalScopes))
                .pkceVerifierLength(OAUTH2_PKCE_VERIFIER_LENGTH);

            return Future.succeededFuture(new OAuth2AuthMiddleware(vertx, name, authHandler, sessionScope));
        };
    }

    /**
     * By patching the issuer and the authorization path, ensure that Keycloak is only visible from the outside as running behind the portal-gateway.
     */
    private void patchPublicKeycloakURIs(OAuth2Auth authProvider, String publicUrl) throws URISyntaxException {
        final OAuth2Options keycloakOAuth2Options = ((OAuth2AuthProviderImpl) authProvider).getConfig();

        // patch issuer
        final URI issuerPath = new URI(keycloakOAuth2Options.getJWTOptions().getIssuer());
        final String newIssuerPath = patchPath(publicUrl, issuerPath);
        keycloakOAuth2Options.getJWTOptions().setIssuer(newIssuerPath);
        LOGGER.debug("patched issuer: {} -> {}", issuerPath, newIssuerPath);

        // patch authorization_endpoint
        final URI authorizationPath = new URI(keycloakOAuth2Options.getAuthorizationPath());
        final String newAuthorizationPath = patchPath(publicUrl, authorizationPath);
        keycloakOAuth2Options.setAuthorizationPath(newAuthorizationPath);
        LOGGER.debug("patched authorization endpoint: {} -> {}", authorizationPath, newAuthorizationPath);
    }

    private List<String> scopes(String sessionScope, List<String> additionalScopes) {
        final List<String> scopes = new LinkedList<>();
        scopes.add(OIDC_SCOPE);
        // add the sessionScope as a OIDC scope for "aud" in JWT
        // see https://www.keycloak.org/docs/latest/server_admin/index.html#_audience
        scopes.add(sessionScope);
        scopes.addAll(additionalScopes);
        return scopes;
    }
}
