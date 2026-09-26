package dev.harrel.jarhell.error;

import io.avaje.jex.http.HttpResponseException;
import io.avaje.jex.http.HttpStatus;

public class BadRequestException extends HttpResponseException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST_400, message);
    }
}
