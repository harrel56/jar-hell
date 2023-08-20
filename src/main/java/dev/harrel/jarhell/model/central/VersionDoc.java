package dev.harrel.jarhell.model.central;

import java.util.List;

public record VersionDoc(String p, Long timestamp, List<String> ec) {}
