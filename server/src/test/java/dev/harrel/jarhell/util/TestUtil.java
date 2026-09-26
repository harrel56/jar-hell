package dev.harrel.jarhell.util;

import io.avaje.jsonb.Jsonb;

import java.util.List;

public class TestUtil {
    public static String writeJson(Object obj) {
        return Jsonb.instance().toJson(obj);
    }

    public static <T> T readJson(String json, Class<T> type) {
        return Jsonb.instance().type(type).fromJson(json);
    }

    public static <T> List<T> readList(String json, Class<T> type) {
        return Jsonb.instance().type(type).list().fromJson(json);
    }
}
