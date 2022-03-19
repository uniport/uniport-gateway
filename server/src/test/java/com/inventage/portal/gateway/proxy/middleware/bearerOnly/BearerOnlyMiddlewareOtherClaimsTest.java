package com.inventage.portal.gateway.proxy.middleware.bearerOnly;

import com.inventage.portal.gateway.TestUtils;
import com.inventage.portal.gateway.proxy.middleware.bearerOnly.customClaimsChecker.*;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class BearerOnlyMiddlewareOtherClaimsTest {

    // necessary for jaeger (OpenTracing)
    static {
        System.setProperty("JAEGER_SERVICE_NAME", "portal-gateway");
    }

    private static final String host = "localhost";


    private static final String publicKeyRS256 =
            "-----BEGIN PUBLIC KEY-----\n" +
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuFJ0A754CTB9+mhomn9Z\n" +
                    "1aVCiSliTm7Mow3PkWko7PCRVshrqqJEHNg6fgl4KNH+u0ZBjq4L5AKtTuwhsx2v\n" +
                    "IcJ8aJ3mQNdyxFU02nLaNzOVm+rOwytUPflAnYIgqinmiFpqyQ8vwj/L82F5kN5h\n" +
                    "nB+G2heMXSep4uoq++2ogdyLtRi4CCr2tuFdPMcdvozsafRJjgJrmKkGggoembuI\n" +
                    "N5mvuJ/YySMmE3F+TxXOVbhZqAuH4A2+9l0d1rbjghJnv9xCS8Tc7apusoK0q8jW\n" +
                    "yBHp6p12m1IFkrKSSRiXXCmoMIQO8ZTCzpyqCQEgOXHKvxvSPRWsSa4GZWHzH3hv\n" +
                    "RQIDAQAB\n" +
                    "-----END PUBLIC KEY-----";
    private static final String publicKeyAlgorithm = "RS256";


    /**
     * payload='{
     *   "typ": "Bearer",
     *   "exp": 1893452400,
     *   "iat": 1627053747,
     *   "iss": "http://test.issuer:1234/auth/realms/test",
     *   "azp": "test-authorized-parties",
     *   "aud": "test-aud",
     *   "scope": "openid email profile Test",
     *   "organisation": "portal",
     *   "email-verified": false,
     *   "acr": 1,
     *   "users": {
     *     "bla": [
     *       1,
     *       2,
     *       5,
     *       6
     *     ],
     *     "data": [
     *       {
     *         "name": "alice",
     *         "age": 42,
     *         "email": "alice@test.ch"
     *       },
     *       {
     *         "name": "bob",
     *         "age": 43,
     *         "email": "bob@test.ch"
     *       },
     *       {
     *         "name": "eve",
     *         "age": 20,
     *         "email": "eve@test.ch"
     *       }
     *     ]
     *   },
     *   "lotto": [
     *     1,
     *     5,
     *     12,
     *     22,
     *     43,
     *     44
     *   ],
     *   "http://hasura.io/jwt/claims": {
     *     "x-hasura-portaluser-id": "1234",
     *     "x-hasura-allowed-roles": [
     *       "KEYCLOAK",
     *       "portaluser"
     *     ]
     *   },
     *   "resource_access": {
     *     "Organisation": {
     *       "roles": "TENANT"
     *     }
     *   }
     * }'
     */
    private static final String validToken =
            "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0eXAiOiJCZWFyZXIiLCJleHAiOjE4OTM0NTI0MDAsImlhdCI6MTYyNzA1Mzc0NywiaXNzIjoiaHR0cDovL3Rlc3QuaXNzdWVyOjEyMzQvYXV0aC9yZWFsbXMvdGVzdCIsImF6cCI6InRlc3QtYXV0aG9yaXplZC1wYXJ0aWVzIiwiYXVkIjoidGVzdC1hdWQiLCJzY29wZSI6Im9wZW5pZCBlbWFpbCBwcm9maWxlIFRlc3QiLCJvcmdhbmlzYXRpb24iOiJwb3J0YWwiLCJlbWFpbC12ZXJpZmllZCI6ZmFsc2UsImFjciI6MSwidXNlcnMiOnsiYmxhIjpbMSwyLDUsNl0sImRhdGEiOlt7Im5hbWUiOiJhbGljZSIsImFnZSI6NDIsImVtYWlsIjoiYWxpY2VAdGVzdC5jaCJ9LHsibmFtZSI6ImJvYiIsImFnZSI6NDMsImVtYWlsIjoiYm9iQHRlc3QuY2gifSx7Im5hbWUiOiJldmUiLCJhZ2UiOjIwLCJlbWFpbCI6ImV2ZUB0ZXN0LmNoIn1dfSwibG90dG8iOlsxLDUsMTIsMjIsNDMsNDRdLCJodHRwOi8vaGFzdXJhLmlvL2p3dC9jbGFpbXMiOnsieC1oYXN1cmEtcG9ydGFsdXNlci1pZCI6IjEyMzQiLCJ4LWhhc3VyYS1hbGxvd2VkLXJvbGVzIjpbIktFWUNMT0FLIiwicG9ydGFsdXNlciJdfSwicmVzb3VyY2VfYWNjZXNzIjp7Ik9yZ2FuaXNhdGlvbiI6eyJyb2xlcyI6IlRFTkFOVCJ9fX0.o_0Q1w0g2ywr3TPOhzy7a7qqS2Fx5GDi2Ovk1C79vcegNFu0POr0RhK5Lvkc-gFELUUTdQJ-cKMNYiKfsgGqjZta1ojstTfy0RGKwMEu_V_HrWvwq3ZU9ogGQzdbpS8GlHhdsV0hKqOPQ_pnShmUlZPowEEw_zVC8WlBZL-St4a7bop-CAKoT7K3bMnCh8LMSXY7Xz1zDn-JvN1VYkhSKf6JjV0aSegoG1LgKF1foezn0JFKDdovZB4eipgw1ktm48fJqOT3DCieSSM8e8dDz7C7rhtQa2RaVuIZsXOTAh2owngiLOGsGWPg4BB6GxObD6VXs6-i3647j-M4ylvKIQ";
    //payload: Key "resource_access" replaced with "resource"
    private static final String invalidPathKeyToken =
        "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0eXAiOiJCZWFyZXIiLCJleHAiOjE4OTM0NTI0MDAsImlhdCI6MTYyNzA1Mzc0NywiaXNzIjoiaHR0cDovL3Rlc3QuaXNzdWVyOjEyMzQvYXV0aC9yZWFsbXMvdGVzdCIsImF6cCI6InRlc3QtYXV0aG9yaXplZC1wYXJ0aWVzIiwiYXVkIjoidGVzdC1hdWQiLCJzY29wZSI6Im9wZW5pZCBlbWFpbCBwcm9maWxlIFRlc3QiLCJvcmdhbmlzYXRpb24iOiJwb3J0YWwiLCJlbWFpbC12ZXJpZmllZCI6ZmFsc2UsImFjciI6MSwidXNlcnMiOnsiYmxhIjpbMSwyLDUsNl0sImRhdGEiOlt7Im5hbWUiOiJhbGljZSIsImFnZSI6NDIsImVtYWlsIjoiYWxpY2VAdGVzdC5jaCJ9LHsibmFtZSI6ImJvYiIsImFnZSI6NDMsImVtYWlsIjoiYm9iQHRlc3QuY2gifSx7Im5hbWUiOiJldmUiLCJhZ2UiOjIwLCJlbWFpbCI6ImV2ZUB0ZXN0LmNoIn1dfSwibG90dG8iOlsxLDUsMTIsMjIsNDMsNDRdLCJodHRwOi8vaGFzdXJhLmlvL2p3dC9jbGFpbXMiOnsieC1oYXN1cmEtcG9ydGFsdXNlci1pZCI6IjEyMzQiLCJ4LWhhc3VyYS1hbGxvd2VkLXJvbGVzIjpbIktFWUNMT0FLIiwicG9ydGFsdXNlciJdfSwicmVzb3VyY2UiOnsiT3JnYW5pc2F0aW9uIjp7InJvbGVzIjoiVEVOQU5UIn19fQ.WQT84V3Ci1Sg4xklEF5htBWoCqDTv_-hUoZlP_SpEVySg87h3RBV6KsqRlSfQq8xcrSGmdqa9BByMag5oTHKIFx5K9Oh0k0sg1-CShtqnPd5Ejhy4e9ljdEAm9U5FGkQQgV5jib1xUCdxYagL8UD3Qqrsn2BPdxz34OS7sdFJIQmAd9ax3570LKomVHCqNfQUxFCQ4Tux4ySZ-2UgHaDoOJQ4f6uRM97h5EW8OPfky3yNukO4HPh1r_6F2U2SnTWkB9jWbGUh4YuHKFIE9kJ1CDI9JN79wTzHMUL-S4faJPzHEzY1cd2U4mDTTBgN12VYBxk6aEHaPYnnFTDc90c2A";
    //payload: "email-verified" value replaced with true
    private static final String invalidBooleanValueToken =
        "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0eXAiOiJCZWFyZXIiLCJleHAiOjE4OTM0NTI0MDAsImlhdCI6MTYyNzA1Mzc0NywiaXNzIjoiaHR0cDovL3Rlc3QuaXNzdWVyOjEyMzQvYXV0aC9yZWFsbXMvdGVzdCIsImF6cCI6InRlc3QtYXV0aG9yaXplZC1wYXJ0aWVzIiwiYXVkIjoidGVzdC1hdWQiLCJzY29wZSI6Im9wZW5pZCBlbWFpbCBwcm9maWxlIFRlc3QiLCJvcmdhbmlzYXRpb24iOiJwb3J0YWwiLCJlbWFpbC12ZXJpZmllZCI6dHJ1ZSwiYWNyIjoxLCJ1c2VycyI6eyJibGEiOlsxLDIsNSw2XSwiZGF0YSI6W3sibmFtZSI6ImFsaWNlIiwiYWdlIjo0MiwiZW1haWwiOiJhbGljZUB0ZXN0LmNoIn0seyJuYW1lIjoiYm9iIiwiYWdlIjo0MywiZW1haWwiOiJib2JAdGVzdC5jaCJ9LHsibmFtZSI6ImV2ZSIsImFnZSI6MjAsImVtYWlsIjoiZXZlQHRlc3QuY2gifV19LCJsb3R0byI6WzEsNSwxMiwyMiw0Myw0NF0sImh0dHA6Ly9oYXN1cmEuaW8vand0L2NsYWltcyI6eyJ4LWhhc3VyYS1wb3J0YWx1c2VyLWlkIjoiMTIzNCIsIngtaGFzdXJhLWFsbG93ZWQtcm9sZXMiOlsiS0VZQ0xPQUsiLCJwb3J0YWx1c2VyIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiT3JnYW5pc2F0aW9uIjp7InJvbGVzIjoiVEVOQU5UIn19fQ.Q8OytQVKKOhOLPQp5BUFfgjmFAtp4emvNLRDDWd-pM6qMH8WE1Lg8ey2CMlNqRXFhhpi4R0NZVZ1ElkG08gMdsXrNsmvyGYKSk3pqVXGRSnUW57o_yVEfhBk5YHjAm76WK_0YXDZfNZ25u8mkEULUEaIveaAmQYAWPMHFPStYY1Zg72IylHOB3XEoUmgWUZIW21nYDwqXKxEdYA-aAc-gAcYtGhqD9LPIjHq_5IoeY1HqCI-1Tphin2D8rRkXKLCALdmtHIlpIsiVrx3eWgLoSvg7rzYpPjm0P3llapfhmMY4ROl3fLhOMkVWoPJLuxukKHmtjWxJmDQEKKQMIZBGQ";

    //payload: Add additional role in the hasura claim entry. "x-hasura-allowed-roles" : ["KEYCLOAK","portaluser"] with ["KEYCLOAK","portaluser", "ADMIN"]
    private static final String invalidHasuraClaimsToken =
        "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0eXAiOiJCZWFyZXIiLCJleHAiOjE4OTM0NTI0MDAsImlhdCI6MTYyNzA1Mzc0NywiaXNzIjoiaHR0cDovL3Rlc3QuaXNzdWVyOjEyMzQvYXV0aC9yZWFsbXMvdGVzdCIsImF6cCI6InRlc3QtYXV0aG9yaXplZC1wYXJ0aWVzIiwiYXVkIjoidGVzdC1hdWQiLCJzY29wZSI6Im9wZW5pZCBlbWFpbCBwcm9maWxlIFRlc3QiLCJvcmdhbmlzYXRpb24iOiJwb3J0YWwiLCJlbWFpbC12ZXJpZmllZCI6ZmFsc2UsImFjciI6MSwidXNlcnMiOnsiYmxhIjpbMSwyLDUsNl0sImRhdGEiOlt7Im5hbWUiOiJhbGljZSIsImFnZSI6NDIsImVtYWlsIjoiYWxpY2VAdGVzdC5jaCJ9LHsibmFtZSI6ImJvYiIsImFnZSI6NDMsImVtYWlsIjoiYm9iQHRlc3QuY2gifSx7Im5hbWUiOiJldmUiLCJhZ2UiOjIwLCJlbWFpbCI6ImV2ZUB0ZXN0LmNoIn1dfSwibG90dG8iOlsxLDUsMTIsMjIsNDMsNDRdLCJodHRwOi8vaGFzdXJhLmlvL2p3dC9jbGFpbXMiOnsieC1oYXN1cmEtcG9ydGFsdXNlci1pZCI6IjEyMzQiLCJ4LWhhc3VyYS1hbGxvd2VkLXJvbGVzIjpbIktFWUNMT0FLIiwicG9ydGFsdXNlciIsIkFETUlOIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiT3JnYW5pc2F0aW9uIjp7InJvbGVzIjoiVEVOQU5UIn19fQ.HKV6CLP550mvpSPXilLrQmHkgh38USHiRSyj4mw2zxj5Fs_8_LiR6iXbo38sqtyeLDlQksYni4CdIdy9Z3w4vEsGmgfAi5yuV962IjRkOfmRDAb7aLXZ942ch7skmLpR4axJOTJumOnAuYZi_EJe9nK53UzUe0rzov1pCS8zr6so52B9E9FLQeRNxoVFS0iiRai2fqc2BGV90Yna3zNUFdB5jWX54a-pWBhCovTLUtrIAHFcy6oGrlSSw6Go_sGtKEzp1QGM5b8w1Xa_IR9HtWEt821fZkuz2yCaXpD6WWgK-8_LwyPztEiN_Q3aESSSXmvV3Ofg1-tnQr1a8xDWzg";

    //payload: "resource_access.Organisation.roles" value replaced with ADMIN.  FYI: Testing if contains verify correctly.
    private static final String invalidRoleToken =
        "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0eXAiOiJCZWFyZXIiLCJleHAiOjE4OTM0NTI0MDAsImlhdCI6MTYyNzA1Mzc0NywiaXNzIjoiaHR0cDovL3Rlc3QuaXNzdWVyOjEyMzQvYXV0aC9yZWFsbXMvdGVzdCIsImF6cCI6InRlc3QtYXV0aG9yaXplZC1wYXJ0aWVzIiwiYXVkIjoidGVzdC1hdWQiLCJzY29wZSI6Im9wZW5pZCBlbWFpbCBwcm9maWxlIFRlc3QiLCJvcmdhbmlzYXRpb24iOiJwb3J0YWwiLCJlbWFpbC12ZXJpZmllZCI6ZmFsc2UsImFjciI6MSwidXNlcnMiOnsiYmxhIjpbMSwyLDUsNl0sImRhdGEiOlt7Im5hbWUiOiJhbGljZSIsImFnZSI6NDIsImVtYWlsIjoiYWxpY2VAdGVzdC5jaCJ9LHsibmFtZSI6ImJvYiIsImFnZSI6NDMsImVtYWlsIjoiYm9iQHRlc3QuY2gifSx7Im5hbWUiOiJldmUiLCJhZ2UiOjIwLCJlbWFpbCI6ImV2ZUB0ZXN0LmNoIn1dfSwibG90dG8iOlsxLDUsMTIsMjIsNDMsNDRdLCJodHRwOi8vaGFzdXJhLmlvL2p3dC9jbGFpbXMiOnsieC1oYXN1cmEtcG9ydGFsdXNlci1pZCI6IjEyMzQiLCJ4LWhhc3VyYS1hbGxvd2VkLXJvbGVzIjpbIktFWUNMT0FLIiwicG9ydGFsdXNlciJdfSwicmVzb3VyY2VfYWNjZXNzIjp7Ik9yZ2FuaXNhdGlvbiI6eyJyb2xlcyI6IlJPT1QifX19.ln4JyJsGPKBbukfHO7Ma-q89p3PR0ddmQ9Jpdti4Z5sgoUnAcQ8cGbLgpA9SYzQRK5rEpjIDd3ykD95NWANkXpaQYKs2RmL8C65BLqBYDSGBjpSfuU__HrBRBCVTS6Ot6MTAzxwm4iknnvRBTlTEHDFfI7dBxjH-NX_wBObhsoQ47EqdVdtha7FeOB_oBljZuYelIwKCKHFn1KnDLzvIsGrqvMShXokXDu0IZZ2r1tNMKTa77YcTwZ1L9r0-8u70RTZJ1Qm5f8KzeoE1lJ_KaDLKfJxCWIkbR4Ws-jPx8pVicfaLihqefWwGtp4XHNnoULAE_f9zQh0-uvxiTGcaFQ";
    private static final String expectedIssuer = "http://test.issuer:1234/auth/realms/test";
    private static final List<String> expectedAudience = List.of("alpha", "beta", "gamma", "test-aud");

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
    private static final JsonObject claimContainStringArray =
            new JsonObject("{\"claimPath\":\"$['resource_access']['Organisation']['roles']\",\"operator\":\"CONTAINS\",\"value\":[\"ADMINISTRATOR\",\"TENANT\"]}");

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
    private static final JsonObject claimEqualObject =
            new JsonObject("{\"claimPath\":\"$['http://hasura.io/jwt/claims']\",\"operator\":\"EQUALS\",\"value\":{\"x-hasura-allowed-roles\":[\"KEYCLOAK\",\"portaluser\"],\"x-hasura-portaluser-id\":\"1234\"}}");

    /**
     * {
     * "claimPath":"$['email-verified']",
     * "operator":"EQUALS",
     * "value":false
     * }
     */
    private static final JsonObject claimEqualBoolean =
            new JsonObject("{\"claimPath\":\"$['email-verified']\",\"operator\":\"EQUALS\",\"value\":false}");

    /**
     * {
     * "claimPath":"$['acr']",
     * "operator":"EQUALS",
     * "value":1
     * }
     */
    private static final JsonObject claimEqualInteger =
            new JsonObject("{\"claimPath\":\"$['acr']\",\"operator\":\"EQUALS\",\"value\":1}");
    /**
     {
     "claimPath":"$['users']
     "operator":"EQUALS",
     "value": {
        "data":[
            {"name":"alice", "age":42, "email": "alice@test.ch"},
            {"name":"bob", "age": 43, "email": "bob@test.ch"},
            {"name":"eve", "age": 20, "email": "eve@test.ch"}
        ],
        "bla":[1,2,5,6]
     }
     }
     */
    private static final JsonObject claimEqualObjectDifferentOrder = new JsonObject("{\"claimPath\":\"$['users']\",\"operator\":\"EQUALS\",\"value\":{\n" +
            "     \"data\":[\n" +
            "     {\"name\":\"alice\", \"age\":42, \"email\": \"alice@test.ch\"},\n" +
            "     {\"name\":\"bob\", \"age\": 43, \"email\": \"bob@test.ch\"},\n" +
            "     {\"name\":\"eve\", \"age\": 20, \"email\": \"eve@test.ch\"}\n" +
            "     ],\n" +
            "     \"bla\":[1,2,5,6]    \n" +
            "     }}");

    private static final JsonObject claimContainIntegerArray =
            new JsonObject("{\"claimPath\":\"$['lotto']\",\"operator\":\"CONTAINS\",\"value\":[1,5,12,22,43,44,50,59]}");

    private static final JsonObject claimContainInteger =
            new JsonObject("{\"claimPath\":\"$['acr']\",\"operator\":\"CONTAINS\",\"value\":[1,9]}");



    private static final JsonArray claims = new JsonArray(List.of(claimEqualString, claimContainStringArray, claimEqualObject,
            claimEqualBoolean, claimEqualInteger, claimEqualObjectDifferentOrder, claimContainIntegerArray, claimContainInteger));

    private HttpServer server;
    private int port;

    @BeforeEach
    public void setup(Vertx vertx) throws Exception {
        JWTAuth claimProvider = JWTAuthClaim.create(vertx, new JWTAuthOptions()
                .addPubSecKey(new PubSecKeyOptions().setAlgorithm(publicKeyAlgorithm).setBuffer(publicKeyRS256))
                .setJWTOptions(new JWTClaimOptions().setOtherClaims(claims).setIssuer(expectedIssuer).setAudience(expectedAudience)));

        boolean optional = false;


        BearerOnlyMiddleware bearerOnly = new BearerOnlyMiddleware(
                JWTAuthClaimHandler.create(claimProvider), optional);

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
    public void validToken(Vertx vertx, VertxTestContext testCtx) {
        RequestOptions reqOpts = new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + validToken);
        doRequest(vertx, testCtx, reqOpts, 200);
    }

    @Test
    public void pathKeyMismatch(Vertx vertx, VertxTestContext testCtx) {
        RequestOptions reqOpts = new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION,
                "Bearer " + invalidPathKeyToken);
        doRequest(vertx, testCtx, reqOpts, 401);
    }

    @Test
    public void booleanValueMismatch(Vertx vertx, VertxTestContext testCtx) {
        RequestOptions reqOpts = new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION,
                "Bearer " + invalidBooleanValueToken);
        doRequest(vertx, testCtx, reqOpts, 401);
    }

    @Test
    public void entryMismatch(Vertx vertx, VertxTestContext testCtx) {
        RequestOptions reqOpts = new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION,
                "Bearer " + invalidHasuraClaimsToken);
        doRequest(vertx, testCtx, reqOpts, 401);
    }

    @Test
    public void entryContainsMismatch(Vertx vertx, VertxTestContext testCtx) {
        RequestOptions reqOpts = new RequestOptions().addHeader(HttpHeaders.AUTHORIZATION,
                "Bearer " + invalidRoleToken);
        doRequest(vertx, testCtx, reqOpts, 401);
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