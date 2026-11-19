package io.github.arthursouto.archwright.desktop.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(
        packages = "io.github.arthursouto.archwright.desktop",
        importOptions = ImportOption.DoNotIncludeTests.class)
class DesktopArchitectureTest {

    private static final String DESKTOP = "io.github.arthursouto.archwright.desktop";
    private static final String ENGINE = "io.github.arthursouto.archwright.engine";

    private static final DescribedPredicate<JavaClass> ENGINE_FORA_DA_FRONTEIRA =
            resideInAPackage(ENGINE + "..")
                    .and(not(resideInAnyPackage(ENGINE + ".application.port.in..", ENGINE + ".config..")))
                    .as("classes do engine fora de application.port.in e config");

    @ArchTest // AW-FF-05
    static final ArchRule desktopSoEnxergaPortasDeEntradaEConfig =
            noClasses().that().resideInAPackage(DESKTOP + "..")
                    .should().dependOnClassesThat(ENGINE_FORA_DA_FRONTEIRA);

    @ArchTest // AW-FF-08
    static final ArchRule modeloDoWizardNaoConheceATela =
            noClasses().that().resideInAnyPackage(DESKTOP + ".wizard..", DESKTOP + ".i18n..", DESKTOP + ".preferences..")
                    .should().dependOnClassesThat().resideInAPackage(DESKTOP + ".view..");

    @ArchTest // AW-FF-06
    static final ArchRule semCiclosEntrePacotes =
            slices().matching(DESKTOP + ".(*)..").should().beFreeOfCycles();
}
