package dev.harrel.jarhell.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.List;

public record ArtifactTree(@JsonUnwrapped ArtifactInfo artifactInfo, List<DependencyInfo> dependencies) {
    public ArtifactTree withOverallSize(Long overallSize) {
        if (overallSize == null) {
            return this;
        }
        ArtifactInfo newInfo = new ArtifactInfo(artifactInfo.groupId(), artifactInfo.artifactId(), artifactInfo.version(), artifactInfo.classifier(),
                artifactInfo.unresolved(), artifactInfo.packageSize(), overallSize, artifactInfo.bytecodeVersion(), artifactInfo.packaging(),
                artifactInfo.name(), artifactInfo.description(), artifactInfo.url(), artifactInfo.inceptionYear(), artifactInfo.licenses());
        return new ArtifactTree(newInfo, dependencies);
    }
}
