package com.inventage.portal.gateway.proxy.middleware;

import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddleware.OIDC_PARAM_STATE;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddlewareTest.CODE_CHALLENGE;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddlewareTest.CODE_CHALLENGE_METHOD;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddlewareTest.PKCE_METHOD_PLAIN;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddlewareTest.PKCE_METHOD_S256;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddlewareTest.SCOPE;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory.DEFAULT_OIDC_RESPONSE_MODE;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2MiddlewareFactory.OIDC_RESPONSE_MODE;
import static com.inventage.portal.gateway.proxy.middleware.replacedSessionCookieDetection.ReplacedSessionCookieDetectionMiddlewareFactory.DEFAULT_DETECTION_COOKIE_NAME;
import static com.inventage.portal.gateway.proxy.middleware.session.SessionMiddlewareFactory.DEFAULT_SESSION_COOKIE_NAME;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;

import com.inventage.portal.gateway.proxy.middleware.oauth2.relyingParty.StateWithUri;
import com.inventage.portal.gateway.proxy.middleware.sessionBag.CookieUtil;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.junit5.VertxTestContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;
import org.assertj.core.api.AbstractAssert;

/**
 * Custom assertion class for authentication redirect request of a relying party (RP).
 * <p>
 * see also https://assertj.github.io/doc/#assertj-core-custom-assertions-creation
 */
