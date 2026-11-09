# Dicionário de dados — events-api (PostgreSQL 17 + Flyway)

Versão: 1.0 · Fase 5 (apenas migrations e dicionário)
Banco: PostgreSQL 17 · Schema: público · Ferramenta de migração: Flyway (versionadas, idempotentes, reversíveis)

## 1. Padrões de tipo aplicados (DBA)

| Padrão | Aplicação neste projeto |
|--------|-------------------------|
| Chave interna | `bigint GENERATED ALWAYS AS IDENTITY` (substituta, nunca exposta) |
| Chave exposta em URL | `uuid` — **não aplicado ainda**: nenhuma tabela expõe chave em URL nesta fase; `event_id` é chave de negócio (`varchar`), não identificador de URL |
| Instante | `timestamptz` (sempre; UTC) — `timestamp`, `created_at`, `occurred_at`, `expires_at`, `created_at`, `updated_at`, `processed_at` |
| Dinheiro | `numeric(15,2)` — não se aplica (sem campo financeiro nesta fase) |
| Dado sem esquema | `jsonb` — usado em `saga_instance.payload` e `outbox_event.payload` |
| Integridade | `NOT NULL`, `CHECK`, `UNIQUE`, `FOREIGN KEY` no banco (não no app Java) |
| Toda FK | indexada |
| Multi-tenant | **não aplicado** (projeto năo é multi-tenant; sem `tenant_id`). `scope` serve como namespace da idempotência (ADR-0003). Se adicionado futuramente: `tenant_id` em toda tabela + RLS |

### Decisão uuid vs bigint identity
A PK interna de todas as tabelas é `bigint GENERATED ALWAYS AS IDENTITY`. `event_id` (tabela de eventos) é mantida como `varchar(100)` UNIQUE porque é a **chave de negócio** (ex.: `EVT-1001`), não um identificador sequencial exposto em URL. Não há endpoint `GET /events/{eventId}` nesta fase; caso seja adicionado e exija UUID imprevisível, avaliar migração para `uuid` como chave exposta (adicionar coluna `uuid`, nunca substituir o `bigint` interno sem ADR).

### Decisão timestamptz
Todos os instantes usam `timestamptz`. Hibernate mapeia `java.time.Instant` para `timestamptz` no PostgreSQL (com `hibernate.jdbc.time_zone=UTC`). Nunca `timestamp` sem fuso.

## 2. Tabelas de negócio (V001)

### tb_factory_events

| Coluna | Tipo | Nulo | Default | Domínio / restrição | Classif. LGPD | Retenção | Regra protegida |
|--------|------|------|---------|---------------------|---------------|----------|-----------------|
| id | bigint | NOT NULL | GENERATED ALWAYS AS IDENTITY | PK | interno | permanente (năo excluir) | — |
| event_id | varchar(100) | NOT NULL | — | UNIQUE; length 1..100 | interno | permanente | RN-02 (único) |
| event_type | varchar(20) | NOT NULL | — | CHECK IN (PRODUCTION, MAINTENANCE, QUALITY) | público | permanente | RN-01 |
| description | varchar(1000) | NOT NULL | — | length 1..1000 | **confidencial** (pode conter dado pessoal) | 24 meses; anonimizar description após 24 meses (eventId mantido) | RN-01, RN-17, RN-18 |
| timestamp | timestamptz | NOT NULL | — | instante UTC | interno | permanente | RN-01 |

Índices:
- `pk_factory_events` (id) — PK.
- `uk_events_event_id` (event_id) — UNIQUE (RN-02; impedimento de duplicidade).
- `idx_factory_events_timestamp_desc_id_desc` ("timestamp" DESC, id DESC) — listagem paginada ordenada (RN-09, RN-11). Consulta real: `ORDER BY "timestamp" DESC, id DESC LIMIT/OFFSET`. **EXPLAIN (ANALYZE, BUFFERS) antes/depois: a medir na fase 6 (docs/db/plans/).**

### tb_audit_logs

| Coluna | Tipo | Nulo | Default | Domínio / restrição | Classif. LGPD | Retenção | Regra protegida |
|--------|------|------|---------|---------------------|---------------|----------|-----------------|
| id | bigint | NOT NULL | GENERATED ALWAYS AS IDENTITY | PK | interno | permanente | — |
| event_id | varchar(100) | NOT NULL | — | FK → tb_factory_events(event_id), ON DELETE RESTRICT | interno | permanente | RN-06 |
| log_message | varchar(2000) | NOT NULL | — | length 1..2000 | **confidencial** | 24 meses (segue o evento) | RN-06, RN-17 |
| created_at | timestamptz | NOT NULL | — | instante UTC | interno | permanente | RN-06 |

