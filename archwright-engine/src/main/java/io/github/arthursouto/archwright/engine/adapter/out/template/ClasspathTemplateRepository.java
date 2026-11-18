package io.github.arthursouto.archwright.engine.adapter.out.template;

import io.github.arthursouto.archwright.engine.adapter.out.yaml.YamlResources;
import io.github.arthursouto.archwright.engine.application.port.out.TemplateRepository;
import io.github.arthursouto.archwright.engine.domain.model.Manifest;

import java.io.IOException;
import java.io.InputStream;

public final class ClasspathTemplateRepository implements TemplateRepository {

    private static final String DEFAULT_BASE_PATH = "archwright/templates";
    private static final String MANIFEST = "manifest.yaml";

    private final ClassLoader loader;
    private final String basePath;
    private final ManifestParser parser = new ManifestParser();

    public ClasspathTemplateRepository() {
        this(ClasspathTemplateRepository.class.getClassLoader(), DEFAULT_BASE_PATH);
    }

    public ClasspathTemplateRepository(ClassLoader loader, String basePath) {
        this.loader = loader;
        this.basePath = basePath;
    }

    @Override
    public Manifest manifest(String directory) {
        String source = "template: " + directory + "/" + MANIFEST;
        Object document = YamlResources.load(loader, basePath + "/" + directory + "/" + MANIFEST, source);

        return parser.parse(YamlResources.mapping(source, document));
    }

    @Override
    public byte[] content(String path) {
        try (InputStream stream = loader.getResourceAsStream(basePath + "/" + path)) {
            if (stream == null) {
                throw new IllegalStateException("template: " + path + ": not found on classpath");
            }

            return stream.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("template: " + path + ": " + exception.getMessage(), exception);
        }
    }
}
