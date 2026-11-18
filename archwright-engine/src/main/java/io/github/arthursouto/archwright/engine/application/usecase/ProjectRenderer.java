package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.application.port.out.TemplateRenderer;
import io.github.arthursouto.archwright.engine.application.port.out.TemplateRepository;
import io.github.arthursouto.archwright.engine.domain.model.FilePlan;
import io.github.arthursouto.archwright.engine.domain.model.GeneratedFile;
import io.github.arthursouto.archwright.engine.domain.model.GeneratedProject;
import io.github.arthursouto.archwright.engine.domain.model.LineEnding;
import io.github.arthursouto.archwright.engine.domain.model.PlannedFile;
import io.github.arthursouto.archwright.engine.domain.model.TemplateContext;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ProjectRenderer {

    private static final String LF = "\n";
    private static final String CRLF = "\r\n";
    private static final String CR = "\r";

    private final TemplateRepository repository;
    private final TemplateRenderer renderer;

    public ProjectRenderer(TemplateRepository repository, TemplateRenderer renderer) {
        this.repository = repository;
        this.renderer = renderer;
    }

    public GeneratedProject render(FilePlan plan, TemplateContext context) {
        return new GeneratedProject(plan.files().stream().map(file -> generate(file, context)).toList());
    }

    private GeneratedFile generate(PlannedFile file, TemplateContext context) {
        String source = new String(repository.content(file.template()), StandardCharsets.UTF_8);
        String text = file.render() ? renderText(file, source, context) : source;
        String normalized = withLineEnding(text, file.lineEnding());

        return new GeneratedFile(file.target(), normalized.getBytes(StandardCharsets.UTF_8), file.executable());
    }

    private String renderText(PlannedFile file, String source, TemplateContext context) {
        Map<String, Object> scope = new LinkedHashMap<>(context.values());
        scope.putAll(file.variables());

        try {
            return renderer.render(source, scope);
        } catch (IllegalStateException failure) {
            throw new IllegalStateException(
                    "template " + file.template() + " (for " + file.target() + "): " + failure.getMessage(), failure);
        }
    }

    private String withLineEnding(String text, LineEnding lineEnding) {
        String unix = text.replace(CRLF, LF).replace(CR, LF);

        return lineEnding == LineEnding.CRLF ? unix.replace(LF, CRLF) : unix;
    }
}
