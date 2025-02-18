package com.inventage.portal.gateway.proxy.middleware.matomo;

import static com.inventage.portal.gateway.TestUtils.buildConfiguration;
import static com.inventage.portal.gateway.TestUtils.withMiddleware;
import static com.inventage.portal.gateway.TestUtils.withMiddlewareOpts;
import static com.inventage.portal.gateway.TestUtils.withMiddlewares;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareTestBase;
import com.inventage.portal.gateway.proxy.middleware.VertxAssertions;
import com.inventage.portal.gateway.proxy.middleware.mock.TestBearerOnlyJWTProvider;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jakarta.json.Json;
import java.util.List;
import java.util.stream.Stream;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(VertxExtension.class)
class MatomoMiddlewareTest extends MiddlewareTestBase {

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {
        final JsonObject simple = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", MatomoMiddlewareFactory.MATOMO,
                    withMiddlewareOpts(JsonObject.of(
                        MatomoMiddlewareFactory.MATOMO_JWT_PATH_ROLES, "$.resource_access.Example.roles",
                        MatomoMiddlewareFactory.MATOMO_JWT_PATH_GROUP, "$.tenant",
                        MatomoMiddlewareFactory.MATOMO_JWT_PATH_EMAIL, "$.email",
                        MatomoMiddlewareFactory.MATOMO_JWT_PATH_USERNAME, "$.preferred_username")))));

        final JsonObject minimal = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", MatomoMiddlewareFactory.MATOMO)));

        final JsonObject missingOptions = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", MatomoMiddlewareFactory.MATOMO)));

        final JsonObject unknownProperty = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", MatomoMiddlewareFactory.MATOMO,
                    withMiddlewareOpts(
                        JsonObject.of("bar", "blub")))));

        return Stream.of(
            Arguments.of("accept simple config", simple, complete, expectedTrue),
            Arguments.of("accept minimal config", minimal, complete, expectedTrue),
            Arguments.of("accept config with no options", missingOptions, complete, expectedTrue),
            Arguments.of("reject config with unknown property", unknownProperty, complete, expectedFalse)

        );
    }

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

            VertxAssertions.assertNotNull(testCtx, username, "Following entry expected to be included in request header: " + HEADER_MATOMO_USERNAME);
            VertxAssertions.assertNotNull(testCtx, email, "Following entry expected to be included in request header: " + HEADER_MATOMO_EMAIL);
            VertxAssertions.assertNotNull(testCtx, role, "Following entry expected to be included in request header: " + HEADER_MATOMO_ROLE);
            VertxAssertions.assertNotNull(testCtx, group, "Following entry expected to be included in request header: " + HEADER_MATOMO_GROUP);

            VertxAssertions.assertEquals(testCtx, "ips", username);
            VertxAssertions.assertEquals(testCtx, "ips@inventage.com", email);
            VertxAssertions.assertEquals(testCtx, "Admin", role);
            VertxAssertions.assertEquals(testCtx, "portal", group);

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

            VertxAssertions.assertNotNull(testCtx, role, "Following entry expected to be included in request header: " + HEADER_MATOMO_ROLE);
            VertxAssertions.assertNotEquals(testCtx, selfSetRole, role);

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
            VertxAssertions.assertEquals(testCtx, HttpStatus.SC_FORBIDDEN, response.statusCode());
            testCtx.completeNow();
        });

    }

}
