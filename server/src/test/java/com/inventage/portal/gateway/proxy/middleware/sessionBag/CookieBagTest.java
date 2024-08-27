package com.inventage.portal.gateway.proxy.middleware.sessionBag;

import com.inventage.portal.gateway.proxy.middleware.VertxAssertions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class CookieBagTest {

    @Test
    void serializeAndDeserialize(Vertx vertx, VertxTestContext testCtx) {
        // given
        final Cookie cookie1 = Cookie.cookie("foo", "bar")
            .setDomain("example.com")
            .setPath("/test")
            .setMaxAge(1337L)
            .setSecure(true)
            .setHttpOnly(true)
            .setSameSite(CookieSameSite.STRICT);

        final Cookie cookie2 = Cookie.cookie("blub", "123abc");

        final CookieBag cookieBag = new CookieBag();
        cookieBag.add(cookie1);
        cookieBag.add(cookie2);

        final Buffer buffer = Buffer.buffer();

        // when
        cookieBag.writeToBuffer(buffer);
        final CookieBag actualCookieBag = new CookieBag();
        actualCookieBag.readFromBuffer(0, buffer);

        // then
        VertxAssertions.assertEquals(testCtx, cookieBag.size(), actualCookieBag.size(),
            String.format("Serialized and deserialized cookie bag size is not equal to original."));
        VertxAssertions.assertTrue(testCtx, hasAtLeastTheSameCookies(cookieBag, actualCookieBag),
            "Serialized and deserialized cookie bag is missing some cookies.");
        VertxAssertions.assertTrue(testCtx, hasAtLeastTheSameCookies(actualCookieBag, cookieBag),
            "Serialized and deserialized cookie bag is missing some cookies.");

        testCtx.completeNow();
    }

    boolean hasAtLeastTheSameCookies(CookieBag some, CookieBag other) {
        boolean allPresent = true;
        for (Cookie c1 : some) {
            boolean isPresent = false;
            for (Cookie c2 : other) {
                if (c1.encode().equals(c2.encode())) {
                    isPresent = true;
                    break;
                }
            }
            allPresent &= isPresent;
        }
        return allPresent;
    }

}
