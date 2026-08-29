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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@NullMarked
@Singleton
class JarAnalyzer {
    private static final Pattern BUILD_JDK_REGEX = Pattern.compile("(?<ver>1\\.\\d+|\\d+)");
    private static final Pattern MR_MODULE_INFO_REGEX = Pattern.compile("META-INF/versions/\\d+/module-info\\.class");
    private static final String SERVICES_PREFIX = "META-INF/services/";
    private static final String MULTI_RELEASE_PREFIX = "META-INF/versions/";
    private static final String MODULE_INFO = "module-info.class";
    private static final String PACKAGE_INFO = "package-info.class";
    private static final int MAX_ENTRY_SIZE = 2 * 1024 * 1024;
    private static final long MAX_TOTAL_SIZE = 1024L * 1024 * 1024;

    JarInfo analyzeJar(JarInputStream jis) throws IOException {
        long totalSize = 0;
        Map<ContentType, ContentAggregate> contents = new EnumMap<>(ContentType.class);
        Map<ClassType, Integer> publicClasses = new EnumMap<>(ClassType.class);
        int nonPublicClasses = 0;
        BytecodeVersion bytecodeVersion = null;
        String buildJdk = null;
        boolean multiReleaseJar = false;
        boolean executable = false;
        Set<String> services = new LinkedHashSet<>();
        ModuleType moduleType = ModuleType.UNNAMED;
        String moduleName = null;

        Manifest manifest = jis.getManifest();
        if (manifest != null) {
            // let's ignore manifest size for the sake of simplicity
            contents.computeIfAbsent(ContentType.RESOURCE, _ -> new ContentAggregate()).count++;

            buildJdk = manifest.getMainAttributes().getValue("Build-Jdk-Spec");
            if (buildJdk == null) {
                buildJdk = parseBuildJdk(manifest.getMainAttributes().getValue("Build-Jdk"));
            }

            String mrJar = manifest.getMainAttributes().getValue("Multi-Release");
            multiReleaseJar = "true".equals(mrJar);

            executable = manifest.getMainAttributes().getValue("Main-Class") != null;
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
            if (name.startsWith(SERVICES_PREFIX)) {
                services.add(name.substring(SERVICES_PREFIX.length()));
            }

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

            if (shouldCountClassType(name, classModel)) {
                if (classModel.flags().has(AccessFlag.PUBLIC)) {
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

            if (name.equals(MODULE_INFO) || multiReleaseJar && MR_MODULE_INFO_REGEX.matcher(name).matches()) {
                Optional<String> mName = classModel.findAttribute(Attributes.module()).map(attr -> attr.moduleName().name().stringValue());
                if (mName.isPresent()) {
                    moduleType = ModuleType.NAMED;
                    moduleName = mName.get();
                }
            }
        }

        return new JarInfo(toContents(contents), Collections.unmodifiableMap(publicClasses), nonPublicClasses, Objects.toString(bytecodeVersion, null),
                buildJdk, multiReleaseJar, executable, Collections.unmodifiableSet(services), moduleType, moduleName);
    }

    /*
    17 -> 17
    1.8 -> 1.8
    1.8.0 -> 1.8
    1.8_245 -> 1.8
    17.0.1-dev -> 17
     */
    private static @Nullable String parseBuildJdk(@Nullable String buildJdk) {
        if (buildJdk == null) {
            return null;
        }
        Matcher matcher = BUILD_JDK_REGEX.matcher(buildJdk);
        return matcher.lookingAt() ? matcher.group("ver") : null;
    }

    // do not count MR classes, module-info, package-info, local, anonymous and synthetic classes
    private static boolean shouldCountClassType(String name, ClassModel classModel) {
        return !name.startsWith(MULTI_RELEASE_PREFIX) && !name.endsWith(MODULE_INFO) && !name.endsWith(PACKAGE_INFO)
                && !classModel.flags().has(AccessFlag.SYNTHETIC) && classModel.findAttribute(Attributes.enclosingMethod()).isEmpty();
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
                   @Nullable String buildJdk,
                   boolean multiReleaseJar,
                   boolean executable,
                   Set<String> services,
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
            // treat explicitly module-info.class as java
            if (classModel.flags().has(AccessFlag.MODULE)) {
                return ContentType.JAVA;
            }
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
