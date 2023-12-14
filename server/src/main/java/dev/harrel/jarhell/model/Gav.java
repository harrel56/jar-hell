package dev.harrel.jarhell.model;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

public record Gav(String groupId, String artifactId, String version, String classifier) implements Comparable<Gav> {
    public Gav {
        Objects.requireNonNull(groupId, "Field 'groupId' is required");
        Objects.requireNonNull(artifactId, "Field 'artifactId' is required");
        Objects.requireNonNull(version, "Field 'version' is required");
    }

    public Gav(String groupId, String artifactId, String version) {
        this(groupId, artifactId, version, null);
    }

    public static Optional<Gav> fromCoordinate(String coordinate) {
        String[] split = coordinate.split(":");
        if (split.length == 3) {
            return Optional.of(new Gav(split[0], split[1], split[2]));
        } else if (split.length == 4) {
            return Optional.of(new Gav(split[0], split[1], split[2], split[3]));
        } else {
            return Optional.empty();
        }
    }

    public Gav stripClassifier() {
        return classifier == null ? this : new Gav(groupId, artifactId, version);
    }


    private static final Comparator<Gav> COMPARATOR = Comparator
            .comparing(Gav::groupId)
            .thenComparing(Gav::artifactId)
            .thenComparing(Gav::version)
            .thenComparing(Gav::classifier, Comparator.nullsFirst(Comparator.naturalOrder()));

    @Override
    public int compareTo(Gav o) {
        return COMPARATOR.compare(this, o);
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
