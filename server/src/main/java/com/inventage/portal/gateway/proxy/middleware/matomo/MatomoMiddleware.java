package com.inventage.portal.gateway.proxy.middleware.matomo;

import com.inventage.portal.gateway.proxy.middleware.TraceMiddleware;
import com.jayway.jsonpath.JsonPath;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.impl.jose.JWT;
import io.vertx.ext.web.RoutingContext;
import java.util.Objects;
import net.minidev.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class MatomoMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatomoMiddleware.class);

    private static final String HEADER_USERNAME = "matomo-username";
    private static final String HEADER_EMAIL = "matomo-email";
    private static final String HEADER_ROLE = "matomo-role";
    private static final String HEADER_GROUP = "matomo-group";

    private final String name;
    private final String jwtPathUsername;
    private final String jwtPathEmail;
    private final String jwtPathRoles;
    private final String jwtPathGroup;

    public MatomoMiddleware(
        String name,
        String jwtPathRoles,
        String jwtPathGroup,
        String jwtPathUsername,
        String jwtPathEmail
    ) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(jwtPathUsername, "jwtPathUsername must not be null");
        Objects.requireNonNull(jwtPathEmail, "jwtPathEmail must not be null");
        Objects.requireNonNull(jwtPathRoles, "jwtPathRoles must not be null");
        Objects.requireNonNull(jwtPathGroup, "jwtPathGroup must not be null");

        this.name = name;
        this.jwtPathUsername = jwtPathUsername;
        this.jwtPathEmail = jwtPathEmail;
        this.jwtPathRoles = jwtPathRoles;
        this.jwtPathGroup = jwtPathGroup;
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        final String authorizationHeader = ctx.request().headers().get(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null) {
            ctx.fail(HttpResponseStatus.FORBIDDEN.code());
            return;
        }

        final String authorization = authorizationHeader.split(" ")[1];
        final JsonObject jwt = JWT.parse(authorization);
        final JsonObject payload = jwt.getJsonObject("payload");
        final String username = JsonPath.read(payload.toString(), jwtPathUsername);
        final String email = JsonPath.read(payload.toString(), jwtPathEmail);
        final JSONArray roles = JsonPath.read(payload.toString(), jwtPathRoles);
        final String role = (String) roles.get(0);
        final String group = JsonPath.read(payload.toString(), jwtPathGroup);

        final MultiMap headers = ctx.request().headers();
        headers.remove(HEADER_USERNAME);
        headers.remove(HEADER_EMAIL);
        headers.remove(HEADER_GROUP);
        headers.remove(HEADER_ROLE);

        headers.set(HEADER_USERNAME, username);
        headers.set(HEADER_ROLE, role);
        headers.set(HEADER_GROUP, group);
        headers.set(HEADER_EMAIL, email);

        LOGGER.debug("{}: Handling '{}'", name, payload);
        ctx.next();
    }
}
