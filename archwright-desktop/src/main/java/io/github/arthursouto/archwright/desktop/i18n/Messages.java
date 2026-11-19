package io.github.arthursouto.archwright.desktop.i18n;

import io.github.arthursouto.archwright.engine.application.port.in.Violation;
import io.github.arthursouto.archwright.engine.application.port.in.Warning;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public final class Messages {

    private static final String BUNDLE = "io.github.arthursouto.archwright.desktop.messages";
    private static final String VIOLATION_PREFIX = "violation.";
    private static final String WARNING_PREFIX = "warning.";

    private final ResourceBundle bundle;

    private Messages(ResourceBundle bundle) {
        this.bundle = bundle;
    }

    public static Messages portuguese() {
        return of(Locale.of("pt", "BR"));
    }

    public static Messages of(Locale locale) {
        return new Messages(ResourceBundle.getBundle(BUNDLE, locale));
    }

    public String text(String key) {
        return bundle.containsKey(key) ? bundle.getString(key) : key;
    }

    public String text(String key, Map<String, String> params) {
        return fill(text(key), params);
    }

    public String text(String key, String name, String value) {
        return text(key, Map.of(name, value));
    }

    public String violation(Violation violation) {
        return coded(VIOLATION_PREFIX + violation.code(), violation.code(), violation.params());
    }

    public String warning(Warning warning) {
        return coded(WARNING_PREFIX + warning.code(), warning.code(), warning.params());
    }

    private String coded(String key, String code, Map<String, String> params) {
        return bundle.containsKey(key) ? fill(bundle.getString(key), params) : code;
    }

    private String fill(String template, Map<String, String> params) {
        String filled = template;

        for (Map.Entry<String, String> param : params.entrySet()) {
            filled = filled.replace("{" + param.getKey() + "}", param.getValue());
        }

        return filled;
    }
}
