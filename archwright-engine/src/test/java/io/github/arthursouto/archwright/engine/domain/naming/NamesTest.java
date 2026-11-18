package io.github.arthursouto.archwright.engine.domain.naming;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class NamesTest {

    @ParameterizedTest
    @CsvSource({
            "pedido-api, PedidoApi",
            "order.service, OrderService",
            "app, App",
            "a-b-c, ABC"
    })
    void convertsToPascalCase(String input, String expected) {
        assertThat(Names.pascalCase(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "pedido-api, pedidoapi",
            "order.service, orderservice",
            "Pedido-API, pedidoapi"
    })
    void buildsPackageSegmentWithoutSeparators(String input, String expected) {
        assertThat(Names.packageSegment(input)).isEqualTo(expected);
    }

    @Test
    void mainClassNameAppendsApplicationSuffix() {
        assertThat(Names.mainClassName("pedido-api")).isEqualTo("PedidoApiApplication");
    }

    @Test
    void mainClassNameIsPrefixedWhenStartingWithDigit() {
        assertThat(Names.mainClassName("3d-viewer")).isEqualTo("App3dViewerApplication");
    }

    @Test
    void suggestsBasePackageFromGroupAndArtifact() {
        assertThat(Names.suggestBasePackage("com.acme", "pedido-api")).isEqualTo("com.acme.pedidoapi");
    }

    @Test
    void suggestsNothingWhenGroupIsBlank() {
        assertThat(Names.suggestBasePackage("  ", "pedido-api")).isEmpty();
    }

    @Test
    void suggestsNothingWhenArtifactIsBlank() {
        assertThat(Names.suggestBasePackage("com.acme", "")).isEmpty();
    }
}
