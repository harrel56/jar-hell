package dev.harrel.jarhell.error;

import dev.harrel.jarhell.model.Gav;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(Gav gav) {
        super("Package with coordinates [%s] not found".formatted(gav));
    }
}
