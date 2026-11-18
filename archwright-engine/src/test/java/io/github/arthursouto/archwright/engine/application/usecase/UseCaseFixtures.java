package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.adapter.out.template.ClasspathTemplateRepository;
import io.github.arthursouto.archwright.engine.adapter.out.template.MustacheTemplateRenderer;
import io.github.arthursouto.archwright.engine.application.port.in.ProjectRequest;
import io.github.arthursouto.archwright.engine.application.port.out.TemplateRenderer;
import io.github.arthursouto.archwright.engine.application.port.out.TemplateRepository;
import io.github.arthursouto.archwright.engine.domain.kit.Kits;
import io.github.arthursouto.archwright.engine.domain.style.ArchitectureStyles;
import io.github.arthursouto.archwright.engine.domain.validation.SpecValidator;

import java.util.List;

final class UseCaseFixtures {

    private UseCaseFixtures() {
    }

    static ProjectPlanning planning(TemplateRepository templates, TemplateRenderer renderer) {
        Kits kits = Kits.standard();

        return new ProjectPlanning(
                new SpecValidator(SpecBuilder.catalog(), ArchitectureStyles.standard(), kits),
                new ContextBuilder(new ProfileResolver(SpecBuilder.catalog()), SpecBuilder.catalog(), SpecBuilder.repository().styles(), kits),
                new FilePlanner(templates, renderer, SpecBuilder.repository().styles()));
    }

    static ProjectPlanning realPlanning() {
        return planning(new ClasspathTemplateRepository(), new MustacheTemplateRenderer());
    }

    static ProjectRequest validRequest() {
        return new ProjectRequest(
                "pedido-api", "com.acme", null, "API de pedidos",
                "HEXAGONAL_MODULAR", List.of("pedidos", "pagamentos"),
                "4.1.2", "25", "BASIC",
                List.of("web", "validation", "actuator", "jpa", "flyway", "lombok"),
                List.of("docker-compose"), true);
    }

    static ProjectRequest withTwoViolations() {
        return new ProjectRequest(
                "Pedido API", "com.acme", null, "",
                "HEXAGONAL_MODULAR", List.of("pedidos"),
                "4.1.2", "25", "BASIC",
                List.of("web"), List.of("nao-existe"), true);
    }
}
