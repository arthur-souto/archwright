package io.github.arthursouto.archwright.engine.adapter.out.catalog;

import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.BootVersion;
import io.github.arthursouto.archwright.engine.domain.model.DependencyId;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class BootProfileParser {

    static final String FILE = "boot-profiles.yaml";

    private static final Pattern LINE = Pattern.compile("(\\d{1,4})\\.(\\d{1,4})");

    private static final Comparator<BootProfile> NEWEST_FIRST =
            Comparator.comparing(BootProfileParser::version).reversed();

    List<BootProfile> parse(List<YamlRow> rows) {
        List<BootProfile> profiles = rows.stream().map(this::parseRow).toList();

        CatalogChecks.requireUnique(FILE, "line", profiles.stream().map(BootProfile::line).toList());

        return profiles.stream().sorted(NEWEST_FIRST).toList();
    }

    private BootProfile parseRow(YamlRow row) {
        String line = row.text("line");

        if (!LINE.matcher(line).matches()) {
            throw row.problem("'line' must look like 3.5 (quote it in YAML)");
        }

        try {
            return new BootProfile(
                    line,
                    row.integer("javaMin"),
                    row.integer("javaMaxTested"),
                    row.flag("legacy"),
                    availableDependencies(row));
        } catch (IllegalArgumentException exception) {
            throw CatalogChecks.problem(FILE, exception.getMessage());
        }
    }

    private Set<DependencyId> availableDependencies(YamlRow row) {
        return row.keysOf("artifacts").stream()
                .map(key -> DependencyId.parse(key)
                        .orElseThrow(() -> row.problem("invalid dependency id '" + key + "' in artifacts")))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static BootVersion version(BootProfile profile) {
        var matcher = LINE.matcher(profile.line());
        matcher.matches();

        return new BootVersion(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), 0);
    }
}
