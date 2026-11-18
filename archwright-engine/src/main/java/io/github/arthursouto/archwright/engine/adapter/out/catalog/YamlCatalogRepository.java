package io.github.arthursouto.archwright.engine.adapter.out.catalog;

import io.github.arthursouto.archwright.engine.application.port.out.CatalogRepository;
import io.github.arthursouto.archwright.engine.domain.model.ArchitectureStyleDefinition;
import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.DependencyDefinition;
import io.github.arthursouto.archwright.engine.domain.model.ExtraDefinition;

import java.util.List;

public final class YamlCatalogRepository implements CatalogRepository {

    private static final String DEFAULT_BASE_PATH = "archwright/catalog";

    private final List<ArchitectureStyleDefinition> styles;
    private final List<BootProfile> bootProfiles;
    private final List<DependencyDefinition> dependencies;
    private final List<ExtraDefinition> extras;

    public YamlCatalogRepository() {
        this(YamlCatalogRepository.class.getClassLoader(), DEFAULT_BASE_PATH);
    }

    public YamlCatalogRepository(ClassLoader loader, String basePath) {
        CatalogReader reader = new CatalogReader(loader, basePath);

        this.styles = new StyleParser().parse(reader.read(StyleParser.FILE));
        this.bootProfiles = new BootProfileParser().parse(reader.read(BootProfileParser.FILE));
        this.dependencies = new DependencyParser().parse(reader.read(DependencyParser.FILE));
        this.extras = new ExtraParser().parse(reader.read(ExtraParser.FILE));

        new CatalogConsistency().check(bootProfiles, dependencies, extras);
    }

    @Override
    public List<ArchitectureStyleDefinition> styles() {
        return styles;
    }

    @Override
    public List<BootProfile> bootProfiles() {
        return bootProfiles;
    }

    @Override
    public List<DependencyDefinition> dependencies() {
        return dependencies;
    }

    @Override
    public List<ExtraDefinition> extras() {
        return extras;
    }
}
