package io.github.arthursouto.archwright.engine.adapter.out.catalog;

import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.BootVersion;
import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.model.MavenCoordinate;
import io.github.arthursouto.archwright.engine.domain.model.PersistenceNamespace;
import io.github.arthursouto.archwright.engine.domain.model.TestStrategy;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                    persistenceNamespace(row),
                    testStrategy(row),
                    coordinates(row, "artifacts"),
                    row.has("testArtifacts") ? coordinates(row, "testArtifacts") : Map.of(),
                    row.textMap("pinnedVersions"));
        } catch (IllegalArgumentException exception) {
            throw CatalogChecks.problem(FILE, exception.getMessage());
        }
    }

    private PersistenceNamespace persistenceNamespace(YamlRow row) {
        String raw = row.text("persistenceNamespace");

        return PersistenceNamespace.parse(raw)
                .orElseThrow(() -> row.problem("'persistenceNamespace' must be javax or jakarta, found '" + raw + "'"));
    }

    private TestStrategy testStrategy(YamlRow row) {
        String raw = row.text("tests");

        return TestStrategy.parse(raw)
                .orElseThrow(() -> row.problem("'tests' must be SINGLE_STARTER or PER_TECHNOLOGY, found '" + raw + "'"));
    }

    private Map<DependencyId, List<MavenCoordinate>> coordinates(YamlRow row, String key) {
        Map<DependencyId, List<MavenCoordinate>> coordinates = new LinkedHashMap<>();

        row.groupedRows(key).forEach((name, entries) ->
                coordinates.put(dependencyId(row, key, name), entries.stream().map(CoordinateParser::parse).toList()));

        return coordinates;
    }

    private DependencyId dependencyId(YamlRow row, String key, String name) {
        return DependencyId.parse(name)
                .orElseThrow(() -> row.problem("invalid dependency id '" + name + "' in " + key));
    }

    private static BootVersion version(BootProfile profile) {
        Matcher matcher = LINE.matcher(profile.line());
        matcher.matches();

        return new BootVersion(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), 0);
    }
}
