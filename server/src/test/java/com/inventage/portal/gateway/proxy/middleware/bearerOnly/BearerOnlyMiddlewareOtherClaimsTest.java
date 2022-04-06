package com.inventage.portal.gateway.proxy.middleware.bearerOnly;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker.JWTAuthClaim;
import com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker.JWTClaimOptions;
import com.inventage.portal.gateway.proxy.middleware.mock.TestBearerOnlyJWTProvider;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static com.inventage.portal.gateway.proxy.middleware.MiddlewareServerBuilder.httpServer;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A series of tests to check that the implementation of custom claims behaves correctly.
 */
@ExtendWith(VertxExtension.class)
public class BearerOnlyMiddlewareOtherClaimsTest {

    // necessary for jaeger (OpenTracing)
    static {
        System.setProperty("JAEGER_SERVICE_NAME", "portal-gateway");
    }

    private static final String publicKeyRS256 = "-----BEGIN PUBLIC KEY-----\n" + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuFJ0A754CTB9+mhomn9Z\n" + "1aVCiSliTm7Mow3PkWko7PCRVshrqqJEHNg6fgl4KNH+u0ZBjq4L5AKtTuwhsx2v\n" + "IcJ8aJ3mQNdyxFU02nLaNzOVm+rOwytUPflAnYIgqinmiFpqyQ8vwj/L82F5kN5h\n" + "nB+G2heMXSep4uoq++2ogdyLtRi4CCr2tuFdPMcdvozsafRJjgJrmKkGggoembuI\n" + "N5mvuJ/YySMmE3F+TxXOVbhZqAuH4A2+9l0d1rbjghJnv9xCS8Tc7apusoK0q8jW\n" + "yBHp6p12m1IFkrKSSRiXXCmoMIQO8ZTCzpyqCQEgOXHKvxvSPRWsSa4GZWHzH3hv\n" + "RQIDAQAB\n" + "-----END PUBLIC KEY-----";

    /**
     * Corresponding private key
     * -----BEGIN PRIVATE KEY-----
     * MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC4UnQDvngJMH36
     * aGiaf1nVpUKJKWJObsyjDc+RaSjs8JFWyGuqokQc2Dp+CXgo0f67RkGOrgvkAq1O
     * 7CGzHa8hwnxoneZA13LEVTTacto3M5Wb6s7DK1Q9+UCdgiCqKeaIWmrJDy/CP8vz
     * YXmQ3mGcH4baF4xdJ6ni6ir77aiB3Iu1GLgIKva24V08xx2+jOxp9EmOAmuYqQaC
     * Ch6Zu4g3ma+4n9jJIyYTcX5PFc5VuFmoC4fgDb72XR3WtuOCEme/3EJLxNztqm6y
     * grSryNbIEenqnXabUgWSspJJGJdcKagwhA7xlMLOnKoJASA5ccq/G9I9FaxJrgZl
     * YfMfeG9FAgMBAAECggEAUgJlmfDZ6YTI2Gwx9mOpLbSMyQg/tBP6OqX/b3wxeFKE
     * 5+7ecJon4gmW5NMDwm3Ef8B/lKL9qWJDh/Tp/Y68iDHHNqjidIEnkBE1JeiqDJuH
     * +kpb2lxh6/0Fcc8fB3cDGjHYh0KZhjzqWoxJEVrQZS1ly41kp2HpZYu4ukSAj941
     * SvKjrmiiXW1aheCJ59DaFcd2n8oNNKHi7CUoBQbxtP2IkUAdbHGQIETLzfxmw3W3
     * 6ZrTzhD0AOK02V5y5mw+VmiggrYtYRxuX2cRzJ+Ejye0sdLBMOlmaQPO6xiPnRFd
     * aPPSaCx5QIAg8gSFWd438n6cAyLycW+uH816T4659QKBgQDy/He7AaPQ2tUoYDND
     * 8fal0lytqAUXAfhaVjPOgg9XLf8n/26MRrBD1/8HqTQG5y6zqZkEycaysFOUXdoK
     * oX1cboFKGdYIrVWtcxLVUswJGJeEu430xY1D6/Nzc2gXwEmwRbhT1CpWzxY51tZ3
     * q1sVcJ6bWqWwKDiwHjrPf02bBwKBgQDCMad4iw0Ll0WdojGTVQoT2+Q7g+9KyOYk
     * r70qxPhzwwzDc7A+aeSL2AnAFMp7BoO4EWeIve6aZ9B5EArOJCFKDM5DEC/9SK6X
     * OxZYvChLle11xbiFLtS29E+juuhtFmatFk4Dyo5p8+gWkvS7VvLhg36bGquQ4Kf4
     * 00JERHB0UwKBgExG3tsQl7kviOyEznMM2O2TDM7iyL5BOxI4r0irYV9vrAKFV9Gn
     * OxwBCSkBf7iPCAUUP1nWcY6UdZhEofbmXPEQK3v0glD1AMlTL11SAYT1eFMNgXGO
     * NltVDXZ3ivyxuzAfos6F8siPd52uiGLGovAnC1MfcJXM+oam5rVOdDEvAoGBAIhu
     * UCU9M9YP2gXsPH4xAZqAIzG4+HYG/MGLghA5QAA8aYwrjAfdZ4bkKb18HeEe6413
     * FNEZ9zcddnaHUcAP3B9lLcgp6D0/QgHXrlR7JKgt1h2m4oMXKrS6ofT6zG5PjaVP
     * BVJejX1csZKifjPb21mag2k+7IglfX0wFt9VYdgnAoGBAOkt6v8bx/ziO2tgax5i
     * 4nTJ6Ue3wAdGvQrreH++YhdmQpbgZlbQZ5TQsG/yq+oh2sPx2ayXnl6m8NEVraev
     * t4JI1wFsHUdwK6QLFfyKVwY4vk7f7FPpwAqZWhq0nqATJGaGMpeIElHxZBA6DpLN
     * 9iyB7vI2V2sW0bgB4ngnS3Nq
     * -----END PRIVATE KEY-----
     */
    private static final String publicKeyAlgorithm = "RS256";


