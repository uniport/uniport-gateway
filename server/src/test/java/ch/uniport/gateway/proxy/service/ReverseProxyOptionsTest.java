package ch.uniport.gateway.proxy.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ch.uniport.gateway.proxy.config.DynamicConfiguration;
import ch.uniport.gateway.proxy.config.model.ServiceModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class ReverseProxyOptionsTest {

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
            DynamicConfiguration.SERVICE_NAME, aName,
            DynamicConfiguration.SERVICE_SERVERS, List.of(
                Map.of(
                    DynamicConfiguration.SERVICE_SERVER_PROTOCOL, serverProto,
                    DynamicConfiguration.SERVICE_SERVER_HOST, serverHost,
                    DynamicConfiguration.SERVICE_SERVER_PORT, serverPort,
                    DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS, Map.of(
                        DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS_VERIFY_HOSTNAME, httpsVerifyHostname,
                        DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_ALL, httpsTrustAll,
                        DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PATH, httpsTrustStorePath,
                        DynamicConfiguration.SERVICE_SERVER_HTTPS_OPTIONS_TRUST_STORE_PASSWORD, httpsTrustStorePassword))),
            DynamicConfiguration.SERVICE_VERBOSE, verbose);

        // when
        final ThrowingSupplier<ServiceModel> parse = () -> new ObjectMapper().readValue(json.encode(), ServiceModel.class);

        // then
        final ServiceModel options = assertDoesNotThrow(parse);
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
