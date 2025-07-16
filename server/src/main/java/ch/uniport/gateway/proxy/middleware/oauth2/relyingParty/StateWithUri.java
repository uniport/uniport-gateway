package ch.uniport.gateway.proxy.middleware.oauth2.relyingParty;

import io.vertx.core.http.HttpMethod;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is for handling the enhanced state parameter of the OIDC
 * authentication flow.
 * The enhanced state parameter contains beside a random value also an
 * (optional) initial uri, which triggered the
 * authentication flow. Additionally, the String value is Base64 encoded.
 */
public final class StateWithUri {
    private static final Logger LOGGER = LoggerFactory.getLogger(StateWithUri.class);
    private static final String SPLITTER1 = ":";
    private static final String SPLITTER2 = "@";
    private final String state;
    private final Optional<String> uri;
    private final Optional<String> method;
    private final String encoded;

    /**
     * Constructor from a state parameter.
     * If the given state parameter is not Base64 encoded it will be used as an
     * opaque value and an uri value
     * will not be available.
     *
     * @param stateParameterBase64Encoded
     *            state parameter with format
     *            base64(<state>:<uri>)
     *            base64(<state>:HTTP-METHOD=base64(<uri>))
     */
    public StateWithUri(String stateParameterBase64Encoded) {
        if (stateParameterBase64Encoded == null) {
            throw new IllegalArgumentException("Null is not a valid state parameter value!");
        }
        final Optional<String> optionalBase64Decoded = base64Decode(stateParameterBase64Encoded);
        if (optionalBase64Decoded.isPresent()) {
            final String[] strings = optionalBase64Decoded.get().split(SPLITTER1, 2);
            this.state = strings[0];
            if (strings.length == 2) {
                final String[] values = strings[1].split(SPLITTER2);
                if (values.length == 2) {
                    // base64(<state>:HTTP-METHOD=base64(<uri>))
                    final Optional<String> optionalUrl = base64Decode(values[1]);
                    if (optionalUrl.isPresent()) {
                        this.method = Optional.of(values[0]);
                        this.uri = Optional.of(ensureRelativeUri(optionalUrl.get()));
                    } else {
                        this.uri = Optional.empty();
                        this.method = Optional.empty();
                    }
                } else {
                    // base64(<state>:<uri>)
                    this.uri = Optional.of(ensureRelativeUri(values[0]));
                    this.method = Optional.empty();
                }
            } else {
                // base64(<state>)
                this.uri = Optional.empty();
                this.method = Optional.empty();
            }
            this.encoded = encode();
        } else {
            // <state>
            this.state = stateParameterBase64Encoded;
            this.uri = Optional.empty();
            this.method = Optional.empty();
            this.encoded = stateParameterBase64Encoded;
        }
    }

    /**
     * Constructor with the state and the uri value.
     *
     * @param state
     * @param uri
     */
    public StateWithUri(String state, String uri) {
        this(state, uri, null);
    }

    public StateWithUri(String state, String uri, HttpMethod httpMethod) {
        if (state == null) {
            throw new IllegalArgumentException("Null is not a valid state value!");
        }
        this.state = state;
        this.uri = Optional.ofNullable(uri);
        this.method = Optional.ofNullable(httpMethod == null ? null : httpMethod.name());
        this.encoded = encode();
    }

    public String state() {
        return state;
    }

    public Optional<String> uri() {
        return uri;
    }

    public Optional<String> httpMethod() {
        return method;
    }

    /**
     * Get the encoded value for state and uri.
     *
     * @return a base64 encode String containing the state and the uri value
     */
    public String toStateParameter() {
        return encoded;
    }

    private String encode() {
        final StringBuilder stateParameter = new StringBuilder();
        stateParameter.append(state());
        if (uri().isPresent()) {
            stateParameter.append(SPLITTER1);
            if (httpMethod().isPresent()) {
                stateParameter.append(httpMethod().get());
                stateParameter.append(SPLITTER2);
                stateParameter.append(base64Encode(uri().get()));
            } else {
                stateParameter.append(uri().get());
            }
        }
        return base64Encode(stateParameter.toString());
    }

    private String base64Encode(String value) {
        if (value == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private Optional<String> base64Decode(String stateParameter) {
        try {
            final byte[] bytes = Base64.getDecoder().decode(stateParameter);
            return Optional.of(new String(bytes, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("failed with '{}'", e.getMessage());
        }
        return Optional.empty();
    }

    private String ensureRelativeUri(String anUri) {
        try {
            final URI uri = new URI(anUri);
            final URI relativeURI = new URI(null, null, uri.getPath(), uri.getQuery(), uri.getFragment());
            return relativeURI.toString();
        } catch (URISyntaxException e) {
            LOGGER.warn("URI '{}' couldn't be parsed ('{}'), using '/'", anUri, e.getMessage());
            return "/";
        }
    }

}