Índices:
- `pk_audit_logs` (id) — PK.
- `fk_audit_logs_factory_events_event_id` (FK) — integridade: auditoria só existe se o evento existe; eventos năo são excluídos (RN-22), logo RESTRICT năo bloqueia. **Nota: depende de PA-05 (se compensaçăo excluir fisicamente o evento, revisar FK antes de deletar).**
- `idx_audit_logs_event_id` (event_id) — busca de auditoria por evento (RN-06) + suporte ŕ FK. **EXPLAIN: a medir na fase 6.**

Comentário: eventos QUALITY geram auditoria; PRODUCTION/MAINTENANCE năo geram (RN-06). Eventos imutáveis, năo há PUT/PATCH (RN-08), năo há exclusăo (RN-22).

## 3. Tabelas de consistência — idempotência, saga, outbox (V002)

### idempotency_key

| Coluna | Tipo | Nulo | Default | Domínio / restrição | Classif. LGPD | Retenção | Regra protegida |
|--------|------|------|---------|---------------------|---------------|----------|-----------------|
| id | bigint | NOT NULL | IDENTITY | PK | interno | 24 h (expires_at) | — |
| scope | varchar(100) | NOT NULL | — | namespace (ex.: "events-api") | interno | 24 h | RN-03..05 |
| key | varchar(255) | NOT NULL | — | UNIQUE (scope, key) | interno | 24 h | RN-03..05 |
| request_hash | varchar(64) | NOT NULL | — | hash do corpo da requisiçăo | interno | 24 h | RN-04 |
| response_status | smallint | NOT NULL | — | CHECK 100..599 | interno | 24 h | RN-03 |
| response_body | text | NOT NULL | — | corpo da primeira resposta | interno | 24 h | RN-03 |
| expires_at | timestamptz | NOT NULL | — | agora + 24 h | interno | 24 h | RN-03 |
| created_at | timestamptz | NOT NULL | now() | instante UTC | interno | 24 h | — |

Índices:
- `pk_idempotency_key` (id).
- `uk_idempotency_key_scope_key` (scope, key) UNIQUE — mecanismo de dedupe; claim atômico (nunca "existe?" antes do insert). **EXPLAIN: a medir na fase 6.**
- `idx_idempotency_key_expires_at` (expires_at) — job de expurgo em lote (`WHERE expires_at < $1 LIMIT :lote`). Tabela năo pode crescer infinitamente. **EXPLAIN: a medir na fase 6.**

Retenção: janela de 24 h (ADR-0003; PA-02 em aberto, confirmar com negocio-analista). Expurgo automático em lote por `expires_at`.

### saga_instance

| Coluna | Tipo | Nulo | Default | Domínio / restrição | Classif. LGPD | Retenção | Regra protegida |
|--------|------|------|---------|---------------------|---------------|----------|-----------------|
| id | bigint | NOT NULL | IDENTITY | PK | interno | até concluída/compensada | RN-06, RN-07 |
| saga_type | varchar(100) | NOT NULL | — | tipo da saga | interno | — | — |
| business_key | varchar(255) | NOT NULL | — | chave de correlaçăo (ex.: eventId) | interno | — | — |
| state | varchar(40) | NOT NULL | — | CHECK STARTED/IN_PROGRESS/SUCCEEDED/FAILED/FAILED_COMPENSATED/COMPENSATED | interno | — | RN-07 |
| payload | jsonb | NOT NULL | — | entrada da saga | confidencial (pode conter description) | — | RN-17 |
| attempts | integer | NOT NULL | 0 | >= 0 | interno | — | — |
| error | text | NULL | — | motivo da falha | interno | — | — |
| created_at | timestamptz | NOT NULL | now() | instante UTC | interno | — | — |
| updated_at | timestamptz | NOT NULL | now() | instante UTC | interno | — | — |

Índices:
- `pk_saga_instance` (id).
- `uq_saga_instance_active` UNIQUE INDEX (saga_type, business_key) WHERE state IN ('STARTED','IN_PROGRESS') — uma saga ativa por negócio. **EXPLAIN: a medir na fase 6.**
- `idx_saga_instance_business_key` (business_key) — consulta de estado da saga por negócio. **EXPLAIN: a medir na fase 6.**

