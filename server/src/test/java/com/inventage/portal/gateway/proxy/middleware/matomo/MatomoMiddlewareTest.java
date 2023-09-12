package com.inventage.portal.gateway.proxy.middleware.matomo;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import com.inventage.portal.gateway.proxy.middleware.mock.TestBearerOnlyJWTProvider;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.RequestOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jakarta.json.Json;
import java.util.List;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class MatomoMiddlewareTest {

    private static final String DEFAULT_JWT_PATH_ROLES = "$.resource_access.Analytics.roles";
    private static final String DEFAULT_JWT_PATH_GROUP = "$.tenant";
    private static final String DEFAULT_JWT_PATH_USERNAME = "$.preferred_username";
    private static final String DEFAULT_JWT_PATH_EMAIL = "$.email";
    private static final String HEADER_MATOMO_USERNAME = "matomo-username";
    private static final String HEADER_MATOMO_EMAIL = "matomo-email";
    private static final String HEADER_MATOMO_GROUP = "matomo-group";
    private static final String HEADER_MATOMO_ROLE = "matomo-role";

    private static final jakarta.json.JsonObject VALID_PAYLOAD_TEMPLATE = Json.createObjectBuilder()
        .add("preferred_username", "ips")
        .add("email", "ips@inventage.com")
        .add("tenant", "portal")
        .add("resource_access", Json.createObjectBuilder()
            .add("Analytics", Json.createObjectBuilder()
                .add("roles", Json.createArrayBuilder(List.of("Admin")))))
        .build();

    @Test
    void headersParsedCorrectlyForAutoLoginPlugin(Vertx vertx, VertxTestContext testCtx) {
        //given
        final int port = TestUtils.findFreePort();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withMatomoMiddleware(DEFAULT_JWT_PATH_ROLES, DEFAULT_JWT_PATH_GROUP, DEFAULT_JWT_PATH_USERNAME, DEFAULT_JWT_PATH_EMAIL)
            .withProxyMiddleware(port)
            .build().start();

        final jakarta.json.JsonObject payload = Json.createObjectBuilder(VALID_PAYLOAD_TEMPLATE)
            .build();
        final String accessToken = TestBearerOnlyJWTProvider.signToken(payload);
        RequestOptions requestOptions = new RequestOptions();
        requestOptions.addHeader("Authorization", "Bearer " + accessToken);

        vertx.createHttpServer().requestHandler((proxyRequestHandler) -> {
            final MultiMap headers = proxyRequestHandler.headers();
            final HttpServerResponse response = proxyRequestHandler.response();
            headers.entries().forEach(entry -> {
                response.putHeader(entry.getKey(), entry.getValue());
            });
            response.end();
        }).listen(port);

        gateway.incomingRequest(HttpMethod.GET, "/", requestOptions, response -> {
            final MultiMap headers = response.headers();
            final String username = headers.get(HEADER_MATOMO_USERNAME);
            final String email = headers.get(HEADER_MATOMO_EMAIL);
            final String role = headers.get(HEADER_MATOMO_ROLE);
            final String group = headers.get(HEADER_MATOMO_GROUP);

            Assertions.assertNotNull(username, "Following entry expected to be included in request header: " + HEADER_MATOMO_USERNAME);
            Assertions.assertNotNull(email, "Following entry expected to be included in request header: " + HEADER_MATOMO_EMAIL);
            Assertions.assertNotNull(role, "Following entry expected to be included in request header: " + HEADER_MATOMO_ROLE);
            Assertions.assertNotNull(group, "Following entry expected to be included in request header: " + HEADER_MATOMO_GROUP);

            Assertions.assertEquals("ips", username);
            Assertions.assertEquals("ips@inventage.com", email);
            Assertions.assertEquals("Admin", role);
            Assertions.assertEquals("portal", group);

            testCtx.completeNow();
        });

    }

    @Test
    void discardAutoLoginHeaderNotSetByPortalGateway(Vertx vertx, VertxTestContext testCtx) {
        //given
        final int port = TestUtils.findFreePort();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withMatomoMiddleware(DEFAULT_JWT_PATH_ROLES, DEFAULT_JWT_PATH_GROUP, DEFAULT_JWT_PATH_USERNAME, DEFAULT_JWT_PATH_EMAIL)
            .withProxyMiddleware(port)
            .build().start();

        final jakarta.json.JsonObject payload = Json.createObjectBuilder(VALID_PAYLOAD_TEMPLATE)
            .build();
        final String accessToken = TestBearerOnlyJWTProvider.signToken(payload);
        final String selfSetRole = "Superuser";
        RequestOptions requestOptions = new RequestOptions();
        requestOptions.addHeader("Authorization", "Bearer " + accessToken);
        requestOptions.addHeader(HEADER_MATOMO_ROLE, selfSetRole);

        vertx.createHttpServer().requestHandler((proxyRequestHandler) -> {
            final MultiMap headers = proxyRequestHandler.headers();
            final HttpServerResponse response = proxyRequestHandler.response();
            headers.entries().forEach(entry -> {
                response.putHeader(entry.getKey(), entry.getValue());
            });
            response.end();
        }).listen(port);

        gateway.incomingRequest(HttpMethod.GET, "/", requestOptions, response -> {
            final MultiMap headers = response.headers();
            final String role = headers.get(HEADER_MATOMO_ROLE);

            Assertions.assertNotNull(role, "Following entry expected to be included in request header: " + HEADER_MATOMO_ROLE);
            Assertions.assertNotEquals(selfSetRole, role);

            testCtx.completeNow();
        });

    }

    @Test
    void noAuthorizationHeader(Vertx vertx, VertxTestContext testCtx) {
        //given
        final int port = TestUtils.findFreePort();
        final MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withMatomoMiddleware(DEFAULT_JWT_PATH_ROLES, DEFAULT_JWT_PATH_GROUP, DEFAULT_JWT_PATH_USERNAME, DEFAULT_JWT_PATH_EMAIL)
            .withProxyMiddleware(port)
            .build().start();

        RequestOptions requestOptions = new RequestOptions();

        vertx.createHttpServer().requestHandler((proxyRequestHandler) -> {
            final HttpServerResponse response = proxyRequestHandler.response();
            response.end();
        }).listen(port);

        gateway.incomingRequest(HttpMethod.GET, "/", requestOptions, response -> {
            Assertions.assertEquals(HttpStatus.SC_FORBIDDEN, response.statusCode());
            testCtx.completeNow();
        });

    }

}