    /**
     * payload='{
     * "typ": "Bearer",
     * "exp": 1893452400,
     * "iat": 1627053747,
     * "iss": "http://test.issuer:1234/auth/realms/test",
     * "azp": "test-authorized-parties",
     * "aud": "test-aud",
     * "scope": "openid email profile Test",
     * "organisation": "portal",
     * "email-verified": false,
     * "acr": 1,
     * "users": {
     * "bla": [
     * 1,
     * 2,
     * 5,
     * 6
     * ],
     * "data": [
     * {
     * "name": "alice",
     * "age": 42,
     * "email": "alice@test.ch"
     * },
     * {
     * "name": "bob",
     * "age": 43,
     * "email": "bob@test.ch"
     * },
     * {
     * "name": "eve",
     * "age": 20,
     * "email": "eve@test.ch"
     * }
     * ]
     * },
     * "lotto": [
     * 1,
     * 5,
     * 12,
     * 22,
     * 43,
     * 44
     * ],
     * "http://hasura.io/jwt/claims": {
     * "x-hasura-portaluser-id": "1234",
     * "x-hasura-allowed-roles": [
     * "KEYCLOAK",
     * "portaluser"
     * ]
     * },
     * "resource_access": {
     * "Organisation": {
     * "roles": "TENANT"
     * }
     * }
     * }'
     */
    private static final String validToken =
            "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0eXAiOiJCZWFyZXIiLCJleHAiOjE4OTM0NTI0MDAsImlhdCI6MTYyNzA1Mzc0NywiaXNzIjoiaHR0cDovL3Rlc3QuaXNzdWVyOjEyMzQvYXV0aC9yZWFsbXMvdGVzdCIsImF6cCI6InRlc3QtYXV0aG9yaXplZC1wYXJ0aWVzIiwiYXVkIjoiT3JnYW5pc2F0aW9uIiwic2NvcGUiOiJvcGVuaWQgZW1haWwgcHJvZmlsZSBUZXN0Iiwib3JnYW5pc2F0aW9uIjoicG9ydGFsIiwiZW1haWwtdmVyaWZpZWQiOmZhbHNlLCJhY3IiOjEsInVzZXJzIjp7ImJsYSI6WzEsMiw1LDZdLCJkYXRhIjpbeyJuYW1lIjoiYWxpY2UiLCJhZ2UiOjQyLCJlbWFpbCI6ImFsaWNlQHRlc3QuY2gifSx7Im5hbWUiOiJib2IiLCJhZ2UiOjQzLCJlbWFpbCI6ImJvYkB0ZXN0LmNoIn0seyJuYW1lIjoiZXZlIiwiYWdlIjoyMCwiZW1haWwiOiJldmVAdGVzdC5jaCJ9XX0sImxvdHRvIjpbMSw1LDEyLDIyLDQzLDQ0XSwiaHR0cDovL2hhc3VyYS5pby9qd3QvY2xhaW1zIjp7IngtaGFzdXJhLXBvcnRhbHVzZXItaWQiOiIxMjM0IiwieC1oYXN1cmEtYWxsb3dlZC1yb2xlcyI6WyJLRVlDTE9BSyIsInBvcnRhbHVzZXIiXX0sInJlc291cmNlX2FjY2VzcyI6eyJPcmdhbmlzYXRpb24iOnsicm9sZXMiOiJURU5BTlQifX19.ktCaVHgzgjHMUFSZxqft1AiyTc-5VIn6xj9vYl0aPn2Bgee0Nydupy0_qfwgvSTF7nlui4xhhn9WYhmmKFn30oe_dA20cJHJUyO9MiYe19gExicbNeT0P8PdwBD5uJjG7ngKa0E6Z0aQCCyCLp623Q28_bTwYNRghPAu27Ov59aPwVHIA9uq3rwy5cxvbFyheb5dZ_9J5O1ftrTeJ4jDfkxSB3P0K_DUT1KpZrEb7L03vqt6efWuTo9MchW1zfZDc-K0OBXK0cxGg95GZsvMUJE0EGyzKWq2AaXqLEWes266a0QFr1MdQd10-KyiYcJcswOH2UtRxl7MwTODszTCzA";

