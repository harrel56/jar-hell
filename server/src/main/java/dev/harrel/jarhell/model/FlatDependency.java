package dev.harrel.jarhell.model;

public record FlatDependency(Gav gav, boolean optional, String scope) {}
