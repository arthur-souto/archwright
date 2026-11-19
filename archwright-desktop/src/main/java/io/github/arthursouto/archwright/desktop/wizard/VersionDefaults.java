package io.github.arthursouto.archwright.desktop.wizard;

import io.github.arthursouto.archwright.engine.application.port.in.BootVersionOption;
import io.github.arthursouto.archwright.engine.application.port.in.JavaVersionOption;

import java.util.List;
import java.util.Optional;

public final class VersionDefaults {

    private VersionDefaults() {
    }

    public static Optional<BootVersionOption> boot(List<BootVersionOption> options, String current) {
        return options.stream()
                .filter(option -> option.version().equals(current))
                .findFirst()
                .or(() -> options.stream().filter(option -> !option.legacy() && option.latestOfLine()).findFirst())
                .or(() -> options.stream().findFirst());
    }

    public static Optional<JavaVersionOption> java(List<JavaVersionOption> options, String current) {
        return options.stream()
                .filter(option -> option.version().equals(current))
                .findFirst()
                .or(() -> options.stream().filter(JavaVersionOption::isDefault).findFirst())
                .or(() -> options.stream().findFirst());
    }
}
