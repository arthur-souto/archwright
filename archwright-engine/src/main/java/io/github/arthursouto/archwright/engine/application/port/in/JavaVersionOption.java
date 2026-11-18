package io.github.arthursouto.archwright.engine.application.port.in;

public record JavaVersionOption(String version, boolean lts, boolean aboveTested, boolean isDefault) {
}
