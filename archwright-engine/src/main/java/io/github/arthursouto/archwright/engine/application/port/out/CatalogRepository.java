package io.github.arthursouto.archwright.engine.application.port.out;

import io.github.arthursouto.archwright.engine.domain.model.ArchitectureStyleDefinition;
import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.DependencyDefinition;
import io.github.arthursouto.archwright.engine.domain.model.ExtraDefinition;
import io.github.arthursouto.archwright.engine.domain.model.FitnessRuleDefinition;

import java.util.List;

public interface CatalogRepository {

    List<BootProfile> bootProfiles();

    List<DependencyDefinition> dependencies();

    List<ExtraDefinition> extras();

    List<FitnessRuleDefinition> fitnessRules();

    List<ArchitectureStyleDefinition> styles();
}
