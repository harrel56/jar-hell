package dev.harrel.jarhell.model;

public record ArtifactInfo(String groupId,
                           String artifactId,
                           String version,
                           Long packageSize,
                           String bytecodeVersion,
                           String packaging) {
    public static ArtifactInfo create(Gav gav, PackageInfo packageInfo, PomInfo pomInfo) {
        return new ArtifactInfo(gav.groupId(), gav.artifactId(), gav.version(),
                packageInfo.size(), packageInfo.bytecodeVersion(), pomInfo.packaging());
    }
}
