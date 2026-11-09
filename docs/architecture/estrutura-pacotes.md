# Estrutura de pacotes alvo — events-api (hexagonal)

Pacote base: `com.factory.events`.

```
com.factory.events
├── domain
│   ├── model
│   │   ├── FactoryEvent.java        # entidade de domínio pura (record/class imutável)
│   │   ├── EventType.java           # enum de domínio
│   │   └── AuditLog.java            # registro de auditoria de domínio
│   ├── port.in
│   │   ├── RegisterEventUseCase.java
│   │   ├── ListEventsUseCase.java
│   │   └── RegisterEventCommand.java # record de comando (entrada do caso de uso)
│   └── port.out
│       ├── EventRepositoryPort.java
│       ├── AuditRepositoryPort.java
│       └── ClockPort.java
├── application
│   └── RegisterEventService.java    # implementa port.in; @Transactional
├── adapter.in.web
│   ├── EventController.java
│   ├── dto
│   │   ├── EventRequestDTO.java
│   │   ├── EventResponseDTO.java
│   │   └── ApiError.java
│   ├── mapper
│   │   └── EventWebMapper.java
│   └── GlobalExceptionHandler.java
├── adapter.out.persistence
│   ├── entity
│   │   ├── FactoryEventJpaEntity.java
│   │   └── AuditLogJpaEntity.java
│   ├── repository
│   │   ├── SpringDataEventRepository.java
│   │   └── SpringDataAuditRepository.java
│   ├── adapter
│   │   ├── EventRepositoryAdapter.java   # implementa EventRepositoryPort
│   │   └── AuditRepositoryAdapter.java   # implementa AuditRepositoryPort
│   └── mapper
│       └── EventPersistenceMapper.java
└── config
    ├── ApplicationConfig.java        # @Configuration/@Bean (Clock, OpenAPI)
    └── EventTimestampDeserializer.java
```

## Responsabilidades por camada

| Pacote | Pode depender de | Não pode depender de |
| --- | --- | --- |
| `domain` | apenas JDK (records, sealed, pattern matching) | Spring, JPA, Jackson, HTTP, Kafka |
| `application` | `domain`, portas | `adapter.*` |
| `adapter.in.web` | `application`, `domain`, Spring Web, Bean Validation, Swagger | `adapter.out.persistence` |
| `adapter.out.persistence` | `application`, `domain`, Spring Data JPA | `adapter.in.web` |
| `config` | qualquer (único lugar com `@Configuration`/`@Bean`) | — |

## Portas

- `port.in.RegisterEventUseCase`: `FactoryEvent register(RegisterEventCommand command)`.
- `port.in.ListEventsUseCase`: `List<FactoryEvent> listAll()`.
- `port.out.EventRepositoryPort`: `boolean existsByEventId(String)`, `Optional<FactoryEvent> findByEventId(String)`, `FactoryEvent save(FactoryEvent)`, `List<FactoryEvent> findAllOrdered()`.
- `port.out.AuditRepositoryPort`: `AuditLog save(AuditLog)`.
- `port.out.ClockPort`: `Instant now()` (substitui `java.time.Clock` injetado, mantendo testabilidade sem Spring).

## Regras de dependência

- Dependências apontam para dentro; nenhum ciclo entre pacotes (ArchUnit).
- `domain` e `application` não importam anotação de documentação `io.swagger.v3.oas.annotations..`.
- Nenhuma entidade JPA fora de `adapter.out.persistence`; nenhum DTO HTTP fora de `adapter.in.web`.
- Toda porta `out` implementada por exatamente um adaptador ativo por perfil.
- `ClockPort` implementado por um adaptador em `adapter.out` (ou bean em `config` que implementa a porta); a decisão de onde mora fica com `backend-java`, desde que a porta seja implementada por um único adaptador.

## Convenções

- Value objects e comandos são `record`.
- Entidades de domínio são imutáveis (`record` ou classe `final` com campos `final`); mutação ocorre por criação de nova instância.
- Exceções de domínio (`DuplicateEventException`) moram em `domain` (ou `application`? aqui: `domain` como exceção de negócio).
- Mappers são classes `final` com métodos estáticos ou instância sem estado.
