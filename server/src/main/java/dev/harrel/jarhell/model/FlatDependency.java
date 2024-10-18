package dev.harrel.jarhell.model;

public record FlatDependency(Gav gav, boolean optional, String scope) {
    @Override
    public String toString() {
        return "%s (%s)%s".formatted(gav, scope, optional ? "?" : "");
    }
}
