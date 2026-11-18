package io.github.arthursouto.archwright.engine.adapter.out.catalog;

import io.github.arthursouto.archwright.engine.adapter.out.yaml.YamlRow;
import io.github.arthursouto.archwright.engine.domain.model.DependencyDefinition;
import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.model.MavenCoordinate;

import java.util.List;

final class DependencyParser {

    static final String FILE = "dependencies.yaml";

    private static final String ANNOTATION_PROCESSORS = "annotationProcessors";

    List<DependencyDefinition> parse(List<YamlRow> rows) {
        List<DependencyDefinition> dependencies = rows.stream().map(this::parseRow).toList();

        CatalogChecks.requireUnique(FILE, "dependency", dependencies.stream().map(dependency -> dependency.id().value()).toList());

        return dependencies;
    }

    private DependencyDefinition parseRow(YamlRow row) {
        DependencyId id = dependencyId(row, row.text("id"));

        return new DependencyDefinition(
                id,
                orDefault(row.optionalText("label"), id.value()),
                orDefault(row.optionalText("description"), ""),
                orDefault(row.optionalText("group"), ""),
                implied(row),
                annotationProcessors(row));
    }

    private String orDefault(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private List<DependencyId> implied(YamlRow row) {
        return row.texts("implies").stream().map(id -> dependencyId(row, id)).toList();
    }

    private List<MavenCoordinate> annotationProcessors(YamlRow row) {
        if (!row.has(ANNOTATION_PROCESSORS)) {
            return List.of();
        }

        return row.listOfRows(ANNOTATION_PROCESSORS).stream().map(this::processor).toList();
    }

    private MavenCoordinate processor(YamlRow entry) {
        MavenCoordinate coordinate = CoordinateParser.parse(entry);

        if (coordinate.versionProperty().isEmpty()) {
            throw entry.problem("annotation processor must reference a version property, like ${lombok.version}");
        }

        return coordinate;
    }

    private DependencyId dependencyId(YamlRow row, String raw) {
        return DependencyId.parse(raw).orElseThrow(() -> row.problem("invalid dependency id '" + raw + "'"));
    }
}
