package dev.harrel.jarhell.analyze;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrel.jarhell.model.*;
import dev.harrel.jarhell.model.descriptor.DescriptorInfo;

import java.net.http.HttpClient;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

class Analyzer {
    private final MavenRunner mavenRunner;
    private final ApiClient apiClient;
    private final PackageAnalyzer packageAnalyzer;

    public Analyzer(ObjectMapper objectMapper, MavenRunner mavenRunner) {
        this.mavenRunner = mavenRunner;
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        this.apiClient = new ApiClient(objectMapper, httpClient);
        this.packageAnalyzer = new PackageAnalyzer(httpClient);
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

        return createArtifactInfo(gav, packageInfo, descriptorInfo);
    }

    public Long calculateTotalSize(ArtifactTree at) {
        if (at.artifactInfo().packageSize() == null) {
            return null;
        }
        return traverseTotalSize(at, new HashSet<>());
    }

    private ArtifactInfo createArtifactInfo(Gav gav, PackageInfo packageInfo, DescriptorInfo descriptorInfo) {
        return new ArtifactInfo(gav.groupId(), gav.artifactId(), gav.version(), gav.classifier(), null,
                packageInfo.size(), null, packageInfo.bytecodeVersion(), descriptorInfo.packaging(),
                descriptorInfo.name(), descriptorInfo.description(), descriptorInfo.url(), descriptorInfo.inceptionYear(),
                descriptorInfo.licences());
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

