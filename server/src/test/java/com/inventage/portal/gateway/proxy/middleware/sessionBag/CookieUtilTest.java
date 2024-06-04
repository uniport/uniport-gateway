package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import io.vertx.core.http.Cookie;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CookieUtilTest {

    @Test
    public void testCookieMapNull() {
        // given
        // when
        final Map<String, Cookie> cookieMap = CookieUtil.cookieMapFromRequestHeader(null);
        // then
        Assertions.assertTrue(cookieMap.isEmpty());
    }

    @Test
    public void testCookieMapNullString() {
        // given
        final String cookieHeader = null;
        // when
        final Map<String, Cookie> cookieMap = CookieUtil.cookieMapFromRequestHeader(Arrays.asList(cookieHeader));
        // then
        Assertions.assertTrue(cookieMap.isEmpty());
    }

    @Test
    public void testCookieMapEmptyString() {
        // given
        final String cookieHeader = "";
        // when
        final Map<String, Cookie> cookieMap = CookieUtil.cookieMapFromRequestHeader(List.of(cookieHeader));
        // then
        Assertions.assertTrue(cookieMap.isEmpty());
    }

    @Test
    public void testCookieMap4Cookies() {
        // given
        final String cookieHeader = "KEYCLOAK_SESSION=portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b; KEYCLOAK_SESSION_LEGACY=portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b; app-platform=iOS App Store; uniport.session=73ba363ad1ab36ea17681b882687e70458f14b2b2ff89d9215e0f655f3660d80";
        // when
        final Map<String, Cookie> cookieMap = CookieUtil.cookieMapFromRequestHeader(List.of(cookieHeader));
        // then
        Assertions.assertEquals("portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b",
            cookieMap.get("KEYCLOAK_SESSION").getValue());
        Assertions.assertEquals("portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b",
            cookieMap.get("KEYCLOAK_SESSION_LEGACY").getValue());
        Assertions.assertEquals("iOS App Store", cookieMap.get("app-platform").getValue());
        Assertions.assertEquals("73ba363ad1ab36ea17681b882687e70458f14b2b2ff89d9215e0f655f3660d80",
            cookieMap.get("uniport.session").getValue());
    }

    @Test
    public void testCookieMap4CookiesFrom2Entries() {
        // given
        final String cookieHeader1 = "KEYCLOAK_SESSION=portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b";
        final String cookieHeader2 = "KEYCLOAK_SESSION_LEGACY=portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b; app-platform=iOS App Store; uniport.session=73ba363ad1ab36ea17681b882687e70458f14b2b2ff89d9215e0f655f3660d80";
        // when
        final Map<String, Cookie> cookieMap = CookieUtil
            .cookieMapFromRequestHeader(List.of(cookieHeader1, cookieHeader2));
        // then
        Assertions.assertEquals("portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b",
            cookieMap.get("KEYCLOAK_SESSION").getValue());
        Assertions.assertEquals("portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b",
            cookieMap.get("KEYCLOAK_SESSION_LEGACY").getValue());
        Assertions.assertEquals("iOS App Store", cookieMap.get("app-platform").getValue());
        Assertions.assertEquals("73ba363ad1ab36ea17681b882687e70458f14b2b2ff89d9215e0f655f3660d80",
            cookieMap.get("uniport.session").getValue());
    }

    @Test
    public void testCookieMap1CookiesFrom2Entries() {
        // given
        final String cookieHeader1 = "KEYCLOAK_SESSION=portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b";
        final String cookieHeader2 = "KEYCLOAK_SESSION=portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b";
        // when
        final Map<String, Cookie> cookieMap = CookieUtil
            .cookieMapFromRequestHeader(List.of(cookieHeader1, cookieHeader2));
        // then
        Assertions.assertEquals("portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b",
            cookieMap.get("KEYCLOAK_SESSION").getValue());
        Assertions.assertEquals(1, cookieMap.size());
    }
}
