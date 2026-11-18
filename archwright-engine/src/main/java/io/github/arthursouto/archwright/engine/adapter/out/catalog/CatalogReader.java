package io.github.arthursouto.archwright.engine.adapter.out.catalog;

import io.github.arthursouto.archwright.engine.adapter.out.yaml.YamlResources;
import io.github.arthursouto.archwright.engine.adapter.out.yaml.YamlRow;

import java.util.List;

final class CatalogReader {

    private final ClassLoader loader;
    private final String basePath;

    CatalogReader(ClassLoader loader, String basePath) {
        this.loader = loader;
        this.basePath = basePath;
    }

    List<YamlRow> read(String file) {
        String source = "catalog: " + file;
        Object document = YamlResources.load(loader, basePath + "/" + file, source);

        return YamlResources.rows(source, document);
    }
}
