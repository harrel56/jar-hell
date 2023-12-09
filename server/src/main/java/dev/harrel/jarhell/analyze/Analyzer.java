package dev.harrel.jarhell.analyze;

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
    private final ApiClient apiClient;
    private final PackageAnalyzer packageAnalyzer;

    Analyzer(MavenRunner mavenRunner, ApiClient apiClient, PackageAnalyzer packageAnalyzer) {
        this.mavenRunner = mavenRunner;
        this.apiClient = apiClient;
        this.packageAnalyzer = packageAnalyzer;
    }

    public boolean checkIfArtifactExists(Gav gav) {
        return apiClient.checkIfArtifactExists(gav);
    }

    public ArtifactInfo analyze(String groupId, String artifactId) {
        String version = apiClient.fetchLatestVersion(groupId, artifactId);
        return analyze(new Gav(groupId, artifactId, version));
    }

    public ArtifactInfo analyze(Gav gav) {
        FilesInfo filesInfo = apiClient.fetchFilesInfo(gav);
        DescriptorInfo descriptorInfo = mavenRunner.resolveDescriptor(gav);
        PackageInfo packageInfo = packageAnalyzer.analyzePackage(gav, filesInfo, descriptorInfo.packaging());

        return createArtifactInfo(gav, filesInfo, packageInfo, descriptorInfo);
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
}

