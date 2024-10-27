package dev.harrel.jarhell.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.List;

public record ArtifactTree(@JsonUnwrapped ArtifactInfo artifactInfo, List<DependencyInfo> dependencies) {}
