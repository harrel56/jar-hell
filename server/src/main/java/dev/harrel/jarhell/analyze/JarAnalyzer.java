package dev.harrel.jarhell.analyze;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import javax.inject.Singleton;
import java.io.IOException;
import java.lang.classfile.AccessFlags;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.reflect.AccessFlag;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@NullMarked
@Singleton
class JarAnalyzer {
    private static final Pattern MR_MODULE_INFO = Pattern.compile("META-INF/versions/\\d+/module-info\\.class");
    private static final String MULTI_RELEASE_PREFIX = "META-INF/versions/";
    private static final String MODULE_INFO = "module-info.class";
    private static final String PACKAGE_INFO = "package-info.class";
    private static final int MAX_ENTRY_SIZE = 2 * 1024 * 1024;
    private static final long MAX_TOTAL_SIZE = 1024L * 1024 * 1024;

    JarInfo analyzeJar(JarInputStream jis) throws IOException {
        Map<ContentType, ContentAggregate> contents = new EnumMap<>(ContentType.class);
        Map<ClassType, Integer> publicClasses = new EnumMap<>(ClassType.class);
        int nonPublicClasses = 0;
        BytecodeVersion bytecodeVersion = null;
        boolean multiReleaseJar = false;
        ModuleType moduleType = ModuleType.UNNAMED;
        String moduleName = null;
        long totalSize = 0;

        Manifest manifest = jis.getManifest();
        if (manifest != null) {
            // let's ignore manifest size for the sake of simplicity
            contents.computeIfAbsent(ContentType.RESOURCE, _ -> new ContentAggregate()).count++;

            String mrJar = manifest.getMainAttributes().getValue("Multi-Release");
            multiReleaseJar = "true".equals(mrJar);

            moduleName = manifest.getMainAttributes().getValue("Automatic-Module-Name");
            if (moduleName != null) {
                moduleType = ModuleType.AUTOMATIC;
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
                contents.computeIfAbsent(ContentType.from(classModel), _ -> new ContentAggregate()).addEntry(entry);
            } catch (IllegalArgumentException e) {
                contents.computeIfAbsent(ContentType.INVALID, _ -> new ContentAggregate()).addEntry(entry);
                continue;
            }

            // do not count MR classes, module-info, package-info and synthetic classes
            if (!name.startsWith(MULTI_RELEASE_PREFIX) && !name.endsWith(MODULE_INFO) && !name.endsWith(PACKAGE_INFO) && !classModel.flags().has(AccessFlag.SYNTHETIC)) {
                if (isPublic(classModel)) {
                    publicClasses.merge(ClassType.from(classModel), 1, Integer::sum);
                } else {
                    nonPublicClasses++;
                }
            }

            if (!name.startsWith(MULTI_RELEASE_PREFIX) && !name.endsWith(MODULE_INFO)) {
                BytecodeVersion bc = new BytecodeVersion(classModel.majorVersion(), classModel.minorVersion());
                if (bytecodeVersion == null || bytecodeVersion.compareTo(bc) < 0) {
                    bytecodeVersion = bc;
                }
            }

            if (name.equals(MODULE_INFO) || multiReleaseJar && MR_MODULE_INFO.matcher(name).matches()) {
                Optional<String> mName = classModel.findAttribute(Attributes.module()).map(attr -> attr.moduleName().name().stringValue());
                if (mName.isPresent()) {
                    moduleType = ModuleType.NAMED;
                    moduleName = mName.get();
                }
            }
        }

        return new JarInfo(toContents(contents), Collections.unmodifiableMap(publicClasses), nonPublicClasses, Objects.toString(bytecodeVersion, null),
                multiReleaseJar, moduleType, moduleName);
    }

    private static boolean isPublic(ClassModel classModel) {
        // ignore local & anonymous classes, protected nested classes will be treated as public
        return classModel.findAttribute(Attributes.enclosingMethod()).isEmpty() && classModel.flags().has(AccessFlag.PUBLIC);
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
                   Map<ClassType, Integer> publicClasses,
                   int nonPublicClasses,
                   @Nullable String bytecodeVersion,
                   boolean multiReleaseJar,
                   ModuleType moduleType,
                   @Nullable String moduleName) {}

    enum ClassType {
        CLASS, ABSTRACT_CLASS, INTERFACE, ANNOTATION, ENUM, RECORD;

        static ClassType from(ClassModel classModel) {
            AccessFlags flags = classModel.flags();
            if (flags.has(AccessFlag.ANNOTATION)) { // before INTERFACE
                return ClassType.ANNOTATION;
            } else if (flags.has(AccessFlag.INTERFACE)) {
                return ClassType.INTERFACE;
            } else if (flags.has(AccessFlag.ENUM)) {
                return ClassType.ENUM;
            } else if (flags.has(AccessFlag.ABSTRACT)) {
                return ClassType.ABSTRACT_CLASS;
            } else if (classModel.findAttribute(Attributes.record()).isPresent()) {
                return ClassType.RECORD;
            } else {
                return ClassType.CLASS;
            }
        }
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

        static ContentType from(ClassModel classModel) {
            return classModel.findAttribute(Attributes.sourceFile())
                    .map(attr -> attr.sourceFile().stringValue())
                    .map(ContentType::resolveExtension)
                    .map(ext -> BY_EXTENSION.getOrDefault(ext, UNKNOWN))
                    .orElseGet(() -> classModel.flags().has(AccessFlag.SYNTHETIC) ? ContentType.SYNTHETIC : ContentType.UNKNOWN);
        }

        private static String resolveExtension(String sourceFile) {
            int dotIndex = sourceFile.lastIndexOf('.');
            return dotIndex < 0 ? "" : sourceFile.substring(dotIndex + 1).toLowerCase();
        }
    }

    record Content(int count, long size, long compressedSize) {}

    enum ModuleType { NAMED, AUTOMATIC, UNNAMED }

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
