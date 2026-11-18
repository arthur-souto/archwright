package io.github.arthursouto.archwright.engine.matrix;

import io.github.arthursouto.archwright.engine.application.port.in.BootVersionOption;
import io.github.arthursouto.archwright.engine.application.port.in.CatalogQuery;
import io.github.arthursouto.archwright.engine.application.port.in.JavaVersionOption;
import io.github.arthursouto.archwright.engine.application.port.in.KitOption;
import io.github.arthursouto.archwright.engine.application.port.in.ProjectRequest;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

final class MatrixCases {

    static final String ARTIFACT_ID = "matrix-app";

    private static final String LEGACY_JAVA = "8";
    private static final String LEGACY_JDK = "21";
    private static final List<String> EXTRA_DEPENDENCIES = List.of("springdoc", "security");
    private static final List<String> EXTRAS = List.of("docker-compose", "github-actions");

    private final CatalogQuery catalog;

    MatrixCases(CatalogQuery catalog) {
        this.catalog = catalog;
    }

    List<MatrixCase> all() {
        return catalog.bootVersions().versions().stream()
                .filter(BootVersionOption::latestOfLine)
                .flatMap(this::casesOfLine)
                .toList();
    }

    private Stream<MatrixCase> casesOfLine(BootVersionOption boot) {
        List<JavaVersionOption> options = catalog.javaVersions(boot.version());

        return javaVersionsToExercise(options).stream()
                .flatMap(java -> catalog.architectureStyles().stream()
                        .flatMap(style -> catalog.kits().stream().map(kit -> matrixCase(boot, java, style.id(), kit))));
    }

    private Set<String> javaVersionsToExercise(List<JavaVersionOption> options) {
        Set<String> versions = new LinkedHashSet<>();

        options.stream().filter(JavaVersionOption::isDefault).forEach(option -> versions.add(option.version()));
        options.stream().filter(option -> !option.aboveTested()).reduce((first, second) -> second)
                .ifPresent(newestTested -> versions.add(newestTested.version()));

        if (options.stream().anyMatch(option -> option.version().equals(LEGACY_JAVA))) {
            versions.add(LEGACY_JAVA);
        }

        return versions;
    }

    private MatrixCase matrixCase(BootVersionOption boot, String java, String styleId, KitOption kit) {
        String name = "boot-" + boot.version() + "-" + styleId.toLowerCase().replace('_', '-') + "-" + kit.id().toLowerCase() + "-java" + java;

        return new MatrixCase(name, java, jdkFor(java), request(boot, java, styleId, kit));
    }

    private ProjectRequest request(BootVersionOption boot, String java, String styleId, KitOption kit) {
        boolean modular = styleId.equals("HEXAGONAL_MODULAR");
        List<String> dependencies = kit.dependencies().isEmpty()
                ? List.of()
                : Stream.concat(kit.dependencies().stream(), EXTRA_DEPENDENCIES.stream()).toList();
        List<String> extras = kit.dependencies().isEmpty() ? List.of("github-actions") : EXTRAS;

        return new ProjectRequest(
                ARTIFACT_ID, "com.acme", null, "Projeto gerado pela matriz de compilação",
                styleId, modular ? List.of("pedidos", "pagamentos") : List.of("paciente"),
                boot.version(), java, kit.id(), dependencies, extras, true);
    }

    private String jdkFor(String java) {
        return java.equals(LEGACY_JAVA) ? LEGACY_JDK : java;
    }
}
