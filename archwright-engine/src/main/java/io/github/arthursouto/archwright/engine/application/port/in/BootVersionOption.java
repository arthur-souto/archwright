package io.github.arthursouto.archwright.engine.application.port.in;

public record BootVersionOption(String version, String line, boolean legacy, boolean latestOfLine) {
}
