package dev.harrel.jarhell.analyze;

import dev.harrel.jarhell.MavenApiClient;
import dev.harrel.jarhell.model.*;
import dev.harrel.jarhell.model.descriptor.DescriptorInfo;

import javax.inject.Singleton;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Singleton
class Analyzer {
    private final MavenRunner mavenRunner;
    private final MavenApiClient mavenApiClient;
    private final PackageAnalyzer packageAnalyzer;

    Analyzer(MavenRunner mavenRunner, MavenApiClient mavenApiClient, PackageAnalyzer packageAnalyzer) {
        this.mavenRunner = mavenRunner;
        this.mavenApiClient = mavenApiClient;
        this.packageAnalyzer = packageAnalyzer;
    }

    public boolean checkIfArtifactExists(Gav gav) {
        return mavenApiClient.checkIfArtifactExists(gav);
    }

    public AnalysisOutput analyze(String groupId, String artifactId) {
        String version = mavenApiClient.fetchLatestVersion(groupId, artifactId);
        return analyze(new Gav(groupId, artifactId, version));
    }

    public AnalysisOutput analyze(Gav gav) {
        FilesInfo filesInfo = mavenApiClient.fetchFilesInfo(gav);
        DescriptorInfo descriptorInfo = mavenRunner.resolveDescriptor(gav);
        PackageInfo packageInfo = packageAnalyzer.analyzePackage(gav, filesInfo, descriptorInfo.packaging());

        return new AnalysisOutput(createArtifactInfo(gav, filesInfo, packageInfo, descriptorInfo), descriptorInfo.dependencies());
    }

    public Long calculateTotalSize(ArtifactTree at) {
        if (at.artifactInfo().packageSize() == null) {
            return null;
        }
        return traverseTotalSize(at, new HashSet<>());
    }

    private ArtifactInfo createArtifactInfo(Gav gav, FilesInfo filesInfo, PackageInfo packageInfo, DescriptorInfo descriptorInfo) {
        return new ArtifactInfo(gav.groupId(), gav.artifactId(), gav.version(), gav.classifier(), null,
                packageInfo.size(), null, packageInfo.bytecodeVersion(), descriptorInfo.packaging(),
                descriptorInfo.name(), descriptorInfo.description(), descriptorInfo.url(), descriptorInfo.inceptionYear(),
                descriptorInfo.licences(), List.copyOf(filesInfo.classifiers()), null);
    }

    private long traverseTotalSize(ArtifactTree at, Set<Gav> visited) {
        ArtifactInfo info = at.artifactInfo();
        Gav gav = new Gav(info.groupId(), info.artifactId(), info.version(), info.classifier());
        if (visited.contains(gav)) {
            return 0;
        }

        Long size = Optional.ofNullable(info.packageSize()).orElse(0L);
        return size + at.dependencies().stream()
                .filter(d -> !Boolean.TRUE.equals(d.optional()))
                .map(DependencyInfo::artifact)
                .mapToLong(d -> traverseTotalSize(d, visited))
                .sum();
    }

    public record AnalysisOutput(ArtifactInfo artifactInfo, List<FlatDependency> dependencies) {}
}

