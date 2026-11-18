package io.github.arthursouto.archwright.engine.adapter.out.template;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.MustacheException;
import io.github.arthursouto.archwright.engine.application.port.out.TemplateRenderer;

import java.util.Map;

public final class MustacheTemplateRenderer implements TemplateRenderer {

    private final Mustache.Compiler compiler = Mustache.compiler()
            .escapeHTML(false)
            .strictSections(true);

    @Override
    public String render(String template, Map<String, Object> context) {
        try {
            return compiler.compile(template).execute(context);
        } catch (MustacheException exception) {
            throw new IllegalStateException(exception.getMessage(), exception);
        }
    }
}
