package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.analyze.JarAnalyzer.ClassType;
import dev.harrel.jarhell.analyze.JarAnalyzer.Content;
import dev.harrel.jarhell.analyze.JarAnalyzer.ContentType;
import dev.harrel.jarhell.analyze.JarAnalyzer.JarInfo;
import dev.harrel.jarhell.analyze.JarAnalyzer.ModuleType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.jar.JarInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class JarAnalyzerTest {
    private final JarAnalyzer analyzer = new JarAnalyzer();

    @Test
    void shouldAnalyzeEmptyJar() throws IOException {
        JarInfo info = analyze(JarBuilder.jar());

        assertThat(info.contents()).isEmpty();
        assertThat(info.publicClasses()).isEmpty();
        assertThat(info.nonPublicClasses()).isZero();
        assertThat(info.bytecodeVersion()).isNull();
        assertThat(info.buildJdk()).isNull();
        assertThat(info.multiReleaseJar()).isFalse();
        assertThat(info.executable()).isFalse();
        assertThat(info.services()).isEmpty();
        assertThat(info.moduleType()).isEqualTo(ModuleType.UNNAMED);
        assertThat(info.moduleName()).isNull();
    }

    @Test
    void shouldAnalyzeJarWithManifestOnly() throws IOException {
        JarInfo info = analyze(JarBuilder.jar()
                .mainAttribute("Build-Jdk-Spec", "21")
                .mainAttribute("Main-Class", "com.example.Main"));

        assertThat(info.buildJdk()).isEqualTo("21");
        assertThat(info.executable()).isTrue();
        // manifest is counted as a single resource, its size is deliberately ignored
        assertThat(info.contents()).containsExactly(entry(ContentType.RESOURCE, new Content(1, 0, 0)));
        assertThat(info.publicClasses()).isEmpty();
        assertThat(info.nonPublicClasses()).isZero();
        assertThat(info.bytecodeVersion()).isNull();
        assertThat(info.moduleType()).isEqualTo(ModuleType.UNNAMED);
    }

    @Test
    void shouldAnalyzeSinglePublicClass() throws IOException {
        byte[] classBytes = JarBuilder.publicClass("com.example.Simple");
        JarInfo info = analyze(JarBuilder.jar().manifest().javaClass("com.example.Simple"));

        assertThat(info.publicClasses()).containsExactly(entry(ClassType.CLASS, 1));
        assertThat(info.nonPublicClasses()).isZero();
        assertThat(info.bytecodeVersion()).isEqualTo("61.0");
        assertThat(info.contents()).containsOnlyKeys(ContentType.JAVA, ContentType.RESOURCE);

        Content java = info.contents().get(ContentType.JAVA);
        assertThat(java.count()).isEqualTo(1);
        assertThat(java.size()).isEqualTo(classBytes.length);
        assertThat(java.compressedSize()).isPositive();
    }

    private JarInfo analyze(JarBuilder builder) throws IOException {
        try (JarInputStream jis = builder.open()) {
            return analyzer.analyzeJar(jis);
        }
    }
}
