package com.inventage.portal.gateway.proxy.middleware.claimToHeader;

import static com.inventage.portal.gateway.TestUtils.buildConfiguration;
import static com.inventage.portal.gateway.TestUtils.withMiddleware;
import static com.inventage.portal.gateway.TestUtils.withMiddlewareOpts;
import static com.inventage.portal.gateway.TestUtils.withMiddlewares;
import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.portalGateway;
import static io.vertx.core.http.HttpMethod.GET;

import com.inventage.portal.gateway.proxy.middleware.MiddlewareServer;
import com.inventage.portal.gateway.proxy.middleware.MiddlewareTestBase;
import com.inventage.portal.gateway.proxy.middleware.VertxAssertions;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(VertxExtension.class)
public class ClaimToHeaderMiddlewareTest extends MiddlewareTestBase {

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<Arguments> provideConfigValidationTestData() {
        final JsonObject simple = buildConfiguration(
            withMiddlewares(
                withMiddleware("claimToHeader", ClaimToHeaderMiddlewareFactory.TYPE,
                    withMiddlewareOpts(
                        new JsonObject()
                            .put(ClaimToHeaderMiddlewareFactory.PATH, "claimPath")
                            .put(ClaimToHeaderMiddlewareFactory.NAME, "headerName")))));

        final JsonObject missingRequiredPropertyPath = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ClaimToHeaderMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        ClaimToHeaderMiddlewareFactory.NAME, "blub")))));

        final JsonObject missingRequiredPropertyName = buildConfiguration(
            withMiddlewares(
                withMiddleware("foo", ClaimToHeaderMiddlewareFactory.TYPE,
                    withMiddlewareOpts(JsonObject.of(
                        ClaimToHeaderMiddlewareFactory.PATH, "blub")))));

        return Stream.of(
            Arguments.of("accept simple config", simple, complete, expectedTrue),
            Arguments.of("reject config missing required property (path)", missingRequiredPropertyPath, complete, expectedFalse),
            Arguments.of("reject config missing required property (name)", missingRequiredPropertyName, complete, expectedFalse)

        );
    }

    @Test
    public void withAuthorizationBearerValue(Vertx vertx, VertxTestContext testCtx) {
        // given
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add(HttpHeaders.AUTHORIZATION,
            "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ1Y0p4dWNXRDFWZnFSNU56QmtKZng2RnNZYmJHeEcxOHk5bVZrazFYYWJZIn0.eyJleHAiOjE2MTE5Nzg2MjAsImlhdCI6MTYxMTkzNTQyMCwianRpIjoiOWY0MGNiZTEtZDJhMC00N2Q0LWFlNjYtYTI5ZTQyNDg4ZTFmIiwiaXNzIjoiaHR0cDovL2RvY2tlci10ZXN0LmludmVudGFnZS5jb206ODA4MC9hdXRoL3JlYWxtcy9wb3J0YWwiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiYWIxNTk0MTMtODkyNC00ZjEwLThkYjEtMTc5NGY0MzFlNzJiIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiT3JnYW5pc2F0aW9uIiwic2Vzc2lvbl9zdGF0ZSI6IjU3MWEzNTU3LWNiOWMtNDg4ZS05YzMwLWQyZTAwYjQ4MzRkMCIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7Ik9yZ2FuaXNhdGlvbiI6eyJyb2xlcyI6WyJLRVlDTE9BSyJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJzZXJ2aWNlLWFjY291bnQtb3JnYW5pc2F0aW9uIn0.XkgBLTzgUHvmYFu96d55aZdSpkzKEGO6cyQxMfSZt3Ryfp5gLhLywtdXSyaHJ4qgDHDkB3rGcY6NH2tAT4wwDRSCz_4gzOZV5qkwirdvQ8OXwi-Oa16yPjFgj3I8Mn1UO7QkchiiIZ-2J9roVLPhet5McgBSkGB5YyzlWRyFIr2imM5JKuBjT2zVM6HiqzpvRoW11lQBR1h9kbf4sU-RTX4DCHjm8H4pfJx798X0oL6dezirq91QK3gizfR3wboLfEmtA4i-pnTpAOnfOoL9aqKoAweUp83uXJvbPDZjanoJsQhCTiaJprvUtF1CFBWDwtJBWR93Ki9TfBSk3CqTig");
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withRoutingContextHolder(routingContext)
            .withClaimToMiddleware("sub", "X-Uniport-Tenant")
            .build().start();

        // when
        gateway.incomingRequest(GET, "/", new RequestOptions().setHeaders(headers), (outgoingResponse) -> {
            // then
            VertxAssertions.assertEquals(testCtx, "ab159413-8924-4f10-8db1-1794f431e72b", routingContext.get().request().headers().get("X-Uniport-Tenant"));
            testCtx.completeNow();
        });
    }

    @Test
    public void withAuthorizationBearerValue_Syntax(Vertx vertx, VertxTestContext testCtx) {
        // given
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add(HttpHeaders.AUTHORIZATION,
            "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ1Y0p4dWNXRDFWZnFSNU56QmtKZng2RnNZYmJHeEcxOHk5bVZrazFYYWJZIn0.eyJleHAiOjE2MTE5Nzg2MjAsImlhdCI6MTYxMTkzNTQyMCwianRpIjoiOWY0MGNiZTEtZDJhMC00N2Q0LWFlNjYtYTI5ZTQyNDg4ZTFmIiwiaXNzIjoiaHR0cDovL2RvY2tlci10ZXN0LmludmVudGFnZS5jb206ODA4MC9hdXRoL3JlYWxtcy9wb3J0YWwiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiYWIxNTk0MTMtODkyNC00ZjEwLThkYjEtMTc5NGY0MzFlNzJiIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiT3JnYW5pc2F0aW9uIiwic2Vzc2lvbl9zdGF0ZSI6IjU3MWEzNTU3LWNiOWMtNDg4ZS05YzMwLWQyZTAwYjQ4MzRkMCIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7Ik9yZ2FuaXNhdGlvbiI6eyJyb2xlcyI6WyJLRVlDTE9BSyJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJzZXJ2aWNlLWFjY291bnQtb3JnYW5pc2F0aW9uIn0.XkgBLTzgUHvmYFu96d55aZdSpkzKEGO6cyQxMfSZt3Ryfp5gLhLywtdXSyaHJ4qgDHDkB3rGcY6NH2tAT4wwDRSCz_4gzOZV5qkwirdvQ8OXwi-Oa16yPjFgj3I8Mn1UO7QkchiiIZ-2J9roVLPhet5McgBSkGB5YyzlWRyFIr2imM5JKuBjT2zVM6HiqzpvRoW11lQBR1h9kbf4sU-RTX4DCHjm8H4pfJx798X0oL6dezirq91QK3gizfR3wboLfEmtA4i-pnTpAOnfOoL9aqKoAweUp83uXJvbPDZjanoJsQhCTiaJprvUtF1CFBWDwtJBWR93Ki9TfBSk3CqTig");
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withRoutingContextHolder(routingContext)
            .withClaimToMiddleware("$.sub", "X-Uniport-Tenant")
            .build().start();

        // when
        gateway.incomingRequest(GET, "/", new RequestOptions().setHeaders(headers), (outgoingResponse) -> {
            // then
            VertxAssertions.assertEquals(testCtx, "ab159413-8924-4f10-8db1-1794f431e72b", routingContext.get().request().headers().get("X-Uniport-Tenant"));
            testCtx.completeNow();
        });
    }

    @Test
    public void withAuthorizationBearerValue_SyntaxBracket(Vertx vertx, VertxTestContext testCtx) {
        // given
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add(HttpHeaders.AUTHORIZATION,
            "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ1Y0p4dWNXRDFWZnFSNU56QmtKZng2RnNZYmJHeEcxOHk5bVZrazFYYWJZIn0.eyJleHAiOjE2MTE5Nzg2MjAsImlhdCI6MTYxMTkzNTQyMCwianRpIjoiOWY0MGNiZTEtZDJhMC00N2Q0LWFlNjYtYTI5ZTQyNDg4ZTFmIiwiaXNzIjoiaHR0cDovL2RvY2tlci10ZXN0LmludmVudGFnZS5jb206ODA4MC9hdXRoL3JlYWxtcy9wb3J0YWwiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiYWIxNTk0MTMtODkyNC00ZjEwLThkYjEtMTc5NGY0MzFlNzJiIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiT3JnYW5pc2F0aW9uIiwic2Vzc2lvbl9zdGF0ZSI6IjU3MWEzNTU3LWNiOWMtNDg4ZS05YzMwLWQyZTAwYjQ4MzRkMCIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7Ik9yZ2FuaXNhdGlvbiI6eyJyb2xlcyI6WyJLRVlDTE9BSyJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJzZXJ2aWNlLWFjY291bnQtb3JnYW5pc2F0aW9uIn0.XkgBLTzgUHvmYFu96d55aZdSpkzKEGO6cyQxMfSZt3Ryfp5gLhLywtdXSyaHJ4qgDHDkB3rGcY6NH2tAT4wwDRSCz_4gzOZV5qkwirdvQ8OXwi-Oa16yPjFgj3I8Mn1UO7QkchiiIZ-2J9roVLPhet5McgBSkGB5YyzlWRyFIr2imM5JKuBjT2zVM6HiqzpvRoW11lQBR1h9kbf4sU-RTX4DCHjm8H4pfJx798X0oL6dezirq91QK3gizfR3wboLfEmtA4i-pnTpAOnfOoL9aqKoAweUp83uXJvbPDZjanoJsQhCTiaJprvUtF1CFBWDwtJBWR93Ki9TfBSk3CqTig");
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withRoutingContextHolder(routingContext)
            .withClaimToMiddleware("$.resource_access.Organisation.roles", "roles")
            .build().start();

        // when
        gateway.incomingRequest(GET, "/", new RequestOptions().setHeaders(headers), (outgoingResponse) -> {
            // then
            VertxAssertions.assertEquals(testCtx, "[\"KEYCLOAK\"]", routingContext.get().request().headers().get("roles"));
            testCtx.completeNow();
        });
    }

    @Test
    public void withAuthorizationBearerNoValue(Vertx vertx, VertxTestContext testCtx) {
        // given
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withRoutingContextHolder(routingContext)
            .withClaimToMiddleware("sub", "X-Uniport-Tenant")
            .build().start();

        // when
        gateway.incomingRequest(GET, "/", new RequestOptions().setHeaders(headers), (outgoingResponse) -> {
            // then
            VertxAssertions.assertFalse(testCtx, routingContext.get().request().headers().contains("X-Uniport-Tenant"));
            testCtx.completeNow();
        });
    }

    @Test
    public void withAuthorizationBearerInvalidValue(Vertx vertx, VertxTestContext testCtx) {
        // given
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer invalidValueForJWT");
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withRoutingContextHolder(routingContext)
            .withClaimToMiddleware("sub", "X-Uniport-Tenant")
            .build().start();

        // when
        gateway.incomingRequest(GET, "/", new RequestOptions().setHeaders(headers), (outgoingResponse) -> {
            // then
            VertxAssertions.assertFalse(testCtx, routingContext.get().request().headers().contains("X-Uniport-Tenant"));
            testCtx.completeNow();
        });
    }

    @Test
    public void withAuthorizationBasicValue(Vertx vertx, VertxTestContext testCtx) {
        // given
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add(HttpHeaders.AUTHORIZATION, "Basic ldUIiwia2lkIiA6ICJ1Y0p4dW");
        final AtomicReference<RoutingContext> routingContext = new AtomicReference<>();
        MiddlewareServer gateway = portalGateway(vertx, testCtx)
            .withRoutingContextHolder(routingContext)
            .withClaimToMiddleware("sub", "X-Uniport-Tenant")
            .build().start();

        // when
        gateway.incomingRequest(GET, "/", new RequestOptions().setHeaders(headers), (outgoingResponse) -> {
            // then
            VertxAssertions.assertFalse(testCtx, routingContext.get().request().headers().contains("X-Uniport-Tenant"));
            testCtx.completeNow();
        });
    }

}

/**
 * {
 * "exp": 1611978620,
 * "iat": 1611935420,
 * "jti": "9f40cbe1-d2a0-47d4-ae66-a29e42488e1f",
 * "iss": "http://docker-test.inventage.com:8080/auth/realms/portal",
 * "aud": "account",
 * "sub": "ab159413-8924-4f10-8db1-1794f431e72b",
 * "typ": "Bearer",
 * "azp": "Organisation",
 * "session_state": "571a3557-cb9c-488e-9c30-d2e00b4834d0",
 * "acr": "1",
 * "realm_access": {
 * "roles": [
 * "offline_access",
 * "uma_authorization"
 * ]
 * },
 * "resource_access": {
 * "Organisation": {
 * "roles": [
 * "KEYCLOAK"
 * ]
 * },
 * "account": {
 * "roles": [
 * "manage-account",
 * "manage-account-links",
 * "view-profile"
 * ]
 * }
 * },
 * "scope": "profile email",
 * "email_verified": false,
 * "preferred_username": "service-account-organisation"
 * }
 */