    //Testing the whitespace contains function. Removed profile & test from the validToken scope entry.
    private static final String validTokenContainsWhitespaceScope =
            "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsInByb3BYIjp0cnVlfQ.eyJ0eXAiOiJCZWFyZXIiLCJleHAiOjE4OTM0NTI0MDAsImlhdCI6MTYyNzA1Mzc0NywiaXNzIjoiaHR0cDovL3Rlc3QuaXNzdWVyOjEyMzQvYXV0aC9yZWFsbXMvdGVzdCIsImF6cCI6InRlc3QtYXV0aG9yaXplZC1wYXJ0aWVzIiwiYXVkIjoiT3JnYW5pc2F0aW9uIiwic2NvcGUiOiJvcGVuaWQgZW1haWwiLCJvcmdhbmlzYXRpb24iOiJwb3J0YWwiLCJlbWFpbC12ZXJpZmllZCI6ZmFsc2UsImFjciI6MSwidXNlcnMiOnsiYmxhIjpbMSwyLDUsNl0sImRhdGEiOlt7Im5hbWUiOiJhbGljZSIsImFnZSI6NDIsImVtYWlsIjoiYWxpY2VAdGVzdC5jaCJ9LHsibmFtZSI6ImJvYiIsImFnZSI6NDMsImVtYWlsIjoiYm9iQHRlc3QuY2gifSx7Im5hbWUiOiJldmUiLCJhZ2UiOjIwLCJlbWFpbCI6ImV2ZUB0ZXN0LmNoIn1dfSwibG90dG8iOlsxLDUsMTIsMjIsNDMsNDRdLCJodHRwOi8vaGFzdXJhLmlvL2p3dC9jbGFpbXMiOnsieC1oYXN1cmEtcG9ydGFsdXNlci1pZCI6IjEyMzQiLCJ4LWhhc3VyYS1hbGxvd2VkLXJvbGVzIjpbIktFWUNMT0FLIiwicG9ydGFsdXNlciJdfSwicmVzb3VyY2VfYWNjZXNzIjp7Ik9yZ2FuaXNhdGlvbiI6eyJyb2xlcyI6IlRFTkFOVCJ9fX0.S0g3GT6_vK-bKwH1T-1Dwnn6KOO18BllfvN8R2aVPUoATkDIroiHWbCYUZLzqkg8YRdos87Z-qv9QjgBmTzwGo4XYEdbWwUk4S81Ny1oxKvgd7i9HWAlnp9h_jOcy1VHMAN9Y_CxH4HJxirOjePPx8PCwSEf9RYgg3dZQfbrGgB5DKbzvnx-Nxr8GzyLqbLwj0TUYH0Md2jAVea_1jotgDPVMNqrVheIrmOwiDo8hz4TYscjmArDaEKl36_u0uX9Dt6KVUFs2rISkK9nRGqfMjg412GG0lMbDh0uvNNnUDx0qt6MPNTJ45wvnhv7-T6bg37jTFwo4P8VbAfDQ6q6PQ";

