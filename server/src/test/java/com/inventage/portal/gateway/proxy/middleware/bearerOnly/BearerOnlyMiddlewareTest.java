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

    String host = "localhost";

    String publicKeyRS128 = "-----BEGIN PUBLIC KEY-----\nMIGJAoGBAMaZN/RHwa2NTWhgOwoMgCJjV2SZwFNIKDJPNDNVqxH1W4+KTtchse6h aPBPUo5WiLOQngLwNpSZPnnKdr+RfhfD0cG+uImsp92WLMFWsNoj+Xk+cvv8VsFn yxJWhWxsT2PBwfezQy9oMovxeBGeHswVSVBLN+XOwT/0n3A19NSHAgMBAAE=\n-----END PUBLIC KEY-----";
    String publicKeyRS256 = "-----BEGIN PUBLIC KEY-----\n MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA179CJI+BJzyHanO75PLE ZxSGxsY3pPnKT9UQ41/LHPDRb0OTsnT/aYE+2PMuj+F/pjgVxy1orBAHUfOwgHpM spptD5B7+zmnQi4JXI/sMc22PliNXb1XLzgBNOUl0r1Ky/EQ9gtzGKuVUzW+Imlb Y+Q9WgqYlhrWfCQ8E4me17P5+LgvBEEx+UMPCFF0Myqt7eeV1OZc2SKZV6RSCVyh +gz82dAuD/d2wAAAeNA21EBl7Gt5JAlDfiXRncpXBJb0L60+w2sweuf9D4SEq3EI lYUXQuhPi9yMGmXIPwIXAfUI67h3AeoMx3lI779AlF0qxW2Nfhfltam4grOtLUNF uwIDAQAB\n-----END PUBLIC KEY-----";

    // RSA
    // ssh-keygen -t rsa -b 2048
    // ssh-keygen -f test_key.pub -e -m pem
    // OR
    // openssl genrsa -des3 -out test_key.pem 2048
    // openssl rsa -in test_key.pem -outform PEM -pubout -out test_key_public.pem

    private JWTAuth authProvider;
    private HttpServer server;
    private int port;

    @BeforeEach
    public void setup(Vertx vertx) throws Exception {
        authProvider = JWTAuth.create(vertx,
                new JWTAuthOptions()
                        .addPubSecKey(new PubSecKeyOptions().setAlgorithm("RS256").setBuffer(publicKeyRS256))
                        .setJWTOptions(new JWTOptions().setIssuer("issuer").setAudience(List.of("audience"))));

        BearerOnlyMiddleware bearerOnly = new BearerOnlyMiddleware(JWTAuthHandler.create(authProvider));

        Handler<RoutingContext> endHandler = ctx -> ctx.response().end("ok");

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
        RequestOptions reqOpts = new RequestOptions().setHost(host).setPort(port).setURI("/").setMethod(HttpMethod.GET);
        doRequest(vertx, testCtx, reqOpts, 401);
    }

    @Test
    public void invalidSignature(Vertx vertx, VertxTestContext testCtx) {
    }

    @Test
    public void issuerMismatch(Vertx vertx, VertxTestContext testCtx) {
    }

    @Test
    public void audienceMismatch(Vertx vertx, VertxTestContext testCtx) {
    }

    @Test
    public void validWithRSA256(Vertx vertx, VertxTestContext testCtx) {
    }

    void doRequest(Vertx vertx, VertxTestContext testCtx, RequestOptions reqOpts, int expectedStatusCode) {
        vertx.createHttpClient().request(reqOpts).compose(req -> req.send()).onComplete(testCtx.succeeding(resp -> {
            testCtx.verify(() -> {
                assertEquals(resp.statusCode(), expectedStatusCode, "unexpected status code");
            });
            testCtx.completeNow();
        }));
    }
}
