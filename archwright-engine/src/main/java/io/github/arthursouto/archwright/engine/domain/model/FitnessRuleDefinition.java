package io.github.arthursouto.archwright.engine.domain.model;

import java.util.List;

public record FitnessRuleDefinition(String testClass, String template, List<String> when, List<FitnessRule> rules) {

    public FitnessRuleDefinition {
        if (testClass == null || testClass.isBlank()) {
            throw new IllegalArgumentException("fitness test class must not be blank");
        }

        if (template == null || template.isBlank()) {
            throw new IllegalArgumentException("fitness test class " + testClass + ": template must not be blank");
        }

        when = List.copyOf(when);
        rules = List.copyOf(rules);
    }

    public boolean appliesTo(TemplateContext context) {
        return when.stream().allMatch(context::satisfies);
    }
}
