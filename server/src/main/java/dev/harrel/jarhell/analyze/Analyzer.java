package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.MavenApiClient;
import dev.harrel.jarhell.model.*;
import dev.harrel.jarhell.model.descriptor.DescriptorInfo;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
class Analyzer {
    private static final Logger logger = LoggerFactory.getLogger(Analyzer.class);

    private final MavenRunner mavenRunner;
    private final MavenApiClient mavenApiClient;
    private final PackageAnalyzer packageAnalyzer;

    Analyzer(MavenRunner mavenRunner, MavenApiClient mavenApiClient, PackageAnalyzer packageAnalyzer) {
        this.mavenRunner = mavenRunner;
        this.mavenApiClient = mavenApiClient;
        this.packageAnalyzer = packageAnalyzer;
    }

    public CollectedDependencies analyzeDeps(Gav gav) {
        return mavenRunner.collectDependencies(gav);
    }

    public ArtifactInfo analyzePackage(Gav gav) {
        try {
            FilesInfo filesInfo = mavenApiClient.fetchFilesInfo(gav);
            DescriptorInfo descriptorInfo = mavenRunner.resolveDescriptor(gav);
            PackageInfo packageInfo = packageAnalyzer.analyzePackage(gav, filesInfo);

            return createArtifactInfo(gav, filesInfo, packageInfo, descriptorInfo);
        } catch (Exception e) {
            logger.warn("Failed to analyze artifact: {}, marking it as unresolved", gav, e);
            return ArtifactInfo.unresolved(gav, ExceptionUtils.getRootCauseMessage(e));
        }
    }

    public ArtifactInfo.EffectiveValues computeEffectiveValues(ArtifactInfo info, List<DependencyInfo> partialDeps) {
        if (Boolean.TRUE.equals(info.unresolved())) {
            return null;
        }

        List<ArtifactInfo> requiredDeps = partialDeps.stream()
                .filter(d -> !d.optional())
                .map(DependencyInfo::artifact)
                .map(ArtifactTree::artifactInfo)
                .toList();
        int optionalDeps = partialDeps.size() - requiredDeps.size();
        int unresolvedDeps = Math.toIntExact(requiredDeps.stream().filter(dep -> Boolean.TRUE.equals(dep.unresolved())).count());
        long totalSize = Objects.requireNonNullElse(info.packageSize(), 0L) +
                requiredDeps.stream()
                        .mapToLong(a -> Objects.requireNonNullElse(a.packageSize(), 0L))
                        .sum();
        BytecodeVersion bytecodeVersion = Stream.concat(Stream.of(info), requiredDeps.stream())
                .map(ArtifactInfo::jarInfo)
                .filter(Objects::nonNull)
                .map(JarAnalyzer.JarInfo::bytecodeVersion)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        List<LicenseCount> effectiveLicenses = Stream.concat(Stream.of(info), requiredDeps.stream())
                .filter(a -> !Boolean.TRUE.equals(a.unresolved()))
                .map(a -> a.licenseTypes() == null || a.licenseTypes().isEmpty() ? List.of(LicenseType.NO_LICENSE) : a.licenseTypes())
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey(LicenseType.COMPARATOR))
                .map(entry -> new LicenseCount(entry.getKey(), entry.getValue()))
                .toList();
        LicenseType effectiveLicense = effectiveLicenses.getFirst().licenseType();
        return new ArtifactInfo.EffectiveValues(requiredDeps.size(), unresolvedDeps, optionalDeps, totalSize, bytecodeVersion,
                effectiveLicense, effectiveLicenses);
    }

    private ArtifactInfo createArtifactInfo(Gav gav, FilesInfo filesInfo, PackageInfo packageInfo, DescriptorInfo descriptorInfo) {
        return new ArtifactInfo(gav.groupId(), gav.artifactId(), gav.version(), gav.classifier(), null, null, null, null,
                packageInfo.created(), packageInfo.size(), descriptorInfo.packaging(),
                descriptorInfo.name(), descriptorInfo.description(), descriptorInfo.url(),
                descriptorInfo.scmUrl(), descriptorInfo.issuesUrl(), descriptorInfo.inceptionYear(),
                descriptorInfo.licenses(), descriptorInfo.licenseTypes(), List.copyOf(filesInfo.classifiers()), List.copyOf(filesInfo.extensions()),
                packageInfo.jarInfo(), null, null);
    }
}

