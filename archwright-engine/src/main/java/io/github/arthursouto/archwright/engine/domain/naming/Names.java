package io.github.arthursouto.archwright.engine.domain.naming;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Names {

    private static final Pattern SEPARATORS = Pattern.compile("[-.]");
    private static final String MAIN_CLASS_SUFFIX = "Application";
    private static final String DIGIT_PREFIX = "App";

    private Names() {
    }

    public static String pascalCase(String value) {
        return SEPARATORS.splitAsStream(value)
                .filter(part -> !part.isEmpty())
                .map(Names::capitalize)
                .collect(Collectors.joining());
    }

    public static String packageSegment(String value) {
        return SEPARATORS.matcher(value).replaceAll("").toLowerCase(Locale.ROOT);
    }

    public static String mainClassName(String artifactId) {
        String className = pascalCase(artifactId) + MAIN_CLASS_SUFFIX;

        return Character.isDigit(className.charAt(0)) ? DIGIT_PREFIX + className : className;
    }

    public static String suggestBasePackage(String groupId, String artifactId) {
        String group = groupId.trim();
        String segment = packageSegment(artifactId.trim());

        if (group.isEmpty() || segment.isEmpty()) {
            return "";
        }

        return group + "." + segment;
    }

    private static String capitalize(String part) {
        return Character.toUpperCase(part.charAt(0)) + part.substring(1);
    }
}
