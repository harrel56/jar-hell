package dev.harrel.jarhell.analyze;

import javax.inject.Singleton;
import java.io.IOException;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.reflect.AccessFlag;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

@Singleton
class JarAnalyzer {
    private static final String MULTI_RELEASE_PREFIX = "META-INF/versions/";
    private static final String MODULE_INFO = "module-info.class";

    JarInfo analyzeJar(JarInputStream jis) throws IOException {
        Map<ContentType, Content> contents = new EnumMap<>(ContentType.class);
        SortedMap<Integer, Integer> multiReleaseVersions = new TreeMap<>();
        int maxBytecodeVersion = 0;

        JarEntry entry;
        while ((entry = jis.getNextJarEntry()) != null) {
            if (entry.isDirectory()) {
                continue;
            }
            String name = entry.getName();
            if (!name.endsWith(".class")) {
                jis.closeEntry();
                mergeContent(contents, ContentType.RESOURCES, entry);
                continue;
            }

            ClassModel classModel = parseClass(jis.readAllBytes());
            mergeContent(contents, resolveContentType(classModel), entry);

            Integer multiReleaseVersion = resolveMultiReleaseVersion(name);
            if (multiReleaseVersion != null) {
                multiReleaseVersions.merge(multiReleaseVersion, 1, Integer::sum);
            } else if (classModel != null && !name.endsWith(MODULE_INFO)) {
                maxBytecodeVersion = Math.max(maxBytecodeVersion, classModel.majorVersion());
            }
        }

        return new JarInfo(Map.copyOf(contents),
                maxBytecodeVersion == 0 ? null : maxBytecodeVersion,
                Collections.unmodifiableSortedMap(multiReleaseVersions));
    }

    /** Returns the {@code META-INF/versions/<N>} release of an entry, or {@code null} if it is not versioned. */
    private static Integer resolveMultiReleaseVersion(String name) {
        if (!name.startsWith(MULTI_RELEASE_PREFIX)) {
            return null;
        }
        int slashIndex = name.indexOf('/', MULTI_RELEASE_PREFIX.length());
        if (slashIndex < 0) {
            return null;
        }
        try {
            return Integer.valueOf(name.substring(MULTI_RELEASE_PREFIX.length(), slashIndex));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void mergeContent(Map<ContentType, Content> contents, ContentType contentType, JarEntry entry) {
        Content content = new Content(1, Math.max(entry.getSize(), 0), Math.max(entry.getCompressedSize(), 0));
        contents.merge(contentType, content, Content::add);
    }

    private ClassModel parseClass(byte[] classBytes) {
        try {
            return ClassFile.of().parse(classBytes);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private ContentType resolveContentType(ClassModel classModel) {
        if (classModel == null) {
            return ContentType.UNKNOWN;
        }
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
        /** Class file with no {@code SourceFile} attribute, flagged as {@code ACC_SYNTHETIC}. */
        SYNTHETIC,
        /** Class file that could not be attributed to any known language. */
        UNKNOWN,
        /** Any jar entry that is not a class file. */
        RESOURCES;

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

    /**
     * @param maxBytecodeVersion highest class file major version among base classes, {@code null} if there are none
     * @param multiReleaseVersions {@code META-INF/versions/<N>} release to class count, empty if not a multi-release jar
     */
    record JarInfo(Map<ContentType, Content> contents,
                   Integer maxBytecodeVersion,
                   SortedMap<Integer, Integer> multiReleaseVersions) {}

    record Content(int count, long size, long compressedSize) {
        Content add(Content other) {
            return new Content(count + other.count, size + other.size, compressedSize + other.compressedSize);
        }
    }
}
