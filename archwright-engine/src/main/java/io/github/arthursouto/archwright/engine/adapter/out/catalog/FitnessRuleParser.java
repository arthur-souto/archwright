package io.github.arthursouto.archwright.engine.adapter.out.catalog;

import io.github.arthursouto.archwright.engine.adapter.out.yaml.YamlRow;
import io.github.arthursouto.archwright.engine.domain.model.FitnessRule;
import io.github.arthursouto.archwright.engine.domain.model.FitnessRuleDefinition;

import java.util.List;
import java.util.regex.Pattern;

final class FitnessRuleParser {

    static final String FILE = "fitness-rules.yaml";

    private static final Pattern RULE_ID = Pattern.compile("FF-[A-Z]{3}-\\d{2}");
    private static final Pattern CLASS_NAME = Pattern.compile("[A-Z][A-Za-z0-9]*");

    List<FitnessRuleDefinition> parse(List<YamlRow> rows) {
        List<FitnessRuleDefinition> definitions = rows.stream().map(this::parseRow).toList();

        CatalogChecks.requireUnique(FILE, "test class", definitions.stream().map(FitnessRuleDefinition::testClass).toList());
        CatalogChecks.requireUnique(FILE, "rule", definitions.stream()
                .flatMap(definition -> definition.rules().stream())
                .map(FitnessRule::id)
                .toList());

        return definitions;
    }

    private FitnessRuleDefinition parseRow(YamlRow row) {
        String testClass = row.text("testClass");

        if (!CLASS_NAME.matcher(testClass).matches()) {
            throw row.problem("invalid test class name '" + testClass + "'");
        }

        List<FitnessRule> rules = row.listOfRows("rules").stream().map(this::parseRule).toList();

        if (rules.isEmpty()) {
            throw row.problem("must declare at least one rule");
        }

        return new FitnessRuleDefinition(testClass, row.text("template"), row.texts("when"), rules);
    }

    private FitnessRule parseRule(YamlRow row) {
        String id = row.text("id");

        if (!RULE_ID.matcher(id).matches()) {
            throw row.problem("invalid rule id '" + id + "', expected like FF-DOM-01");
        }

        return new FitnessRule(id, row.text("description"));
    }
}
