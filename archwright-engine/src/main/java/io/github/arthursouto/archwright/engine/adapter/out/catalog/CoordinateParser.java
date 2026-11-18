package io.github.arthursouto.archwright.engine.adapter.out.catalog;

import io.github.arthursouto.archwright.engine.domain.model.MavenCoordinate;

final class CoordinateParser {

    private CoordinateParser() {
    }

    static MavenCoordinate parse(YamlRow entry) {
        return new MavenCoordinate(
                entry.text("g"),
                entry.text("a"),
                entry.optionalText("v"),
                entry.optionalText("scope"));
    }
}
