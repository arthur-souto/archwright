package io.github.arthursouto.archwright.engine.adapter.out.catalog;

import io.github.arthursouto.archwright.engine.domain.model.DependencyDefinition;
import io.github.arthursouto.archwright.engine.domain.model.DependencyId;

import java.util.List;

final class DependencyParser {

    static final String FILE = "dependencies.yaml";

    List<DependencyDefinition> parse(List<YamlRow> rows) {
        List<DependencyDefinition> dependencies = rows.stream().map(this::parseRow).toList();

        CatalogChecks.requireUnique(FILE, "dependency", dependencies.stream().map(dependency -> dependency.id().value()).toList());

        return dependencies;
    }

    private DependencyDefinition parseRow(YamlRow row) {
        return new DependencyDefinition(dependencyId(row, row.text("id")), implied(row));
    }

    private List<DependencyId> implied(YamlRow row) {
        return row.texts("implies").stream().map(id -> dependencyId(row, id)).toList();
    }

    private DependencyId dependencyId(YamlRow row, String raw) {
        return DependencyId.parse(raw).orElseThrow(() -> row.problem("invalid dependency id '" + raw + "'"));
    }
}
