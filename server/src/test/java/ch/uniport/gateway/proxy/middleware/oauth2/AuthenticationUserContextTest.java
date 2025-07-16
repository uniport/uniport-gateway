package ch.uniport.gateway.proxy.middleware.oauth2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.impl.OAuth2AuthProviderImpl;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class AuthenticationUserContextTest {

    @Test
    void serializeAndDeserialize(Vertx vertx, VertxTestContext testCtx) {
        // given
        OAuth2Options options = new OAuth2Options()
            // we set some config options the keycloak discovery also sets {@link OpenIDConnectAuth}
            .setClientId("testClient")
            .setAuthorizationPath("/test/authorization")
            .setTokenPath("/test/token")
            .setLogoutPath("/test/logout")
            .setRevocationPath("/test/revocation")
            .setUserInfoPath("/test/userinfo")
            .setJwkPath("/test/jwk")
            .setIntrospectionPath("/test/introspect");
        OAuth2Auth oauth2 = OAuth2Auth.create(vertx, options);

        User user = User.fromName("test");

        AuthenticationUserContext authContext = AuthenticationUserContext.of(oauth2, user);

        Buffer buffer = Buffer.buffer();

        // when
        authContext.writeToBuffer(buffer);
        final AuthenticationUserContext actualAuthContext = new AuthenticationUserContext();
        actualAuthContext.readFromBuffer(0, buffer);

        // then
        assertEquals(
            ((OAuth2AuthProviderImpl) authContext.getAuthenticationProvider(vertx)).getConfig().toJson(),
            ((OAuth2AuthProviderImpl) actualAuthContext.getAuthenticationProvider(vertx)).getConfig().toJson(),
            String.format("Serialized and deserialized authentication context oauth2 provider config is not equal to original."));
        assertEquals(authContext.getUser(), actualAuthContext.getUser(),
            String.format("Serialized and deserialized authentication context user is not equal to original."));
        assertEquals(authContext.getSessionScope(), actualAuthContext.getSessionScope(),
            String.format("Serialized and deserialized authentication context session scope is not equal to original."));

        testCtx.completeNow();
    }

}