### saga_step

| Coluna | Tipo | Nulo | Default | Domínio / restrição | Classif. LGPD | Retenção | Regra protegida |
|--------|------|------|---------|---------------------|---------------|----------|-----------------|
| id | bigint | NOT NULL | IDENTITY | PK | interno | — | — |
| saga_id | bigint | NOT NULL | — | FK → saga_instance(id), ON DELETE CASCADE | interno | — | RN-07 |
| seq | smallint | NOT NULL | — | ordem do passo | interno | — | — |
| name | varchar(100) | NOT NULL | — | nome do passo | interno | — | — |
| state | varchar(40) | NOT NULL | — | CHECK estados do passo | interno | — | RN-07 |
| compensation_state | varchar(40) | NULL | — | CHECK PENDING/IN_PROGRESS/DONE/FAILED | interno | — | RN-07 (auditoria de compensaçăo) |
| updated_at | timestamptz | NOT NULL | now() | instante UTC | interno | — | — |

Índices:
- `pk_saga_step` (id).
- `fk_saga_step_saga_instance` (FK saga_id) — integridade.
- `idx_saga_step_saga_id_seq` (saga_id, seq) — listar passos de uma saga ordenados. **EXPLAIN: a medir na fase 6.**

### outbox_event

| Coluna | Tipo | Nulo | Default | Domínio / restrição | Classif. LGPD | Retenção | Regra protegida |
|--------|------|------|---------|---------------------|---------------|----------|-----------------|
| id | bigint | NOT NULL | IDENTITY | PK | interno | após publicaçăo (arquivar) | — |
| aggregate_type | varchar(100) | NOT NULL | — | tipo do agregado | interno | — | — |
| aggregate_id | varchar(255) | NOT NULL | — | id do agregado | interno | — | — |
| event_type | varchar(100) | NOT NULL | — | tipo do evento de domínio | interno | — | — |
| event_version | varchar(20) | NOT NULL | — | versăo do evento | interno | — | ADR-0004 |
| payload | jsonb | NOT NULL | — | payload do evento | confidencial (pode conter description) | — | RN-17 |
| occurred_at | timestamptz | NOT NULL | now() | instante UTC | interno | — | — |
| published_at | timestamptz | NULL | — | instante de publicaçăo pelo relay | interno | — | — |

Índices:
- `pk_outbox_event` (id).
- `idx_outbox_event_unpublished_occurred_at` (occurred_at) WHERE published_at IS NULL — **índice parcial para o relay** (`WHERE published_at IS NULL ORDER BY occurred_at FOR UPDATE SKIP LOCKED`). Justificado por ADR-0004 (at-least-once). **EXPLAIN (ANALYZE, BUFFERS) antes/depois: a medir na fase 6.**
- `idx_outbox_event_aggregate` (aggregate_type, aggregate_id, occurred_at) — rastreabilidade de eventos de um agregado. **EXPLAIN: a medir na fase 6.**

### processed_event

| Coluna | Tipo | Nulo | Default | Domínio / restrição | Classif. LGPD | Retenção | Regra protegida |
|--------|------|------|---------|---------------------|---------------|----------|-----------------|
| consumer | varchar(100) | NOT NULL | — | PK composta com event_id | interno | janela de reentrega do broker | — |
| event_id | varchar(255) | NOT NULL | — | PK composta com consumer | interno | janela de reentrega do broker | — |
| processed_at | timestamptz | NOT NULL | now() | instante UTC | interno | — | — |

Índices:
- `pk_processed_event` (consumer, event_id) — PK composta; já provê índice para dedupe (`WHERE consumer = $1 AND event_id = $2`). **EXPLAIN: a medir na fase 6.**

Retenção: igual à janela de reentrega do broker (nunca infinita); expurgo em lote.

## 4. Retenção e expurgo (LGPD — RN-18)

- `description` de `tb_factory_events` (e `log_message` de auditoria vinculada): anonimização após 24 meses; `eventId` mantido para integridade da trilha (RN-18).
- `idempotency_key`: expurgo por `expires_at` (24 h) em lote; tabela que só cresce é dívida operacional (acima de 10 M linhas, particionar por `created_at`).
- `processed_event`: retenção = janela de reentrega do broker; nunca infinita.
- `outbox_event`: arquivar após publicação (`published_at` preenchido).
- Nenhum `DELETE` gigante; expurgo em lotes com `lock_timeout`.

