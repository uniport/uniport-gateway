package com.inventage.portal.gateway.proxy.middleware.proxy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class ProxyMiddlewareOptionsTest {

    @Test
    public void shouldParse() {
        // given
        final String aName = "aName";
        final String serverProto = "aServerProto";
        final String serverHost = "aServerHost";
        final Integer serverPort = 1234;
        final Boolean httpsVerifyHostname = true;
        final Boolean httpsTrustAll = true;
        final String httpsTrustStorePath = "aPath";
        final String httpsTrustStorePassword = "aPassword";
        final Boolean verbose = true;

        final JsonObject json = JsonObject.of(
            ProxyMiddlewareFactory.NAME, aName,
            ProxyMiddlewareFactory.SERVERS, List.of(
                Map.of(
                    ProxyMiddlewareFactory.SERVER_PROTOCOL, serverProto,
                    ProxyMiddlewareFactory.SERVER_HOST, serverHost,
                    ProxyMiddlewareFactory.SERVER_PORT, serverPort,
                    ProxyMiddlewareFactory.SERVER_HTTPS_OPTIONS, Map.of(
                        ProxyMiddlewareFactory.VERIFY_HOSTNAME, httpsVerifyHostname,
                        ProxyMiddlewareFactory.TRUST_ALL, httpsTrustAll,
                        ProxyMiddlewareFactory.TRUST_STORE_PATH, httpsTrustStorePath,
                        ProxyMiddlewareFactory.TRUST_STORE_PASSWORD, httpsTrustStorePassword))),
            ProxyMiddlewareFactory.VERBOSE, verbose);

        // when
        final ThrowingSupplier<ProxyMiddlewareOptions> parse = () -> new ObjectMapper().readValue(json.encode(), ProxyMiddlewareOptions.class);

        // then
        final ProxyMiddlewareOptions options = assertDoesNotThrow(parse);
        assertNotNull(options);

        assertNotNull(options.getServers());
        assertNotNull(options.getServers().get(0));

        assertEquals(serverProto, options.getServers().get(0).getProtocol());
        assertEquals(serverHost, options.getServers().get(0).getHost());
        assertEquals(serverPort, options.getServers().get(0).getPort());

        assertNotNull(options.getServers().get(0).getHTTPs());
        assertEquals(httpsVerifyHostname, options.getServers().get(0).getHTTPs().verifyHostname());
        assertEquals(httpsTrustAll, options.getServers().get(0).getHTTPs().trustAll());
        assertEquals(httpsTrustStorePath, options.getServers().get(0).getHTTPs().getTrustStorePath());
        assertEquals(httpsTrustStorePassword, options.getServers().get(0).getHTTPs().getTrustStorePassword());

        assertEquals(verbose, options.isVerbose());
    }
}
