package io.github.arthursouto.archwright.desktop.wizard;

import io.github.arthursouto.archwright.engine.application.port.in.PreviewFile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PackageTreesTest {

    private static final String BASE = "src/main/java/com/acme/app/";

    private static PreviewFile info(String pack, String description) {
        return new PreviewFile(BASE + pack + (pack.isEmpty() ? "" : "/") + "package-info.java", 4, description);
    }

    private static PreviewFile source(String path, int lines) {
        return new PreviewFile(BASE + path, lines, "");
    }

    private static PackageNode child(PackageNode node, String name) {
        return node.children().stream().filter(candidate -> candidate.name().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void buildsTheTreeFromPackageInfosAndClasses() {
        List<PreviewFile> files = List.of(
                new PreviewFile("pom.xml", 80, ""),
                source("App.java", 12),
                info("", "Raiz."),
                info("domain", "Domínio."),
                info("domain/model", "Modelo."),
                info("application/usecase", "Casos de uso."),
                info("application/port/in", "Entrada."),
                info("application/port/out", "Saída."));

        List<PackageNode> roots = PackageTrees.from(files);

        assertThat(roots).hasSize(1);
        PackageNode root = roots.getFirst();
        assertThat(root.name()).isEqualTo("com.acme.app");
        assertThat(root.children()).extracting(PackageNode::name).containsExactly("application", "domain.model", "App.java");
        assertThat(child(child(root, "application"), "port").children()).extracting(PackageNode::name).containsExactly("in", "out");
    }

    @Test
    void eachPackageKeepsItsDescriptionAndQualifiedName() {
        List<PreviewFile> files = List.of(
                info("", "Raiz."), source("App.java", 3), info("domain", "Domínio."), info("domain/model", "Modelo."), source("domain/Order.java", 30));

        PackageNode root = PackageTrees.from(files).getFirst();
        PackageNode domain = child(root, "domain");

        assertThat(root.description()).isEqualTo("Raiz.");
        assertThat(root.qualifiedName()).isEqualTo("com.acme.app");
        assertThat(domain.description()).isEqualTo("Domínio.");
        assertThat(domain.qualifiedName()).isEqualTo("com.acme.app.domain");
        assertThat(child(domain, "model").qualifiedName()).isEqualTo("com.acme.app.domain.model");
    }

    @Test
    void compactsChainsOfEmptyPackagesAndKeepsTheDeepestDescription() {
        List<PreviewFile> files = List.of(info("adapter", "Adapters."), info("adapter/in", "Entrada."), info("adapter/in/http", "HTTP."));

        PackageNode root = PackageTrees.from(files).getFirst();

        assertThat(root.name()).isEqualTo("com.acme.app.adapter.in.http");
        assertThat(root.description()).isEqualTo("HTTP.");
        assertThat(root.children()).isEmpty();
    }

    @Test
    void countsPackagesClassesAndLinesIncludingCompactedPackages() {
        List<PreviewFile> files = List.of(
                info("", "Raiz."),
                info("web", "Web."),
                info("web/request", "Requests."),
                source("web/Controller.java", 40),
                source("App.java", 10));

        PackageNode root = PackageTrees.from(files).getFirst();

        assertThat(root.packages()).isEqualTo(3);
        assertThat(root.classes()).isEqualTo(2);
        assertThat(root.totalLines()).isEqualTo(4 + 4 + 4 + 40 + 10);
        assertThat(child(root, "web").totalLines()).isEqualTo(4 + 4 + 40);
    }

    @Test
    void aCompactedNodeCountsEveryPackageItStandsFor() {
        List<PreviewFile> files = List.of(info("a", "A."), info("a/b", "B."), info("a/b/c", "C."));

        PackageNode node = PackageTrees.from(files).getFirst();

        assertThat(node.name()).isEqualTo("com.acme.app.a.b.c");
        assertThat(node.packages()).isEqualTo(3);
        assertThat(node.totalLines()).isEqualTo(12);
    }

    @Test
    void aPackageWithClassesIsNotMergedWithItsChild() {
        List<PreviewFile> files = List.of(source("Main.java", 5), info("web", "Web."));

        PackageNode root = PackageTrees.from(files).getFirst();

        assertThat(root.name()).isEqualTo("com.acme.app");
        assertThat(root.children()).extracting(PackageNode::name).containsExactly("web", "Main.java");
        assertThat(child(root, "Main.java").file()).isTrue();
        assertThat(child(root, "Main.java").lines()).isEqualTo(5);
    }

    @Test
    void listsPackagesBeforeClassesInAlphabeticalOrder() {
        List<PreviewFile> files = List.of(source("Zeta.java", 1), source("Alpha.java", 1), info("b", ""), info("a", ""));

        assertThat(PackageTrees.from(files).getFirst().children()).extracting(PackageNode::name)
                .containsExactly("a", "b", "Alpha.java", "Zeta.java");
    }

    @Test
    void ignoresEverythingOutsideMainSources() {
        List<PreviewFile> files = List.of(
                new PreviewFile("pom.xml", 80, ""),
                new PreviewFile("src/test/java/com/acme/FooTest.java", 9, ""),
                new PreviewFile("src/main/resources/application.yaml", 3, ""));

        assertThat(PackageTrees.from(files)).isEmpty();
    }

    @Test
    void emptyInputGivesAnEmptyTree() {
        assertThat(PackageTrees.from(List.of())).isEmpty();
    }
}
