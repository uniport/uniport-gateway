package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

import java.util.*;
import java.util.stream.Collectors;

public class CookieUtil {

    public static Set<Cookie> fromRequestHeader(List<String> cookieEntries) {
        if (cookieEntries == null) {
            return Collections.emptySet();
        }
        return cookieEntries.stream()
                .flatMap(cookieEntry -> ServerCookieDecoder.LAX.decode(cookieEntry).stream())
                .collect(Collectors.toSet());
    }

    public static Map<String, Cookie> cookieMapFromRequestHeader(List<String> cookieEntries) {
        Map<String, Cookie> cookieMap = new HashMap<>();
        if (cookieEntries != null) {
            cookieEntries.stream()
                    .flatMap(cookieEntry -> ServerCookieDecoder.LAX.decode(cookieEntry).stream())
                    .forEach(cookie -> cookieMap.put(cookie.name(), cookie));
        }
        return cookieMap;
    }

}
