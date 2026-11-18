package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.application.port.out.CatalogRepository;
import io.github.arthursouto.archwright.engine.application.port.out.TemplateRepository;
import io.github.arthursouto.archwright.engine.domain.model.FitnessRuleDefinition;

public final class FitnessTemplateVerifier {

    public static final String TEMPLATE_DIRECTORY = "fitness";

    private final CatalogRepository catalog;
    private final TemplateRepository templates;

    public FitnessTemplateVerifier(CatalogRepository catalog, TemplateRepository templates) {
        this.catalog = catalog;
        this.templates = templates;
    }

    public void verify() {
        catalog.fitnessRules().forEach(this::verifyTemplate);
    }

    private void verifyTemplate(FitnessRuleDefinition definition) {
        String path = TEMPLATE_DIRECTORY + "/" + definition.template();

        try {
            templates.content(path);
        } catch (IllegalStateException missing) {
            throw new IllegalStateException(
                    "catalog: fitness-rules.yaml: " + definition.testClass() + ": template not found: " + path, missing);
        }
    }
}
