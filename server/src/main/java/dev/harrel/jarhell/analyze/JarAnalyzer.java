package dev.harrel.jarhell.analyze;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import javax.inject.Singleton;
import java.io.IOException;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.reflect.AccessFlag;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

@NullMarked
@Singleton
class JarAnalyzer {
    private static final String MULTI_RELEASE_PREFIX = "META-INF/versions/";
    private static final String MODULE_INFO = "module-info.class";
    private static final int MAX_ENTRY_SIZE = 2 * 1024 * 1024;
    private static final long MAX_TOTAL_SIZE = 1024L * 1024 * 1024;

    JarInfo analyzeJar(JarInputStream jis) throws IOException {
        Map<ContentType, ContentAggregate> contents = new EnumMap<>(ContentType.class);
        BytecodeVersion bytecodeVersion = null;
        boolean multiReleaseJar = false;
        long totalSize = 0;

        Manifest manifest = jis.getManifest();
        if (manifest != null) {
            // let's ignore manifest size for the sake of simplicity
            contents.computeIfAbsent(ContentType.RESOURCE, _ -> new ContentAggregate()).count++;
            String mrJar = manifest.getMainAttributes().getValue("Multi-Release");
            if ("true".equals(mrJar)) {
                multiReleaseJar = true;
            }
        }

        JarEntry entry;
        while ((entry = jis.getNextJarEntry()) != null) {
            if (entry.isDirectory()) {
                continue;
            }

            String name = entry.getName();
            if (!name.endsWith(".class")) {
                jis.closeEntry();
                totalSize = verifyTotalSize(entry, totalSize);
                contents.computeIfAbsent(ContentType.RESOURCE, _ -> new ContentAggregate()).addEntry(entry);
                continue;
            }

            byte[] bytes = jis.readNBytes(MAX_ENTRY_SIZE + 1);
            jis.closeEntry();
            totalSize = verifyTotalSize(entry, totalSize);
            if (bytes.length > MAX_ENTRY_SIZE) {
                contents.computeIfAbsent(ContentType.INVALID, _ -> new ContentAggregate()).addEntry(entry);
                continue;
            }

            ClassModel classModel;
            try {
                classModel = ClassFile.of().parse(bytes);
                classModel.findAttribute(Attributes.module()).ifPresent(attr -> attr.moduleName().name().stringValue());
                contents.computeIfAbsent(resolveContentType(classModel), _ -> new ContentAggregate()).addEntry(entry);
            } catch (IllegalArgumentException e) {
                contents.computeIfAbsent(ContentType.INVALID, _ -> new ContentAggregate()).addEntry(entry);
                continue;
            }

            if (!name.startsWith(MULTI_RELEASE_PREFIX) && !name.endsWith(MODULE_INFO)) {
                BytecodeVersion bc = new BytecodeVersion(classModel.majorVersion(), classModel.minorVersion());
                if (bytecodeVersion == null || bytecodeVersion.compareTo(bc) < 0) {
                    bytecodeVersion = bc;
                }
            }
        }

        return new JarInfo(toContents(contents), Objects.toString(bytecodeVersion, null), multiReleaseJar);
    }

    private ContentType resolveContentType(ClassModel classModel) {
        return classModel.findAttribute(Attributes.sourceFile())
                .map(attr -> attr.sourceFile().stringValue())
                .map(JarAnalyzer::resolveExtension)
                .map(ContentType::fromExtension)
                .orElseGet(() -> classModel.flags().has(AccessFlag.SYNTHETIC) ? ContentType.SYNTHETIC : ContentType.UNKNOWN);
    }

    private static String resolveExtension(String sourceFile) {
        int dotIndex = sourceFile.lastIndexOf('.');
        return dotIndex < 0 ? "" : sourceFile.substring(dotIndex + 1).toLowerCase();
    }

    private static Map<ContentType, Content> toContents(Map<ContentType, ContentAggregate> map) {
        EnumMap<ContentType, Content> res = new EnumMap<>(ContentType.class);
        map.forEach((k, v) -> res.put(k, v.toContent()));
        return Collections.unmodifiableMap(res);
    }

    // entry.getSize() is only reliable after reading or closing current entry
    private static long verifyTotalSize(JarEntry entry, long totalSize) throws IOException {
        totalSize += Math.max(entry.getSize(), 0);
        if (totalSize > MAX_TOTAL_SIZE) {
            throw new IOException("Exceeded maximum jar size (%d)".formatted(MAX_TOTAL_SIZE));
        }
        return totalSize;
    }

    record JarInfo(Map<ContentType, Content> contents,
                   @Nullable String bytecodeVersion,
                   boolean multiReleaseJar) {}

    enum ContentType {
        JAVA("java"),
        KOTLIN("kt", "kts"),
        SCALA("scala", "sc"),
        GROOVY("groovy", "gvy", "gsh"),
        CLOJURE("clj", "cljc"),
        JRUBY("rb"),
        JYTHON("py"),
        FANTOM("fan"),
        GOSU("gs"),
        CEYLON("ceylon"),
        FREGE("fr"),
        GOLO("golo"),
        FLIX("flix"),
        BALLERINA("bal"),
        X10("x10"),
        MIRAH("mirah", "duby"),
        WHILEY("whiley"),
        KAWA("scm"),
        JASMIN("j"),

        SYNTHETIC, // unknown source file with synthetic flag
        UNKNOWN, // unknown source file or unknown extension
        INVALID, // parsing classfile failed or too big

        RESOURCE; // non .class file

        private static final Map<String, ContentType> BY_EXTENSION = Arrays.stream(values())
                .flatMap(type -> type.extensions.stream().map(ext -> Map.entry(ext, type)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        private final Set<String> extensions;

        ContentType(String... extensions) {
            this.extensions = Set.of(extensions);
        }

        static ContentType fromExtension(String extension) {
            return BY_EXTENSION.getOrDefault(extension, UNKNOWN);
        }
    }

    record Content(int count, long size, long compressedSize) {}

    private static final class ContentAggregate {
        int count;
        long size, compressedSize;

        void addEntry(JarEntry entry) {
            count++;
            size += Math.max(entry.getSize(), 0);
            compressedSize += Math.max(entry.getCompressedSize(), 0);
        }

        Content toContent() {
            return new Content(count, size, compressedSize);
        }
    }

    private record BytecodeVersion(int major, int minor) implements Comparable<BytecodeVersion> {
        private static final Comparator<BytecodeVersion> COMPARATOR = Comparator
                .comparingInt(BytecodeVersion::major)
                .thenComparingInt(BytecodeVersion::minor);

        @Override
        public String toString() {
            return major + "." + minor;
        }

        @Override
        public int compareTo(BytecodeVersion o) {
            return COMPARATOR.compare(this, o);
        }
    }
}
