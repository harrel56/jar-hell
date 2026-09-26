package dev.harrel.jarhell.error;

import dev.harrel.jarhell.model.Gav;
import io.avaje.jex.http.HttpResponseException;
import io.avaje.jex.http.HttpStatus;

public class ResourceNotFoundException extends HttpResponseException {
    public ResourceNotFoundException(Gav gav) {
        super(HttpStatus.NOT_FOUND_404, "Package with coordinates [%s] not found".formatted(gav));
    }
}
