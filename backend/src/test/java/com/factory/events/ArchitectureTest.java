package com.factory.events;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.factory.events")
class ArchitectureTest {

  // 1. Domínio puro: zero dependência de framework (Spring, JPA, Jackson,
  //    HTTP, segurança, criptografia, resiliência, observabilidade).
  @ArchTest
  static final ArchRule domain_is_pure =
    noClasses().that().resideInAPackage("..domain..")
      .should().dependOnClassesThat().resideInAnyPackage(
        "org.springframework..",
        "jakarta.persistence..",
        "com.fasterxml.jackson..",
        "org.springframework.web..",
        "io.swagger.v3.oas.annotations..",
        "org.springframework.security..",
        "io.jsonwebtoken..",
        "javax.crypto..",
        "java.security..",
        "io.github.resilience4j..",
        "io.opentelemetry..");

  // 2. Application depende apenas de domain + portas; nunca de adapter,
  //    nem de camadas de infraestrutura expostas pelo adapter.
  @ArchTest
  static final ArchRule application_only_domain_and_ports =
    noClasses().that().resideInAPackage("..application..")
      .should().dependOnClassesThat().resideInAnyPackage(
        "..adapter..",
        "org.springframework.web..",
        "jakarta.servlet..",
        "jakarta.persistence..");

  // 3. Domain/Application não conhecem o adaptador web (nenhum DTO HTTP
  //    cruza a fronteira do domínio).
  @ArchTest
  static final ArchRule no_web_dtos_in_core =
    noClasses().that().resideInAnyPackage("..domain..", "..application..")
      .should().dependOnClassesThat().resideInAnyPackage("..adapter.in.web..");

  // 4. Domain/Application não conhecem o adaptador de persistência
  //    (nenhuma entidade JPA cruza a fronteira).
  @ArchTest
  static final ArchRule core_does_not_know_persistence_adapter =
    noClasses().that().resideInAnyPackage("..domain..", "..application..")
      .should().dependOnClassesThat().resideInAPackage("..adapter.out.persistence..");

  // 5. Web não toca persistência diretamente (sempre via aplicação/portas).
  @ArchTest
  static final ArchRule web_does_not_touch_persistence =
    noClasses().that().resideInAPackage("..adapter.in.web..")
      .should().dependOnClassesThat().resideInAPackage("..adapter.out.persistence..");

  // 6. Portas são interfaces.
  @ArchTest
  static final ArchRule ports_are_interfaces =
    classes().that().resideInAPackage("..port..").should().beInterfaces();

  // 7. Portas dependem apenas de domínio + JDK.
  @ArchTest
  static final ArchRule ports_only_domain_and_jdk =
    noClasses().that().resideInAPackage("..port..")
      .should().dependOnClassesThat().resideInAnyPackage(
        "..adapter..",
        "org.springframework..",
        "jakarta.persistence..",
        "com.fasterxml.jackson..",
        "io.github.resilience4j..",
        "io.opentelemetry..");

  // 8. Sem ciclos entre pacotes.
  @ArchTest
  static final ArchRule no_cycles =
    slices().matching("com.factory.events.(*)..").should().beFreeOfCycles();

  // 9. Documentação (Swagger/OpenAPI) apenas em adapter.in.web.
  @ArchTest
  static final ArchRule docs_only_in_web_adapter =
    noClasses().that().resideOutsideOfPackage("..adapter.in.web..")
      .should().dependOnClassesThat().resideInAnyPackage("io.swagger.v3.oas.annotations..");

  // 10. Entidades JPA apenas no adaptador de persistência.
  @ArchTest
  static final ArchRule jpa_entities_only_in_persistence_adapter =
    classes().that().areAnnotatedWith("jakarta.persistence.Entity")
      .should().resideInAPackage("..adapter.out.persistence..");

  // 11. Cache e resiliência nunca no núcleo (domain/application).
  @ArchTest
  static final ArchRule cache_and_resilience_never_in_core =
    noClasses().that().resideInAnyPackage("..domain..", "..application..")
      .should().dependOnClassesThat().resideInAnyPackage(
        "org.springframework.cache.annotation..",
        "com.github.benmanes.caffeine..",
        "io.github.resilience4j..",
        "org.springframework.retry.annotation..");

  // 12. Saga não conhece HTTP nem JPA.
  @ArchTest
  static final ArchRule saga_does_not_know_http =
    noClasses().that().resideInAPackage("..application.saga..")
      .should().dependOnClassesThat().resideInAnyPackage(
        "org.springframework.web..",
        "jakarta.servlet..",
        "jakarta.persistence..")
      .allowEmptyShould(true);

  // 13. Filtro de idempotência vive no adaptador web.
  @ArchTest
  static final ArchRule idempotency_filter_is_web_adapter =
    classes().that().haveSimpleNameEndingWith("IdempotencyFilter")
      .should().resideInAPackage("..adapter.in.web..")
      .allowEmptyShould(true);

