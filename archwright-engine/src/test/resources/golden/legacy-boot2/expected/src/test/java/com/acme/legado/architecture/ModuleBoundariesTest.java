package com.acme.legado.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.acme.legado", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundariesTest {

    private static final String BASE_PACKAGE = "com.acme.legado";
    private static final Set<String> NOT_MODULES = new HashSet<>(Arrays.asList("bootstrap", "shared"));

    @ArchTest
    static final ArchRule FF_MOD_01 = classes()
            .should(useOnlyThePublicSurfaceOfOtherModules())
            .as("FF-MOD-01: a module only uses application.port.in and domain of another module");

    @ArchTest
    static final ArchRule FF_MOD_02 = classes()
            .that().resideInAPackage(BASE_PACKAGE + ".shared..")
            .should(dependOnNoModule())
            .as("FF-MOD-02: the shared kernel depends on no module");

    @ArchTest
    static final ArchRule FF_MOD_03 = slices()
            .matching(BASE_PACKAGE + ".(*)..")
            .should().beFreeOfCycles()
            .as("FF-MOD-03: no cycles between modules");

    private static ArchCondition<JavaClass> useOnlyThePublicSurfaceOfOtherModules() {
        return new ArchCondition<JavaClass>("use only domain and application.port.in of other modules") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                String module = moduleOf(origin);

                if (module == null) {
                    return;
                }

                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String other = moduleOf(target);

                    if (other != null && !other.equals(module) && !isPublicSurface(target, other)) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> dependOnNoModule() {
        return new ArchCondition<JavaClass>("depend on no module") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    if (moduleOf(dependency.getTargetClass()) != null) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    private static String moduleOf(JavaClass type) {
        String prefix = BASE_PACKAGE + ".";
        String packageName = type.getPackageName();

        if (!packageName.startsWith(prefix)) {
            return null;
        }

        String first = packageName.substring(prefix.length()).split("\\.")[0];

        return NOT_MODULES.contains(first) ? null : first;
    }

    private static boolean isPublicSurface(JavaClass type, String module) {
        String root = BASE_PACKAGE + "." + module;

        return isInside(type.getPackageName(), root + ".domain")
                || isInside(type.getPackageName(), root + ".application.port.in");
    }

    private static boolean isInside(String packageName, String root) {
        return packageName.equals(root) || packageName.startsWith(root + ".");
    }
}
