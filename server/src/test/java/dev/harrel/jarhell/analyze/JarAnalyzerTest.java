package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.analyze.JarAnalyzer.ClassType;
import dev.harrel.jarhell.analyze.JarAnalyzer.Content;
import dev.harrel.jarhell.analyze.JarAnalyzer.ContentType;
import dev.harrel.jarhell.analyze.JarAnalyzer.JarInfo;
import dev.harrel.jarhell.analyze.JarAnalyzer.ModuleType;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.attribute.EnclosingMethodAttribute;
import java.lang.classfile.attribute.InnerClassInfo;
import java.lang.classfile.attribute.InnerClassesAttribute;
import java.lang.classfile.attribute.RecordAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class JarAnalyzerTest {
    private static final int PUBLIC_CLASS = ClassFile.ACC_PUBLIC | ClassFile.ACC_SUPER;

    private final JarAnalyzer analyzer = new JarAnalyzer();

    @Test
    void shouldAnalyzeEmptyJar() throws IOException {
        JarInfo info = analyze(new JarBuilder());

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
        JarBuilder builder = new JarBuilder().manifest(
                Map.entry("Build-Jdk-Spec", "21"),
                Map.entry("Main-Class", "com.example.Main")
        );
        JarInfo info = analyze(builder);

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
        JarBuilder builder = new JarBuilder().manifest().classEntry("com/example", "Simple", "Simple.java", PUBLIC_CLASS, 52);
        JarInfo info = analyze(builder);

        assertThat(info.publicClasses()).containsExactly(entry(ClassType.CLASS, 1));
        assertThat(info.nonPublicClasses()).isZero();
        assertThat(info.bytecodeVersion()).isEqualTo("52.0");
        assertThat(info.contents()).containsOnlyKeys(ContentType.JAVA, ContentType.RESOURCE);

        Content java = info.contents().get(ContentType.JAVA);
        assertThat(java.count()).isEqualTo(1);
        assertThat(java.size()).isPositive();
        assertThat(java.compressedSize()).isPositive();
    }

    @ParameterizedTest
    @MethodSource("buildJdkValues")
    void shouldResolveBuildJdk(String buildJdk, @Nullable String expected) throws IOException {
        JarBuilder builder = new JarBuilder().manifest(Map.entry("Build-Jdk", buildJdk));
        JarInfo info = analyze(builder);

        assertThat(info.buildJdk()).isEqualTo(expected);
    }

    @Test
    void shouldPreferBuildJdkSpecOverBuildJdk() throws IOException {
        JarBuilder builder = new JarBuilder().manifest(
                Map.entry("Build-Jdk", "1.8.0_292"),
                Map.entry("Build-Jdk-Spec", "21")
        );
        JarInfo info = analyze(builder);

        // Build-Jdk alone would resolve to 1.8, so 21 proves the spec header takes precedence
        assertThat(info.buildJdk()).isEqualTo("21");
    }

    @Test
    void shouldCountContentTypePerLanguage() throws IOException {
        JarBuilder builder = new JarBuilder().manifest()
                .classEntry("com/example", "JavaClass", "JavaClass.java", PUBLIC_CLASS, 52)
                .classEntry("com/example/hello", "Hello", "Hello.java", PUBLIC_CLASS, 61)
                .classEntry("com/example", "KotlinClass", "KotlinClass.kt", PUBLIC_CLASS, 52)
                .classEntry("com/example", "KotlinScript", "KotlinScript.kts", PUBLIC_CLASS, 52)
                .classEntry("com/example", "ScalaClass", "ScalaClass.scala", PUBLIC_CLASS, 52)
                .classEntry("com/example", "GroovyClass", "GroovyClass.groovy", PUBLIC_CLASS, 52)
                .classEntry("com/example", "ClojureClass", "ClojureClass.clj", PUBLIC_CLASS, 52);
        JarInfo info = analyze(builder);

        assertThat(info.contents().get(ContentType.JAVA).count()).isEqualTo(2);
        assertThat(info.contents().get(ContentType.KOTLIN).count()).isEqualTo(2);
        assertThat(info.contents().get(ContentType.SCALA).count()).isEqualTo(1);
        assertThat(info.contents().get(ContentType.GROOVY).count()).isEqualTo(1);
        assertThat(info.contents().get(ContentType.CLOJURE).count()).isEqualTo(1);
        assertThat(info.contents().get(ContentType.RESOURCE).count()).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("contentTypes")
    void shouldResolveContentType(@Nullable String sourceFile, int flags, ContentType expected) throws IOException {
        JarBuilder builder = new JarBuilder().classEntry("com/example", "Klass", sourceFile, flags, 52);
        JarInfo info = analyze(builder);

        assertThat(info.contents().get(expected).count()).isEqualTo(1);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"module-info.java", "module-info.kt"})
    void shouldTreatModuleInfoAsJava(@Nullable String sourceFile) throws IOException {
        JarBuilder builder = new JarBuilder().classEntry("", "module-info", sourceFile, ClassFile.ACC_MODULE, 61);
        JarInfo info = analyze(builder);

        assertThat(info.contents().get(ContentType.JAVA).count()).isEqualTo(1);
        assertThat(info.publicClasses()).isEmpty();
        assertThat(info.nonPublicClasses()).isZero();
    }

    @Test
    void shouldCountNonClassEntryAsResource() throws IOException {
        JarBuilder builder = new JarBuilder().entry("com/example/data.txt", "hello".getBytes(UTF_8));
        JarInfo info = analyze(builder);

        assertThat(info.contents().get(ContentType.RESOURCE).count()).isEqualTo(1);
        assertThat(info.publicClasses()).isEmpty();
        assertThat(info.bytecodeVersion()).isNull();
    }

    @Test
    void shouldMarkUnparsableClassAsInvalidAndContinue() throws IOException {
        JarBuilder builder = new JarBuilder()
                .entry("com/example/Broken.class", new byte[]{1, 2, 3})
                .classEntry("com/example", "Valid", "Valid.java", PUBLIC_CLASS, 52);
        JarInfo info = analyze(builder);

        assertThat(info.contents().get(ContentType.JAVA).count()).isEqualTo(1);
        assertThat(info.contents().get(ContentType.INVALID).count()).isEqualTo(1);
        assertThat(info.publicClasses()).containsExactly(entry(ClassType.CLASS, 1));
        assertThat(info.bytecodeVersion()).isEqualTo("52.0");
    }

    @ParameterizedTest
    @MethodSource("classTypes")
    void shouldResolveClassType(int flags, ClassType expected) throws IOException {
        JarBuilder builder = new JarBuilder().classEntry("com/example", "Klass", "Klass.java", flags, 52);
        JarInfo info = analyze(builder);

        assertThat(info.publicClasses()).containsExactly(entry(expected, 1));
    }

    @Test
    void shouldResolveRecordClassType() throws IOException {
        JarBuilder builder = new JarBuilder().classEntry("com/example", "Rec", "Rec.java",
                PUBLIC_CLASS | ClassFile.ACC_FINAL, 61, RecordAttribute.of());
        JarInfo info = analyze(builder);

        assertThat(info.publicClasses()).containsExactly(entry(ClassType.RECORD, 1));
    }

    @Test
    void shouldCountNonPublicClasses() throws IOException {
        JarBuilder builder = new JarBuilder()
                .classEntry("com/example", "Pub", "Pub.java", PUBLIC_CLASS, 52)
                .classEntry("com/example", "PackagePrivate", "PackagePrivate.java", ClassFile.ACC_SUPER, 52);
        JarInfo info = analyze(builder);

        assertThat(info.publicClasses()).containsExactly(entry(ClassType.CLASS, 1));
        assertThat(info.nonPublicClasses()).isEqualTo(1);
    }

    @Test
    void shouldCountNestedPublicAndProtectedClassesAsPublic() throws IOException {
        // javac emits ACC_PUBLIC for both public and protected nested classes,
        // the source modifier only survives in the InnerClasses attribute
        JarBuilder builder = new JarBuilder()
                .classEntry("com/example", "Outer", "Outer.java", PUBLIC_CLASS, 52)
                .classEntry("com/example", "Outer$Pub", "Outer.java", PUBLIC_CLASS, 52,
                        innerClass("Outer$Pub", "Pub", ClassFile.ACC_PUBLIC))
                .classEntry("com/example", "Outer$Prot", "Outer.java", PUBLIC_CLASS, 52,
                        innerClass("Outer$Prot", "Prot", ClassFile.ACC_PROTECTED));
        JarInfo info = analyze(builder);

        assertThat(info.publicClasses()).containsExactly(entry(ClassType.CLASS, 3));
        assertThat(info.nonPublicClasses()).isZero();
    }

    @Test
    void shouldExcludeAnonymousAndLocalClasses() throws IOException {
        EnclosingMethodAttribute enclosing = EnclosingMethodAttribute.of(ClassDesc.of("com.example.Outer"),
                Optional.of("run"), Optional.of(MethodTypeDesc.of(ConstantDescs.CD_void)));
        JarBuilder builder = new JarBuilder()
                // ACC_PUBLIC proves they are skipped outright, not just filed as non-public
                .classEntry("com/example", "Outer$1", "Outer.java", PUBLIC_CLASS, 52, enclosing)
                .classEntry("com/example", "Outer$1Local", "Outer.java", ClassFile.ACC_SUPER, 52, enclosing);
        JarInfo info = analyze(builder);

        assertThat(info.publicClasses()).isEmpty();
        assertThat(info.nonPublicClasses()).isZero();
        assertThat(info.contents().get(ContentType.JAVA).count()).isEqualTo(2);
    }

    @Test
    void shouldExcludeSyntheticAndDescriptorClasses() throws IOException {
        JarBuilder builder = new JarBuilder()
                .classEntry("com/example", "Switch", "Switch.java", PUBLIC_CLASS | ClassFile.ACC_SYNTHETIC, 52)
                .classEntry("", "module-info", null, ClassFile.ACC_MODULE, 61)
                // not synthetic, so only the name excludes it
                .classEntry("com/example", "package-info", "package-info.java",
                        ClassFile.ACC_INTERFACE | ClassFile.ACC_ABSTRACT, 52);
        JarInfo info = analyze(builder);

        assertThat(info.publicClasses()).isEmpty();
        assertThat(info.nonPublicClasses()).isZero();
    }

    @Test
    void shouldNotCountMultiReleaseClassesTwice() throws IOException {
        JarBuilder builder = new JarBuilder()
                .classEntry("com/example", "Simple", "Simple.java", PUBLIC_CLASS, 52)
                .entry("META-INF/versions/17/com/example/Simple.class",
                        JarBuilder.classFile("com.example.Simple", "Simple.java", PUBLIC_CLASS, 61));
        JarInfo info = analyze(builder);

        assertThat(info.publicClasses()).containsExactly(entry(ClassType.CLASS, 1));
        assertThat(info.nonPublicClasses()).isZero();
        // the versioned copy is excluded from the bytecode version too
        assertThat(info.bytecodeVersion()).isEqualTo("52.0");
        assertThat(info.contents().get(ContentType.JAVA).count()).isEqualTo(2);
    }

    private static InnerClassesAttribute innerClass(String name, String innerName, int flags) {
        return InnerClassesAttribute.of(InnerClassInfo.of(ClassDesc.of("com.example." + name),
                Optional.of(ClassDesc.of("com.example.Outer")), Optional.of(innerName), flags));
    }

    private static Stream<Arguments> classTypes() {
        return Stream.of(
                arguments(PUBLIC_CLASS, ClassType.CLASS),
                arguments(PUBLIC_CLASS | ClassFile.ACC_ABSTRACT, ClassType.ABSTRACT_CLASS),
                arguments(ClassFile.ACC_PUBLIC | ClassFile.ACC_INTERFACE | ClassFile.ACC_ABSTRACT, ClassType.INTERFACE),
                // annotations are interfaces too, so ANNOTATION has to win
                arguments(ClassFile.ACC_PUBLIC | ClassFile.ACC_INTERFACE | ClassFile.ACC_ABSTRACT | ClassFile.ACC_ANNOTATION,
                        ClassType.ANNOTATION),
                arguments(PUBLIC_CLASS | ClassFile.ACC_ENUM, ClassType.ENUM),
                // an enum with constant bodies is abstract, ENUM still has to win
                arguments(PUBLIC_CLASS | ClassFile.ACC_ENUM | ClassFile.ACC_ABSTRACT, ClassType.ENUM)
        );
    }

    private static Stream<Arguments> contentTypes() {
        return Stream.of(
                arguments("Klass.xyz", PUBLIC_CLASS, ContentType.UNKNOWN),
                arguments("Klass", PUBLIC_CLASS, ContentType.UNKNOWN),
                arguments("Klass.KT", PUBLIC_CLASS, ContentType.KOTLIN),
                arguments(null, PUBLIC_CLASS, ContentType.UNKNOWN),
                arguments(null, ClassFile.ACC_SYNTHETIC, ContentType.SYNTHETIC)
        );
    }

    private static Stream<Arguments> buildJdkValues() {
        return Stream.of(
                arguments("25", "25"),
                arguments("1.8", "1.8"),
                arguments("1", "1"),
                arguments("21.0.10", "21"),
                arguments("1.8.0", "1.8"),
                arguments("1.8.0_292", "1.8"),
                arguments("1.4.2_18", "1.4"),
                arguments("17.0.1-dev", "17"),
                arguments("1.6.0-google-v4", "1.6"),
                arguments("1.3.1_16-b06", "1.3"),
                arguments("openjdk-11", null),
                arguments("JDK", null),
                arguments("", null)
        );
    }

    private JarInfo analyze(JarBuilder builder) throws IOException {
        try (JarInputStream jis = builder.toStream()) {
            return analyzer.analyzeJar(jis);
        }
    }
}
