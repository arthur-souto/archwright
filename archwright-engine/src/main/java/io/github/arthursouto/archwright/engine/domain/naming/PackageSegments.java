package io.github.arthursouto.archwright.engine.domain.naming;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class PackageSegments {

    private static final Pattern IDENTIFIER_SHAPE = Pattern.compile("[a-z][a-z0-9_]*");

    private static final Set<String> RESERVED_WORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "true", "false", "null", "var", "record", "yield", "sealed", "permits");

    private PackageSegments() {
    }

    public static List<String> split(String dottedName) {
        return List.of(dottedName.split("\\.", -1));
    }

    public static boolean hasIdentifierShape(String segment) {
        return IDENTIFIER_SHAPE.matcher(segment).matches();
    }

    public static boolean isValid(String segment) {
        return hasIdentifierShape(segment) && !RESERVED_WORDS.contains(segment);
    }

    public static Optional<String> firstInvalid(String dottedName) {
        return split(dottedName).stream()
                .filter(segment -> !isValid(segment))
                .findFirst();
    }
}
