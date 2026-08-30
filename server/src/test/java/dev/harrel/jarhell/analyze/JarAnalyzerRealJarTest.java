package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.analyze.JarAnalyzer.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.jar.JarInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class JarAnalyzerRealJarTest {
    private final JarAnalyzer analyzer = new JarAnalyzer();

    @Test
    void shouldAnalyzeJsonSchemaJar() throws IOException {
        JarInfo info = analyze("json-schema-1.9.1.jar");

        assertThat(info.bytecodeVersion()).isEqualTo("52.0");
        assertThat(info.buildJdk()).isNull();
        assertThat(info.multiReleaseJar()).isFalse();
        assertThat(info.executable()).isFalse();
        assertThat(info.services()).isEmpty();
        assertThat(info.moduleType()).isEqualTo(ModuleType.AUTOMATIC);
        assertThat(info.moduleName()).isEqualTo("dev.harrel.jsonschema");

        assertThat(info.publicClasses()).containsExactly(
                entry(ClassType.CLASS, 48),
                entry(ClassType.ABSTRACT_CLASS, 2),
                entry(ClassType.INTERFACE, 7),
                entry(ClassType.ENUM, 2)
        );
        assertThat(info.nonPublicClasses()).isEqualTo(78);
        assertThat(info.contents()).containsExactly(
                entry(ContentType.JAVA, new Content(148, 414088, 175461)),
                entry(ContentType.RESOURCE, new Content(13, 21816, 6016))
        );
    }

    @Test
    void shouldAnalyzeTinylogApiJar() throws IOException {
        JarInfo info = analyze("tinylog-api-2.7.0.jar");

        assertThat(info.bytecodeVersion()).isEqualTo("50.0");
        assertThat(info.buildJdk()).isEqualTo("9");
        assertThat(info.multiReleaseJar()).isTrue();
        assertThat(info.executable()).isFalse();
        assertThat(info.services()).containsExactly("org.tinylog.configuration.ConfigurationLoader");
        assertThat(info.moduleType()).isEqualTo(ModuleType.NAMED);
        assertThat(info.moduleName()).isEqualTo("org.tinylog.api");

        assertThat(info.publicClasses()).containsExactly(
                entry(ClassType.CLASS, 25),
                entry(ClassType.ABSTRACT_CLASS, 1),
                entry(ClassType.INTERFACE, 7),
                entry(ClassType.ENUM, 1)
        );
        assertThat(info.nonPublicClasses()).isEqualTo(11);
        assertThat(info.contents()).containsExactly(
                entry(ContentType.JAVA, new Content(53, 120313, 52947)),
                entry(ContentType.RESOURCE, new Content(2, 56, 48))
        );
    }

    private JarInfo analyze(String jarName) throws IOException {
        InputStream resource = getClass().getResourceAsStream("/jars/" + jarName);
        Objects.requireNonNull(resource, () -> "missing test jar: " + jarName);
        try (JarInputStream jis = new JarInputStream(resource)) {
            return analyzer.analyzeJar(jis);
        }
    }
}
