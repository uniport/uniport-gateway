package com.inventage.portal.gateway.proxy.middleware;

import io.netty.handler.codec.http.HttpHeaders;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.ext.web.handler.impl.StateWithUri;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;
import org.assertj.core.api.AbstractAssert;
import org.junit.jupiter.api.Assertions;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddlewareTest.CODE_CHALLENGE;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddlewareTest.CODE_CHALLENGE_METHOD;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddlewareTest.PKCE_METHOD_PLAIN;
import static com.inventage.portal.gateway.proxy.middleware.oauth2.OAuth2AuthMiddlewareTest.PKCE_METHOD_S256;
import static com.inventage.portal.gateway.proxy.middleware.session.SessionMiddleware.COOKIE_NAME_DEFAULT;

/**
 * Custom assertion class for authentication redirect request of a relying party (RP).
 *
 * see also https://assertj.github.io/doc/#assertj-core-custom-assertions-creation
 */
public class AuthenticationRedirectRequestAssert extends AbstractAssert<AuthenticationRedirectRequestAssert, HttpClientResponse> {

    public AuthenticationRedirectRequestAssert(HttpClientResponse actual) {
        super(actual, AuthenticationRedirectRequestAssert.class);
    }

    public static AuthenticationRedirectRequestAssert assertThat(HttpClientResponse actual) {
        return new AuthenticationRedirectRequestAssert(actual);
    }


    public AuthenticationRedirectRequestAssert isRedirectTo(String expectedLocation) {
        Assertions.assertTrue(String.valueOf(actual.statusCode()).startsWith("3"), "Redirect status code expected, but was: " +actual.statusCode());
        String location = actual.getHeader("location");
        Assertions.assertEquals(expectedLocation, location);
        return this;
    }

    public AuthenticationRedirectRequestAssert isValidAuthenticationRequest() {
        return isValidAuthenticationRequest(null);
    }
    public AuthenticationRedirectRequestAssert isValidAuthenticationRequest(Map<String, String> expectedLocationParameters) {
        String set_cookie = actual.getHeader("set-cookie");
        String location = actual.getHeader("location");
        Assertions.assertNotNull(location, "No 'location' header found in response");
        Map<String, String> locationParameters = extractParametersFromHeader(location);
        Assertions.assertNotNull(locationParameters);
        if (expectedLocationParameters != null) {
            expectedLocationParameters.entrySet().stream().filter(entry -> locationParameters.containsKey(entry.getKey())).findAny().orElseThrow(() -> new IllegalStateException("expecting: " +expectedLocationParameters.toString()+ ", found: " +locationParameters));
        }
        return this;
    }

    public AuthenticationRedirectRequestAssert isUsingFormPost() {
        Map<String, String> locationParameters = extractParametersFromHeader(actual.getHeader("location"));
        Assertions.assertEquals(locationParameters.get("response_mode"), "form_post");
        return this;
    }

    public AuthenticationRedirectRequestAssert hasPKCE() {
        Map<String, String> locationParameters = extractParametersFromHeader(actual.getHeader("location"));

        Assertions.assertNotNull(locationParameters.get(CODE_CHALLENGE));
        Assertions.assertNotNull(locationParameters.get(CODE_CHALLENGE_METHOD));
        Assertions.assertEquals(PKCE_METHOD_S256, locationParameters.get(CODE_CHALLENGE_METHOD));
        Assertions.assertNotEquals(PKCE_METHOD_PLAIN, locationParameters.get(CODE_CHALLENGE_METHOD));

        return this;
    }


    private Map<String, String> extractParametersFromHeader(String header) {
        List<NameValuePair> responseParamsList = null;
        try {
            responseParamsList = URLEncodedUtils.parse(new URI(header), Charset.forName("UTF-8"));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertNotNull(responseParamsList);
        Map<String, String> responseParamsMap = responseParamsList.stream().collect(Collectors.toMap(
                entry -> entry.getName(), entry -> entry.getValue()
        ));

        return responseParamsMap;
    }

    public AuthenticationRedirectRequestAssert hasSetCookieForSession(String withValue) {
        String set_cookie = actual.getHeader(HttpHeaders.Names.SET_COOKIE);
        Assertions.assertNotNull(set_cookie);
        String sessionCookie = valueFromSetCookie(set_cookie);
        Assertions.assertNotNull(sessionCookie);
        if (withValue != null) {
            Assertions.assertEquals(withValue, sessionCookie);
        }
        return this;
    }

    public AuthenticationRedirectRequestAssert hasNotSetCookieForSession() {
        String set_cookie = actual.getHeader(HttpHeaders.Names.SET_COOKIE);
        Assertions.assertNull(set_cookie);
        return this;
    }

    private String valueFromSetCookie(String aSetCookieHeader) {
        return Arrays.stream(aSetCookieHeader.split(";")).filter(element -> element.startsWith(COOKIE_NAME_DEFAULT)).findFirst().orElse(null);
    }

    public AuthenticationRedirectRequestAssert hasStatusCode(int expectedStatusCode) {
        Assertions.assertEquals(expectedStatusCode, actual.statusCode());
        return this;
    }

    public AuthenticationRedirectRequestAssert hasHeader(String expectedHeader, String expectedValue) {
        String value = actual.getHeader(expectedHeader);
        Assertions.assertEquals(expectedValue, value);
        return this;
    }

    public AuthenticationRedirectRequestAssert hasStateWithUri(String expectedUriInStateParameter) {
        Map<String, String> locationParameters = extractParametersFromHeader(actual.getHeader("location"));
        Assertions.assertEquals(expectedUriInStateParameter, new StateWithUri(locationParameters.get("state")).uri().orElse(null));
        return this;
    }

    public AuthenticationRedirectRequestAssert hasSetCookieForSessionDifferentThan(String sessionCookie) {
        String set_cookie = actual.getHeader(HttpHeaders.Names.SET_COOKIE);
        Assertions.assertNotEquals(sessionCookie, set_cookie);
        return this;
    }
}
