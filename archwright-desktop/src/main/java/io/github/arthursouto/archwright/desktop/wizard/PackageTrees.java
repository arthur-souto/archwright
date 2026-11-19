package io.github.arthursouto.archwright.desktop.wizard;

import io.github.arthursouto.archwright.engine.application.port.in.PreviewFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class PackageTrees {

    private static final String SOURCE_ROOT = "src/main/java/";
    private static final String PACKAGE_INFO = "package-info.java";

    private PackageTrees() {
    }

    public static List<PackageNode> from(List<PreviewFile> files) {
        Branch root = new Branch("");

        for (PreviewFile file : files) {
            if (file.path().startsWith(SOURCE_ROOT)) {
                root.add(file.path().substring(SOURCE_ROOT.length()).split("/"), 0, file);
            }
        }

        return root.packages.entrySet().stream().map(entry -> entry.getValue().toNode(entry.getKey(), 0, 0)).toList();
    }

    private static final class Branch {

        private final String qualifiedName;
        private final Map<String, Branch> packages = new TreeMap<>();
        private final Map<String, Integer> sources = new TreeMap<>();
        private String description = "";
        private boolean declared;
        private int lines;

        Branch(String qualifiedName) {
            this.qualifiedName = qualifiedName;
        }

        void add(String[] segments, int index, PreviewFile file) {
            String segment = segments[index];

            if (index < segments.length - 1) {
                packages.computeIfAbsent(segment, name -> new Branch(join(qualifiedName, name))).add(segments, index + 1, file);
                return;
            }

            if (segment.equals(PACKAGE_INFO)) {
                declared = true;
                description = file.description();
                lines = file.lines();
                return;
            }

            sources.put(segment, file.lines());
        }

        PackageNode toNode(String name, int carriedPackages, int carriedLines) {
            int merged = carriedPackages + (declared ? 1 : 0);

            if (sources.isEmpty() && packages.size() == 1) {
                Map.Entry<String, Branch> only = packages.entrySet().iterator().next();

                return only.getValue().toNode(name + "." + only.getKey(), merged, carriedLines + lines);
            }

            List<PackageNode> children = new ArrayList<>();
            packages.forEach((childName, child) -> children.add(child.toNode(childName, 0, 0)));
            sources.forEach((source, sourceLines) -> children.add(PackageNode.source(source, sourceLines)));

            return PackageNode.pack(name, qualifiedName, description, carriedLines + lines, merged, children);
        }

        private String join(String parent, String name) {
            return parent.isEmpty() ? name : parent + "." + name;
        }
    }
}
