package dev.harrel.jarhell.util;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class TestUtil {
    public static <T> List<T> iteratorToList(Iterator<T> it) {
        Stream.Builder<T> builder = Stream.builder();
        it.forEachRemaining(builder);
        return builder.build().toList();
    }
}
