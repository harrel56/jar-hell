package dev.harrel.jarhell.model;

import java.time.LocalDateTime;

public record PackageInfo(LocalDateTime created, Long size, String bytecodeVersion) {}
