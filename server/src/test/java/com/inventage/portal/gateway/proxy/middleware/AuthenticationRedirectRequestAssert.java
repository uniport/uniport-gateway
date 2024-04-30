package com.inventage.portal.gateway.proxy.middleware;

import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddleware.OIDC_PARAM_STATE;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddlewareTest.CODE_CHALLENGE;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddlewareTest.CODE_CHALLENGE_METHOD;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddlewareTest.PKCE_METHOD_PLAIN;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddlewareTest.PKCE_METHOD_S256;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory.OIDC_RESPONSE_MODE;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory.OIDC_RESPONSE_MODE_DEFAULT;
import static com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.ReplacedSessionCookieDetectionMiddlewareFactory.DEFAULT_DETECTION_COOKIE_NAME;
import static com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;

import com.inventage.portal.gateway.proxy.middleware.oauth2.relyingParty.StateWithUri;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpClientResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;
import org.assertj.core.api.AbstractAssert;
import org.junit.jupiter.api.Assertions;

/**
 * Custom assertion class for authentication redirect request of a relying party (RP).
 * <p>
 * see also https://assertj.github.io/doc/#assertj-core-custom-assertions-creation
 */
public class AuthenticationRedirectRequestAssert
    extends AbstractAssert<AuthenticationRedirectRequestAssert, HttpClientResponse> {

    public AuthenticationRedirectRequestAssert(HttpClientResponse actual) {
        super(actual, AuthenticationRedirectRequestAssert.class);
    }

    public static AuthenticationRedirectRequestAssert assertThat(HttpClientResponse actual) {
        return new AuthenticationRedirectRequestAssert(actual);
    }

    public AuthenticationRedirectRequestAssert isRedirectTo(String expectedLocation) {
        Assertions.assertTrue(String.valueOf(actual.statusCode()).startsWith("3"),
            "Redirect status code expected, but was: " + actual.statusCode());
        final String location = actual.getHeader(LOCATION);
        Assertions.assertEquals(expectedLocation, location);
        return this;
    }

    public AuthenticationRedirectRequestAssert isValidAuthenticationRequest() {
        return isValidAuthenticationRequest(null);
    }

    public AuthenticationRedirectRequestAssert isValidAuthenticationRequest(
        Map<String, String> expectedLocationParameters
    ) {
        // final String setCookie = actual.getHeader(HttpHeaderNames.SET_COOKIE);
        // Assertions.assertNull(setCookie);
        final String location = actual.getHeader(LOCATION);
        Assertions.assertNotNull(location, "No 'location' header found in response");
        final Map<String, String> locationParameters = extractParametersFromHeader(location);
        Assertions.assertNotNull(locationParameters);
        if (expectedLocationParameters != null) {
            expectedLocationParameters.entrySet().stream()
                .filter(entry -> locationParameters.containsKey(entry.getKey())).findAny()
                .orElseThrow(() -> new IllegalStateException(
                    "expecting: " + expectedLocationParameters + ", found: " + locationParameters));
        }
        return this;
    }

    public AuthenticationRedirectRequestAssert isUsingFormPost() {
        final Map<String, String> locationParameters = extractParametersFromHeader(actual.getHeader(LOCATION));
        Assertions.assertEquals(locationParameters.get(OIDC_RESPONSE_MODE), OIDC_RESPONSE_MODE_DEFAULT);
        return this;
    }

    public AuthenticationRedirectRequestAssert hasPKCE() {
        final Map<String, String> locationParameters = extractParametersFromHeader(actual.getHeader(LOCATION));

        Assertions.assertNotNull(locationParameters.get(CODE_CHALLENGE));
        Assertions.assertNotNull(locationParameters.get(CODE_CHALLENGE_METHOD));
        Assertions.assertEquals(PKCE_METHOD_S256, locationParameters.get(CODE_CHALLENGE_METHOD));
        Assertions.assertNotEquals(PKCE_METHOD_PLAIN, locationParameters.get(CODE_CHALLENGE_METHOD));

        return this;
    }

    private Map<String, String> extractParametersFromHeader(String header) {
        List<NameValuePair> responseParamsList = null;
        try {
            responseParamsList = URLEncodedUtils.parse(new URI(header), StandardCharsets.UTF_8);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertNotNull(responseParamsList);
        final Map<String, String> responseParamsMap = responseParamsList.stream().collect(Collectors.toMap(
            entry -> entry.getName(), entry -> entry.getValue()));

        return responseParamsMap;
    }

    public AuthenticationRedirectRequestAssert hasSetSessionCookie() {
        return hasSetSessionCookie(null);
    }

    public AuthenticationRedirectRequestAssert hasSetSessionCookie(String withValue) {
        return hasSetCookie(DEFAULT_SESSION_COOKIE_NAME, withValue);
    }

    public AuthenticationRedirectRequestAssert hasSetSessionCookieDifferentThan(String sessionCookie) {
        for (String cookie : actual.cookies()) {
            Assertions.assertNotEquals(sessionCookie, cookie);
        }
        return this;
    }

    public AuthenticationRedirectRequestAssert hasNotSetSessionCookie() {
        return hasNotSetCookie(DEFAULT_SESSION_COOKIE_NAME);
    }

    public AuthenticationRedirectRequestAssert hasSetDetectionCookie() {
        return hasSetDetectionCookie(null);
    }

    public AuthenticationRedirectRequestAssert hasSetDetectionCookie(String withValue) {
        return hasSetCookie(DEFAULT_DETECTION_COOKIE_NAME, withValue);
    }

    public AuthenticationRedirectRequestAssert hasNotSetDetectionCookie() {
        return hasNotSetCookie(DEFAULT_DETECTION_COOKIE_NAME);
    }

    public AuthenticationRedirectRequestAssert hasSetCookie(String cookieName) {
        return assertSetCookie(cookieName, true);
    }

    public AuthenticationRedirectRequestAssert hasSetCookie(String cookieName, String withValue) {
        final List<String> cookieValues = valueFromSetCookie(actual.cookies(), cookieName);
        Assertions.assertNotNull(cookieValues);
        if (withValue != null) {
            Assertions.assertTrue(
                cookieValues.stream()
                    .anyMatch(value -> value.equals(withValue)));
        }
        return this;
    }

    public AuthenticationRedirectRequestAssert hasNotSetCookie(String cookieName) {
        return assertSetCookie(cookieName, false);
    }

    private AuthenticationRedirectRequestAssert assertSetCookie(String cookieName, boolean present) {
        Assertions.assertEquals(filterSetCookies(actual.cookies(), cookieName).isEmpty(), !present);
        return this;
    }

    private List<String> valueFromSetCookie(List<String> cookieHeaders, String cookieName) {
        Assertions.assertNotNull(cookieHeaders);
        return filterSetCookies(cookieHeaders, cookieName).stream()
            .map(cookie -> cookie.getValue())
            .toList();
    }

    private List<Cookie> filterSetCookies(List<String> cookieHeaders, String cookieName) {
        Assertions.assertNotNull(cookieHeaders);
        return parseCookies(cookieHeaders).stream()
            .filter(cookie -> cookie.getName().equals(cookieName))
            .toList();
    }

    private Set<Cookie> parseCookies(List<String> cookieHeaders) {
        if (cookieHeaders == null) {
            return Collections.emptySet();
        }
        // LAX, otherwise cookies like "app-platform=iOS App Store" are not returned
        return cookieHeaders.stream()
            .flatMap(cookieEntry -> ServerCookieDecoder.LAX.decode(cookieEntry).stream())
            .map(cookie -> fromNettyCookie(cookie))
            .collect(Collectors.toSet());
    }

    private Cookie fromNettyCookie(io.netty.handler.codec.http.cookie.Cookie nettyCookie) {
        return Cookie.cookie(nettyCookie.name(), nettyCookie.value())
            .setDomain(nettyCookie.domain())
            .setHttpOnly(nettyCookie.isHttpOnly())
            .setMaxAge(nettyCookie.maxAge())
            .setPath(nettyCookie.path())
            .setSecure(nettyCookie.isSecure());
    }

    public AuthenticationRedirectRequestAssert hasStatusCode(int expectedStatusCode) {
        Assertions.assertEquals(expectedStatusCode, actual.statusCode());
        return this;
    }

    public AuthenticationRedirectRequestAssert hasHeader(String expectedHeader) {
        final String value = actual.getHeader(expectedHeader);
        Assertions.assertNotNull(value);
        return this;
    }

    public AuthenticationRedirectRequestAssert hasHeader(String expectedHeader, String expectedValue) {
        final String value = actual.getHeader(expectedHeader);
        Assertions.assertEquals(expectedValue, value);
        return this;
    }

    public AuthenticationRedirectRequestAssert hasStateWithUri(String expectedUriInStateParameter) {
        final Map<String, String> locationParameters = extractParametersFromHeader(actual.getHeader(LOCATION));
        Assertions.assertEquals(expectedUriInStateParameter,
            new StateWithUri(locationParameters.get(OIDC_PARAM_STATE)).uri().orElse(null));
        return this;
    }

}
