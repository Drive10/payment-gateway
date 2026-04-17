package dev.payment.common.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureRules {

    private JavaClasses classes;

    @BeforeEach
    void setUp() {
        classes = new ClassFileImporter()
            .importPackages("dev.payment..");
    }

    @Test
    void no_classes_should_use_java_util_logging() {
        ArchRule rule = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
        rule.check(classes);
    }

    @Test
    void no_classes_should_access_standard_streams() {
        ArchRule rule = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
        rule.check(classes);
    }

    @Test
    void no_classes_should_throw_generic_exceptions() {
        ArchRule rule = GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
        rule.check(classes);
    }

    @Test
    void repositories_should_be_interfaces() {
        ArchRule rule = classes()
            .that(resideInAPackage("..repository.."))
            .should()
            .beInterfaces();

        rule.check(classes);
    }

    @Test
    void controllers_should_reside_in_controller_package() {
        ArchRule rule = classes()
            .that(simpleNameEndingWith("Controller"))
            .should()
            .resideInAPackage("..controller..");

        rule.check(classes);
    }

    @Test
    void services_should_reside_in_service_package() {
        ArchRule rule = classes()
            .that(simpleNameEndingWith("Service"))
            .should()
            .resideInAPackage("..service..");

        rule.check(classes);
    }

    @Test
    void entities_should_reside_in_entity_package() {
        ArchRule rule = classes()
            .that(simpleNameEndingWith("Entity"))
            .should()
            .resideInAPackage("..entity..");

        rule.check(classes);
    }
}