    //payload: Key "resource_access" replaced with "resource"
    private static final String invalidPathKeyToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0eXAiOiJCZWFyZXIiLCJleHAiOjE4OTM0NTI0MDAsImlhdCI6MTYyNzA1Mzc0NywiaXNzIjoiaHR0cDovL3Rlc3QuaXNzdWVyOjEyMzQvYXV0aC9yZWFsbXMvdGVzdCIsImF6cCI6InRlc3QtYXV0aG9yaXplZC1wYXJ0aWVzIiwiYXVkIjoiT3JnYW5pc2F0aW9uIiwic2NvcGUiOiJvcGVuaWQgZW1haWwgcHJvZmlsZSBUZXN0Iiwib3JnYW5pc2F0aW9uIjoicG9ydGFsIiwiZW1haWwtdmVyaWZpZWQiOmZhbHNlLCJhY3IiOjEsInVzZXJzIjp7ImJsYSI6WzEsMiw1LDZdLCJkYXRhIjpbeyJuYW1lIjoiYWxpY2UiLCJhZ2UiOjQyLCJlbWFpbCI6ImFsaWNlQHRlc3QuY2gifSx7Im5hbWUiOiJib2IiLCJhZ2UiOjQzLCJlbWFpbCI6ImJvYkB0ZXN0LmNoIn0seyJuYW1lIjoiZXZlIiwiYWdlIjoyMCwiZW1haWwiOiJldmVAdGVzdC5jaCJ9XX0sImxvdHRvIjpbMSw1LDEyLDIyLDQzLDQ0XSwiaHR0cDovL2hhc3VyYS5pby9qd3QvY2xhaW1zIjp7IngtaGFzdXJhLXBvcnRhbHVzZXItaWQiOiIxMjM0IiwieC1oYXN1cmEtYWxsb3dlZC1yb2xlcyI6WyJLRVlDTE9BSyIsInBvcnRhbHVzZXIiXX0sInJlc291cmNlIjp7Ik9yZ2FuaXNhdGlvbiI6eyJyb2xlcyI6IlRFTkFOVCJ9fX0.PSYvx19cqVTAGZehTuRayyjpeOlRJixwa90j8wafDGoX3nyZBR7Z1QXUEgP7E_c8vnZzsS5NK06LsByTZfH2i0kuUf8ULNvfrktsDfMG-Ow0B5QyXaTSHcwQAOTY2j9WmFdOalZ8GUKFdspHr5PFRsfxFsJNgrVyOWtaakZLPSK3XD_Son6-tIhHXHwCGbdpcOHsW96C_mCX_hTYppzAVOWdWSSe8mxqSTgJ2_CwAxc4jYiMpaIzdx4tdUEiZAmu7wK_CS50rxbflYNu9asr8m8OocZbZx75LwN1_5nfpv89wTCzc2wHFca2K4ZEnhsBfY2J05SYtKt9-fx-xf3kBg";
    //payload: "email-verified" value replaced with true
    private static final String invalidBooleanValueToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0eXAiOiJCZWFyZXIiLCJleHAiOjE4OTM0NTI0MDAsImlhdCI6MTYyNzA1Mzc0NywiaXNzIjoiaHR0cDovL3Rlc3QuaXNzdWVyOjEyMzQvYXV0aC9yZWFsbXMvdGVzdCIsImF6cCI6InRlc3QtYXV0aG9yaXplZC1wYXJ0aWVzIiwiYXVkIjoiT3JnYW5pc2F0aW9uIiwic2NvcGUiOiJvcGVuaWQgZW1haWwgcHJvZmlsZSBUZXN0Iiwib3JnYW5pc2F0aW9uIjoicG9ydGFsIiwiZW1haWwtdmVyaWZpZWQiOnRydWUsImFjciI6MSwidXNlcnMiOnsiYmxhIjpbMSwyLDUsNl0sImRhdGEiOlt7Im5hbWUiOiJhbGljZSIsImFnZSI6NDIsImVtYWlsIjoiYWxpY2VAdGVzdC5jaCJ9LHsibmFtZSI6ImJvYiIsImFnZSI6NDMsImVtYWlsIjoiYm9iQHRlc3QuY2gifSx7Im5hbWUiOiJldmUiLCJhZ2UiOjIwLCJlbWFpbCI6ImV2ZUB0ZXN0LmNoIn1dfSwibG90dG8iOlsxLDUsMTIsMjIsNDMsNDRdLCJodHRwOi8vaGFzdXJhLmlvL2p3dC9jbGFpbXMiOnsieC1oYXN1cmEtcG9ydGFsdXNlci1pZCI6IjEyMzQiLCJ4LWhhc3VyYS1hbGxvd2VkLXJvbGVzIjpbIktFWUNMT0FLIiwicG9ydGFsdXNlciJdfSwicmVzb3VyY2VfYWNjZXNzIjp7Ik9yZ2FuaXNhdGlvbiI6eyJyb2xlcyI6IlRFTkFOVCJ9fX0.Eqf5mgfx7gQeIlirXjxUj1-bHZNT-rGJkOErSAqdpc0kVDynhhd0IGa8u7aK77w0AFZUxVN3785VOu1q_nfAh8Ev4IAgUcbtED9NzRh8ctLFmClKWmZKcHgIW_kbqMaMBgrdJVWVU4nOJ5dZKvfmn5PAKBe2Tadaih0aQ9jscHdaXwaxTnLirwvA0XGfFtl0su8snafnRHzNxdNo4bkgjZB4eriD7Z-zcLxHfYDNPPeHdwtTsKP5C1yGPVEqKrgeUlawN0qlK5eg93WpGjwuijKzD8zCy327JWrYkx5lv_2a-Qe6gMet1Q56KQijNgfmIAYIo-nGL3JNabKYXJiqiQ";
    //payload: Add additional role in the hasura claim entry. "x-hasura-allowed-roles" : ["KEYCLOAK","portaluser"] with ["KEYCLOAK","portaluser", "ADMIN"]
    private static final String invalidHasuraClaimsToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0eXAiOiJCZWFyZXIiLCJleHAiOjE4OTM0NTI0MDAsImlhdCI6MTYyNzA1Mzc0NywiaXNzIjoiaHR0cDovL3Rlc3QuaXNzdWVyOjEyMzQvYXV0aC9yZWFsbXMvdGVzdCIsImF6cCI6InRlc3QtYXV0aG9yaXplZC1wYXJ0aWVzIiwiYXVkIjoiT3JnYW5pc2F0aW9uIiwic2NvcGUiOiJvcGVuaWQgZW1haWwgcHJvZmlsZSBUZXN0Iiwib3JnYW5pc2F0aW9uIjoicG9ydGFsIiwiZW1haWwtdmVyaWZpZWQiOmZhbHNlLCJhY3IiOjEsInVzZXJzIjp7ImJsYSI6WzEsMiw1LDZdLCJkYXRhIjpbeyJuYW1lIjoiYWxpY2UiLCJhZ2UiOjQyLCJlbWFpbCI6ImFsaWNlQHRlc3QuY2gifSx7Im5hbWUiOiJib2IiLCJhZ2UiOjQzLCJlbWFpbCI6ImJvYkB0ZXN0LmNoIn0seyJuYW1lIjoiZXZlIiwiYWdlIjoyMCwiZW1haWwiOiJldmVAdGVzdC5jaCJ9XX0sImxvdHRvIjpbMSw1LDEyLDIyLDQzLDQ0XSwiaHR0cDovL2hhc3VyYS5pby9qd3QvY2xhaW1zIjp7IngtaGFzdXJhLXBvcnRhbHVzZXItaWQiOiIxMjM0IiwieC1oYXN1cmEtYWxsb3dlZC1yb2xlcyI6WyJLRVlDTE9BSyIsIkFETUlOIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiT3JnYW5pc2F0aW9uIjp7InJvbGVzIjoiVEVOQU5UIn19fQ.hs3joji_2sgzAui5zocpPp94NDkGkemzed7KZmka-J0-2Cux3NDT3vNZvI65ZYz-6MIDRc6MvDpo-PwOsDbhr6rhZT_EifcM1LNAniAGG4DsivMVWAwnd03jybzkxjQ0LflwFu0KjJHkuNDUy8SuVcQiFC3zLSV3AYHL1dWLbXBLJR0BYKqN0XFlhiwt8d6Ol8E2nGt85LE9KQ1QuZXHWxlnxgGYh7jGi-P2SsqgIoVCVSwLXLsH6k7dl1GVUPzkW4zfqpKk4RQkYjBhSuFx5w3VrSsw2qjLfVzxIaJwkG9YSizjIYaFGlY0sMCTHuwb0lveAi5lnrTY4rZPO7q4ug";

