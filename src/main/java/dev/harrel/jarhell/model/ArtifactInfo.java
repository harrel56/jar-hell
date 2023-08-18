package dev.harrel.jarhell.model;

public record ArtifactInfo(String groupId,
                           String artifactId,
                           String version,
                           Long jarSize,
                           String bytecodeVersion,
                           String packaging) {
    public static ArtifactInfo create(Gav gav, JarInfo jarInfo, PomInfo pomInfo) {
        return new ArtifactInfo(gav.groupId(), gav.artifactId(), gav.version(),
                jarInfo.size(), jarInfo.bytecodeVersion(), pomInfo.packaging());
    }
}
