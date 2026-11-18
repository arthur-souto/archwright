package io.github.arthursouto.archwright.engine.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BasePackageTest {

    @ParameterizedTest
    @DisplayName("BR-003 aceita pacotes com segmentos válidos")
    @ValueSource(strings = {"com", "com.acme", "com.acme.pedidoapi", "br.com.a_b"})
    void acceptsValidPackages(String value) {
        assertThat(new BasePackage(value).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @DisplayName("BR-003 recusa palavras reservadas, maiúsculas e segmentos vazios")
    @ValueSource(strings = {"", "com.new", "com.class", "Com.acme", "com..acme", "com.acme.", "com.1x"})
    void rejectsInvalidPackages(String value) {
        assertThatThrownBy(() -> new BasePackage(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