public class AuthenticationRedirectRequestAssert
    extends AbstractAssert<AuthenticationRedirectRequestAssert, HttpClientResponse> {

    final VertxTestContext testCtx;

    public AuthenticationRedirectRequestAssert(VertxTestContext testCtx, HttpClientResponse actual) {
        super(actual, AuthenticationRedirectRequestAssert.class);
        this.testCtx = testCtx;
    }

    public static AuthenticationRedirectRequestAssert assertThat(VertxTestContext testCtx, HttpClientResponse actual) {
        return new AuthenticationRedirectRequestAssert(testCtx, actual);
    }

    public AuthenticationRedirectRequestAssert isRedirectTo(String expectedLocation) {
        VertxAssertions.assertTrue(
            testCtx, 300 <= actual.statusCode() && actual.statusCode() <= 399,
            "Redirect status code expected, but was: " + actual.statusCode());

        final String location = actual.getHeader(LOCATION);
        VertxAssertions.assertEquals(testCtx, expectedLocation, location);
        return this;
    }

    public AuthenticationRedirectRequestAssert isRedirectToWithoutParameters(String expectedLocation) {
        VertxAssertions.assertTrue(
            testCtx, 300 <= actual.statusCode() && actual.statusCode() <= 399,
            "Redirect status code expected, but was: " + actual.statusCode());

        final String location = actual.getHeader(LOCATION);
        final String locationWithoutParameters = location.split("\\?")[0];
        VertxAssertions.assertEquals(testCtx, expectedLocation, locationWithoutParameters);
        return this;
    }

    public AuthenticationRedirectRequestAssert isValidAuthenticationRequest() {
        return isValidAuthenticationRequest(null);
    }

    public AuthenticationRedirectRequestAssert isValidAuthenticationRequest(
        Map<String, String> expectedLocationParameters
    ) {
        VertxAssertions.assertTrue(
            testCtx, 300 <= actual.statusCode() && actual.statusCode() <= 399,
            "Redirect status code expected, but was: " + actual.statusCode());

        final String location = actual.getHeader(LOCATION);
        VertxAssertions.assertNotNull(testCtx, location, "No 'location' header found in response");

        final Map<String, String> locationParameters = extractParametersFromHeader(location);
        VertxAssertions.assertNotNull(testCtx, locationParameters);

        if (expectedLocationParameters != null) {
            for (Entry<String, String> expectedLocationParameter : expectedLocationParameters.entrySet()) {
                VertxAssertions.assertNotNull(testCtx,
                    locationParameters.get(expectedLocationParameter.getKey()),
                    locationParameters.toString());
                VertxAssertions.assertEquals(testCtx,
                    expectedLocationParameter.getValue(),
                    locationParameters.get(expectedLocationParameter.getKey()),
                    "No 'location' header found in response");
            }
        }
        return this;
    }

    public AuthenticationRedirectRequestAssert isUsingFormPost() {
        final Map<String, String> locationParameters = extractParametersFromHeader(actual.getHeader(LOCATION));
        VertxAssertions.assertEquals(testCtx, DEFAULT_OIDC_RESPONSE_MODE, locationParameters.get(OIDC_RESPONSE_MODE));
        return this;
    }

    public AuthenticationRedirectRequestAssert hasPKCE() {
        final Map<String, String> locationParameters = extractParametersFromHeader(actual.getHeader(LOCATION));

        VertxAssertions.assertNotNull(testCtx, locationParameters.get(CODE_CHALLENGE));
        VertxAssertions.assertNotNull(testCtx, locationParameters.get(CODE_CHALLENGE_METHOD));
        VertxAssertions.assertEquals(testCtx, PKCE_METHOD_S256, locationParameters.get(CODE_CHALLENGE_METHOD));
        VertxAssertions.assertNotEquals(testCtx, PKCE_METHOD_PLAIN, locationParameters.get(CODE_CHALLENGE_METHOD));

        return this;
    }

    public AuthenticationRedirectRequestAssert hasScopes(List<String> expectedScopes) {
        final Map<String, String> locationParameters = extractParametersFromHeader(actual.getHeader(LOCATION));
        final List<String> actualScopes = Stream.of(locationParameters.get(SCOPE).split(" ")).toList();
        VertxAssertions.assertTrue(testCtx, actualScopes.containsAll(expectedScopes),
            String.format("Expected session scopes '%s', but was '%s'", expectedScopes, actualScopes));
        return this;
    }

    private Map<String, String> extractParametersFromHeader(String header) {
        if (header == null) {
            testCtx.failNow("header is null");
            return null;
        }

        List<NameValuePair> responseParamsList = null;
        try {
            responseParamsList = URLEncodedUtils.parse(new URI(header), StandardCharsets.UTF_8);
        } catch (URISyntaxException e) {
            testCtx.failNow(e);
            return null;
        }
        VertxAssertions.assertNotNull(testCtx, responseParamsList);
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
            VertxAssertions.assertNotEquals(testCtx, sessionCookie, cookie);
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
        VertxAssertions.assertNotNull(testCtx, cookieValues);
        if (withValue != null) {
            VertxAssertions.assertTrue(testCtx, cookieValues.stream().anyMatch(value -> value.equals(withValue)));
        }
        return this;
    }

    public AuthenticationRedirectRequestAssert hasNotSetCookie(String cookieName) {
        return assertSetCookie(cookieName, false);
    }

    private AuthenticationRedirectRequestAssert assertSetCookie(String cookieName, boolean present) {
        VertxAssertions.assertEquals(testCtx, filterSetCookies(actual.cookies(), cookieName).isEmpty(), !present);
        return this;
    }

    private List<String> valueFromSetCookie(List<String> cookieHeaders, String cookieName) {
        VertxAssertions.assertNotNull(testCtx, cookieHeaders);
        return filterSetCookies(cookieHeaders, cookieName).stream()
            .map(cookie -> cookie.getValue())
            .toList();
    }

    private List<Cookie> filterSetCookies(List<String> cookieHeaders, String cookieName) {
        VertxAssertions.assertNotNull(testCtx, cookieHeaders);
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
            .map(cookie -> CookieUtil.fromNettyCookie(cookie))
            .collect(Collectors.toSet());
    }

    public AuthenticationRedirectRequestAssert hasStatusCode(int expectedStatusCode) {
        VertxAssertions.assertEquals(testCtx, expectedStatusCode, actual.statusCode());
        return this;
    }

    public AuthenticationRedirectRequestAssert hasHeader(String expectedHeader) {
        final String value = actual.getHeader(expectedHeader);
        VertxAssertions.assertNotNull(testCtx, value);
        return this;
    }

    public AuthenticationRedirectRequestAssert hasHeader(String expectedHeader, String expectedValue) {
        final String value = actual.getHeader(expectedHeader);
        VertxAssertions.assertEquals(testCtx, expectedValue, value);
        return this;
    }

    public AuthenticationRedirectRequestAssert hasHeader(String expectedHeader, Set<String> expectedValues) {
        final String value = actual.getHeader(expectedHeader);
        VertxAssertions.assertNotNull(testCtx, value);
        final Set<String> values = Set.of(value.split(","));
        VertxAssertions.assertEquals(testCtx, expectedValues, values);
        return this;
    }

    public AuthenticationRedirectRequestAssert hasStateWithUri(String expectedUriInStateParameter) {
        final Map<String, String> locationParameters = extractParametersFromHeader(actual.getHeader(LOCATION));
        VertxAssertions.assertEquals(testCtx, expectedUriInStateParameter,
            new StateWithUri(locationParameters.get(OIDC_PARAM_STATE)).uri().orElse(null));
        return this;
    }
}
