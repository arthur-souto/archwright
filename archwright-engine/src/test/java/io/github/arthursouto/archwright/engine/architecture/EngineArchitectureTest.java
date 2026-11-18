package io.github.arthursouto.archwright.engine.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Fitness functions do próprio engine (ARCHITECTURE.md > Regras de dependência).
 * AW-FF-05 vive no módulo desktop (Fase 2).
 */
@AnalyzeClasses(
        packages = "io.github.arthursouto.archwright.engine",
        importOptions = ImportOption.DoNotIncludeTests.class)
class EngineArchitectureTest {

    private static final String ROOT = "io.github.arthursouto.archwright.engine";

    @ArchTest // AW-FF-01
    static final ArchRule domainNaoDependeDeApplicationNemAdapter =
            noClasses().that().resideInAPackage(ROOT + ".domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            ROOT + ".application..", ROOT + ".adapter..", ROOT + ".config..");

    @ArchTest // AW-FF-01
    static final ArchRule applicationNaoDependeDeAdapterNemConfig =
            noClasses().that().resideInAPackage(ROOT + ".application..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            ROOT + ".adapter..", ROOT + ".config..");

    @ArchTest // AW-FF-01
    static final ArchRule adapterNaoDependeDeConfig =
            noClasses().that().resideInAPackage(ROOT + ".adapter..")
                    .should().dependOnClassesThat().resideInAPackage(ROOT + ".config..");

    @ArchTest // AW-FF-02
    static final ArchRule dominioNaoFazEs =
            noClasses().that().resideInAPackage(ROOT + ".domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.yaml.snakeyaml..", "com.samskivert.mustache..",
                            "java.nio.file..", "java.net..", "java.util.zip..")
                    .orShould().dependOnClassesThat().areAssignableTo(java.io.File.class);

    @ArchTest // AW-FF-03
    static final ArchRule applicationNaoConheceYamlNemMustache =
            noClasses().that().resideInAPackage(ROOT + ".application..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.yaml.snakeyaml..", "com.samskivert.mustache..");

    @ArchTest // AW-FF-04
    static final ArchRule portasSaoInterfacesRecordsOuEnums =
            classes().that().resideInAnyPackage(ROOT + ".application.port.in..", ROOT + ".application.port.out..")
                    .and().areNotNestedClasses()
                    .should().beInterfaces().orShould().beRecords().orShould().beEnums()
                    .because("portas são contratos, não implementações");

    @ArchTest // AW-FF-06
    static final ArchRule semCiclosEntrePacotes =
            slices().matching(ROOT + ".(*)..").should().beFreeOfCycles();
}
