package dev.harrel.jarhell.model;

import java.util.Objects;

public record Gav(String groupId, String artifactId, String version) {
    public Gav {
        Objects.requireNonNull(groupId, "Field 'groupId' is required");
        Objects.requireNonNull(artifactId, "Field 'artifactId' is required");
        Objects.requireNonNull(version, "Field 'version' is required");
    }

    public static Gav fromCoordinate(String coordinate) {
        String[] split = coordinate.split(":");
        if (split.length != 3) {
            throw new IllegalArgumentException("Invalid coordinate format");
        }
        return new Gav(split[0], split[1], split[2]);
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }
}
