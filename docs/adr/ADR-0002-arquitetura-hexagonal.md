# ADR-0002: Arquitetura Hexagonal (Ports & Adapters) — pacotes, fronteira de domínio e pilha obrigatória

Status: aceito
Data: 2026-11-05
Substitui: ADR-0001 (status `substituído`)

## Contexto

O módulo `events-api` (backend) estava organizado em camadas MVC clássicas
(`controller` → `service` → `repository`), com entidades JPA
(`FactoryEvent`, `AuditLog`) expostas diretamente como modelo de domínio, DTOs
acoplados a `@Schema`/Jackson, H2 em memória com `ddl-auto`, e nenhuma
idempotência por chave, saga nem outbox.

A spec de negócio (`docs/spec/regras-negocio.md`, RN-01..RN-22, CA-01..CA-17)
e o contrato do squad (`AGENTS.md`) exigem:

- PostgreSQL 17 + Flyway (migrations versionadas, idempotentes, reversíveis) — pilha obrigatória.
- Idempotência por header `Idempotency-Key` com tabela `idempotency_key`, janela
  de retenção e 409s padronizados (RN-03, RN-04, RN-05, CA-05..CA-08).
- Saga orquestrada com compensação para registro QUALITY + auditoria
  (RN-06, RN-07, CA-09), que cruza agregados (evento + auditoria).
- Transactional outbox com `event_version` (at-least-once, consumidor deduplica).
- Domínio puro: sem Spring, JPA, Jackson, HTTP, segurança, criptografia,
  cache ou resiliência no `domain` (Lei #1 do squad).

ADR-0001 (status `proposto`) rejeitou exatamente esses mecanismos
(idempotency_key, saga, outbox, cache, Flyway) e manteve H2 + `ddl-auto`;
entra em conflito direto com as diretivas aceitas. Deve ser substituído.

## Decisão

1. **Pacotes base** sob `com.factory.events`:

   - `domain` — modelo de domínio (records imutáveis), exceções de domínio,
     portas `port.in` (casos de uso) e `port.out` (dependências do domínio).
     Puro: sem frameworks.
   - `application` — implementação dos casos de uso, transações
     (`@Transactional` apenas aqui) e orquestração de sagas em
     `application/saga` e escrita de evento de saída (outbox) em `application/outbox`. Depende **apenas** de `domain` e de `port.*`.
   - `adapter.in.web` — controllers REST, DTOs HTTP, mappers,
     `GlobalExceptionHandler` (ProblemDetail RFC 9457), filtro de idempotência.
     Anotações Swagger/OpenAPI **apenas** aqui.
   - `adapter.out.persistence` — entidades JPA, repositórios Spring Data,
     mappers `JPA ↔ domínio`.
   - `adapter.out.cache` — decorators de cache sobre portas de leitura
     (não introduzido agora; veja ADR-0005).
   - `adapter.out.client` — clients REST (`RestClient`) com Resilience4j
     (circuit breaker, retry, bulkhead, rate limiter).
   - `config` — único lugar com `@Configuration`/`@Bean`
     (segurança, serialização, observabilidade, Resilience4j).

2. **Direção da dependência**: `adapter` → `application` → `domain`;
   `domain` não depende de nada externo; ciclos entre pacotes quebram o build
   (ArchUnit).

3. **Modelo de domínio**: `record` imutáveis para value objects e comandos;
   `sealed interface` + pattern matching para resultados de caso de uso e
   estados de saga; `switch` com padrões em vez de cadeias de `instanceof`;
   JSpecify null-safety (`@NonNull`/`@Nullable`) nas APIs públicas; nenhuma
   porta retorna `Optional` nem `null`.

4. **Fronteiras**: nenhuma entidade JPA e nenhum DTO HTTP cruza a fronteira do
   domínio; sempre mapper. Nenhuma anotação de documentação
   (`io.swagger.v3.oas.annotations`) fora de `adapter.in.web`.
   Nenhum `@Cacheable`/`@Retryable`/`@Transactional` no `domain`;
   `@Transactional` apenas em `application`.

5. **Pilha obrigatória adotada**: Java 25, Spring Boot 4, PostgreSQL 17,
   Flyway, Jackson, Bean Validation, Resilience4j, OpenTelemetry,
   Micrometer/Actuator, ArchUnit, JSpecify.

## Consequências

Fica fácil:
- Testar domínio sem Spring/JPA (mock de portas).
- Trocar persistência alterando apenas `adapter.out.persistence`.
- Localizar responsabilidade: porta = intenção de negócio; adaptador = mecânica
  de infra.
- Aplicar ArchUnit no build (trava violações automaticamente).

Fica difícil:
- Mais classes e mappers (indireção); custo aceito porque o domínio é o ativo
  de longo prazo.
- Toda mudança de contrato de persistência passa por mapper.

## Números (força registrada)

- Zero anotações de framework permitidas no `domain`.
- `@Transactional` restrito a `application`.
- Swagger/Jackson restritos a `adapter.in.web`/`config`.
- Idempotência, saga, outbox, Flyway: obrigatórios por diretiva do squad
  (não opcionais).

## Alternativas descartadas

- **Manter MVC `controller → service → repository`.** Rejeitada: mantém
  acoplamento `domain ↔ JPA`; impede teste puro do domínio; custo de não mudar
  cresce a cada regra nova (RN-01..RN-22).
- **Manter H2 + `ddl-auto` sem Flyway.** Rejeitada: viola a pilha obrigatória
  do `AGENTS.md` (PostgreSQL 17 + Flyway versionadas).
- **Manter `eventId` como única chave de idempotência.** Rejeitado: não atende
  RN-03/RN-04/RN-05 (janela de retenção, `409 idempotency-key-reused`,
  `idempotency-in-progress`, replay seguro). Detalhe em ADR-0003.
- **Não introduzir saga nem outbox.** Rejeitado: `QUALITY → auditoria` cruza
  agregados e RN-07 exige compensação visível, não rollback silencioso.
  Detalhe em ADR-0004.
- **Introduzir cache agora.** Rejeitado: sem medição de leitura, volume
  pequeno, sem RN de TTL; RN-21 exige leitura direta. Detalhe em ADR-0005.

## Referências

- `AGENTS.md` — contrato do squad (pilha, leis de arquitetura, estrutura de
  pacotes).
- `docs/spec/regras-negocio.md` — RN-xx, CA-xx, E-01..E-13.
- `docs/adr/ADR-0003` — Idempotência; `ADR-0004` — Saga + outbox;
  `ADR-0005` — Cache.
- `backend/src/test/java/com/factory/events/ArchitectureTest.java` — regras
  ArchUnit.
