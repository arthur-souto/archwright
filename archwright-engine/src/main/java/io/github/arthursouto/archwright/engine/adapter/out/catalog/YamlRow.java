package io.github.arthursouto.archwright.engine.adapter.out.catalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

final class YamlRow {

    private final String file;
    private final int position;
    private final String identity;
    private final String path;
    private final Map<String, Object> values;

    YamlRow(String file, int position, Map<String, Object> values) {
        this(file, position, identityOf(values), "", values);
    }

    private YamlRow(String file, int position, String identity, String path, Map<String, Object> values) {
        this.file = file;
        this.position = position;
        this.identity = identity;
        this.path = path;
        this.values = values;
    }

    String text(String key) {
        if (!(values.get(key) instanceof String text) || text.isBlank()) {
            throw problem("'" + key + "' must be a non-blank text (quote it in YAML)");
        }

        return text;
    }

    boolean has(String key) {
        return values.get(key) != null;
    }

    String optionalText(String key) {
        return values.get(key) == null ? null : text(key);
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

    Map<String, String> textMap(String key) {
        Object value = values.get(key);

        if (value == null) {
            return Map.of();
        }

        if (!(value instanceof Map<?, ?> mapping)) {
            throw problem("'" + key + "' must be a mapping");
        }

        Map<String, String> texts = new LinkedHashMap<>();
        mapping.forEach((name, item) -> texts.put(String.valueOf(name), asText(key + "." + name, item)));

        return texts;
    }

    Map<String, List<YamlRow>> groupedRows(String key) {
        if (!(values.get(key) instanceof Map<?, ?> mapping)) {
            throw problem("'" + key + "' must be a mapping");
        }

        Map<String, List<YamlRow>> grouped = new LinkedHashMap<>();
        mapping.forEach((name, entries) -> grouped.put(String.valueOf(name), childRows(key + "." + name, entries)));

        return grouped;
    }

    IllegalStateException problem(String message) {
        return CatalogChecks.problem(file, describe() + ": " + message);
    }

    private List<YamlRow> childRows(String childPath, Object entries) {
        if (!(entries instanceof List<?> items)) {
            throw problem("'" + childPath + "' must be a list");
        }

        return IntStream.range(0, items.size())
                .mapToObj(index -> child(childPath + "[" + (index + 1) + "]", items.get(index)))
                .toList();
    }

    private YamlRow child(String childPath, Object item) {
        if (!(item instanceof Map<?, ?> mapping)) {
            throw problem("'" + childPath + "' must be a mapping");
        }

        Map<String, Object> childValues = new LinkedHashMap<>();
        mapping.forEach((name, value) -> childValues.put(String.valueOf(name), value));

        return new YamlRow(file, position, identity, childPath, childValues);
    }

    private String asText(String key, Object item) {
        if (!(item instanceof String text) || text.isBlank()) {
            throw problem("'" + key + "' must contain only non-blank texts (quote numbers in YAML)");
        }

        return text;
    }

    private String describe() {
        String location = path.isEmpty() ? "" : " " + path;

        return "entry " + position + identity + location;
    }

    private static String identityOf(Map<String, Object> values) {
        Object id = values.containsKey("id") ? values.get("id") : values.get("line");

        return id instanceof String text ? " (" + text + ")" : "";
    }
}
