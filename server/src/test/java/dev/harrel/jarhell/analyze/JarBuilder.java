package dev.harrel.jarhell.analyze;

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassFileVersion;
import java.lang.classfile.attribute.SourceFileAttribute;
import java.lang.constant.ClassDesc;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.*;

final class JarBuilder {
    private final Map<String, byte[]> entries = new LinkedHashMap<>();
    private Manifest manifest;

    @SafeVarargs
    final JarBuilder manifest(Map.Entry<String, String>... attributes) {
        manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        for (Map.Entry<String, String> attr : attributes) {
            manifest.getMainAttributes().putValue(attr.getKey(), attr.getValue());
        }
        return this;
    }

    JarBuilder classEntry(String path, String name, @Nullable String sourceFile, int flags, ClassFileVersion version,
                          ClassElement... elements) {
        String entryPath = path.isEmpty() ? name + ".class" : path + "/" + name + ".class";
        String className = path.isEmpty() ? name : path.replace('/', '.') + "." + name;
        entries.put(entryPath, classFile(className, sourceFile, flags, version, elements));
        return this;
    }

    JarBuilder dirEntry(String path) {
        return entry(path.endsWith("/") ? path : path + "/", new byte[0]);
    }

    JarBuilder entry(String path, byte[] content) {
        entries.put(path, content);
        return this;
    }

    JarInputStream toStream() throws IOException {
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

    static byte[] classFile(String binaryName, @Nullable String sourceFile, int flags, ClassFileVersion version,
                            ClassElement... elements) {
        return ClassFile.of().build(ClassDesc.of(binaryName), classBuilder -> {
            classBuilder.with(version);
            classBuilder.withFlags(flags);
            if (sourceFile != null) {
                classBuilder.with(SourceFileAttribute.of(sourceFile));
            }
            for (ClassElement element : elements) {
                classBuilder.with(element);
            }
        });
    }
}
