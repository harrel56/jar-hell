package dev.harrel.jarhell.model.pom;


import dev.harrel.jarhell.model.Gav;

import java.util.List;
import java.util.Map;

public record ProjectModel(Gav parent,
                           String packaging,
                           Map<String, String> properties,
                           List<Dependency> dependencies) {
}