    //payload: "resource_access.Organisation.roles" value replaced with Root.  FYI: Testing the contains implementation
    private static final String invalidRoleToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0eXAiOiJCZWFyZXIiLCJleHAiOjE4OTM0NTI0MDAsImlhdCI6MTYyNzA1Mzc0NywiaXNzIjoiaHR0cDovL3Rlc3QuaXNzdWVyOjEyMzQvYXV0aC9yZWFsbXMvdGVzdCIsImF6cCI6InRlc3QtYXV0aG9yaXplZC1wYXJ0aWVzIiwiYXVkIjoiT3JnYW5pc2F0aW9uIiwic2NvcGUiOiJvcGVuaWQgZW1haWwgcHJvZmlsZSBUZXN0Iiwib3JnYW5pc2F0aW9uIjoicG9ydGFsIiwiZW1haWwtdmVyaWZpZWQiOmZhbHNlLCJhY3IiOjEsInVzZXJzIjp7ImJsYSI6WzEsMiw1LDZdLCJkYXRhIjpbeyJuYW1lIjoiYWxpY2UiLCJhZ2UiOjQyLCJlbWFpbCI6ImFsaWNlQHRlc3QuY2gifSx7Im5hbWUiOiJib2IiLCJhZ2UiOjQzLCJlbWFpbCI6ImJvYkB0ZXN0LmNoIn0seyJuYW1lIjoiZXZlIiwiYWdlIjoyMCwiZW1haWwiOiJldmVAdGVzdC5jaCJ9XX0sImxvdHRvIjpbMSw1LDEyLDIyLDQzLDQ0XSwiaHR0cDovL2hhc3VyYS5pby9qd3QvY2xhaW1zIjp7IngtaGFzdXJhLXBvcnRhbHVzZXItaWQiOiIxMjM0IiwieC1oYXN1cmEtYWxsb3dlZC1yb2xlcyI6WyJLRVlDTE9BSyIsInBvcnRhbHVzZXIiXX0sInJlc291cmNlX2FjY2VzcyI6eyJPcmdhbmlzYXRpb24iOnsicm9sZXMiOiJST09UIn19fQ.PJ3PvNMfdOkMcvk_wxhKH-0BzKVEnHXLpPUte6NmCR8x_Tdp_nxM0uRQpAS1PFHL3Z1KR96K1JJUfhhj5BhFvTKqGG_-9Lx4PS8M51mT82Cot1M2jD4PYAnEKqgzh9a2JfmvILLJQ8QQvrQc1RltwS6ixMYvp8st6xF3ELF2iSE5A21Xn9TDg13o15k5sKX55y--gbl-T1s-APp4pX-YmWx0bO5XTf1qImOHKLqN3dtwUQmGvB0JZQWupPuWtcil1MAkVg30TqEXV4aHjnVYiokoo_fRFhzb2cDm-w8ko1Leyf_QzsFPyQGlSUIXyjeTjM0zawMgdk9sxsAUE6bNQw";
    private static final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
    private static final List<String> expectedAudience = List.of("Organisation", "Portal-Gateway");

