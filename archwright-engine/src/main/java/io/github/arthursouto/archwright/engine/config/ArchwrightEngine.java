package io.github.arthursouto.archwright.engine.config;

import io.github.arthursouto.archwright.engine.adapter.out.catalog.YamlCatalogRepository;
import io.github.arthursouto.archwright.engine.adapter.out.export.FolderProjectExporter;
import io.github.arthursouto.archwright.engine.adapter.out.export.ZipProjectExporter;
import io.github.arthursouto.archwright.engine.adapter.out.template.ClasspathTemplateRepository;
import io.github.arthursouto.archwright.engine.adapter.out.template.MustacheTemplateRenderer;
import io.github.arthursouto.archwright.engine.adapter.out.version.EmbeddedBootVersions;
import io.github.arthursouto.archwright.engine.adapter.out.version.FileVersionCache;
import io.github.arthursouto.archwright.engine.adapter.out.version.MavenCentralBootVersionSource;
import io.github.arthursouto.archwright.engine.adapter.out.version.OfflineBootVersionSource;
import io.github.arthursouto.archwright.engine.application.port.in.CatalogQuery;
import io.github.arthursouto.archwright.engine.application.port.in.GenerateProjectUseCase;
import io.github.arthursouto.archwright.engine.application.port.in.PreviewProjectQuery;
import io.github.arthursouto.archwright.engine.application.port.out.BootVersionSource;
import io.github.arthursouto.archwright.engine.application.port.out.CatalogRepository;
import io.github.arthursouto.archwright.engine.application.port.out.TemplateRenderer;
import io.github.arthursouto.archwright.engine.application.port.out.TemplateRepository;
import io.github.arthursouto.archwright.engine.application.usecase.BootVersionLookup;
import io.github.arthursouto.archwright.engine.application.usecase.BrowseCatalog;
import io.github.arthursouto.archwright.engine.application.usecase.CatalogVerifier;
import io.github.arthursouto.archwright.engine.application.usecase.ContextBuilder;
import io.github.arthursouto.archwright.engine.application.usecase.FilePlanner;
import io.github.arthursouto.archwright.engine.application.usecase.ProfileResolver;
import io.github.arthursouto.archwright.engine.application.usecase.GenerateProject;
import io.github.arthursouto.archwright.engine.application.usecase.ProjectPlanning;
import io.github.arthursouto.archwright.engine.application.usecase.PreviewProject;
import io.github.arthursouto.archwright.engine.application.usecase.ProjectRenderer;
import io.github.arthursouto.archwright.engine.domain.kit.Kits;
import io.github.arthursouto.archwright.engine.domain.model.Catalog;
import io.github.arthursouto.archwright.engine.domain.style.ArchitectureStyles;
import io.github.arthursouto.archwright.engine.domain.validation.SpecValidator;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

public final class ArchwrightEngine {

    private static final String CACHE_FILE = "boot-versions.json";

    private final GenerateProjectUseCase generateProject;
    private final PreviewProjectQuery previewProject;
    private final CatalogQuery catalog;

    private ArchwrightEngine(Builder builder) {
        CatalogRepository repository = new YamlCatalogRepository();
        Kits kits = Kits.standard();
        ArchitectureStyles styles = ArchitectureStyles.standard();

        new CatalogVerifier(repository, kits, styles).verify();

        Catalog loaded = new Catalog(repository.bootProfiles(), repository.dependencies(), repository.extras());
        TemplateRepository templates = new ClasspathTemplateRepository();
        TemplateRenderer renderer = new MustacheTemplateRenderer();
        ProfileResolver resolver = new ProfileResolver(loaded);

        ProjectPlanning planning = new ProjectPlanning(
                new SpecValidator(loaded, styles, kits),
                new ContextBuilder(resolver, loaded, repository.styles(), kits),
                new FilePlanner(templates, renderer, repository.styles()));

        this.generateProject = new GenerateProject(
                planning, new ProjectRenderer(templates, renderer), List.of(new ZipProjectExporter(), new FolderProjectExporter()));
        this.previewProject = new PreviewProject(planning, resolver);
        this.catalog = new BrowseCatalog(loaded, repository.styles(), styles, kits, versionLookup(builder, loaded));
    }

    public static ArchwrightEngine create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public GenerateProjectUseCase generateProject() {
        return generateProject;
    }

    public PreviewProjectQuery previewProject() {
        return previewProject;
    }

    public CatalogQuery catalog() {
        return catalog;
    }

    private static BootVersionLookup versionLookup(Builder builder, Catalog catalog) {
        BootVersionSource source = builder.offline ? new OfflineBootVersionSource() : new MavenCentralBootVersionSource();

        return new BootVersionLookup(source, versionCache(builder), new EmbeddedBootVersions(), catalog, Clock.systemUTC());
    }

    private static FileVersionCache versionCache(Builder builder) {
        return builder.cacheDirectory == null
                ? FileVersionCache.inUserHome()
                : new FileVersionCache(builder.cacheDirectory.resolve(CACHE_FILE));
    }

    public static final class Builder {

        private Path cacheDirectory;
        private boolean offline;

        private Builder() {
        }

        public Builder cacheDirectory(Path cacheDirectory) {
            this.cacheDirectory = Objects.requireNonNull(cacheDirectory, "cacheDirectory");
            return this;
        }

        public Builder offline(boolean offline) {
            this.offline = offline;
            return this;
        }

        public ArchwrightEngine build() {
            return new ArchwrightEngine(this);
        }
    }
}