  // 14. @Transactional apenas no caso de uso (application).
  @ArchTest
  static final ArchRule transactional_only_in_application =
    classes().that().areAnnotatedWith(
      "org.springframework.transaction.annotation.Transactional")
      .should().resideInAPackage("..application..")
      .allowEmptyShould(true);

  // 15. @Configuration apenas em config.
  @ArchTest
  static final ArchRule configuration_only_in_config =
    noClasses().that().resideOutsideOfPackage("..config..")
      .should().beAnnotatedWith("org.springframework.context.annotation.Configuration");

  // 16. @Bean apenas em config.
  @ArchTest
  static final ArchRule bean_only_in_config =
    noMethods().that().areAnnotatedWith(
      "org.springframework.beans.factory.annotation.Bean")
      .should().beDeclaredInClassesThat().resideInAPackage("..config..")
      .allowEmptyShould(true);

  // 17. Sem injeção de campo.
  @ArchTest
  static final ArchRule no_field_injection =
    noFields().should().beAnnotatedWith(
      "org.springframework.beans.factory.annotation.Autowired");

  // 18. Sem estado mutável em serviço/adaptador (exceto entidades JPA,
  //    cujo ciclo de vida é gerenciado pelo framework).
  @ArchTest
  static final ArchRule services_have_no_mutable_state =
    noClasses().that().resideInAnyPackage("..application..", "..adapter..")
      .should(new ArchCondition<>("ter campo de instância mutável") {
        @Override
        public void check(JavaClass item, ConditionEvents events) {
          if (item.isAnnotatedWith("jakarta.persistence.Entity")) return;
          for (JavaField field : item.getFields()) {
            if (field.getModifiers().contains(JavaModifier.STATIC)) continue;
            if (field.getModifiers().contains(JavaModifier.FINAL)) continue;
            events.add(SimpleConditionEvent.violated(field,
              item.getSimpleName() + "." + field.getName()
                + " é campo de instância mutável"));
          }
        }
      });

  // 19. Domínio não faz log direto (dado pessoal/segredo não vazam em log do
  //    núcleo; log é responsabilidade do adaptador/observabilidade).
  @ArchTest
  static final ArchRule no_logging_in_domain =
    noClasses().that().resideInAPackage("..domain..")
      .should().dependOnClassesThat()
      .haveFullyQualifiedName("org.slf4j.Logger");

  // 20. Segurança/criptografia não vaza para núcleo.
  @ArchTest
  static final ArchRule security_lives_in_adapter =
    noClasses().that().resideInAnyPackage("..domain..", "..application..")
      .should().dependOnClassesThat().resideInAnyPackage(
        "org.springframework.security..",
        "io.jsonwebtoken..",
        "javax.crypto..",
        "java.security..");

  // 21. Portas de saída de idempotência, outbox e saga são interfaces puras de domínio
  //    (domain/port/out) e não dependem de adaptador nem framework.
  @ArchTest
  static final ArchRule outbound_ports_are_pure_domain_interfaces =
    classes().that().resideInAPackage("..domain.port.out..")
      .and().haveNameMatching(".*(IdempotencyStore|OutboxPort|SagaStepPort).*")
      .should().beInterfaces();

  @ArchTest
  static final ArchRule outbound_ports_do_not_depend_on_adapter =
    noClasses().that().resideInAPackage("..domain.port.out..")
      .should().dependOnClassesThat().resideInAnyPackage(
        "..adapter..",
        "org.springframework..",
        "jakarta.persistence..",
        "com.fasterxml.jackson..",
        "io.github.resilience4j..",
        "io.opentelemetry..");

  // 22. application/saga depende apenas de domain + port.out (sem adapter, web, JPA,
  //    resiliência nem observabilidade direta — essas vivem em adapter.out.client/config).
  @ArchTest
  static final ArchRule saga_only_domain_and_ports =
    noClasses().that().resideInAPackage("..application.saga..")
      .should().dependOnClassesThat().resideInAnyPackage(
        "..adapter..",
        "org.springframework.web..",
        "jakarta.servlet..",
        "jakarta.persistence..",
        "io.github.resilience4j..",
        "io.opentelemetry..");

  // 23. application/outbox depende apenas de domain + port.out (mesma regra de 22).
  @ArchTest
  static final ArchRule outbox_only_domain_and_ports =
    noClasses().that().resideInAPackage("..application.outbox..")
      .should().dependOnClassesThat().resideInAnyPackage(
        "..adapter..",
        "org.springframework.web..",
        "jakarta.servlet..",
        "jakarta.persistence..",
        "io.github.resilience4j..",
        "io.opentelemetry..")
      .allowEmptyShould(true);
}
