package com.zuk.minispring.beans;

import com.zuk.minispring.annotation.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Compiles a couple of beans at test time, packs them into a JAR and scans that JAR. */
class JarScanningTest {

    @TempDir
    Path tempDir;

    @Test
    void scansBeansInsideJarIncludingSubpackages() throws Exception {
        Path sources = tempDir.resolve("src");
        Path classes = tempDir.resolve("classes");
        writeSource(sources, "jarbeans/JarBean.java", """
                package jarbeans;
                @com.zuk.minispring.annotation.Component
                public class JarBean {}
                """);
        writeSource(sources, "jarbeans/sub/NestedJarBean.java", """
                package jarbeans.sub;
                @com.zuk.minispring.annotation.Service
                public class NestedJarBean {}
                """);
        compile(sources, classes);
        Path jar = packJar(classes, tempDir.resolve("beans.jar"));

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, getClass().getClassLoader())) {
            BeanFactory beanFactory = new BeanFactory(classLoader);
            beanFactory.instantiate("jarbeans");

            assertEquals(2, beanFactory.getSingletons().size());
            assertEquals("jarbeans.JarBean", beanFactory.getBean("JarBean").getClass().getName());
            assertEquals("jarbeans.sub.NestedJarBean", beanFactory.getBean("NestedJarBean").getClass().getName());
        }
    }

    private static void writeSource(Path root, String path, String code) throws IOException {
        Path file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, code);
    }

    private static void compile(Path sources, Path output) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Tests must run on a JDK, not a JRE");
        String annotationsOnClasspath = Path.of(Component.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        try (Stream<Path> files = Stream.of(
                sources.resolve("jarbeans/JarBean.java"), sources.resolve("jarbeans/sub/NestedJarBean.java"))) {
            String[] args = Stream.concat(
                    Stream.of("-d", output.toString(), "-cp", annotationsOnClasspath),
                    files.map(Path::toString)).toArray(String[]::new);
            assertEquals(0, compiler.run(null, null, null, args), "Compilation failed");
        }
    }

    /** Writes directory entries too, as Maven and Gradle do; class loaders need them to find a package. */
    private static Path packJar(Path classes, Path jar) throws IOException {
        try (OutputStream out = Files.newOutputStream(jar);
             JarOutputStream jarOut = new JarOutputStream(out);
             Stream<Path> paths = Files.walk(classes)) {
            for (Path path : paths.sorted().toList()) {
                if (path.equals(classes)) {
                    continue;
                }
                String name = classes.relativize(path).toString().replace('\\', '/');
                if (Files.isDirectory(path)) {
                    jarOut.putNextEntry(new JarEntry(name + "/"));
                } else {
                    jarOut.putNextEntry(new JarEntry(name));
                    Files.copy(path, jarOut);
                }
                jarOut.closeEntry();
            }
        }
        return jar;
    }
}
