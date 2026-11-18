package io.github.arthursouto.archwright.engine.adapter.out.template;

import io.github.arthursouto.archwright.engine.adapter.out.yaml.YamlRow;
import io.github.arthursouto.archwright.engine.domain.model.ArchitectureStyle;
import io.github.arthursouto.archwright.engine.domain.model.LineEnding;
import io.github.arthursouto.archwright.engine.domain.model.Manifest;
import io.github.arthursouto.archwright.engine.domain.model.ManifestFile;
import io.github.arthursouto.archwright.engine.domain.model.ManifestPackage;
import io.github.arthursouto.archwright.engine.domain.model.Repeat;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

final class ManifestParser {

    private static final Set<String> ROOT_KEYS = Set.of("style", "files", "packages");
    private static final Set<String> FILE_KEYS =
            Set.of("template", "target", "when", "executable", "render", "lineEnding", "forEach");
    private static final Set<String> PACKAGE_KEYS = Set.of("package", "description", "forEach");
    private static final Pattern CONDITION = Pattern.compile("!?[A-Za-z][A-Za-z0-9.-]*");

    Manifest parse(YamlRow root) {
        root.requireOnly(ROOT_KEYS);
        requireKnownStyle(root);

        return new Manifest(files(root), packages(root));
    }

    private void requireKnownStyle(YamlRow root) {
        if (!root.has("style")) {
            return;
        }

        String style = root.text("style");

        if (ArchitectureStyle.parse(style).isEmpty()) {
            throw root.problem("unknown architecture style '" + style + "'");
        }
    }

    private List<ManifestFile> files(YamlRow root) {
        if (!root.has("files")) {
            return List.of();
        }

        return root.listOfRows("files").stream().map(this::file).toList();
    }

    private List<ManifestPackage> packages(YamlRow root) {
        if (!root.has("packages")) {
            return List.of();
        }

        return root.listOfRows("packages").stream().map(this::packageEntry).toList();
    }

    private ManifestFile file(YamlRow row) {
        row.requireOnly(FILE_KEYS);

        return new ManifestFile(
                row.text("template"),
                row.text("target"),
                conditions(row),
                row.flag("executable"),
                row.flagOrDefault("render", true),
                lineEnding(row),
                repeat(row));
    }

    private ManifestPackage packageEntry(YamlRow row) {
        row.requireOnly(PACKAGE_KEYS);

        return new ManifestPackage(row.textAllowingEmpty("package"), row.text("description"), repeat(row));
    }

    private List<String> conditions(YamlRow row) {
        List<String> conditions = row.texts("when");

        conditions.stream()
                .filter(condition -> !CONDITION.matcher(condition).matches())
                .findFirst()
                .ifPresent(invalid -> {
                    throw row.problem("'when' accepts only flag names, optionally negated with '!', found '" + invalid + "'");
                });

        return conditions;
    }

    private LineEnding lineEnding(YamlRow row) {
        if (!row.has("lineEnding")) {
            return LineEnding.LF;
        }

        String raw = row.text("lineEnding");

        return LineEnding.parse(raw).orElseThrow(() -> row.problem("'lineEnding' must be LF or CRLF, found '" + raw + "'"));
    }

    private Repeat repeat(YamlRow row) {
        if (!row.has("forEach")) {
            return Repeat.NONE;
        }

        String raw = row.text("forEach");

        return Repeat.parse(raw).orElseThrow(() -> row.problem("'forEach' must be modules or aggregates, found '" + raw + "'"));
    }
}
