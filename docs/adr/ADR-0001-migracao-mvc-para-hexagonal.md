# ADR-0001: Migração de MVC (controller → service → repository) para arquitetura hexagonal (Ports & Adapters)

Status: substituído por ADR-0002
Data: 2026-11-05

## Contexto

O projeto `events-api` está organizado em camadas MVC clássicas: `controller` →
`service` → `repository`, com entidades JPA (`FactoryEvent`, `AuditLog`) usadas
diretamente como modelo de domínio e DTOs acoplados à anotação `@Schema`/Jackson.
A idempotência existe por restrição única (`uk_events_event_id`) e a auditoria
`QUALITY` é gravada na mesma transação do evento. O agregado é único
(`FactoryEvent` + `AuditLog` como registro derivado), sem publicação de eventos,
sem chamada externa e sem leitura de alto volume.

Força em jogo: a lógica de negócio (duplicidade, regra `QUALITY` → auditoria) está
misturada com JPA, Hibernate e Spring Data; testes de unidade precisam de mocks de
repositório JPA; a troca de persistência (H2 ↔ PostgreSQL) exige alterar o domínio.

## Decisão

1. Reorganizar o pacote base `com.factory.events` nas camadas hexagonais:
   `domain`, `application`, `adapter.in.web`, `adapter.out.persistence`, `config`.
2. O `domain` passa a conter apenas `FactoryEvent`, `EventType`, `AuditLog` como
   entidades/modelos puros (sem JPA, Spring, Jackson, HTTP), além das portas
   `port.in` (`RegisterEventUseCase`, `ListEventsUseCase`) e `port.out`
   (`EventRepositoryPort`, `AuditRepositoryPort`, `ClockPort`).
3. Casos de uso vivem em `application` (ex.: `RegisterEventService`), com
   `@Transactional` e orquestração; dependem apenas de `domain` e das portas.
4. Adaptadores JPA (`adapter.out.persistence`) possuem entidades JPA próprias e
   mappers `domain ↔ JPA`; nenhuma entidade JPA atravessa a fronteira.
5. Adaptador web (`adapter.in.web`) contém controller, DTOs, mapper e
   `GlobalExceptionHandler`; anotações Swagger apenas ali.
6. `config` é o único lugar com `@Configuration`/`@Bean`.
7. **Persistência: manter H2 em memória** para dev/test (ausência de PostgreSQL
   local e escopo do desafio). A porta `EventRepositoryPort` isola o domínio; a
   troca para PostgreSQL no futuro é mudança de adaptador, não de domínio.
   Flyway/migrations não são introduzidas nesta fase (DDL via `ddl-auto` do
   adaptador) — migração para Flyway fica para ADR separado quando houver
   PostgreSQL.
8. **Idempotência: preservar a existente.** A unicidade de `eventId` é garantida
   pela constraint `uk_events_event_id` no adaptador de persistência; o caso de uso
   verifica existência prévia e converte `DataIntegrityViolationException` em
   `DuplicateEventException` (HTTP 409). Não se introduz tabela
   `idempotency_key` nem header `Idempotency-Key`: a chave natural do agregado é o
   `eventId`, e a janela de retenção equivale à vida útil do registro (sem
   expurgo). Retry do cliente reexecuta a verificação e devolve 409, que é a
   semântica acordada.
9. **Saga, outbox e cache: não introduzir.** O fluxo toca um único agregado e
   grava auditoria na mesma transação; não há fronteira de serviço, publicação de
   evento nem leitura de alto volume medida. Adicionar esses mecanismos seria
   indireção sem força justificadora (ver Alternativas).
10. Regras ArchUnit (arquivo `ArchitectureTest`) travam as dependências no build:
    domínio puro, application sem adapter, portas são interfaces, sem ciclos,
    Swagger só em `adapter.in.web`, sem cache no núcleo, sem JPA no web,
    sem injeção por campo, sem estado mutável em serviço/adaptador.

## Consequências

Fica fácil:
- Testar regras de negócio sem Spring/JPA (mock das portas).
- Trocar H2 por PostgreSQL alterando apenas `adapter.out.persistence`.
- Localizar responsabilidade: porta expressa intenção, adaptador expressa mecânica.

Fica difícil:
- Mais classes e mappers (indireção); custo aceito porque o domínio é o ativo de
  longo prazo.
- Toda mudança de contrato de persistência passa por mapper (em vez de anotação
  direta na entidade).

Números:
- Idempotência: retenção = vida útil do registro (permanente, sem expurgo);
  concorrência coberta por constraint única + tratamento de violação.
- Cache: não aplicável. Reavaliar quando a listagem exceder 10 000 registros ou o
  p95 do `GET /api/events` superar 200 ms, com baseline medido.
- Saga: não aplicável (agregado único, 1 participante).

## Alternativas descartadas

- **Manter MVC com service transacional.** Rejeitada: mantém acoplamento domínio↔JPA
  e impede teste puro; o custo de não mudar cresce com cada regra nova.
- **Introduzir saga orquestrada.** Rejeitada: não há travessia de agregados nem de
  serviços; um orquestrador seria código sem força justificadora.
- **Introduzir transactional outbox.** Rejeitada: não há consumidor de evento; a
  auditoria é registro derivado na mesma transação, não evento publicado.
- **Introduzir cache (Caffeine/Redis).** Rejeitada: sem medição de leitura,
  volume pequeno, risco de servir dado desatualizado em escrita; regra de negócio
  não declara TTL.
- **Introduzir tabela `idempotency_key` + header `Idempotency-Key`.** Rejeitada:
  o `eventId` já é a chave natural e única; duplicar a chave seria indireção sem
  variação prevista.

## Substituído por

Este ADR foi substituído pelo **ADR-0002** (arquitetura hexagonal), que adota as diretivas do squad (`AGENTS.md`):
PostgreSQL 17 + Flyway, idempotência por header `Idempotency-Key` com janela de retenção, saga orquestrada com compensação e transactional outbox. Os itens 7, 8 e 9 do ADR-0001 (H2 + `ddl-auto`, idempotência apenas por `eventId`, ausência de saga/outbox) entram em conflito direto com a spec (`docs/spec/regras-negocio.md`, RN-02..RN-07, CA-05..CA-09) e com a pilha obrigatória do squad.

## Referências

- Estrutura de pacotes: `docs/architecture/estrutura-pacotes.md`
- Mapeamento de entidades: `docs/architecture/mapeamento-entidades.md`
- Regras ArchUnit: `backend/src/test/java/com/factory/events/ArchitectureTest.java`
- ADRs que substituem: `ADR-0002` (hexagonal), `ADR-0003` (idempotência), `ADR-0004` (saga/outbox), `ADR-0005` (cache)
