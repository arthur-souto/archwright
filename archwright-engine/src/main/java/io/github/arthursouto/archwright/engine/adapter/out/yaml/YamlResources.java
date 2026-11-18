package io.github.arthursouto.archwright.engine.adapter.out.yaml;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public final class YamlResources {

    private YamlResources() {
    }

    public static Object load(ClassLoader loader, String resource, String source) {
        try (Reader reader = open(loader, resource, source)) {
            return yaml().load(reader);
        } catch (IOException | YAMLException exception) {
            throw new IllegalStateException(source + ": " + exception.getMessage(), exception);
        }
    }

    public static List<YamlRow> rows(String source, Object document) {
        if (!(document instanceof List<?> entries) || entries.isEmpty()) {
            throw new IllegalStateException(source + ": must be a non-empty list");
        }

        return IntStream.range(0, entries.size())
                .mapToObj(index -> row(source, index + 1, entries.get(index)))
                .toList();
    }

    public static YamlRow mapping(String source, Object document) {
        return row(source, 1, document);
    }

    private static Reader open(ClassLoader loader, String resource, String source) {
        InputStream stream = loader.getResourceAsStream(resource);

        if (stream == null) {
            throw new IllegalStateException(source + ": not found on classpath");
        }

        return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }

    private static Yaml yaml() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);

        return new Yaml(new SafeConstructor(options));
    }

    private static YamlRow row(String source, int position, Object entry) {
        if (!(entry instanceof Map<?, ?> mapping)) {
            throw new IllegalStateException(source + ": entry " + position + " must be a mapping");
        }

        Map<String, Object> values = new LinkedHashMap<>();
        mapping.forEach((key, value) -> values.put(String.valueOf(key), value));

        return new YamlRow(source, position, values);
    }
}
