package io.github.arthursouto.archwright.engine.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record TemplateContext(Map<String, Object> values) {

    public TemplateContext {
        values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public Optional<Object> find(String path) {
        Object current = values;

        for (String key : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> mapping) || !mapping.containsKey(key)) {
                return Optional.empty();
            }

            current = mapping.get(key);
        }

        return Optional.of(current);
    }

    public boolean flag(String path) {
        return find(path)
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElseThrow(() -> new IllegalArgumentException("not a flag in the template context: " + path));
    }

    public String text(String path) {
        return find(path)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .orElseThrow(() -> new IllegalArgumentException("not a text in the template context: " + path));
    }

    public TemplateContext section(String path) {
        return find(path)
                .flatMap(TemplateContext::asSection)
                .orElseThrow(() -> new IllegalArgumentException("not a section in the template context: " + path));
    }

    public List<TemplateContext> sections(String path) {
        if (!(find(path).orElse(null) instanceof List<?> items)) {
            throw new IllegalArgumentException("not a list in the template context: " + path);
        }

        return items.stream()
                .map(item -> asSection(item)
                        .orElseThrow(() -> new IllegalArgumentException("list item is not a section: " + path)))
                .toList();
    }

    private static Optional<TemplateContext> asSection(Object value) {
        if (!(value instanceof Map<?, ?> mapping)) {
            return Optional.empty();
        }

        Map<String, Object> copy = new LinkedHashMap<>();
        mapping.forEach((key, item) -> copy.put(String.valueOf(key), item));

        return Optional.of(new TemplateContext(copy));
    }
}
