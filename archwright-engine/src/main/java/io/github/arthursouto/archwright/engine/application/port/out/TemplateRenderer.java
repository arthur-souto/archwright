package io.github.arthursouto.archwright.engine.application.port.out;

import java.util.Map;

public interface TemplateRenderer {

    String render(String template, Map<String, Object> context);
}
