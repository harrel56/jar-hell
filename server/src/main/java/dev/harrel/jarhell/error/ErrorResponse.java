package dev.harrel.jarhell.error;

import io.avaje.jsonb.Json;

@Json
public record ErrorResponse(String url, String method, String message) {}
