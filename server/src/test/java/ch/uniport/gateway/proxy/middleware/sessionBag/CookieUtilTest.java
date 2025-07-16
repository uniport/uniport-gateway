package ch.uniport.gateway.proxy.middleware.sessionBag;

import io.netty.handler.codec.http.cookie.CookieHeaderNames.SameSite;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.impl.CookieJar;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CookieUtilTest {

    @Test
    public void testCookieMapEmptyString() {
        // given
        final String cookieHeader = "";
        // when
        final Map<String, Cookie> cookieMap = getCookieFromHeader(cookieHeader);
        // then
        Assertions.assertTrue(cookieMap.isEmpty());
    }

    @Test
    public void testCookieMap4Cookies() {
        // given
        final String cookieHeader = "KEYCLOAK_SESSION=portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b; KEYCLOAK_SESSION_LEGACY=portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b; uniport.session=73ba363ad1ab36ea17681b882687e70458f14b2b2ff89d9215e0f655f3660d80";
        // when
        final Map<String, Cookie> cookieMap = getCookieFromHeader(cookieHeader);
        // then
        Assertions.assertEquals("portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b", cookieMap.get("KEYCLOAK_SESSION").getValue());
        Assertions.assertEquals("portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b", cookieMap.get("KEYCLOAK_SESSION_LEGACY").getValue());
        Assertions.assertEquals("73ba363ad1ab36ea17681b882687e70458f14b2b2ff89d9215e0f655f3660d80", cookieMap.get("uniport.session").getValue());
    }

    private Map<String, Cookie> getCookieFromHeader(String header) {
        // according to RFC 6265, multiple COOKIE headers in the request are no allowed 
        // here we just assure our understanding of the cookie handling in vertx
        final CookieJar cookies = new CookieJar(header);
        return cookies.stream()
            .collect(Collectors.toMap(
                Cookie::getName,
                Function.identity(),
                (existing, replacement) -> existing));
    }

    static Stream<Arguments> cookieArgs() {
        return Stream.of(
            Arguments.of("aName", "aValue", "aDomain", "aPath", Long.valueOf(4242), Boolean.TRUE, Boolean.FALSE, "Lax"),
            Arguments.of("aName", "aValue", null, null, null, null, null, "Strict"),
            Arguments.of("aName", "aValue", null, null, null, null, null, "None"),
            Arguments.of("aName", "aValue", null, null, null, null, null, null)

        );
    }

    @ParameterizedTest
    @MethodSource("cookieArgs")
    public void testFromNettyCookie(String name, String value, String domain, String path, Long maxAge, Boolean httpOnly, Boolean secure, String sameSite) {
        // given
        final DefaultCookie nettyCookie = new DefaultCookie(name, value);
        if (domain != null) {
            nettyCookie.setDomain(domain);
        }
        if (path != null) {
            nettyCookie.setPath(path);
        }
        if (maxAge != null) {
            nettyCookie.setMaxAge(maxAge);
        }
        if (secure != null) {
            nettyCookie.setSecure(secure);
        }
        if (httpOnly != null) {
            nettyCookie.setHttpOnly(httpOnly);
        }
        if (sameSite != null) {
            nettyCookie.setSameSite(SameSite.valueOf(sameSite));
        }

        // when
        final Cookie cookie = CookieUtil.fromNettyCookie(nettyCookie);

        // then
        Assertions.assertEquals(name, cookie.getName());
        Assertions.assertEquals(value, cookie.getValue());
        if (domain != null) {
            Assertions.assertEquals(domain, cookie.getDomain());
        }
        if (path != null) {
            Assertions.assertEquals(path, cookie.getPath());
        }
        if (maxAge != null) {
            Assertions.assertEquals(maxAge, cookie.getMaxAge());
        }
        if (httpOnly != null) {
            Assertions.assertEquals(httpOnly, cookie.isHttpOnly());
        }
        if (secure != null) {
            Assertions.assertEquals(secure, cookie.isSecure());
        }
        if (sameSite != null) {
            Assertions.assertEquals(sameSite, cookie.getSameSite().toString());
        }
    }

    @ParameterizedTest
    @MethodSource("cookieArgs")
    public void testToNettyCookie(String name, String value, String domain, String path, Long maxAge, Boolean httpOnly, Boolean secure, String sameSite) {
        // given
        final Cookie cookie = Cookie.cookie(name, value);
        if (domain != null) {
            cookie.setDomain(domain);
        }
        if (path != null) {
            cookie.setPath(path);
        }
        if (maxAge != null) {
            cookie.setMaxAge(maxAge);
        }
        if (secure != null) {
            cookie.setSecure(secure);
        }
        if (httpOnly != null) {
            cookie.setHttpOnly(httpOnly);
        }
        if (sameSite != null) {
            cookie.setSameSite(CookieSameSite.valueOf(sameSite.toUpperCase()));
        }

        // when
        final DefaultCookie nettyCookie = CookieUtil.toNettyCookie(cookie);

        // then
        Assertions.assertEquals(name, nettyCookie.name());
        Assertions.assertEquals(value, nettyCookie.value());
        if (domain != null) {
            Assertions.assertEquals(domain, nettyCookie.domain());
        }
        if (path != null) {
            Assertions.assertEquals(path, nettyCookie.path());
        }
        if (maxAge != null) {
            Assertions.assertEquals(maxAge, nettyCookie.maxAge());
        }
        if (httpOnly != null) {
            Assertions.assertEquals(httpOnly, nettyCookie.isHttpOnly());
        }
        if (secure != null) {
            Assertions.assertEquals(secure, nettyCookie.isSecure());
        }
        if (sameSite != null) {
            Assertions.assertEquals(sameSite, nettyCookie.sameSite().toString());
        }
    }

}