    /**
     * {
     * "claimPath":"$.organisation",
     * "operator":"EQUALS",
     * "value":"portal"
     * }
     */
    private static final JsonObject claimEqualString = new JsonObject("{\"claimPath\":\"$['organisation']\",\"operator\":\"EQUALS\",\"value\":\"portal\"}");

    /**
     * {
     * "claimPath":"$['resource_access']['Organisation']['roles']",
     * "operator":"CONTAINS",
     * "value":[
     * "ADMINISTRATOR",
     * "TENANT"
     * ]
     * }
     */
    private static final JsonObject claimContainStringArray = new JsonObject("{\"claimPath\":\"$['resource_access']['Organisation']['roles']\",\"operator\":\"CONTAINS\",\"value\":[\"ADMINISTRATOR\",\"TENANT\"]}");

    /**
     * {
     * "claimPath":"$['http://hasura.io/jwt/claims']",
     * "operator":"EQUALS",
     * "value":{
     * "x-hasura-allowed-roles":[
     * "KEYCLOAK",
     * "portaluser"
     * ],
     * "x-hasura-portaluser-id":"1234"
     * }
     * }
     */
    private static final JsonObject claimEqualObject = new JsonObject("{\"claimPath\":\"$['http://hasura.io/jwt/claims']\",\"operator\":\"EQUALS\",\"value\":{\"x-hasura-allowed-roles\":[\"KEYCLOAK\",\"portaluser\"],\"x-hasura-portaluser-id\":\"1234\"}}");

