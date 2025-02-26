package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import io.netty.handler.codec.http.cookie.CookieHeaderNames.SameSite;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CookieUtil {

    public static Map<String, Cookie> cookieMapFromRequestHeader(List<String> cookieHeaders) {
        return fromRequestHeader(cookieHeaders).stream()
            .collect(Collectors.toMap(
                Cookie::getName,
                Function.identity(),
                (existing, replacement) -> existing));
    }

    public static Set<Cookie> fromRequestHeader(List<String> cookieHeaders) {
        if (cookieHeaders == null) {
            return Collections.emptySet();
        }
        // LAX, otherwise cookies like "app-platform=iOS App Store" are not returned
        return cookieHeaders.stream()
            .filter(s -> s != null)
            .flatMap(cookieEntry -> ServerCookieDecoder.LAX.decode(cookieEntry).stream())
            .filter(cookie -> cookie != null)
            .map(cookie -> CookieUtil.fromNettyCookie(cookie))
            .collect(Collectors.toSet());
    }

    public static Cookie fromNettyCookie(io.netty.handler.codec.http.cookie.Cookie nettyCookie) {
        if (nettyCookie == null) {
            return null;
        }

        final Cookie cookie = Cookie.cookie(nettyCookie.name(), nettyCookie.value())
            .setDomain(nettyCookie.domain())
            .setPath(nettyCookie.path())
            .setMaxAge(nettyCookie.maxAge())
            .setHttpOnly(nettyCookie.isHttpOnly())
            .setSecure(nettyCookie.isSecure());

        // SameSite is not added to the Cookie interface yet
        // See https://github.com/netty/netty/issues/6509
        if (nettyCookie instanceof DefaultCookie) {
            final DefaultCookie c = (DefaultCookie) nettyCookie;
            if (c.sameSite() != null) {
                cookie.setSameSite(CookieSameSite.valueOf(c.sameSite().name().toUpperCase()));
            }
        }

        return cookie;
    }

    public static DefaultCookie toNettyCookie(Cookie cookie) {
        if (cookie == null) {
            return null;
        }

        final DefaultCookie nettyCookie = new DefaultCookie(cookie.getName(), cookie.getValue());
        nettyCookie.setDomain(cookie.getDomain());
        nettyCookie.setPath(cookie.getPath());
        nettyCookie.setMaxAge(cookie.getMaxAge());
        nettyCookie.setHttpOnly(cookie.isHttpOnly());
        nettyCookie.setSecure(cookie.isSecure());

        if (cookie.getSameSite() != null) {
            nettyCookie.setSameSite(SameSite.valueOf(cookie.getSameSite().toString()));
        }

        return nettyCookie;
    }

}
