package dev.harrel.jarhell.model;

public record Gav(String groupId, String artifactId, String version) {
    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }
}
