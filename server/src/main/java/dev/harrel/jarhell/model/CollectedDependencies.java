package dev.harrel.jarhell.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record CollectedDependencies(List<FlatDependency> directDependencies,
                                    List<FlatDependency> allDependencies,
                                    Map<Gav, Set<Gav>> cyclesToBreak) {}
