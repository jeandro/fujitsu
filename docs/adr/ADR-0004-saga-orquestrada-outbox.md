# ADR-0004: Saga orquestrada + transactional outbox (registro QUALITY → auditoria)

Status: aceito
Data: 2026-11-05
Substitui: item 9 do ADR-0001 (via ADR-0002)

## Contexto

RN-06: evento QUALITY gera log de auditoria vinculado ao `eventId`, na mesma
unidade de trabalho. RN-07: se o log de auditoria não puder ser gravado, executa
compensação (desfaz registro do evento, grava registro de compensação com
data/motivo/responsável "sistema"), nenhum efeito parcial visível, usuário
recebe erro (E-05). CA-09.

O código atual grava ambas as entidades em uma única transação local em
`RegisterEventService`, sem compensação explícita nem estado persistido. Isso
não atende a spec: a falha de auditoria exige **compensação de negócio**
(comando novo, auditável, idempotente), não rollback silencioso; e o fluxo
cruza dois agregados (evento + log de auditoria), fronteiras de consistência
distintas.

ADR-0001 rejeitou saga e outbox por "um único agregado". A spec mostra que há
**dois** agregados, logo há travessia de agregados.

## Decisão

1. **Saga orquestrada** em `application/saga` (caso de uso `...Saga`, sem
   HTTP). Orquestra os passos:
   - Passo 1: registrar evento (`EventRepositoryPort.save`).
   - Passo 2: gravar log de auditoria QUALITY (`AuditRepositoryPort.save`) —
     apenas quando `eventType == QUALITY`.
   Cada passo declara **ação** e **compensação** com semântica de negócio
   (RN-07). Compensação = comando novo (registro de compensação), auditável e
   idempotente; não é rollback de banco.

2. **Estado persistido** em tabelas `saga_instance` + `saga_step`
   (saga, passo, status, tentativa, erro, atualizado em). Nunca variável de
   memória. Estados modelados com `sealed interface`
   (`Started`, `InProgress`, `Succeeded`, `Failed`, `FailedCompensated`,
   `Compensated`) + pattern matching; transição inválida → exceção de domínio.

3. **Timeouts**: passo 30 s, global 5 min. **Retry**: 3× com backoff
   exponencial + jitter. Ao esgotar → `FAILED_COMPENSATED` com motivo e
   trilha de auditoria; nunca engole exceção.

4. **Transactional outbox**: evento de saída gravado por `OutboxPort` na
   **mesma transação** do agregado; tabelas `outbox_event` com
   `event_version`. Relay (adaptador) publica e marca `published_at`. Publicar
   evento fora da transação é proibido. Consumidor deduplica por `event_id` em
   `processed_event`; rejeita versão desconhecida de `event_version`.

5. **Comunicação entre passos**: at-least-once; ordem por chave de
   correlação, nunca por timing.

6. **Saga não conhece HTTP nem JPA** (`application/saga` depende só de
   `domain` e portas); usa conexões curtas (não segura conexão de banco entre
   passos).

## Por que cruza agregados (QUALITY → auditoria)

O evento (`FactoryEvent`) e o log de auditoria (`AuditLog`) são agregados com
identidades e ciclos de vida distintos (auditoria é imutável; evento nunca é
apagado, RN-22). Uma transação local única garante atomicidade de banco, mas
não a **semântica de compensação de negócio** exigida por RN-07 (registro de
compensação, visibilidade, erro ao usuário). Como o fluxo toca mais de um
agregado, a solução é saga orquestrada. Coreografia é vetada acima de 2
participantes (ninguém responderia "onde este fluxo parou"); aqui são 2
participantes, dentro de orquestração simples.

## Consequências

Fica fácil: consistência sob falha (CA-09), trilha de auditoria completa,
estado de saga consultável (contrato: `202` + `Location` + `GET /sagas/{id}`),
replay/idempotência de passos.

Fica difícil: mais tabelas, orquestrador, versionamento de evento
(`event_version`), testes de compensação (100% dos passos). Custo da indireção
aceito porque consistência distribuída sem 2PC é requisito (RN-06, RN-07).

## Números

- Timeout por passo: 30 s; timeout global: 5 min.
- Retry: 3×, backoff exponencial + jitter.
- Estados de saga: `sealed interface` com 6 estados; transição inválida =
  exceção de domínio.
- `event_version`: todo evento de outbox carrega versão; consumidor rejeita
  versão desconhecida; compatibilidade retroativa por pelo menos 1 versão.

## Alternativas descartadas

- **Transação local única (evento + auditoria no mesmo `@Transactional`).**
  Rejeitada: não expressa compensação de negócio (RN-07); falha de auditoria
  deixaria decisão implícita; sem estado de saga consultável. (Item 9 do
  ADR-0001.)
- **Coreografia de saga.** Rejeitada: 2 participantes é o limite máximo;
  acima disso ninguém responde "onde parou". Os 2 participantes deste fluxo
  justificam orquestração simples.
- **Publicar evento fora da transação.** Proibido: viola atomicidade; exige
  outbox.

## Referências

- `docs/spec/regras-negocio.md` — RN-06, RN-07, CA-09; glossário (registro de
  compensação).
- `docs/spec/gherkin/saga-auditoria-qualidade.feature` — @CA-09.
- `docs/spec/erros.md` — E-05.
- `application/saga` — orquestrador (fase 6);
  `domain/port/out/SagaStepPort.java`, `domain/port/out/OutboxPort.java` —
  portas; `domain/model/result/SagaStatus.java`, `SagaStepOutcome.java` —
  tipos selados de domínio.
