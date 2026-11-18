package io.github.arthursouto.archwright.engine.domain.model;

import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record BootVersion(int major, int minor, int patch) implements Comparable<BootVersion> {

    public static final BootVersion MINIMUM_SUPPORTED = new BootVersion(2, 7, 0);

    private static final Pattern STABLE = Pattern.compile("(\\d{1,4})\\.(\\d{1,4})\\.(\\d{1,4})");

    private static final Comparator<BootVersion> ORDER = Comparator
            .comparingInt(BootVersion::major)
            .thenComparingInt(BootVersion::minor)
            .thenComparingInt(BootVersion::patch);

    public BootVersion {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("negative version component");
        }
    }

    public static Optional<BootVersion> parse(String raw) {
        Matcher matcher = STABLE.matcher(raw.trim());

        if (!matcher.matches()) {
            return Optional.empty();
        }

        return Optional.of(new BootVersion(component(matcher, 1), component(matcher, 2), component(matcher, 3)));
    }

    public String line() {
        return major + "." + minor;
    }

    @Override
    public int compareTo(BootVersion other) {
        return ORDER.compare(this, other);
    }

    @Override
    public String toString() {
        return line() + "." + patch;
    }

    private static int component(Matcher matcher, int group) {
        return Integer.parseInt(matcher.group(group));
    }
}
