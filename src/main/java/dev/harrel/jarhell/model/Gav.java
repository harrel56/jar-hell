package dev.harrel.jarhell.model;

import java.util.Objects;

public record Gav(String groupId, String artifactId, String version) {

    public Gav {
        Objects.requireNonNull(groupId, "Field 'groupId' is required");
        Objects.requireNonNull(artifactId, "Field 'artifactId' is required");
        Objects.requireNonNull(version, "Field 'version' is required");
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }
}