## 5. Índices — justificativa e status de medição

| Índice | Tabela | Consulta real que justifica | Status EXPLAIN (ANALYZE, BUFFERS) |
|--------|--------|----------------------------|-----------------------------------|
| uk_events_event_id | tb_factory_events | `WHERE event_id = $1` (impedimento de duplicidade, RN-02) | a medir fase 6 |
| idx_factory_events_timestamp_desc_id_desc | tb_factory_events | `ORDER BY "timestamp" DESC, id DESC LIMIT/OFFSET` (listagem paginada, RN-09/RN-11) | a medir fase 6 |
| idx_audit_logs_event_id | tb_audit_logs | `WHERE event_id = $1 ORDER BY created_at DESC` (RN-06) | a medir fase 6 |
| uk_idempotency_key_scope_key | idempotency_key | `INSERT ... (scope, key)` claim atômico (RN-03..05) | a medir fase 6 |
| idx_idempotency_key_expires_at | idempotency_key | `DELETE WHERE expires_at < $1 LIMIT :lote` (expurgo) | a medir fase 6 |
| uq_saga_instance_active | saga_instance | lookup saga ativa por (saga_type, business_key) | a medir fase 6 |
| idx_saga_instance_business_key | saga_instance | consulta de estado por business_key | a medir fase 6 |
| idx_saga_step_saga_id_seq | saga_step | passos de uma saga ordenados por seq | a medir fase 6 |
| idx_outbox_event_unpublished_occurred_at | outbox_event | relay `WHERE published_at IS NULL ORDER BY occurred_at FOR UPDATE SKIP LOCKED` | a medir fase 6 |
| idx_outbox_event_aggregate | outbox_event | eventos de um agregado (rastreabilidade) | a medir fase 6 |
| pk_processed_event | processed_event | dedupe `WHERE consumer=$1 AND event_id=$2` | a medir fase 6 |

**Baseline:** ainda năo existe. Evidência `EXPLAIN (ANALYZE, BUFFERS)` antes/depois deve ser arquivada em `docs/db/plans/` na fase 6, após carga representativa e confirmação de consulta quente via `pg_stat_statements`.

## 6. O que depende da implementação dos casos de uso (fase 6)

- Definir `scope` exato usado no filtro de idempotência (ex.: `"events-api"` ou endpoint) e mapear para a porta `IdempotencyStore`.
- Confirmar janela de retenção da `Idempotency-Key` (PA-02; adotada 24 h por ADR-0003, aguardando confirmação do humano).
- Confirmar se a compensação de falha de auditoria (RN-07) exclui fisicamente o evento ou apenas o marca/cancela (PA-05); afeta a FK `tb_audit_logs.event_id` (ON DELETE RESTRICT).
- Implementar o relay do outbox (consulta parcial `published_at IS NULL`, `FOR UPDATE SKIP LOCKED`, marca `published_at`) e o consumidor dedupe (`processed_event`).
- Implementar expurgo em lote de `idempotency_key` (por `expires_at`) e de `processed_event` (janela do broker).
- Configurar `spring.jpa.hibernate.ddl-auto=none` (ou `validate`) e habilitar Flyway; substituir H2 por PostgreSQL 17 no `application.properties`/Docker Compose.
- Nome das tabelas: `tb_audit_logs` (plural, conforme entidade JPA e README); o contexto da fase usa "tb_audit_log" como abreviação da mesma tabela.
- `event_version`: definir esquema de versionamento (ex.: "1") e política de compatibilidade retroativa (ADR-0004: pelo menos 1 versão).

## 7. Segurança de dados

- Role da aplicação: apenas `SELECT/INSERT/UPDATE/DELETE` nas tabelas necessárias; sem DDL, sem SUPERUSER.
- RLS: năo aplicável (năo é multi-tenant).
- Dado pessoal: `description` e `log_message` classificados confidenciais (RN-17); mascarar em logs e năo devolver a papel sem permissão (RN-20); `no-store` em HTTP para dado pessoal.
- Auditoria de ação sensível: tabela append-only com quem/quando/o quê/de onde — a implementar na fase 6 (não escopo desta migration).
- Backup criptografado e restore testado trimestralmente — fora de escopo do código.
