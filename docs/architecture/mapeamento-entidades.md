# Mapeamento de entidades — domínio ↔ JPA

Regra: **entidade JPA nunca atravessa a fronteira**. Toda conversão ocorre no
adaptador de persistência via mapper.

## FactoryEvent

| Domínio (`domain.model.FactoryEvent`) | JPA (`adapter.out.persistence.entity.FactoryEventJpaEntity`) | Observação |
| --- | --- | --- |
| `eventId: String` | `eventId: String` (`@Column(name="event_id", unique, not null)`) | chave natural / idempotência |
| `eventType: EventType` | `eventType: EventType` (`@Enumerated(EnumType.STRING)`) | enum compartilhado (domínio é a fonte) |
| `description: String` | `description: String` (`@Column(length=1000, not null)`) | |
| `timestamp: Instant` | `timestamp: Instant` (`@Column(not null)`) | |
| (não existe) | `id: Long` (`@Id @GeneratedValue IDENTITY`) | identificador interno, só no adaptador |

- `FactoryEvent` de domínio é imutável; não carrega `id` interno. A ordenação por `id`
  interno é responsabilidade do adaptador (`findAllOrdered`).
- O `EventType` enum mora no `domain`; o adaptador reutiliza o mesmo enum (não
  duplica). Se no futuro o JPA precisar de enum próprio, o mapper converte.

## AuditLog

| Domínio (`domain.model.AuditLog`) | JPA (`adapter.out.persistence.entity.AuditLogJpaEntity`) | Observação |
| --- | --- | --- |
| `eventId: String` | `eventId: String` (`@Column(length=100, not null)`) | |
| `logMessage: String` | `logMessage: String` (`@Column(length=2000, not null)`) | |
| `createdAt: Instant` | `createdAt: Instant` (`@Column(not null)`) | |
| (não existe) | `id: Long` (`@Id @GeneratedValue IDENTITY`) | identificador interno, só no adaptador |

## Mapper (`EventPersistenceMapper`)

Métodos mínimos:

- `FactoryEventJpaEntity toJpa(FactoryEvent domain)`
- `FactoryEvent toDomain(FactoryEventJpaEntity jpa)`
- `AuditLogJpaEntity toJpa(AuditLog domain)`
- `AuditLog toDomain(AuditLogJpaEntity jpa)`

O mapper é `final`, sem estado, e vive em `adapter.out.persistence.mapper`. Nenhum
outro pacote o importa além do adaptador.

## Regras de conversão

1. Conversão ocorre **dentro** do adaptador; o caso de uso recebe/retorna modelos de
   domínio.
2. `null` não é retornado por porta; uso de `Optional` apenas onde o contrato da porta
   declara ausência legítima (`findByEventId`).
3. O adaptador preserva a constraint `uk_events_event_id` e o tratamento de
   `DataIntegrityViolationException` → `DuplicateEventException`.
4. `saveAndFlush` (forçar disputa da chave única) permanece no adaptador, não no
   domínio.
5. Ordenação (`timestamp desc, id desc`) é implementada no adaptador via query Spring
   Data; o domínio pede `findAllOrdered()`.
6. Nenhuma anotação Jackson/Swagger/JPA aparece em `domain` ou `application`.
