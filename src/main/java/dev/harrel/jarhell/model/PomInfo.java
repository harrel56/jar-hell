package dev.harrel.jarhell.model;

import dev.harrel.jarhell.model.pom.Dependency;

import java.util.List;

public record PomInfo(String packaging, List<Dependency> dependencies) {}
