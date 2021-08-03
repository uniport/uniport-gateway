package com.inventage.portal.gateway.proxy.middleware.bearerOnly;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.inventage.portal.gateway.TestUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class BearerOnlyMiddlewareTest {

    private static final String host = "localhost";

    // Generate RSA key pair
    // openssl genrsa -out test_private_key.pem 2048
    // openssl rsa -in test_private_key.pem -outform PEM -pubout -out test_public_key.pem

    /*
    -----BEGIN RSA PRIVATE KEY-----
    MIIEpQIBAAKCAQEA5A7cv3Vyv2EaR/j4IPCIX4+W73XxUelSKr+D38DiLMb7Lpc7
    VWNC/dYq2u6FiLIIr/cdIe+zOnf0yfMnIZVbHVqLI6ll4gn07etpf97FhF7X3mg4
    EfoWIl+PNZze7dDrdfwfOle5hQX2WWdHKk860diw6e/YPsMQzfw0M1SL6mo44rgS
    x6hYeHOFaq/mcKzcUgwyQeWjhyu+JZnPxFYL2N3/AC/X6SzqrmR8JWJECxhW0ooJ
    y6L+xsQQiCAqYjhRDslIcbKBYtavHx5s/Kzq1M8isyfBIJOUpltNTW/CwFnFJj6S
    AzPHLlxfrOz/X5p3SsfwH2UVIjaineMkiEPZfwIDAQABAoIBAQDN2YhxnL6ldi11
    t4momdROhVpU7N2U9QiAo3uSNRUyG61QAZvB2CX43x6xnMiVeTWUN3ZpUmYxqWMg
    AkVY7+pdVYPv/ZCD9j7JnksM63TXpZAuJV4vA9CE2EJ4vw8OFBzk010QmWxQYPBb
    BHjc/MT38yLFFgPXqtT5SOOJTZA5VlGJB3SV0dLEq77ef/ngzUmWJzGQgv26ymz9
    gHJl8qy5aP6bgmiQBytxEktEPHHgriGSuSrLta9ezh8GIGGTw2ttqfOr5isUVbQK
    5eTTkJtkK7ok+lRITp+miGN7sAUy4Tl+K5Hl1TkuSxEgNMhmgwpVWeXjhc3HlktW
    I77FgTRBAoGBAPfkx7Zesmphs90umSGNDq4Li50iCjDSMkTSKlwRwiwPRMyf7bCg
    KcT+HGEATXvk7CLX2ADK/I5B3+S/tt2AfDTVOenNvXuz/6PNIz27KsHIFeSe8P67
    apaKbh60OYuk1VibqvN337TZcoW08WWWiS6h6BxaXGMxpy6NrF5D6cihAoGBAOuE
    B7J3vfVdoxezM+5mi3iG1U0VfGDFGTjlj0cUAlwM3AK/mZnq3/dMw9b3H4f4VaSn
    VowS0dl3SHxhF5VRFrHt/Dq3s0x/dB4fAaoKkmObjB36Xgo8MOY7o5qW5PKO+wMR
    JvT7de9kg3H/H6+iX1Zb9E3w0nSuPUbSEa7Ro84fAoGAEM1G5AuDGEbLBCDWbDm9
    VvqdWecmvaxhj9yW1mq1uHrIdP4aBDC25A09Ky30EoOvpaTvlQ4tFA9O95gu8tB0
    mrghFsHFKA9JMncC/nojKcNACKDlQL6/OLjlQduBUv+3Hixe5+WmGgHrCzj6a6JK
    Zgi/TLyrKmYBKNydZD5CKEECgYEAolpf2/2Dq7OjDGFyuTNjjfCU9hCLr0HwAzLs
    tDjs73vF5vch8eLiBd6bWoL874SXtWvN073df6YlB+j+kuZVWM8QA4JDTcbGy0Tg
    ptGm3JeL3daMIU4g/3W5cIX4yeUa0KBwVI1MXXzSyDDxLOgoBKZbIaeTzO+YOkvx
    +Kt32k0CgYEAhtSZRqoyUmqfhYpHfH38sTGgrfefE8akaoF2XsaYFehdCmkbHwgm
    s9BMx5Ae/Mry5RPkPE4UKvYrO79NSsew+TC8EvAYlD4gNOD8Tn2L4UagB77BEX9Z
    9SDeZ2OYUlcYzb68RM9Mg3IH4JP25uBJerDfq8yY8KarJqun7Dcg29E=
    -----END RSA PRIVATE KEY-----
    */
    private static final String publicKeyRS256 = "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5A7cv3Vyv2EaR/j4IPCIX4+W73XxUelSKr+D38DiLMb7Lpc7VWNC/dYq2u6FiLIIr/cdIe+zOnf0yfMnIZVbHVqLI6ll4gn07etpf97FhF7X3mg4EfoWIl+PNZze7dDrdfwfOle5hQX2WWdHKk860diw6e/YPsMQzfw0M1SL6mo44rgSx6hYeHOFaq/mcKzcUgwyQeWjhyu+JZnPxFYL2N3/AC/X6SzqrmR8JWJECxhW0ooJy6L+xsQQiCAqYjhRDslIcbKBYtavHx5s/Kzq1M8isyfBIJOUpltNTW/CwFnFJj6SAzPHLlxfrOz/X5p3SsfwH2UVIjaineMkiEPZfwIDAQAB\n-----END PUBLIC KEY-----\n";
    private static final String publicKeyAlgorithm = "RS256";

    /*
        Generate JWT with signature:
        #!/bin/bash
        base64_encode() { echo -n "$(</dev/stdin)" | base64 | tr -d '=' | tr '/+' '_-' | tr -d '\n'; }
        hmacsha256_sign() { echo -n "$(</dev/stdin)" | openssl dgst -sha256 -sign test_private_key.pem; }
        header_payload=$(echo -n "$header" | jq -c | base64_encode).$(echo -n "$payload" | jq -c | base64_encode)
        signature=$(echo -n "${header_payload}" | hmacsha256_sign | base64_encode )
        echo -n "${header_payload}.${signature}"
    */

    // signed with private key from above
    // header: {"alg": "RS256", "typ": "JWT"}
    // payload: {"typ": "Bearer", "exp": 1893452400, "iat": 1627053747, "iss": "http://test.issuer:1234/auth/realms/test", "azp": "test-authorized-parties", "aud": "test-audience", "scope": "openid email profile Test"}
    private static final String validToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0eXAiOiJCZWFyZXIiLCJleHAiOjE4OTM0NTI0MDAsImlhdCI6MTYyNzA1Mzc0NywiaXNzIjoiaHR0cDovL3Rlc3QuaXNzdWVyOjEyMzQvYXV0aC9yZWFsbXMvdGVzdCIsImF6cCI6InRlc3QtYXV0aG9yaXplZC1wYXJ0aWVzIiwiYXVkIjoidGVzdC1hdWRpZW5jZSIsInNjb3BlIjoib3BlbmlkIGVtYWlsIHByb2ZpbGUgVGVzdCJ9.wY8mnoZIZqufUBXd7Ji0bnKI73IsxNX6oMy-2Nt_e9KYMJWNkNUmHXSImCTeDPsirEb8hlUpCVJt-EoR_h6jDHGzE-klOpOfLPq9_oImaqED9j5yFlOmtjSNXEMKJ9UvMLwsEfcKn2Jv2-Evs1A6FHf13jPVkzz3gVJb-z7yqCdOPgZEYQhq39owAd6c9IrEslxLCKylJF9q-XLPNwv_hIMs5fq8jzmPFaYgMJg7hFJvB7wZC1j7uBl5Sf38ly2M5d8WkdE3D6F1-juXlU6zHtsoEeVoX7RVe2FpS4_ubxIpFUsvM5ALLe7RSxQqrcYRPGRVanIuEHudoVDHsbAYMw";
    // payload: {"typ": "Bearer", "exp": 1893452400, "iat": 1627053747, "iss": "http://test.issuer:1234/auth/realms/test", "azp": "test-authorized-parties", "aud": "test-audience", "scope": "openid email profile Test"}
    private static final String invalidSignatureToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0eXAiOiJCZWFyZXIiLCJleHAiOjE4OTM0NTI0MDAsImlhdCI6MTYyNzA1Mzc0NywiaXNzIjoiaHR0cDovL3Rlc3QuaXNzdWVyOjEyMzQvYXV0aC9yZWFsbXMvdGVzdCIsImF6cCI6InRlc3QtYXV0aG9yaXplZC1wYXJ0aWVzIiwiYXVkIjoidGVzdC1hdWRpZW5jZSIsInNjb3BlIjoib3BlbmlkIGVtYWlsIHByb2ZpbGUgVGVzdCJ9.blub";
    // payload: 'test.issuer' replaced with 'malory.issuer'
    private static final String invalidIssuerToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0eXAiOiJCZWFyZXIiLCJleHAiOjE4OTM0NTI0MDAsImlhdCI6MTYyNzA1Mzc0NywiaXNzIjoiaHR0cDovL21hbG9yeS5pc3N1ZXI6MTIzNC9hdXRoL3JlYWxtcy90ZXN0IiwiYXpwIjoidGVzdC1hdXRob3JpemVkLXBhcnRpZXMiLCJhdWQiOiJ0ZXN0LWF1ZGllbmNlIiwic2NvcGUiOiJvcGVuaWQgZW1haWwgcHJvZmlsZSBUZXN0In0.Vr0Fm-X_ri81CGuus5ro2f7bgDyDcDNRls627w_AugDXuR-NQALziNYJlR51kvC9pVgdwpyFzgjoHJd5Gk0qrdxJOsGRNVFny5OL72lMqp2yEmmThR7qKEuNW4qW3_VqZHsn4WgT_sUSmuoLO_r82udmMtufl31FCpkwy1NQV7Z-mJ631dpLQwN_5a9C5NOeXqLn1A2PWK3av_1jK_t32IaLTEmXQvcW_JiTt-BWO2VoemHCPcUe2DajOvHD8sMyIbZeeEiP8EuCCMXqm6o2i05Q6zgX1wBazHHX59MBkM-aSqNLRVW0s8qz_AOuNSWGykOA3rVKOR0YLQq0QA3HQQ";
    // payload: 'test-audience' replaced with 'malory-audience'
    private static final String invalidAudienceToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0eXAiOiJCZWFyZXIiLCJleHAiOjE4OTM0NTI0MDAsImlhdCI6MTYyNzA1Mzc0NywiaXNzIjoiaHR0cDovL3Rlc3QuaXNzdWVyOjEyMzQvYXV0aC9yZWFsbXMvdGVzdCIsImF6cCI6InRlc3QtYXV0aG9yaXplZC1wYXJ0aWVzIiwiYXVkIjoibWFsb3J5LWF1ZGllbmNlIiwic2NvcGUiOiJvcGVuaWQgZW1haWwgcHJvZmlsZSBUZXN0In0.Okf3MwbxD5q1iTa3NeAn_DhZXDFrUE6eg17GqmVqtMoqCHq7PA9FxH5p3kTStaSfu1nqGuwgSel1VN8bRxwXPLuoVL_r-l2w3VLZuv2v-VmhheP2L8OKbpdQXTezsLErMtb1PpS06p4KKqKTr-ryzS3ZNm98oVBiMVAVEWaqq2cR24JI6rRcZfgHoe2kuzyOr8_MlzHVtzrKOBMtZKAIu2_U8Wfqm4oNQykJTYstqc6hIyvdPiWkJvCMEKydkT1MPsnCW6jwOn6d0S0uPg43WSRDKhCIupf3nEwZD_nYYKDIJcqIfujqnIS3fV-IW0CGjkAEO_WN2fLu0LhC2SRMhQ";

    private static final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
    private static final List<String> expectedAudience = List.of("test-audience");

    private JWTAuth authProvider;
    private HttpServer server;
    private int port;

    @BeforeEach
    public void setup(Vertx vertx) throws Exception {
        authProvider = JWTAuth.create(vertx,
                new JWTAuthOptions()
                        .addPubSecKey(new PubSecKeyOptions().setAlgorithm(publicKeyAlgorithm).setBuffer(publicKeyRS256))
                        .setJWTOptions(new JWTOptions().setIssuer(expectedIssuer).setAudience(expectedAudience)));

        BearerOnlyMiddleware bearerOnly = new BearerOnlyMiddleware(JWTAuthHandler.create(authProvider));

        Handler<RoutingContext> endHandler = ctx -> ctx.response().setStatusCode(200).end("ok");

        Router router = Router.router(vertx);
        router.route().handler(bearerOnly).handler(endHandler);

        final CountDownLatch latch = new CountDownLatch(1);

        port = TestUtils.findFreePort();
        server = vertx.createHttpServer().requestHandler(req -> {
            router.handle(req);
        }).listen(port, ready -> {
            if (ready.failed()) {
                throw new RuntimeException(ready.cause());
            }
            latch.countDown();
        });

        latch.await();
    }

    @AfterEach
    public void tearDown() throws Exception {
        server.close();
    }

    @Test
    public void noBearer(Vertx vertx, VertxTestContext testCtx) {
        RequestOptions reqOpts = new RequestOptions();
        doRequest(vertx, testCtx, reqOpts, 401);
    }

    @Test
    public void invalidSignature(Vertx vertx, VertxTestContext testCtx) {
        RequestOptions reqOpts = new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION,
                "Bearer " + invalidSignatureToken);
        doRequest(vertx, testCtx, reqOpts, 401);
    }

    @Test
    public void issuerMismatch(Vertx vertx, VertxTestContext testCtx) {
        RequestOptions reqOpts = new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION,
                "Bearer " + invalidIssuerToken);
        doRequest(vertx, testCtx, reqOpts, 401);
    }

    @Test
    public void audienceMismatch(Vertx vertx, VertxTestContext testCtx) {
        RequestOptions reqOpts = new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION,
                "Bearer " + invalidAudienceToken);
        doRequest(vertx, testCtx, reqOpts, 401);
    }

    @Test
    public void validWithRSA256(Vertx vertx, VertxTestContext testCtx) {
        RequestOptions reqOpts = new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + validToken);
        doRequest(vertx, testCtx, reqOpts, 200);
    }

    void doRequest(Vertx vertx, VertxTestContext testCtx, RequestOptions reqOpts, int expectedStatusCode) {
        reqOpts.setHost(host).setPort(port).setURI("/").setMethod(HttpMethod.GET);
        vertx.createHttpClient().request(reqOpts).compose(req -> req.send()).onComplete(testCtx.succeeding(resp -> {
            testCtx.verify(() -> {
                assertEquals(expectedStatusCode, resp.statusCode(), "unexpected status code");
            });
            testCtx.completeNow();
        }));
    }
}
