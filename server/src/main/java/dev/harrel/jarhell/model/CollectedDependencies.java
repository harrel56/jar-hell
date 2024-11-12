package dev.harrel.jarhell.model;

import java.util.List;

public record CollectedDependencies(List<FlatDependency> directDependencies,
                                    List<FlatDependency> allDependencies) {
    public static CollectedDependencies empty() {
        return new CollectedDependencies(List.of(), List.of());
    }
}