    /**
     * {
     * "claimPath":"$['email-verified']",
     * "operator":"EQUALS",
     * "value":false
     * }
     */
    private static final JsonObject claimEqualBoolean = new JsonObject("{\"claimPath\":\"$['email-verified']\",\"operator\":\"EQUALS\",\"value\":false}");

    /**
     * {
     * "claimPath":"$['acr']",
     * "operator":"EQUALS",
     * "value":1
     * }
     */
    private static final JsonObject claimEqualInteger = new JsonObject("{\"claimPath\":\"$['acr']\",\"operator\":\"EQUALS\",\"value\":1}");

    /**
     * "{
     *  "claimPath":"$['acr']",
     *  "operator":"EQUALS",
     *  "value":1
     *  }"
     */
    private static final JsonObject claimEqualObjectDifferentOrder = new JsonObject("{\"claimPath\":\"$['users']\",\"operator\":\"EQUALS\",\"value\":{\n" + "     \"data\":[\n" + "     {\"name\":\"alice\", \"age\":42, \"email\": \"alice@test.ch\"},\n" + "     {\"name\":\"bob\", \"age\": 43, \"email\": \"bob@test.ch\"},\n" + "     {\"name\":\"eve\", \"age\": 20, \"email\": \"eve@test.ch\"}\n" + "     ],\n" + "     \"bla\":[1,2,5,6]    \n" + "     }}");

    /**
     * {
     * "claimPath":"$['lotto']",
     * "operator":"CONTAINS",
     * "value":[1,5,12,22,43,44,50,59]
     * }
     */
    private static final JsonObject claimContainIntegerArray = new JsonObject("{\"claimPath\":\"$['lotto']\",\"operator\":\"CONTAINS\",\"value\":[1,5,12,22,43,44,50,59]}");
    /**
     * {
     * "claimPath":"$['acr']",
     * "operator":"CONTAINS",
     * "value":[1,9]
     * }
     */
    private static final JsonObject claimContainInteger = new JsonObject("{\"claimPath\":\"$['acr']\",\"operator\":\"CONTAINS\",\"value\":[1,9]}");
    /**
     * {
     * "claimPath":"$['scope']",
     * "operator":"CONTAINS_SUBSTRING_WHITESPACE",
     * "value":" + "["openid", "email", "profile", "Test"]
     * }
     */
    private static final JsonObject claimContainSubstringWhitespace = new JsonObject("{\"claimPath\":\"$['scope']\",\"operator\":\"CONTAINS_SUBSTRING_WHITESPACE\",\"value\":" + "[\"openid\", \"email\", \"profile\", \"Test\"]}");


    /**
     * claimPath is defined according to the Jsonpath standard. To know more on how to formulate specific path or queries, please refer to:
     * https://github.com/json-path/JsonPath
     * or
     */



    private static final JsonArray claims = new JsonArray(List.of(claimEqualString, claimContainStringArray, claimEqualObject, claimEqualBoolean, claimEqualInteger, claimEqualObjectDifferentOrder, claimContainIntegerArray, claimContainInteger, claimContainSubstringWhitespace));

