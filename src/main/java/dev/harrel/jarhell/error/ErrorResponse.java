package dev.harrel.jarhell.error;

import io.javalin.http.HandlerType;

public record ErrorResponse(String url, HandlerType method, String message) {}
