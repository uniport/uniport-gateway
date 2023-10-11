package com.inventage.portal.gateway;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class RuntimeTest {

    /*
     * At this point, we test if we can read a user-configured logging config file correctly.
     * Unfortunately, testing the same for the env var is not as easy because it is essentially a system wide config
     * Also testing, if the default configs are read cannot be done easily because 
     * we check if final config file exists and the default path is in /etc.
     */
    @Test
    void shouldReadLoggingConfigFilePathFromProperty(@TempDir Path tempDir) throws IOException {
        // given
        final Path expectedPath = Files.createFile(tempDir.resolve("some-logback-config.xm"));
        System.setProperty(Runtime.LOGGING_CONFIG_PROPERTY, expectedPath.toString());

        // when
        final Optional<Path> actualPath = Runtime.getLoggingConfigPath();

        // then
        assertTrue(actualPath.isPresent());
        assertTrue(actualPath.get().equals(expectedPath));
    }
}