    private static final JsonObject validPayloadTemplate = new JsonObject("{\n" +
            "  \"typ\": \"Bearer\",\n" +
            "  \"exp\": 1893452400,\n" +
            "  \"iat\": 1627053747,\n" +
            "  \"iss\": \"http://test.issuer:1234/auth/realms/test\",\n" +
            "  \"azp\": \"test-authorized-parties\",\n" +
            "  \"aud\": \"Organisation\",\n" +
            "  \"scope\": \"openid email profile Test\",\n" +
            "  \"organisation\": \"portal\",\n" +
            "  \"email-verified\": false,\n" +
            "  \"acr\": 1,\n" +
            "  \"users\": {\n" +
            "    \"bla\": [\n" +
            "      1,\n" +
            "      2,\n" +
            "      5,\n" +
            "      6\n" +
            "    ],\n" +
            "    \"data\": [\n" +
            "      {\n" +
            "        \"name\": \"alice\",\n" +
            "        \"age\": 42,\n" +
            "        \"email\": \"alice@test.ch\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"name\": \"bob\",\n" +
            "        \"age\": 43,\n" +
            "        \"email\": \"bob@test.ch\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"name\": \"eve\",\n" +
            "        \"age\": 20,\n" +
            "        \"email\": \"eve@test.ch\"\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  \"lotto\": [\n" +
            "    1,\n" +
            "    5,\n" +
            "    12,\n" +
            "    22,\n" +
            "    43,\n" +
            "    44\n" +
            "  ],\n" +
            "  \"http://hasura.io/jwt/claims\": {\n" +
            "    \"x-hasura-portaluser-id\": \"1234\",\n" +
            "    \"x-hasura-allowed-roles\": [\n" +
            "      \"KEYCLOAK\",\n" +
            "      \"portaluser\"\n" +
            "    ]\n" +
            "  },\n" +
            "  \"resource_access\": {\n" +
            "    \"Organisation\": {\n" +
            "      \"roles\": \"TENANT\"\n" +
            "    }\n" +
            "  }\n" +
            "}");
    private int port;

    @BeforeEach
    public void setup() throws Exception {
        port = TestUtils.findFreePort();
    }

    @Test
    public void validToken(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //Throws illegalstateexception: No configproviderresolver implementation found
        String validStringToken = TestBearerOnlyJWTProvider.signToken(validPayloadTemplate.getMap());
        //given
        httpServer(vertx, port).withBearerOnlyMiddlewareOtherClaims(jwtAuth(vertx, expectedIssuer, expectedAudience, claims),false)
                //when
                .doRequest(testCtx, new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(validToken)), (resp) -> {
                    // then
                    assertEquals(200, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    @Test
    public void validTokenContainsWhitespaceScope(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //given
        httpServer(vertx, port).withBearerOnlyMiddlewareOtherClaims(jwtAuth(vertx, expectedIssuer, expectedAudience, claims),false)
                //when
                .doRequest(testCtx, new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(validTokenContainsWhitespaceScope)), (resp) -> {
                    // then
                    assertEquals(200, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    @Test
    public void pathKeyMismatch(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //given
        httpServer(vertx, port).withBearerOnlyMiddlewareOtherClaims(jwtAuth(vertx, expectedIssuer, expectedAudience, claims),false)
                //when
                .doRequest(testCtx, new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(invalidPathKeyToken)), (resp) -> {
                    // then
                    assertEquals(401, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    @Test
    public void booleanValueMismatch(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //given
        httpServer(vertx, port).withBearerOnlyMiddlewareOtherClaims(jwtAuth(vertx, expectedIssuer, expectedAudience, claims),false)
                //when
                .doRequest(testCtx, new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(invalidBooleanValueToken)), (resp) -> {
                    // then
                    assertEquals(401, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    @Test
    public void entryMismatch(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //given
        httpServer(vertx, port).withBearerOnlyMiddlewareOtherClaims(jwtAuth(vertx, expectedIssuer, expectedAudience, claims),false)
                //when
                .doRequest(testCtx, new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(invalidHasuraClaimsToken)), (resp) -> {
                    // then
                    assertEquals(401, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    @Test
    public void entryContainsMismatch(Vertx vertx, VertxTestContext testCtx) throws InterruptedException {
        //given
        httpServer(vertx, port).withBearerOnlyMiddlewareOtherClaims(jwtAuth(vertx, expectedIssuer, expectedAudience, claims),false)
                //when
                .doRequest(testCtx, new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, bearer(invalidRoleToken)), (resp) -> {
                    // then
                    assertEquals(401, resp.statusCode(), "unexpected status code");
                    testCtx.completeNow();
                });
    }

    private JWTAuth jwtAuth(Vertx vertx, String expectedIssuer, List<String> expectedAudience, JsonArray claims) {
        return JWTAuthClaim.create(vertx,
                new JWTAuthOptions()
                        .addPubSecKey(new PubSecKeyOptions().setAlgorithm(publicKeyAlgorithm).setBuffer(publicKeyRS256))
                        .setJWTOptions(new JWTClaimOptions().setOtherClaims(claims).setIssuer(expectedIssuer).setAudience(expectedAudience)));
    }

    private String bearer(String value) {
        return "Bearer " + value;
    }
}