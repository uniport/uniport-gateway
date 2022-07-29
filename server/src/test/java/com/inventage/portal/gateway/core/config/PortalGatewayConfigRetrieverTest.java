package com.inventage.portal.gateway.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PortalGatewayConfigRetrieverTest {

    @TempDir
    static Path tempDir;
    static Path tempFile;
    static String tempFilename = "portal-gateway.json";

    @BeforeAll
    public static void init() throws IOException {
        tempFile = Files.createFile(tempDir.resolve(tempFilename));
    }

    @Test
    public void noConfigFileExists() {
        Optional<Path> staticConfigPath = PortalGatewayConfigRetriever.getStaticConfigPath();
        assertTrue(staticConfigPath.isEmpty());
    }

    @Test
    public void configFileAsEnvVar() {
        // not testable as the environmental values are immutable from within a java process
    }

    @Test
    public void configFileAsProperty() {
        System.setProperty(PortalGatewayConfigRetriever.PROPERTY, tempFile.toAbsolutePath().toString());

        Optional<Path> staticConfigPath = PortalGatewayConfigRetriever.getStaticConfigPath();

        assertTrue(staticConfigPath.isPresent());
        assertEquals(staticConfigPath.get(), tempFile);

        System.clearProperty(PortalGatewayConfigRetriever.PROPERTY);
    }

    @Test
    public void directoryAsProperty() {
        System.setProperty(PortalGatewayConfigRetriever.PROPERTY, "path/to/a/dir");

        Optional<Path> staticConfigPath = PortalGatewayConfigRetriever.getStaticConfigPath();
        assertTrue(staticConfigPath.isEmpty());

        System.clearProperty(PortalGatewayConfigRetriever.PROPERTY);
    }

    @Test
    public void nonExistingFileAsProperty() {
        System.setProperty(PortalGatewayConfigRetriever.PROPERTY, "non/existing/file.json");

        Optional<Path> staticConfigPath = PortalGatewayConfigRetriever.getStaticConfigPath();
        assertTrue(staticConfigPath.isEmpty());

        System.clearProperty(PortalGatewayConfigRetriever.PROPERTY);
    }

    @Test
    public void configFileInDefaultDir() {
        // not testable as tempdirs are created in 'java.io.tmpdir' that is '/tmp'
        // not forcable since the default dir is in /etc (needs root access)
    }

    @Test
    public void configFileInWorkingDir() {
        // not testable as for the same reason as for the default dir
        // with a hack this could be forced though
    }
}
