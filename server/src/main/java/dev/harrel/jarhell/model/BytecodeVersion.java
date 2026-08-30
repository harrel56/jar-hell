package dev.harrel.jarhell.model;

import org.jspecify.annotations.NullMarked;

import java.util.Comparator;

@NullMarked
public record BytecodeVersion(int major, int minor) implements Comparable<BytecodeVersion> {
    private static final Comparator<BytecodeVersion> COMPARATOR = Comparator
            .comparingInt(BytecodeVersion::major)
            .thenComparingInt(BytecodeVersion::minor);

    public static BytecodeVersion from(String ver) {
        String[] parts = ver.split("\\.");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid bytecode version format: " + ver);
        }
        return new BytecodeVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    @Override
    public String toString() {
        return major + "." + minor;
    }

    @Override
    public int compareTo(BytecodeVersion o) {
        return COMPARATOR.compare(this, o);
    }
}
