package ch.uniport.gateway.core.config.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class GatewayConfigRetrieverTest {

    @TempDir
    static Path tempDir;
    static Path tempFile;
    static String tempFilename = "uniport-gateway.json";

    @BeforeAll
    public static void init() throws IOException {
        tempFile = Files.createFile(tempDir.resolve(tempFilename));
    }

    @Test
    public void noConfigFileExists() {
        final Optional<Path> staticConfigPath = GatewayConfigRetriever.getStaticConfigPath();
        assertTrue(staticConfigPath.isEmpty());
    }

    @Test
    public void configFileAsEnvVar() {
        // not testable as the environmental values are immutable from within a java process
    }

    @Test
    public void configFileAsProperty() {
        System.setProperty(GatewayConfigRetriever.PROPERTY, tempFile.toAbsolutePath().toString());

        final Optional<Path> staticConfigPath = GatewayConfigRetriever.getStaticConfigPath();

        assertTrue(staticConfigPath.isPresent());
        assertEquals(staticConfigPath.get(), tempFile);

        System.clearProperty(GatewayConfigRetriever.PROPERTY);
    }

    @Test
    public void directoryAsProperty() {
        System.setProperty(GatewayConfigRetriever.PROPERTY, "path/to/a/dir");

        final Optional<Path> staticConfigPath = GatewayConfigRetriever.getStaticConfigPath();
        assertTrue(staticConfigPath.isEmpty());

        System.clearProperty(GatewayConfigRetriever.PROPERTY);
    }

    @Test
    public void nonExistingFileAsProperty() {
        System.setProperty(GatewayConfigRetriever.PROPERTY, "non/existing/file.json");

        final Optional<Path> staticConfigPath = GatewayConfigRetriever.getStaticConfigPath();
        assertTrue(staticConfigPath.isEmpty());

        System.clearProperty(GatewayConfigRetriever.PROPERTY);
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
