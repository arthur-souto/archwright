package io.github.arthursouto.archwright.engine.adapter.out.catalog;

import io.github.arthursouto.archwright.engine.domain.model.ArchitectureStyle;
import io.github.arthursouto.archwright.engine.domain.model.ArchitectureStyleDefinition;

import java.util.Arrays;
import java.util.List;

final class StyleParser {

    static final String FILE = "styles.yaml";

    List<ArchitectureStyleDefinition> parse(List<YamlRow> rows) {
        List<ArchitectureStyleDefinition> styles = rows.stream().map(this::parseRow).toList();

        CatalogChecks.requireUnique(FILE, "style", styles.stream().map(style -> style.style().name()).toList());
        requireEveryStyle(styles);

        return styles;
    }

    private ArchitectureStyleDefinition parseRow(YamlRow row) {
        String id = row.text("id");

        ArchitectureStyle style = ArchitectureStyle.parse(id)
                .orElseThrow(() -> row.problem("unknown architecture style '" + id + "'"));

        return new ArchitectureStyleDefinition(
                style,
                row.text("label"),
                row.text("description"),
                row.text("templateDir"),
                row.text("modulesLabel"));
    }

    private void requireEveryStyle(List<ArchitectureStyleDefinition> styles) {
        Arrays.stream(ArchitectureStyle.values())
                .filter(style -> styles.stream().noneMatch(definition -> definition.style() == style))
                .findFirst()
                .ifPresent(missing -> {
                    throw CatalogChecks.problem(FILE, "missing style " + missing.name());
                });
    }
}
