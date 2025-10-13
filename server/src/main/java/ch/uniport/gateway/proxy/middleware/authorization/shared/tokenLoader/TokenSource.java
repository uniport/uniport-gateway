package ch.uniport.gateway.proxy.middleware.authorization.shared.tokenLoader;

public enum TokenSource {

    HEADER(JWTAuthTokenLoadHandler.TOKEN_SOURCE_HEADER),

    SESSION_SCOPE(JWTAuthTokenLoadHandler.TOKEN_SOURCE_SESSION_SCOPE);

    private final String name;

    TokenSource(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
