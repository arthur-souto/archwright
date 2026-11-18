package io.github.arthursouto.archwright.engine.adapter.out.catalog;

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

final class CatalogReader {

    private final ClassLoader loader;
    private final String basePath;

    CatalogReader(ClassLoader loader, String basePath) {
        this.loader = loader;
        this.basePath = basePath;
    }

    List<YamlRow> read(String file) {
        Object document = parse(file);

        if (!(document instanceof List<?> entries) || entries.isEmpty()) {
            throw CatalogChecks.problem(file, "must be a non-empty list");
        }

        return IntStream.range(0, entries.size())
                .mapToObj(index -> row(file, index + 1, entries.get(index)))
                .toList();
    }

    private Object parse(String file) {
        try (Reader reader = open(file)) {
            return yaml().load(reader);
        } catch (IOException | YAMLException exception) {
            throw CatalogChecks.problem(file, exception.getMessage());
        }
    }

    private Reader open(String file) {
        InputStream stream = loader.getResourceAsStream(basePath + "/" + file);

        if (stream == null) {
            throw CatalogChecks.problem(file, "not found on classpath");
        }

        return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }

    private Yaml yaml() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);

        return new Yaml(new SafeConstructor(options));
    }

    private YamlRow row(String file, int position, Object entry) {
        if (!(entry instanceof Map<?, ?> mapping)) {
            throw CatalogChecks.problem(file, "entry " + position + " must be a mapping");
        }

        Map<String, Object> values = new LinkedHashMap<>();
        mapping.forEach((key, value) -> values.put(String.valueOf(key), value));

        return new YamlRow(file, position, values);
    }
}
