package com.guidinglight.decisionhub;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ArchitectureTest {

  @Test
  void domain_should_not_depend_on_infra() {
    JavaClasses classes = new ClassFileImporter().importPackages("com.guidinglight.decisionhub");
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage("..infra..")
        .check(classes);
  }
}
