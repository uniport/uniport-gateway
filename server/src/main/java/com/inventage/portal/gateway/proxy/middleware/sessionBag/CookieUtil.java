package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.vertx.core.http.Cookie;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 */
public class CookieUtil {

    public static Set<Cookie> fromRequestHeader(List<String> cookieHeaders) {
        if (cookieHeaders == null) {
            return Collections.emptySet();
        }
        // LAX, otherwise cookies like "app-platform=iOS App Store" are not returned
        return cookieHeaders.stream()
            .filter(s -> s != null)
            .flatMap(cookieEntry -> ServerCookieDecoder.LAX.decode(cookieEntry).stream())
            .map(cookie -> CookieUtil.fromNettyCookie(cookie))
            .collect(Collectors.toSet());
    }

    public static Map<String, Cookie> cookieMapFromRequestHeader(List<String> cookieHeaders) {
        return fromRequestHeader(cookieHeaders).stream()
            .collect(Collectors.toMap(
                Cookie::getName,
                Function.identity(),
                (existing, replacement) -> existing));
    }

    public static Cookie fromNettyCookie(io.netty.handler.codec.http.cookie.Cookie nettyCookie) {
        return Cookie.cookie(nettyCookie.name(), nettyCookie.value())
            .setDomain(nettyCookie.domain())
            .setHttpOnly(nettyCookie.isHttpOnly())
            .setMaxAge(nettyCookie.maxAge())
            .setPath(nettyCookie.path())
            .setSecure(nettyCookie.isSecure());
    }

}
