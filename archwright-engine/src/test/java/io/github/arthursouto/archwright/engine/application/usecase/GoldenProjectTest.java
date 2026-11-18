package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.adapter.out.template.ClasspathTemplateRepository;
import io.github.arthursouto.archwright.engine.adapter.out.template.MustacheTemplateRenderer;
import io.github.arthursouto.archwright.engine.adapter.out.yaml.YamlResources;
import io.github.arthursouto.archwright.engine.adapter.out.yaml.YamlRow;
import io.github.arthursouto.archwright.engine.domain.kit.Kits;
import io.github.arthursouto.archwright.engine.domain.model.GeneratedFile;
import io.github.arthursouto.archwright.engine.domain.model.GeneratedProject;
import io.github.arthursouto.archwright.engine.domain.model.ProjectSpec;
import io.github.arthursouto.archwright.engine.domain.model.TemplateContext;
import io.github.arthursouto.archwright.engine.domain.style.ArchitectureStyles;
import io.github.arthursouto.archwright.engine.domain.validation.ProjectDraft;
import io.github.arthursouto.archwright.engine.domain.validation.SpecValidator;
import io.github.arthursouto.archwright.engine.domain.validation.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GoldenProjectTest {

    private static final Path GOLDEN_ROOT = Path.of("src/test/resources/golden");
    private static final boolean UPDATE = Boolean.getBoolean("golden.update");

    private final ClasspathTemplateRepository repository = new ClasspathTemplateRepository();
    private final MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();

    private final ContextBuilder contextBuilder = new ContextBuilder(
            new ProfileResolver(SpecBuilder.catalog()),
            SpecBuilder.catalog(),
            SpecBuilder.repository().styles(),
            Kits.standard());

    private final FilePlanner planner = new FilePlanner(repository, renderer, SpecBuilder.repository().styles());
    private final ProjectRenderer projectRenderer = new ProjectRenderer(repository, renderer);

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"modular-basic", "modular-none", "legacy-boot2", "layered-basic", "no-fitness"})
    @DisplayName("ENG-RND-01 #5 o projeto gerado é igual, byte a byte, ao projeto esperado")
    void generatesExactlyTheExpectedProject(String name) throws IOException {
        GeneratedProject project = generate(readRequest(name));
        Path expected = GOLDEN_ROOT.resolve(name).resolve("expected");

        if (UPDATE) {
            rewrite(expected, project);
        }

        assertThat(project.files()).extracting(GeneratedFile::path).containsExactlyInAnyOrderElementsOf(expectedPaths(expected));

        for (GeneratedFile file : project.files()) {
            String actual = new String(file.content(), StandardCharsets.UTF_8);
            String golden = Files.readString(expected.resolve(file.path()), StandardCharsets.UTF_8);

            assertThat(actual).as(name + ": " + file.path()).isEqualTo(golden);
        }
    }

    private GeneratedProject generate(ProjectDraft draft) {
        SpecValidator validator = new SpecValidator(SpecBuilder.catalog(), ArchitectureStyles.standard(), Kits.standard());
        ValidationResult result = validator.validate(draft);

        if (!(result instanceof ValidationResult.Valid valid)) {
            throw new AssertionError("golden request is invalid: " + result);
        }

        ProjectSpec spec = valid.spec();
        TemplateContext context = contextBuilder.build(spec);

        return projectRenderer.render(planner.plan(spec, context), context);
    }

    private ProjectDraft readRequest(String name) throws IOException {
        String source = "golden: " + name + "/request.json";
        String json = Files.readString(GOLDEN_ROOT.resolve(name).resolve("request.json"), StandardCharsets.UTF_8);
        YamlRow request = YamlResources.mapping(source, new Yaml(new SafeConstructor(new LoaderOptions())).load(json));

        return new ProjectDraft(
                request.text("artifactId"),
                request.text("groupId"),
                request.textAllowingEmpty("basePackage"),
                request.textAllowingEmpty("description"),
                request.text("architectureStyle"),
                request.texts("modules"),
                request.text("bootVersion"),
                request.text("javaVersion"),
                request.text("kit"),
                request.texts("dependencies"),
                request.texts("extras"),
                request.flag("fitnessFunctions"));
    }

    private List<String> expectedPaths(Path expected) throws IOException {
        try (Stream<Path> files = Files.walk(expected)) {
            return files.filter(Files::isRegularFile)
                    .map(file -> expected.relativize(file).toString().replace('\\', '/'))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private void rewrite(Path expected, GeneratedProject project) throws IOException {
        if (Files.exists(expected)) {
            try (Stream<Path> existing = Files.walk(expected)) {
                existing.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }

        for (GeneratedFile file : project.files()) {
            Path target = expected.resolve(file.path());
            Files.createDirectories(target.getParent());
            Files.write(target, file.content());
        }
    }
}
