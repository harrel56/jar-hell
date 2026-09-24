package dev.harrel.jarhell.repo;

import io.avaje.jsonb.Jsonb;
import io.avaje.jsonb.Types;

import java.util.List;
import java.util.Map;

final class JsonUtil {
    static <T> T convert(Object from, Class<T> toType) {
        Jsonb jsonb = Jsonb.instance();
        byte[] bytes = jsonb.type(Object.class).toJsonBytes(from);
        return jsonb.type(toType).fromJson(bytes);
    }

    static Map<String, Object> convertToMap(Object from) {
        Jsonb jsonb = Jsonb.instance();
        byte[] bytes = jsonb.type(Object.class).toJsonBytes(from);
        return jsonb.type(Object.class).map().fromJson(bytes);
    }

    static <T> List<T> readList(String content, Class<T> type) {
        return Jsonb.instance().type(type).list().fromJson(content);
    }

    @SuppressWarnings("unchecked")
    static <K extends Enum<?>, V> Map<K, V> readEnumMap(String content, Class<K> keyType, Class<V> valueType) {
        return (Map<K, V>) Jsonb.instance().type(Types.newParameterizedType(Map.class, keyType, valueType)).fromJson(content);
    }
}
