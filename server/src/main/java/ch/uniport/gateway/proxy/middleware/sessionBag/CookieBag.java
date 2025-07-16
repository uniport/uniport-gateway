package ch.uniport.gateway.proxy.middleware.sessionBag;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.ClusterSerializable;
import java.util.HashSet;
import java.util.Iterator;

/**
 */
public class CookieBag extends HashSet<Cookie> implements ClusterSerializable {

    private static final String NAME = "name";
    private static final String VALUE = "value";
    private static final String DOMAIN = "domain";
    private static final String PATH = "path";
    private static final String MAX_AGE = "maxAge";
    private static final String SECURE = "secure";
    private static final String HTTP_ONLY = "httpOnly";
    private static final String SAME_SITE = "sameSite";

    /**
     */
    public CookieBag() {
        // do not remove: constructor required by AbstractSession#readDataFromBuffer for
        // ClusterSerializable
    }

    @Override
    public void writeToBuffer(Buffer buffer) {
        final JsonArray json = new JsonArray();
        final Iterator<Cookie> itr = iterator();
        while (itr.hasNext()) {
            final Cookie c = itr.next();
            final JsonObject cookie = new JsonObject();

            cookie.put(NAME, c.getName());
            cookie.put(VALUE, c.getValue());
            cookie.put(DOMAIN, c.getDomain());
            cookie.put(PATH, c.getPath());
            cookie.put(MAX_AGE, c.getMaxAge());
            cookie.put(SECURE, c.isSecure());
            cookie.put(HTTP_ONLY, c.isHttpOnly());

            if (c.getSameSite() != null) {
                cookie.put(SAME_SITE, c.getSameSite().toString());
            }

            json.add(cookie);
        }
        json.writeToBuffer(buffer);
    }

    @Override
    public int readFromBuffer(int pos, Buffer buffer) {
        final JsonArray json = new JsonArray();
        final int read = json.readFromBuffer(pos, buffer);

        for (Object dc : json) {
            final JsonObject decodedCookie = (JsonObject) dc;
            final Cookie c = Cookie.cookie(
                decodedCookie.getString(NAME),
                decodedCookie.getString(VALUE));

            c.setDomain(decodedCookie.getString(DOMAIN));
            c.setPath(decodedCookie.getString(PATH));
            c.setMaxAge(decodedCookie.getLong(MAX_AGE));
            c.setSecure(decodedCookie.getBoolean(SECURE));
            c.setHttpOnly(decodedCookie.getBoolean(HTTP_ONLY));

            if (decodedCookie.containsKey(SAME_SITE)) {
                switch (decodedCookie.getString(SAME_SITE)) {
                    case "None":
                        c.setSameSite(CookieSameSite.NONE);
                        break;
                    case "Strict":
                        c.setSameSite(CookieSameSite.STRICT);
                        break;
                    case "Lax":
                        c.setSameSite(CookieSameSite.LAX);
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }

            this.add(c);
        }

        return read;
    }

}
