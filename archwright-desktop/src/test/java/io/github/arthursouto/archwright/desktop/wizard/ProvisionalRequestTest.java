package io.github.arthursouto.archwright.desktop.wizard;

import io.github.arthursouto.archwright.engine.application.port.in.ProjectRequest;
import io.github.arthursouto.archwright.engine.application.port.in.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProvisionalRequestTest {

    private static ProjectRequest request(String artifact, String group, String basePackage, List<String> modules) {
        return new ProjectRequest(artifact, group, basePackage, "", "HEXAGONAL_MODULAR", modules, "4.1.2", "25", "BASIC",
                List.of("web"), List.of(), true);
    }

    private static Violation violation(String code, String field, Map<String, String> params) {
        return new Violation(code, field, params);
    }

    @Test
    void replacesInvalidNamesWithPlaceholders() {
        ProjectRequest original = request("Pedido API", "", "", List.of("pedidos"));

        Optional<ProjectRequest> repaired = ProvisionalRequest.repair(original, List.of(
                violation("INVALID_ARTIFACT_ID", "artifactId", Map.of()),
                violation("INVALID_GROUP_ID", "groupId", Map.of())));

        assertThat(repaired).contains(request("projeto", "com.empresa", "", List.of("pedidos")));
    }

    @Test
    void dropsAnInvalidBasePackageSoItIsDerivedAgain() {
        ProjectRequest original = request("pedido", "com.acme", "main", List.of("pedidos"));

        Optional<ProjectRequest> repaired = ProvisionalRequest.repair(original,
                List.of(violation("BASE_PACKAGE_TOO_SHORT", "basePackage", Map.of())));

        assertThat(repaired).contains(request("pedido", "com.acme", "", List.of("pedidos")));
    }

    @Test
    void addsAPlaceholderModuleWhenNoneWasTypedYet() {
        Optional<ProjectRequest> repaired = ProvisionalRequest.repair(request("pedido", "com.acme", "", List.of()),
                List.of(violation("MODULES_REQUIRED", "modules", Map.of())));

        assertThat(repaired.orElseThrow().modules()).containsExactly("modulo");
    }

    @Test
    void keepsTheValidModulesAndDropsTheRejectedOnes() {
        ProjectRequest original = request("pedido", "com.acme", "", List.of("pedidos", "Bad", "shared", "pagamentos"));

        Optional<ProjectRequest> repaired = ProvisionalRequest.repair(original, List.of(
                violation("INVALID_MODULE_NAME", "modules", Map.of("name", "Bad")),
                violation("RESERVED_MODULE_NAME", "modules", Map.of("name", "shared"))));

        assertThat(repaired.orElseThrow().modules()).containsExactly("pedidos", "pagamentos");
    }

    @Test
    void removesDuplicatedModules() {
        ProjectRequest original = request("pedido", "com.acme", "", List.of("pedidos", "pedidos", "pagamentos"));

        Optional<ProjectRequest> repaired = ProvisionalRequest.repair(original,
                List.of(violation("DUPLICATED_MODULE_NAME", "modules", Map.of("name", "x"))));

        assertThat(repaired.orElseThrow().modules()).containsExactly("pedidos", "pagamentos");
    }

    @Test
    void usesThePlaceholderWhenEveryModuleWasRejected() {
        Optional<ProjectRequest> repaired = ProvisionalRequest.repair(request("pedido", "com.acme", "", List.of("Bad")),
                List.of(violation("INVALID_MODULE_NAME", "modules", Map.of("name", "Bad"))));

        assertThat(repaired.orElseThrow().modules()).containsExactly("modulo");
    }

    @Test
    void trimsTheListWhenThereAreTooManyModules() {
        ProjectRequest original = request("pedido", "com.acme", "", List.of("a", "b", "c", "d"));

        Optional<ProjectRequest> repaired = ProvisionalRequest.repair(original,
                List.of(violation("TOO_MANY_MODULES", "modules", Map.of("max", "2"))));

        assertThat(repaired.orElseThrow().modules()).containsExactly("a", "b");
    }

    @Test
    void leavesOtherFieldsAloneAndGivesUpWhenNothingCanBeRepaired() {
        ProjectRequest original = request("pedido", "com.acme", "", List.of("pedidos"));

        assertThat(ProvisionalRequest.repair(original,
                List.of(violation("UNSUPPORTED_BOOT_VERSION", "bootVersion", Map.of("value", ""))))).isEmpty();
    }

    @Test
    void givesUpWhenThePlaceholderIsAlreadyInPlace() {
        ProjectRequest original = request("projeto", "com.empresa", "", List.of("pedidos"));

        assertThat(ProvisionalRequest.repair(original,
                List.of(violation("INVALID_ARTIFACT_ID", "artifactId", Map.of())))).isEmpty();
    }
}
