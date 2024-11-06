package dev.harrel.jarhell.analyze;

import java.time.LocalDateTime;
import java.util.Set;

public record FilesInfo(LocalDateTime created, Set<String> extensions, Set<String> classifiers) {}
