package io.github.arthursouto.archwright.desktop.wizard;

import java.util.List;

public record PackageNode(
        String name,
        String qualifiedName,
        String description,
        boolean file,
        int lines,
        int merged,
        List<PackageNode> children) {

    public PackageNode {
        children = List.copyOf(children);
    }

    public static PackageNode pack(String name, String qualifiedName, String description, int lines, int merged, List<PackageNode> children) {
        return new PackageNode(name, qualifiedName, description, false, lines, merged, children);
    }

    public static PackageNode source(String name, int lines) {
        return new PackageNode(name, name, "", true, lines, 0, List.of());
    }

    public int packages() {
        return merged + children.stream().mapToInt(PackageNode::packages).sum();
    }

    public int classes() {
        return (file ? 1 : 0) + children.stream().mapToInt(PackageNode::classes).sum();
    }

    public int totalLines() {
        return lines + children.stream().mapToInt(PackageNode::totalLines).sum();
    }
}
