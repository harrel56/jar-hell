package dev.harrel.jarhell.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.List;

public record ArtifactTree(@JsonUnwrapped ArtifactInfo artifactInfo, List<DependencyInfo> dependencies) {
    public ArtifactTree withTotalSize(Long totalSize) {
        if (totalSize == null) {
            return this;
        }
        ArtifactInfo newInfo = new ArtifactInfo(artifactInfo.groupId(), artifactInfo.artifactId(), artifactInfo.version(), artifactInfo.classifier(),
                artifactInfo.unresolved(), artifactInfo.packageSize(), totalSize, artifactInfo.bytecodeVersion(), artifactInfo.packaging(),
                artifactInfo.name(), artifactInfo.description(), artifactInfo.url(), artifactInfo.inceptionYear(), artifactInfo.licenses(),
                artifactInfo.created());
        return new ArtifactTree(newInfo, dependencies);
    }
}
