package com.inventage.portal.gateway.proxy.middleware.mock;


import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;

import javax.json.JsonObject;
import java.util.HashMap;
import java.util.Map;

public class TestBearerOnlyJWTProvider {
    public static final String ISSUER = "mock.TestBearerOnlyJWTProvider";

    static {
        // JWT signing is normally done in Keycloak
        System.setProperty("smallrye.jwt.sign.key.location", "FOR_DEVELOPMENT_PURPOSE_ONLY-privateKey.pem");
        // token life time in seconds: 1 year
        System.setProperty("smallrye.jwt.new-token.lifespan", "" + (1*60*60*24*365));
    }

    public static void main(String[] args) {
        String token = signToken(new HashMap<String, Object>());
        System.out.println(token);
    }

    public static String signToken(Map<String,Object> jsonMap){
        JwtClaimsBuilder builder = Jwt.issuer(ISSUER).subject("admin").preferredUserName("admin");
        return builder.sign();

    }

    public static String signToken(JsonObject jsonObject) {
        return Jwt.sign(jsonObject);
    }


    /**
   public static String token(String subject, OrganisationRoles... roles) {
        return token(subject, UUID.randomUUID().toString(), Constants.PORTAL_TENANT_ALIAS, roles);
    }

    public static String token(String subject, String preferredUsername, String tenant, OrganisationRoles... roles) {
        // realm roles
        final JsonObjectBuilder realmRoles = Json.createObjectBuilder()
                .add(JsonWebTokenAdapter.ROLES, Json.createArrayBuilder().add("portaluser"));
        // client roles
        final JsonObjectBuilder clientRoles = Json.createObjectBuilder()
                .add(Constants.APPLICATION_ORGANISATION, Json.createObjectBuilder()
                        .add(JsonWebTokenAdapter.ROLES, Json.createArrayBuilder(Arrays.stream(roles).map(organisationRole -> organisationRole.toString()).collect(Collectors.toList()))));
        // hasura claim
        final JsonObjectBuilder hasuraClaim = Json.createObjectBuilder()
                .add("x-hasura-user-id", subject)
                .add("x-hasura-organisation-id", "inventage.com")
                .add("x-hasura-tenant-id", tenant)
                .add("x-hasura-default-role", roles.length > 0 ? roles[0].toString() : "")
                .add("x-hasura-allowed-roles", Json.createArrayBuilder(Arrays.stream(roles).map(organisationRole -> organisationRole.toString()).collect(Collectors.toList())));
        final String token = Jwt.issuer(ISSUER)
                .subject(subject)
                .preferredUserName(preferredUsername)
                .claim(JsonWebTokenAdapter.RESOURCE_ACCESS, clientRoles.build())
                .claim(JsonWebTokenAdapter.REALM_ACCESS, realmRoles.build())
                .claim(JsonWebTokenAdapter.HASURA, hasuraClaim.build())
                .claim(JsonWebTokenAdapter.ORGANISATION, "inventage.com")
                .claim(JsonWebTokenAdapter.TENANT, tenant)
                .sign();
        return token;
    }
     */


}
