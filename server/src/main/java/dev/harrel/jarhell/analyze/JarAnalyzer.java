package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.model.BytecodeVersion;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import javax.inject.Singleton;
import java.io.ByteArrayOutputStream;
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
public final class JarAnalyzer {
    private static final Pattern BUILD_JDK_REGEX = Pattern.compile("(?<ver>1\\.\\d+|\\d+)");
    private static final Pattern MR_MODULE_INFO_REGEX = Pattern.compile("META-INF/versions/\\d+/module-info\\.class");
    private static final String SERVICES_PREFIX = "META-INF/services/";
    private static final String MULTI_RELEASE_PREFIX = "META-INF/versions/";
    private static final String MODULE_INFO = "module-info.class";
    private static final String PACKAGE_INFO = "package-info.class";
    private static final String METADATA_PREFIX = "META-INF/";
    private static final int MAX_ENTRY_SIZE = 2 * 1024 * 1024;
    private static final int MAGIC_LENGTH = 8;
    private static final List<byte[]> NATIVE_MAGIC = List.of(
            new byte[]{0x7F, 'E', 'L', 'F'}, // ELF (linux, bsd, android, aix)
            new byte[]{'M', 'Z'}, // PE (windows dll/exe)
            new byte[]{(byte) 0xFE, (byte) 0xED, (byte) 0xFA, (byte) 0xCE}, // Mach-O 32-bit big-endian
            new byte[]{(byte) 0xFE, (byte) 0xED, (byte) 0xFA, (byte) 0xCF}, // Mach-O 64-bit big-endian
            new byte[]{(byte) 0xCE, (byte) 0xFA, (byte) 0xED, (byte) 0xFE}, // Mach-O 32-bit little-endian
            new byte[]{(byte) 0xCF, (byte) 0xFA, (byte) 0xED, (byte) 0xFE}, // Mach-O 64-bit little-endian
            // Mach-O fat binaries (0xCAFEBABE) are deliberately skipped: same magic as class files
            new byte[]{'!', '<', 'a', 'r', 'c', 'h', '>', '\n'} // ar archive (static libs)
    );
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
            contents.computeIfAbsent(ContentType.METADATA, _ -> new ContentAggregate()).addManifest(manifest);

            buildJdk = manifest.getMainAttributes().getValue("Build-Jdk-Spec");
            if (buildJdk == null) {
                buildJdk = parseBuildJdk(manifest.getMainAttributes().getValue("Build-Jdk"));
            }

            String mrJar = manifest.getMainAttributes().getValue("Multi-Release");
            multiReleaseJar = "true".equalsIgnoreCase(mrJar);

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
                ContentType type = ContentType.fromEntryName(name);
                if (type == ContentType.RESOURCE && isNativeBinary(jis.readNBytes(MAGIC_LENGTH))) {
                    type = ContentType.NATIVE;
                }
                if (name.startsWith(METADATA_PREFIX) && ContentType.METADATA_UNDER_META_INF.contains(type)) {
                    type = ContentType.METADATA;
                }
                jis.closeEntry();
                totalSize = verifyTotalSize(entry, totalSize);
                contents.computeIfAbsent(type, _ -> new ContentAggregate()).addEntry(entry);
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

