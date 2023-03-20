package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import io.netty.handler.codec.http.cookie.Cookie;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CookieUtilTest {

    @Test
    public void test_cookieMap_null_String() {
        // given
        final String cookieHeader = null;
        // when
        final Map<String, Cookie> cookieMap = CookieUtil.cookieMapFromRequestHeader(null);
        // then
        Assertions.assertTrue(cookieMap.isEmpty());
    }

    @Test
    public void test_cookieMap_empty_String() {
        // given
        final String cookieHeader = "";
        // when
        final Map<String, Cookie> cookieMap = CookieUtil.cookieMapFromRequestHeader(List.of(cookieHeader));
        // then
        Assertions.assertTrue(cookieMap.isEmpty());
    }

    @Test
    public void test_cookieMap_4_cookies() {
        // given
        final String cookieHeader = "KEYCLOAK_SESSION=portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b; KEYCLOAK_SESSION_LEGACY=portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b; app-platform=iOS App Store; uniport.session=73ba363ad1ab36ea17681b882687e70458f14b2b2ff89d9215e0f655f3660d80";
        // when
        final Map<String, Cookie> cookieMap = CookieUtil.cookieMapFromRequestHeader(List.of(cookieHeader));
        // then
        Assertions.assertEquals("portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b",
            cookieMap.get("KEYCLOAK_SESSION").value());
        Assertions.assertEquals("portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b",
            cookieMap.get("KEYCLOAK_SESSION_LEGACY").value());
        Assertions.assertEquals("iOS App Store", cookieMap.get("app-platform").value());
        Assertions.assertEquals("73ba363ad1ab36ea17681b882687e70458f14b2b2ff89d9215e0f655f3660d80",
            cookieMap.get("uniport.session").value());
    }

    @Test
    public void test_cookieMap_4_cookies_from_2_entries() {
        // given
        final String cookieHeader1 = "KEYCLOAK_SESSION=portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b";
        final String cookieHeader2 = "KEYCLOAK_SESSION_LEGACY=portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b; app-platform=iOS App Store; uniport.session=73ba363ad1ab36ea17681b882687e70458f14b2b2ff89d9215e0f655f3660d80";
        // when
        final Map<String, Cookie> cookieMap = CookieUtil
            .cookieMapFromRequestHeader(List.of(cookieHeader1, cookieHeader2));
        // then
        Assertions.assertEquals("portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b",
            cookieMap.get("KEYCLOAK_SESSION").value());
        Assertions.assertEquals("portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b",
            cookieMap.get("KEYCLOAK_SESSION_LEGACY").value());
        Assertions.assertEquals("iOS App Store", cookieMap.get("app-platform").value());
        Assertions.assertEquals("73ba363ad1ab36ea17681b882687e70458f14b2b2ff89d9215e0f655f3660d80",
            cookieMap.get("uniport.session").value());
    }

    @Test
    public void test_cookieMap_1_cookies_from_2_entries() {
        // given
        final String cookieHeader1 = "KEYCLOAK_SESSION=portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b";
        final String cookieHeader2 = "KEYCLOAK_SESSION=portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b";
        // when
        final Map<String, Cookie> cookieMap = CookieUtil
            .cookieMapFromRequestHeader(List.of(cookieHeader1, cookieHeader2));
        // then
        Assertions.assertEquals("portal/f:b805becf-9777-4cbb-abd9-9da9ad3ce867:35/535fbc26-86fb-44e4-a971-24526fa7868b",
            cookieMap.get("KEYCLOAK_SESSION").value());
        Assertions.assertEquals(1, cookieMap.size());
    }
}
