package ch.uniport.gateway.proxy.router;

import io.vertx.core.json.JsonObject;

public class PublicProtoHostPort {

    public static final String UNIPORT_GATEWAY_PUBLIC_PROTOCOL = "UNIPORT_GATEWAY_PUBLIC_PROTOCOL";
    public static final String UNIPORT_GATEWAY_PUBLIC_PROTOCOL_DEFAULT = "http";

    public static final String UNIPORT_GATEWAY_PUBLIC_HOSTNAME = "UNIPORT_GATEWAY_PUBLIC_HOSTNAME";
    public static final String UNIPORT_GATEWAY_PUBLIC_HOSTNAME_DEFAULT = "localhost";

    public static final String UNIPORT_GATEWAY_PUBLIC_PORT = "UNIPORT_GATEWAY_PUBLIC_PORT";

    private final String proto;
    private final String host;
    private final String port; // it is only used with env vars so the type string makes sense

    public static PublicProtoHostPort of(JsonObject env, int defaultPort) {
        return new PublicProtoHostPort(env, defaultPort);
    }

    public static PublicProtoHostPort of(String proto, String host, String port) {
        return new PublicProtoHostPort(proto, host, port);
    }

    public static PublicProtoHostPort of(PublicProtoHostPort other) {
        return new PublicProtoHostPort(other);
    }

    public PublicProtoHostPort(JsonObject env, int defaultPort) {
        proto = env.getString(UNIPORT_GATEWAY_PUBLIC_PROTOCOL, UNIPORT_GATEWAY_PUBLIC_PROTOCOL_DEFAULT);
        host = env.getString(UNIPORT_GATEWAY_PUBLIC_HOSTNAME, UNIPORT_GATEWAY_PUBLIC_HOSTNAME_DEFAULT);
        port = env.getString(UNIPORT_GATEWAY_PUBLIC_PORT, "%d".formatted(defaultPort));
    }

    PublicProtoHostPort(String proto, String host, String port) {
        this.proto = proto;
        this.host = host;
        this.port = port;
    }

    PublicProtoHostPort(PublicProtoHostPort other) {
        this.proto = other.proto;
        this.host = other.host;
        this.port = other.port;
    }

    /**
     * @return the proto
     */
    public String getProto() {
        return proto;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @return the port
     */
    public String getPort() {
        return port;
    }

}
