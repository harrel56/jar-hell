package dev.harrel.jarhell.model;

import dev.harrel.jarhell.analyze.JarAnalyzer;

import java.time.LocalDateTime;

public record PackageInfo(LocalDateTime created, Long size, JarAnalyzer.JarInfo jarInfo) {}
