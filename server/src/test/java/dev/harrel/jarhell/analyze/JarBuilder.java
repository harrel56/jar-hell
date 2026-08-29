package dev.harrel.jarhell.analyze;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.attribute.SourceFileAttribute;
import java.lang.constant.ClassDesc;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Builds jars in memory for {@link JarAnalyzer} tests, so no binary fixtures are committed.
 * Class files are synthesized with the classfile API, which lets tests set flags, attributes
 * and bytecode versions that javac would not emit.
 */
final class JarBuilder {
    private final Map<String, byte[]> entries = new LinkedHashMap<>();
    private Manifest manifest;

    static JarBuilder jar() {
        return new JarBuilder();
    }

    /** Adds a manifest with just {@code Manifest-Version} if none was created yet. */
    JarBuilder manifest() {
        if (manifest == null) {
            manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        }
        return this;
    }

    JarBuilder mainAttribute(String name, String value) {
        manifest();
        manifest.getMainAttributes().putValue(name, value);
        return this;
    }

    JarBuilder entry(String path, byte[] content) {
        entries.put(path, content);
        return this;
    }

    /** Adds a public class compiled to Java 17 with a {@code SourceFile} of {@code <SimpleName>.java}. */
    JarBuilder javaClass(String binaryName) {
        return entry(pathOf(binaryName), publicClass(binaryName));
    }

    JarInputStream open() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JarOutputStream jos = manifest == null ? new JarOutputStream(out) : new JarOutputStream(out, manifest)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                jos.putNextEntry(new JarEntry(entry.getKey()));
                jos.write(entry.getValue());
                jos.closeEntry();
            }
        }
        return new JarInputStream(new ByteArrayInputStream(out.toByteArray()));
    }

    static String pathOf(String binaryName) {
        return binaryName.replace('.', '/') + ".class";
    }

    static byte[] publicClass(String binaryName) {
        return classFile(binaryName, simpleNameOf(binaryName) + ".java",
                ClassFile.ACC_PUBLIC | ClassFile.ACC_SUPER, ClassFile.JAVA_17_VERSION);
    }

    static byte[] classFile(String binaryName, String sourceFile, int flags, int majorVersion) {
        return ClassFile.of().build(ClassDesc.of(binaryName), classBuilder -> {
            classBuilder.withVersion(majorVersion, 0);
            classBuilder.withFlags(flags);
            if (sourceFile != null) {
                classBuilder.with(SourceFileAttribute.of(sourceFile));
            }
        });
    }

    private static String simpleNameOf(String binaryName) {
        return binaryName.substring(binaryName.lastIndexOf('.') + 1);
    }
}
