package ch.uniport.gateway.proxy.middleware.sessionBag;

import io.netty.handler.codec.http.cookie.CookieHeaderNames.SameSite;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import java.util.BitSet;
import java.util.Objects;
import org.slf4j.Logger;

public class CookieUtil {

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

    public static boolean isValidCookieName(Logger logger, String name) {
        Objects.requireNonNull(name, "name must not be null");
        final int invalidOctetPos = firstInvalidCookieNameOctet(name);
        if (invalidOctetPos >= 0) {
            logger.debug("Invalid cookie because name '{}' contains invalid char '{}'", name,
                name.charAt(invalidOctetPos));
            return false;
        }
        return true;
    }

    // Taken from
    // https://github.com/netty/netty/blob/netty-4.1.118.Final/codec-http/src/main/java/io/netty/handler/codec/http/cookie/CookieUtil.java#L134-L150
    private static final BitSet VALID_COOKIE_NAME_OCTETS = validCookieNameOctets();

    // See https://datatracker.ietf.org/doc/html/rfc2616#section-2.2
    // token = 1*<any CHAR except CTLs or separators>
    // separators = "(" | ")" | "<" | ">" | "@"
    // | "," | ";" | ":" | "\" | <">
    // | "/" | "[" | "]" | "?" | "="
    // | "{" | "}" | SP | HT
    private static BitSet validCookieNameOctets() {
        final BitSet bits = new BitSet();
        for (int i = 32; i < 127; i++) {
            bits.set(i);
        }
        final int[] separators = new int[] { '(', ')', '<', '>', '@', ',', ';', ':', '\\', '"', '/', '[', ']', '?', '=',
            '{', '}', ' ', '\t' };
        for (int separator : separators) {
            bits.set(separator, false);
        }
        return bits;
    }

    private static int firstInvalidCookieNameOctet(CharSequence cs) {
        return firstInvalidOctet(cs, VALID_COOKIE_NAME_OCTETS);
    }

    private static int firstInvalidOctet(CharSequence cs, BitSet bits) {
        for (int i = 0; i < cs.length(); i++) {
            final char c = cs.charAt(i);
            if (!bits.get(c)) {
                return i;
            }
        }
        return -1;
    }

}
