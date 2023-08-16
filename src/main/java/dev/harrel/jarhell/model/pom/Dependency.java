package dev.harrel.jarhell.model.pom;

import java.util.List;

public record Dependency(String groupId,
                         String artifactId,
                         String version,
                         String type,
                         String classifier,
                         String scope,
                         Boolean optional,
                         List<Exclusion> exclusions) {}
