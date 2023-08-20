package dev.harrel.jarhell.model;

import dev.harrel.jarhell.model.descriptor.DescriptorInfo;

public record ArtifactInfo(String groupId,
                           String artifactId,
                           String version,
                           Long packageSize,
                           String bytecodeVersion,
                           String packaging) {
    public static ArtifactInfo create(Gav gav, PackageInfo packageInfo, DescriptorInfo descriptorInfo) {
        return new ArtifactInfo(gav.groupId(), gav.artifactId(), gav.version(),
                packageInfo.size(), packageInfo.bytecodeVersion(), descriptorInfo.packaging());
    }
}
