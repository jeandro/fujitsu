# ADR-0003: Idempotência por header Idempotency-Key

Status: aceito
Data: 2026-11-05
Substitui: item 8 do ADR-0001 (via ADR-0002)

## Contexto

`POST /api/events` é escrita repetível (duplo clique, retry por timeout, nova
tentativa do cliente). A spec exige (RN-03, RN-04, RN-05, CA-05..CA-08):

- `Idempotency-Key` obrigatório no header da requisição.
- Mesma chave + mesmo corpo dentro da janela de retenção → resposta **idêntica**
  ao primeiro processamento (mesmo status e corpo), um efeito.
- Mesma chave + corpo diferente → `409 idempotency-key-reused` (E-03).
- Chave em execução concorrente → `409 idempotency-in-progress` (E-04).
- Teste de concorrência: duas requisições simultâneas com a mesma chave → um
  efeito, duas respostas iguais (CA-05/CA-06/CA-07/CA-08).

ADR-0001 rejeitou a tabela `idempotency_key` e o header, usando `eventId` como
chave natural com constraint única. Isso não atende a spec.

## Decisão

1. **Tabela `idempotency_key`**:
   `(scope, key, request_hash, response_status, response_body, expires_at)`,
   `UNIQUE(scope, key)`, janela de retenção de **24 h**
   (`expires_at = agora + 24 h`), expurgo de registros expirados.
   Nunca em memória de instância.

2. **Porta `IdempotencyStore`** em `domain/port/out`:
   - `lookup(scope, key)` → `IdempotencyOutcome` (sealed:
     `Processed` | `InProgress` | `None`);
   - `claim(scope, key, expiresAt)` → `boolean` (true apenas para quem deve
     processar; perdedor recebe `409 in-progress`);
   - `storeResult(scope, key, requestHash, responseStatus, responseBody,
     expiresAt)`;
   - `purgeExpired(before)`.

3. **Interceptador/filtro** em `adapter.in.web` (antes do controller): lê
   `Idempotency-Key`, calcula `request_hash` do payload, consulta
   `IdempotencyStore`, devolve resposta cacheada ou `409` conforme o resultado.
   O domínio **não conhece** `Idempotency-Key`, `HttpServletRequest` nem hash
   de payload; só expressa "esta operação é repetível" através da porta.

4. **Header `Idempotency-Key` obrigatório** no contrato `POST /api/events`
   (OpenAPI); ausência → 400.

## Por que não usar apenas eventId como chave natural?

`eventId` é a chave de **negócio** do evento: único por evento, permanente,
imutável (RN-02, RN-08). Identifica *o que* ocorreu.

`Idempotency-Key` é a chave **operacional** da requisição: identificador
gerado pelo cliente para esta tentativa de processamento, com janela de
retenção finita (24 h) e associado ao corpo da requisição.

Um cliente pode replayar com o mesmo `eventId` em contexto distinto (ex.:
reenvia o mesmo `eventId` sob uma nova chave depois de expirar a janela, ou
corrige um campo e reenvia com outra `Idempotency-Key`). Usar `eventId` como
única chave de idempotência:

- não distingue "reenvio idêntico seguro" de "reuso de chave com payload
  diferente" (RN-04 exige 409);
- não tem janela de retenção nem expurgo (retenção = vida útil do registro,
  rejeitada pelo ADR-0001 e pela spec);
- não armazena a resposta para devolver corpo idêntico no replay (RN-03).

Portanto a idempotência operacional precisa de tabela própria,
chave composta `(scope, key)` e armazenamento de `request_hash` + resposta.
`eventId` continua válido como unicidade de negócio (constraint
`uk_events_event_id`), mecanismo distinto da chave de idempotência.

## Consequências

Fica fácil: replay seguro, 409s padronizados (E-03, E-04), teste de
concorrência determinístico (CA-05/06/07/08), domínio desacoplado de HTTP.

Fica difícil: nova tabela + filtro + mapper de resposta. Custo da indireção
(uma camada entre controller e caso de uso) é aceito porque a idempotência é
requisito não negociável (RN-03..RN-05).

## Números

- Janela de retenção: 24 h (PA-02 ainda em aberto; adotado 24 h por exigência
  da tarefa da fase 3 — confirmar com `negocio-analista`).
- `request_hash`: hash do corpo da requisição para detecção de reuso de chave
  com payload diferente.
- Concorrência: `claim` atômico via `UNIQUE(scope, key)`; perdedor recebe
  `409 idempotency-in-progress`.

## Alternativas descartadas

- **Idempotência apenas por `eventId` (constraint única).** Rejeitada: não
  atende janela de retenção, `idempotency-key-reused`, nem devolve resposta
  idêntica; não distingue contexto distinto. (Item 8 do ADR-0001.)
- **Idempotência em memória de instância.** Rejeitada: não sobrevive a
  reinício, não escala para múltiplas instâncias, viola "nunca em memória de
  instância" do padrão de idempotência.
- **Chave derivada do corpo sem header.** Rejeitada: não há como o cliente
  controlar retry intencional; não há `409 in-progress` durante a execução.

## Referências

- `docs/spec/regras-negocio.md` — RN-02..RN-05, CA-05..CA-08, glossário
  (tabela `idempotency_key`).
- `docs/spec/erros.md` — E-03, E-04.
- `docs/adr/ADR-0004` — saga/outbox; `ADR-0005` — cache.
- `domain/port/out/IdempotencyStore.java` — definição da porta.
- `domain/model/result/IdempotencyOutcome.java` — resultado selado da porta.
