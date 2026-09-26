package dev.harrel.jarhell.model;

import io.avaje.jsonb.Json;

import java.util.List;

@Json
public record ArtifactTree(ArtifactInfo artifactInfo, List<DependencyInfo> dependencies) {}
