package io.github.arthursouto.archwright.engine.domain.naming;

import java.util.Map;

public final class Escaping {

    private static final Map<Character, String> XML = Map.of(
            '&', "&amp;",
            '<', "&lt;",
            '>', "&gt;",
            '"', "&quot;",
            '\'', "&apos;");

    private static final Map<Character, String> MARKDOWN = Map.of(
            '\\', "\\\\",
            '*', "\\*",
            '_', "\\_",
            '`', "\\`",
            '[', "\\[",
            ']', "\\]");

    private Escaping() {
    }

    public static String xml(String text) {
        return escape(text, XML);
    }

    public static String markdown(String text) {
        return escape(text, MARKDOWN);
    }

    private static String escape(String text, Map<Character, String> replacements) {
        StringBuilder escaped = new StringBuilder(text.length());

        for (char character : text.toCharArray()) {
            escaped.append(replacements.getOrDefault(character, String.valueOf(character)));
        }

        return escaped.toString();
    }
}