        return new JarInfo(toContents(contents), Collections.unmodifiableMap(publicClasses), nonPublicClasses, bytecodeVersion,
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

    // extension-less executables and shared libraries (e.g. bundled node binaries) are only recognizable by their magic bytes
    private static boolean isNativeBinary(byte[] head) {
        for (byte[] magic : NATIVE_MAGIC) {
            if (head.length >= magic.length && Arrays.mismatch(head, 0, magic.length, magic, 0, magic.length) < 0) {
                return true;
            }
        }
        return false;
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

    public record JarInfo(Map<ContentType, Content> contents,
                          Map<ClassType, Integer> publicClasses,
                          int nonPublicClasses,
                          @Nullable BytecodeVersion bytecodeVersion,
                          @Nullable String buildJdk,
                          boolean multiReleaseJar,
                          boolean executable,
                          Set<String> services,
                          ModuleType moduleType,
                          @Nullable String moduleName) {}

    public enum ClassType {
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

    public enum ContentType {
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

        // non .class entries, classified by extension
        NATIVE("so", "dll", "dylib", "jnilib", "a", "lib", "exe"),
        ARCHIVE("jar", "war", "ear", "aar", "zip", "gz", "tgz", "tar", "xz", "bz2", "zst", "7z", "rar"),
        XML("xml", "xsd", "dtd", "xsl", "xslt", "tld", "wsdl"),
        JSON("json", "jsonc", "jsonl", "json5", "avsc", "geojson"),
        CONFIG("properties", "yml", "yaml", "toml", "conf", "hocon", "ini", "cfg", "kdl"),
        WEB("js", "mjs", "cjs", "ts", "jsx", "tsx", "vue", "css", "scss", "less", "html", "htm", "xhtml", "map"),
        MEDIA("png", "jpg", "jpeg", "gif", "bmp", "webp", "svg", "ico", "icns", "tif", "tiff",
                "ttf", "otf", "woff", "woff2", "eot",
                "mp3", "wav", "ogg", "flac", "aac", "mid", "midi",
                "mp4", "webm", "avi", "mov", "mkv"),
        SCRIPT("sh", "bash", "bat", "cmd", "ps1"),
        TEXT("txt", "md", "markdown"),

        // non .class entries, classified by other means
        SOURCE, // source file of any language listed above
        METADATA, // descriptor-like files under META-INF/ (see METADATA_UNDER_META_INF)
        RESOURCE; // anything else

        private static final Map<String, ContentType> BY_SOURCE_EXTENSION = byExtension(EnumSet.range(JAVA, JASMIN));
        private static final Map<String, ContentType> BY_RESOURCE_EXTENSION = byExtension(EnumSet.range(NATIVE, TEXT));
        static final Set<ContentType> METADATA_UNDER_META_INF = EnumSet.of(XML, JSON, CONFIG, TEXT, RESOURCE);
        private static final Set<String> TEXT_BASENAMES = Set.of("license", "notice", "readme", "copying", "copyright", "changelog", "authors");

        private final Set<String> extensions;

        ContentType(String... extensions) {
            this.extensions = Set.of(extensions);
        }

        private static Map<String, ContentType> byExtension(Set<ContentType> types) {
            return types.stream()
                    .flatMap(type -> type.extensions.stream().map(ext -> Map.entry(ext, type)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        static ContentType from(ClassModel classModel) {
            // treat explicitly module-info.class as java
            if (classModel.flags().has(AccessFlag.MODULE)) {
                return ContentType.JAVA;
            }
            return classModel.findAttribute(Attributes.sourceFile())
                    .map(attr -> attr.sourceFile().stringValue())
                    .map(ContentType::resolveExtension)
                    .map(ext -> BY_SOURCE_EXTENSION.getOrDefault(ext, UNKNOWN))
                    .orElseGet(() -> classModel.flags().has(AccessFlag.SYNTHETIC) ? ContentType.SYNTHETIC : ContentType.UNKNOWN);
        }

        static ContentType fromEntryName(String name) {
            String ext = resolveExtension(name);
            ContentType byExtension = BY_RESOURCE_EXTENSION.get(ext);
            if (byExtension != null) {
                return byExtension;
            }
            if (BY_SOURCE_EXTENSION.containsKey(ext)) {
                return SOURCE;
            }
            String basename = name.substring(name.lastIndexOf('/') + 1).toLowerCase();
            for (String textBasename : TEXT_BASENAMES) {
                if (basename.startsWith(textBasename)) {
                    return TEXT;
                }
            }
            return RESOURCE;
        }

        private static String resolveExtension(String sourceFile) {
            int dotIndex = sourceFile.lastIndexOf('.');
            return dotIndex < 0 || dotIndex < sourceFile.lastIndexOf('/') ? "" : sourceFile.substring(dotIndex + 1).toLowerCase();
        }
    }

    public record Content(int count, long size, long compressedSize) {}

    public enum ModuleType { NAMED, AUTOMATIC, UNNAMED }

    private static final class ContentAggregate {
        int count;
        long size, compressedSize;

        void addEntry(JarEntry entry) {
            count++;
            size += Math.max(entry.getSize(), 0);
            compressedSize += Math.max(entry.getCompressedSize(), 0);
        }

        // JarInputStream consumes the manifest entry before we can see its sizes,
        // so re-serialize it instead; compressed size is a rough guess
        void addManifest(Manifest manifest) throws IOException {
            var out = new ByteArrayOutputStream();
            manifest.write(out);
            count++;
            size += out.size();
            compressedSize += out.size() / 2;
        }

        Content toContent() {
            return new Content(count, size, compressedSize);
        }
    }
}
