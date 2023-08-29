package dev.harrel.jarhell.model;

import java.util.Objects;
import java.util.StringJoiner;

public record Gav(String groupId, String artifactId, String version, String classifier) {
    public Gav {
        Objects.requireNonNull(groupId, "Field 'groupId' is required");
        Objects.requireNonNull(artifactId, "Field 'artifactId' is required");
        Objects.requireNonNull(version, "Field 'version' is required");
    }

    public Gav(String groupId, String artifactId, String version) {
        this(groupId, artifactId, version, null);
    }

    public static Gav fromCoordinate(String coordinate) {
        String[] split = coordinate.split(":");
        if (split.length == 3) {
            return new Gav(split[0], split[1], split[2]);
        } else if (split.length == 4) {
            return new Gav(split[0], split[1], split[2], split[3]);
        } else {
            throw new IllegalArgumentException("Invalid coordinate format");
        }
    }

    public Gav stripClassifier() {
        return classifier == null ? this : new Gav(groupId, artifactId, version);
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(":")
                .add(groupId)
                .add(artifactId)
                .add(version);
        if (classifier != null) {
            joiner.add(classifier);
        }
        return joiner.toString();
    }
}
