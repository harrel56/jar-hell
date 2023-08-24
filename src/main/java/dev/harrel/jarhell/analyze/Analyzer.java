package dev.harrel.jarhell.analyze;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.harrel.jarhell.model.ArtifactInfo;
import dev.harrel.jarhell.model.Gav;
import dev.harrel.jarhell.model.PackageInfo;
import dev.harrel.jarhell.model.descriptor.DescriptorInfo;

import java.net.http.HttpClient;

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

    public ArtifactInfo analyze(String groupId, String artifactId) {
        String version = apiClient.fetchLatestVersion(groupId, artifactId);
        return analyze(new Gav(groupId, artifactId, version));
    }

    public ArtifactInfo analyze(Gav gav) {
        FilesInfo filesInfo = apiClient.fetchFilesInfo(gav);
        if (!filesInfo.extensions().contains("pom")) {
            throw new IllegalArgumentException("Artifact is missing pom file: " + gav);
        }
        DescriptorInfo descriptorInfo = mavenRunner.resolveDescriptor(gav);
        PackageInfo packageInfo = packageAnalyzer.analyzePackage(gav, filesInfo, descriptorInfo.packaging());

        return createArtifactInfo(gav, packageInfo, descriptorInfo);
    }

    private ArtifactInfo createArtifactInfo(Gav gav, PackageInfo packageInfo, DescriptorInfo descriptorInfo) {
        return new ArtifactInfo(gav.groupId(), gav.artifactId(), gav.version(), gav.classifier(),
                packageInfo.size(), packageInfo.bytecodeVersion(), descriptorInfo.packaging(),
                descriptorInfo.name(), descriptorInfo.description(), descriptorInfo.url(), descriptorInfo.inceptionYear(),
                descriptorInfo.licences());
    }
}

