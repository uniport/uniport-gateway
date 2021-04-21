package com.inventage.portal.gateway;

import java.io.IOException;
import java.net.ServerSocket;

public class TestUtils {

    /**
     * Returns a free port number on localhost.
     *
     * Heavily inspired from org.eclipse.jdt.launching.SocketUtil (to avoid a dependency to JDT just because of this).
     * Slightly improved with close() missing in JDT. And throws exception instead of returning -1.
     *
     * @return a free port number on localhost
     * @throws IllegalStateException if unable to find a free port
     */
    public static int findFreePort() {
        ServerSocket socket = null;
        try {
            int port;
            do {
                socket = new ServerSocket(0);
                port = socket.getLocalPort();
            } while (port >= 40000);
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore IOException on close()
            }
            return port;
        } catch (IOException e) {
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
        throw new IllegalStateException(
                "Could not find a free TCP/IP port to start embedded Jetty HTTP Server on");
    }
}
