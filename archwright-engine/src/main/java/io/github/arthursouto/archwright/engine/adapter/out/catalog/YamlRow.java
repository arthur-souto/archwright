package io.github.arthursouto.archwright.engine.adapter.out.catalog;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class YamlRow {

    private final String file;
    private final int position;
    private final Map<String, Object> values;

    YamlRow(String file, int position, Map<String, Object> values) {
        this.file = file;
        this.position = position;
        this.values = values;
    }

    String text(String key) {
        if (!(values.get(key) instanceof String text) || text.isBlank()) {
            throw problem("'" + key + "' must be a non-blank text (quote it in YAML)");
        }

        return text;
    }

    int integer(String key) {
        if (!(values.get(key) instanceof Integer number)) {
            throw problem("'" + key + "' must be an integer");
        }

        return number;
    }

    boolean flag(String key) {
        Object value = values.get(key);

        if (value == null) {
            return false;
        }

        if (!(value instanceof Boolean flag)) {
            throw problem("'" + key + "' must be true or false");
        }

        return flag;
    }

    List<String> texts(String key) {
        Object value = values.get(key);

        if (value == null) {
            return List.of();
        }

        if (!(value instanceof List<?> items)) {
            throw problem("'" + key + "' must be a list");
        }

        return items.stream().map(item -> asText(key, item)).toList();
    }

    Set<String> keysOf(String key) {
        if (!(values.get(key) instanceof Map<?, ?> mapping)) {
            throw problem("'" + key + "' must be a mapping");
        }

        return mapping.keySet().stream()
                .map(String::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    IllegalStateException problem(String message) {
        return CatalogChecks.problem(file, describe() + ": " + message);
    }

    private String asText(String key, Object item) {
        if (!(item instanceof String text) || text.isBlank()) {
            throw problem("'" + key + "' must contain only non-blank texts");
        }

        return text;
    }

    private String describe() {
        return "entry " + position + identification();
    }

    private String identification() {
        Object id = values.containsKey("id") ? values.get("id") : values.get("line");

        return id instanceof String text ? " (" + text + ")" : "";
    }
}
