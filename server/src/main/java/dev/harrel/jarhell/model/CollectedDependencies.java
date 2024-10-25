package dev.harrel.jarhell.model;

import java.util.List;

public record CollectedDependencies(List<FlatDependency> directDependencies,
                                    List<FlatDependency> allDependencies) {}
