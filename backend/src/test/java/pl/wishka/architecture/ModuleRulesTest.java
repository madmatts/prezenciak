package pl.wishka.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RestController;

/**
 * Module boundaries (CLAUDE.md §5): package by feature, modules talk only through each other's {@code api} package
 * (services, events, DTOs), never through repositories or internals. {@code common} is shared by everyone.
 *
 * <p>The layout stays compatible with Spring Modulith (D-34).
 */
@AnalyzeClasses(packages = "pl.wishka", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleRulesTest {

    private static final String ROOT = "pl.wishka.";

    @ArchTest
    static final ArchRule featureModulesHaveNoCycles =
            slices().matching("pl.wishka.(*)..").should().beFreeOfCycles().allowEmptyShould(true);

    @ArchTest
    static final ArchRule modulesUseOnlyOtherModulesApi = classes()
            .that()
            .resideInAPackage("pl.wishka..")
            .should(onlyDependOnOtherModulesThroughTheirApi())
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule commonDoesNotDependOnFeatures = noClasses()
            .that()
            .resideInAPackage("pl.wishka.common..")
            .should()
            .dependOnClassesThat(isInFeatureModule())
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule webLayerDoesNotUseRepositories = noClasses()
            .that()
            .resideInAPackage("..web..")
            .should()
            .dependOnClassesThat()
            .areAssignableTo(Repository.class)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule entitiesStayOutOfTheWebLayer = noClasses()
            .that()
            .resideInAPackage("..web..")
            .should()
            .dependOnClassesThat()
            .areAnnotatedWith(Entity.class)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllersLiveInWebPackages = classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .should()
            .resideInAPackage("..web..")
            .allowEmptyShould(true);

    /** Spoiler protection, layer 1 (golden rule 1). Enabled once the owner/giver modules exist. */
    @Test
    @Disabled("Phase 5: owner-facing web classes must not depend on reservation or giver/public DTOs")
    void ownerViewsDoNotDependOnReservations() {}

    @Test
    @Disabled("Phase 6: item, wishlist, circle and user modules must not depend on the reservation module")
    void coreModulesDoNotDependOnReservations() {}

    private static ArchCondition<JavaClass> onlyDependOnOtherModulesThroughTheirApi() {
        return new ArchCondition<>("only depend on other modules through their api package") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                Optional<String> originModule = module(origin);
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    Optional<String> targetModule = module(target);
                    if (targetModule.isEmpty() || targetModule.equals(originModule)) {
                        continue;
                    }
                    String apiPackage = ROOT + targetModule.get() + ".api";
                    if (!target.getPackageName().startsWith(apiPackage)) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    private static com.tngtech.archunit.base.DescribedPredicate<JavaClass> isInFeatureModule() {
        return com.tngtech.archunit.base.DescribedPredicate.describe(
                "are in a feature module", javaClass -> module(javaClass).isPresent());
    }

    /** Feature module of a class, e.g. {@code item} for {@code pl.wishka.item.web.ItemController}. */
    private static Optional<String> module(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();
        if (!packageName.startsWith(ROOT)) {
            return Optional.empty();
        }
        String rest = packageName.substring(ROOT.length());
        String module = rest.contains(".") ? rest.substring(0, rest.indexOf('.')) : rest;
        return module.equals("common") || module.isEmpty() ? Optional.empty() : Optional.of(module);
    }
}
