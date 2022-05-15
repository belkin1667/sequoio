package ru.sequoio.library.utils;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class JsonUtils {

    public static <V> String mapToJson(Map<String, V> map) {
        return Optional.ofNullable(map)
                .map(JsonUtils::mapToJsonInternal)
                .orElse(null);
    }

    private static <V> String mapToJsonInternal(Map<String, V> map) {
        return "{" +
                map.entrySet().stream()
                        .map(e ->
                            "\"" + e.getKey() + "\": \"" + e.getValue() + "\""
                        )
                        .collect(Collectors.joining(", "))
            + "}";
    }
}
