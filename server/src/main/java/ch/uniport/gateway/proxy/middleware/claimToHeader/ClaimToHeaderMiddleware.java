package ch.uniport.gateway.proxy.middleware.claimToHeader;

import ch.uniport.gateway.proxy.middleware.TraceMiddleware;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware to set an HTTP Header with the value of a JWT claim.
 */
public class ClaimToHeaderMiddleware extends TraceMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClaimToHeaderMiddleware.class);

    private final String name;
    private final String claimPath;
    private final String headerName;

    /**
     *
     * @param name
     *            of this instance
     * @param claimPath
     *            claim read from JWT in JsonPath syntax
     *            (https://github.com/json-path/JsonPath),
     *            which describes the path to the entry in the payload to be
     *            checked.
     * @param headerName
     *            name of the HTTP header to be set
     */
    public ClaimToHeaderMiddleware(String name, String claimPath, String headerName) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(claimPath, "claimPath must not be null");
        Objects.requireNonNull(headerName, "headerName must not be null");

        this.name = name;
        this.claimPath = claimPath;
        this.headerName = headerName;
    }

    @Override
    public void handleWithTraceSpan(RoutingContext ctx, Span span) {
        LOGGER.debug("{}: Handling '{}'", name, ctx.request().absoluteURI());

        LOGGER.debug("Adding claim '{}' to header '{}'", claimPath, headerName);
        ctx.request().headers().remove(headerName);
        // e.g. authorization header = Bearer
        // base64(header).base64(payload).base64(signature)
        final String authorization = ctx.request().headers().get(HttpHeaders.AUTHORIZATION);
        try {
            extractJwtFromHeader(authorization).ifPresent(jwt -> {
                final Object claimValue = JsonPath.read(jwt, claimPath);
                if (claimValue != null) {
                    ctx.request().headers().add(headerName, claimValue.toString());
                    LOGGER.debug("Adding header '{}: {}'", headerName, claimValue);
                }
            });
        } catch (PathNotFoundException e) {
            LOGGER.debug(e.getMessage());
        }
        ctx.next();
    }

    private Optional<String> extractJwtFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        try {
            final String[] pieces = authorizationHeader.split("\\.");
            final String b64payload = pieces[1];
            final String jsonString = new String(java.util.Base64.getDecoder().decode(b64payload), "UTF-8");
            return Optional.of(jsonString);
        } catch (Exception e) {
            LOGGER.debug("Authorization header '{}' resulted in exception '{}'", authorizationHeader, e.getMessage());
        }
        return Optional.empty();
    }
}
