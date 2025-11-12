package ch.uniport.gateway.proxy.middleware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.uniport.gateway.Runtime;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class MiddlewareFactoryLoadedTest {

    @Test
    void shouldLoadFactories(Vertx vertx, VertxTestContext testCtx) {
        List<String> types = MiddlewareFactoryLoader.getInstance().listFactories().stream()
            .map(MiddlewareFactory::provides)
            .toList();

        assertTrue(types.size() > 0);
        assertTrue(types.contains("additionalRoutes"));
        testCtx.completeNow();
    }

    @Test
    void shouldDiscoverExternalJarImplementation() throws Exception {
        // given
        Path tempDir = Files.createTempDirectory("mock-extension-test");
        File mockJarFile = MockExtensionsJarCreator.create(tempDir);
        System.setProperty(Runtime.EXTENSIONS_PATH_PROPERTY, mockJarFile.getParent());

        // when
        ClassLoader parent = MiddlewareFactory.class.getClassLoader();
        URLClassLoader classLoader = new URLClassLoader(new URL[] { mockJarFile.toURI().toURL() }, parent);
        ServiceLoader<MiddlewareFactory> loader = ServiceLoader.load(MiddlewareFactory.class, classLoader);
        List<MiddlewareFactory> factories = loader.stream()
            .map(ServiceLoader.Provider::get)
            .toList();

        // then
        assertNotNull(factories, "no factories loaded");

        Optional<MiddlewareFactory> maybe = factories.stream()
            .filter(f -> f.getClass().getName().equals(MockExtensionsJarCreator.IMPL_FQN))
            .findFirst();

        assertTrue(maybe.isPresent(), "mock extension not found");
        assertEquals(MockExtensionsJarCreator.MOCK_TYPE, maybe.get().provides(), "mock has wrong type");

        tempDir.toFile().delete();
    }

    class MockExtensionsJarCreator {

        static final String INTERFACE_NAME = "ch.uniport.gateway.proxy.middleware.MiddlewareFactory";

        static final String PACKAGE_NAME = "com.mock.extension";
        static final String CLASS_NAME = "MockMiddlewareFactory";
        static final String IMPL_FQN = "%s.%s".formatted(PACKAGE_NAME, CLASS_NAME);

        static final String PACKAGE_PATH = "com/mock/extension";
        static final String SOURCE_PATH = "%s/%s.java".formatted(PACKAGE_PATH, CLASS_NAME);
        static final String CLASS_PATH = "%s/%s.class".formatted(PACKAGE_PATH, CLASS_NAME);

        static final String MOCK_TYPE = "mock";
        static final String MOCK_JAR_NAME = "mock-extension.jar";

        /**
         * Creates a JAR file containing a valid ServiceLoader implementation.
         * 
         * @return The File object pointing to the newly created JAR.
         */
        public static File create(Path tempDir) throws Exception {
            // compile the mock source code
            File compiledClassFile = compileSource(tempDir, getSourceCode());

            // create service provider configuration file
            Path servicesDir = tempDir.resolve("META-INF").resolve("services");
            Files.createDirectories(servicesDir);

            Path configFile = servicesDir.resolve(INTERFACE_NAME);
            Files.writeString(configFile, IMPL_FQN + "\n", StandardOpenOption.CREATE);

            // package everything into the jar
            File jarFile = new File(tempDir.toFile(), MOCK_JAR_NAME);
            createJar(tempDir, jarFile);

            return jarFile;
        }

        private static String getSourceCode() {
            return """
                package PACKAGE_NAME;

                import ch.uniport.gateway.proxy.middleware.Middleware;
                import ch.uniport.gateway.proxy.middleware.MiddlewareFactory;
                import ch.uniport.gateway.proxy.middleware.MiddlewareOptionsModel;
                import io.vertx.core.Future;
                import io.vertx.core.Vertx;
                import io.vertx.core.json.JsonObject;
                import io.vertx.ext.web.Router;
                import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
                import io.vertx.json.schema.common.dsl.Schemas;

                public class CLASS_NAME implements MiddlewareFactory {
                    @Override
                    public String provides() {
                        return "MOCK_TYPE";
                    }

                    @Override
                    public ObjectSchemaBuilder optionsSchema() {
                        return Schemas.objectSchema()
                            .allowAdditionalProperties(false);
                    }

                    @Override
                    public Future<Void> validate(JsonObject options) {
                        return Future.succeededFuture();
                    }

                    @Override
                    public Class<MiddlewareOptionsModel> modelType() {
                        return null;
                    }

                    @Override
                    public Future<Middleware> create(Vertx vertx, String name, Router router, MiddlewareOptionsModel config) {
                        return Future.succeededFuture(null);
                    }
                }
                """
                .replace("PACKAGE_NAME", PACKAGE_NAME)
                .replace("CLASS_NAME", CLASS_NAME)
                .replace("MOCK_TYPE", MOCK_TYPE);
        }

        private static File compileSource(Path workingDir, String source) throws IOException {
            // create source file
            Path sourceFile = workingDir.resolve(SOURCE_PATH);
            Files.createDirectories(sourceFile.getParent());
            Files.writeString(sourceFile, source, StandardOpenOption.CREATE);

            // get the system java compiler
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new IllegalStateException("JDK compiler not available. Ensure you are running with a full JDK.");
            }

            // compile the source file
            int result = compiler.run(null, null, null, sourceFile.toString());
            if (result != 0) {
                throw new RuntimeException("Compilation of mock extension source failed.");
            }

            // return the path to the compiled class file
            Path classPath = workingDir.resolve(CLASS_PATH);
            return classPath.toFile();
        }

        private static void createJar(Path sourceDir, File targetJar) throws IOException {
            URI sourceUri = sourceDir.toUri();
            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(targetJar))) {
                Files.walk(sourceDir)
                    .filter(path -> !Files.isDirectory(path) && !path.equals(targetJar.toPath()))
                    .forEach(path -> {
                        String entryName = sourceUri.relativize(path.toUri()).getPath();
                        try {
                            jos.putNextEntry(new JarEntry(entryName));
                            Files.copy(path, jos);
                            jos.closeEntry();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
            }
        }
    }
}
