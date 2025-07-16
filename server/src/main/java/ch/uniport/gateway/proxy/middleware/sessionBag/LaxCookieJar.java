package ch.uniport.gateway.proxy.middleware.sessionBag;

import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.vertx.core.http.impl.CookieJar;
import io.vertx.core.http.impl.ServerCookie;
import java.util.Objects;
import java.util.Set;

/**
 * So this is a bit ugly.
 * 
 * The cookie jar from vertx only support parsing cookies from the request
 * cookie header in netty's strict mode
 * i.e. with validation of the name and value according to RFC6265.
 * https://github.com/fbuetler/vert.x/blob/4.5.10/src/main/java/io/vertx/core/http/impl/CookieJar.java#L40
 * 
 * However, the session bag also needs to support "invalid" cookies e.g.
 * containing whitespaces.
 * 
 * Due to vertx restricting the visibility of some key components, this class is
 * hacked together.
 * - We cant use 'new CookieJar(cookieHeader)' because it is not visible
 * - We must cast blindly the 'Cookie' to 'ServerCookie' as the 'CookieJar' only
 * supports these.
 * This is based on the assumption of 'CookieImpl implements ServerCookie' and
 * 'ServerCookie extends Cookie'
 * 
 * Example cookie that would fail validation: "app-platform=iOS App Store"
 */
public class LaxCookieJar extends CookieJar {

    public LaxCookieJar() {
        super();
    }

    public LaxCookieJar(CharSequence cookieHeader) {
        super();
        Objects.requireNonNull(cookieHeader, "cookie header cannot be null");

        final Set<io.netty.handler.codec.http.cookie.Cookie> nettyCookies = ServerCookieDecoder.LAX
            .decode(cookieHeader.toString());
        for (io.netty.handler.codec.http.cookie.Cookie cookie : nettyCookies) {
            add((ServerCookie) CookieUtil.fromNettyCookie(cookie));
        }
    }
}
