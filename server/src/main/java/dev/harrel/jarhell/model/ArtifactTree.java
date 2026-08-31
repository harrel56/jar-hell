package dev.harrel.jarhell.model;

import java.util.List;

public record ArtifactTree(ArtifactInfo artifactInfo, List<DependencyInfo> dependencies) {}
