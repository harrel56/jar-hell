package dev.harrel.jarhell.model.descriptor;

import io.avaje.jsonb.Json;

@Json
public record License(String name, String url) {}
