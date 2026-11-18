package io.github.arthursouto.archwright.engine.application.port.in;

import java.util.List;

public interface CatalogQuery {

    List<ArchitectureStyleOption> architectureStyles();

    BootVersions bootVersions();

    List<JavaVersionOption> javaVersions(String bootVersion);

    List<KitOption> kits();

    List<DependencyOption> dependencies(String bootVersion);

    List<ExtraOption> extras();

    List<FitnessRuleSummary> fitnessRules(String style, List<String> dependencies, String bootVersion);

    String suggestBasePackage(String groupId, String artifactId);
}
