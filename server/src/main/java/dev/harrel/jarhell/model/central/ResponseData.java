package dev.harrel.jarhell.model.central;

import java.util.List;

public record ResponseData<T>(int numFound, int start, List<T> docs) {
}
