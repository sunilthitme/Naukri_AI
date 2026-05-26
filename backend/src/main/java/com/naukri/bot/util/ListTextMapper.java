package com.naukri.bot.util;

import java.util.Arrays;
import java.util.List;

public final class ListTextMapper {
    private ListTextMapper() {
    }

    public static String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join("\n", values.stream().filter(value -> value != null && !value.isBlank()).toList());
    }

    public static List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\R|,"))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .distinct()
                .toList();
    }
}
