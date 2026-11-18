package io.github.arthursouto.archwright.engine.adapter.out.catalog;

import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.model.ExtraDefinition;
import io.github.arthursouto.archwright.engine.domain.model.ExtraId;

import java.util.List;

final class ExtraParser {

    static final String FILE = "extras.yaml";

    List<ExtraDefinition> parse(List<YamlRow> rows) {
        List<ExtraDefinition> extras = rows.stream().map(this::parseRow).toList();

        CatalogChecks.requireUnique(FILE, "extra", extras.stream().map(extra -> extra.id().value()).toList());

        return extras;
    }

    private ExtraDefinition parseRow(YamlRow row) {
        String id = row.text("id");

        ExtraId extraId = ExtraId.parse(id).orElseThrow(() -> row.problem("invalid extra id '" + id + "'"));

        return new ExtraDefinition(extraId, required(row));
    }

    private List<DependencyId> required(YamlRow row) {
        return row.texts("requires").stream()
                .map(raw -> DependencyId.parse(raw).orElseThrow(() -> row.problem("invalid dependency id '" + raw + "'")))
                .toList();
    }
}
