package io.github.arthursouto.archwright.engine.domain.naming;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class EscapingTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "a & b|a &amp; b",
            "<tag>|&lt;tag&gt;",
            "say \"hi\"|say &quot;hi&quot;",
            "it's|it&apos;s",
            "API <beta> & cia|API &lt;beta&gt; &amp; cia"
    })
    void escapesXmlSpecialCharacters(String input, String expected) {
        assertThat(Escaping.xml(input)).isEqualTo(expected);
    }

    @Test
    void leavesPlainTextUntouchedForXml() {
        assertThat(Escaping.xml("API de pedidos, ação 123")).isEqualTo("API de pedidos, ação 123");
        assertThat(Escaping.xml("")).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "*bold*|\\*bold\\*",
            "snake_case|snake\\_case",
            "`code`|\\`code\\`",
            "[link]|\\[link\\]"
    })
    void escapesMarkdownControlCharacters(String input, String expected) {
        assertThat(Escaping.markdown(input)).isEqualTo(expected);
    }

    @Test
    void keepsAngleBracketsAndAmpersandsForMarkdown() {
        assertThat(Escaping.markdown("API <beta> & cia")).isEqualTo("API <beta> & cia");
    }

    @Test
    void escapesTheBackslashOnceWithoutDoubleEscaping() {
        assertThat(Escaping.markdown("\\*")).isEqualTo("\\\\\\*");
    }
}
