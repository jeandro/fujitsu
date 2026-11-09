# Dicionário de dados — módulo events-api (PostgreSQL 17)

Classificação de dados pessoais/sensíveis per **RN-17 (LGPD)**:
`público` | `interno` | `confidencial` | `restrito`.

Dono do dado: `dba-postgres`. Regras de negócio que cada constraint protege são citadas como `RN-xx`. Nomes de tabela alinhados às entidades JPA (`tb_factory_events`, `tb_audit_logs`) e às migrations `V001`/`V002`.

## 1. tb_factory_events

Registro de eventos de fábrica (PRODUCTION, MAINTENANCE, QUALITY). Imutável após criação (RN-08, RN-22).

| Coluna | Tipo | Nulo | Default | Domínio / valores | Classificação (RN-17) | Regra protegida |
| --- | --- | --- | --- | --- | --- | --- |
| `id` | `bigint` | NOT NULL | `GENERATED ALWAYS AS IDENTITY` | PK interna | `interno` | PK da tabela |
| `event_id` | `varchar(100)` | NOT NULL | — | 1–100 chars | `interno` | RN-01 (obrigatório), RN-02 (único — `uk_events_event_id`) |
| `event_type` | `varchar(20)` | NOT NULL | — | `PRODUCTION` \| `MAINTENANCE` \| `QUALITY` | `público` | RN-01 (domínio), CHECK |
| `description` | `varchar(1000)` | NOT NULL | — | 1–1000 chars | `confidencial` | RN-01 (tamanho), RN-17 (LGPD), RN-20 (mascarado em logs) |
| `"timestamp"` | `timestamptz` | NOT NULL | — | Instant ISO 8601 UTC | `interno` | RN-01 (obrigatório, válido), RN-09 (ordenação) |

Índices:
- `uk_events_event_id` UNIQUE(`event_id`) — integridade de negócio (RN-02).
- `idx_factory_events_timestamp_desc_id_desc`(`"timestamp"` DESC, `id` DESC) — ordenação da consulta paginada e paginação keyset (RN-09).

## 2. tb_audit_logs

Log de auditoria automático para eventos QUALITY, na mesma transação do evento (RN-06). Imutável.

| Coluna | Tipo | Nulo | Default | Domínio / valores | Classificação (RN-17) | Regra protegida |
| --- | --- | --- | --- | --- | --- | --- |
| `id` | `bigint` | NOT NULL | `GENERATED ALWAYS AS IDENTITY` | PK interna | `interno` | PK da tabela |
| `event_id` | `varchar(100)` | NOT NULL | — | FK → `tb_factory_events(event_id)` | `interno` | RN-06 (vínculo), FK `fk_audit_logs_factory_events_event_id` ON DELETE RESTRICT |
| `log_message` | `varchar(2000)` | NOT NULL | — | 1–2000 chars | `confidencial` | Mensagem descritiva de auditoria (pode conter dado pessoal do evento QUALITY) |
| `created_at` | `timestamptz` | NOT NULL | — | — | `interno` | Instant de criação do log |

Índices:
- `idx_audit_logs_event_id`(`event_id`) — índice da FK (obrigatório) + consulta de auditoria por evento.

## 3. idempotency_key

Dedupe de comando repetido (ADR-0003). Janela de retenção 24 h (`expires_at`). Nunca em memória de instância.

| Coluna | Tipo | Nulo | Default | Domínio / valores | Classificação (RN-17) | Regra protegida |
| --- | --- | --- | --- | --- | --- | --- |
| `id` | `bigint` | NOT NULL | `GENERATED ALWAYS AS IDENTITY` | PK interna | `interno` | PK da tabela |
| `scope` | `varchar(100)` | NOT NULL | — | ex.: `events-api`, `POST /api/events` | `interno` | Parte da UNIQUE(scope, key) |
| `key` | `varchar(255)` | NOT NULL | — | UUID v4 do cliente (Idempotency-Key) | `interno` | Parte da UNIQUE(scope, key) |
| `request_hash` | `varchar(64)` | NOT NULL | — | hash do corpo da requisição | `interno` | Detecta reuso de chave com payload diferente (RN-04) |
| `response_status` | `smallint` | NOT NULL | — | 100–599 (CHECK) | `interno` | Status armazenado para replay (RN-03) |
| `response_body` | `text` | NOT NULL | — | corpo da primeira resposta | `interno` | Replay idêntico (RN-03) |
| `expires_at` | `timestamptz` | NOT NULL | — | `now() + 24 h` | `interno` | Fim da janela de retenção; alvo do job de expurgo |
| `created_at` | `timestamptz` | NOT NULL | `now()` | — | `interno` | Metadado de criação |

Índices:
- `pk_idempotency_key` PK(`id`) + `uk_idempotency_key_scope_key` UNIQUE(`scope`, `key`) — mecanismo de idempotência (único).
- `idx_idempotency_key_expires_at`(`expires_at`) — expurgo de expirados (job em lote).

## 4. saga_instance

Estado persistido de saga orquestrada (ADR-0004). Nunca variável de memória.

| Coluna | Tipo | Nulo | Default | Domínio / valores | Classificação (RN-17) | Regra protegida |
| --- | --- | --- | --- | --- | --- | --- |
| `id` | `bigint` | NOT NULL | `GENERATED ALWAYS AS IDENTITY` | PK interna | `interno` | PK da tabela |
| `saga_type` | `varchar(100)` | NOT NULL | — | ex.: `register-event-quality-audit` | `interno` | Tipo de saga |
| `business_key` | `varchar(255)` | NOT NULL | — | chave de negócio de correlação | `interno` | Ordenação por chave de correlação (não timing) |
| `state` | `varchar(40)` | NOT NULL | — | `STARTED\|IN_PROGRESS\|SUCCEEDED\|FAILED\|FAILED_COMPENSATED\|COMPENSATED` | `interno` | CHECK de estados válidos |
| `payload` | `jsonb` | NOT NULL | — | entrada da saga | `confidencial` | Pode conter `description` do evento (dado pessoal) |
| `attempts` | `integer` | NOT NULL | `0` | ≥ 0 | `interno` | Contador de retry (limite 3×, backoff + jitter) |
| `error` | `text` | NULL | — | motivo da falha, quando aplicável | `interno` | — |
| `created_at` | `timestamptz` | NOT NULL | `now()` | — | `interno` | — |
| `updated_at` | `timestamptz` | NOT NULL | `now()` | — | `interno` | Última atualização do estado |

Índices:
- `uq_saga_instance_active` UNIQUE(`saga_type`, `business_key`) WHERE estado ativo — evita sagas duplicadas para mesma correlação.
- `idx_saga_instance_business_key`(`business_key`) — consulta de saga por chave de negócio.

## 5. saga_step

Passos da saga com ação e compensação por passo (auditoria de compensação, não log).

| Coluna | Tipo | Nulo | Default | Domínio / valores | Classificação (RN-17) | Regra protegida |
| --- | --- | --- | --- | --- | --- | --- |
| `id` | `bigint` | NOT NULL | `GENERATED ALWAYS AS IDENTITY` | PK interna | `interno` | PK da tabela |
| `saga_id` | `bigint` | NOT NULL | — | FK → `saga_instance(id)` ON DELETE CASCADE | `interno` | FK `fk_saga_step_saga_instance` |
| `seq` | `smallint` | NOT NULL | — | ordem de execução do passo | `interno` | `ux_saga_step_seq` (implícita via índice composto) |
| `name` | `varchar(100)` | NOT NULL | — | nome do passo | `interno` | — |
| `state` | `varchar(40)` | NOT NULL | — | estados do passo (CHECK) | `interno` | — |
| `compensation_state` | `varchar(40)` | NULL | — | `PENDING\|IN_PROGRESS\|DONE\|FAILED` (auditoria de compensação) | `interno` | RN-07 |
| `updated_at` | `timestamptz` | NOT NULL | `now()` | — | `interno` | Última atualização do passo |

Índices:
- `idx_saga_step_saga_id_seq`(`saga_id`, `seq`) — índice da FK (obrigatório) + listagem de passos de uma saga ordenados por seq.

## 6. outbox_event

Transactional outbox (at-least-once), gravado na mesma transação do agregado. Publicar fora da transação é proibido.

| Coluna | Tipo | Nulo | Default | Domínio / valores | Classificação (RN-17) | Regra protegida |
| --- | --- | --- | --- | --- | --- | --- |
| `id` | `bigint` | NOT NULL | `GENERATED ALWAYS AS IDENTITY` | PK interna | `interno` | PK da tabela |
| `aggregate_type` | `varchar(100)` | NOT NULL | — | tipo do agregado de origem | `interno` | — |
| `aggregate_id` | `varchar(255)` | NOT NULL | — | identificador do agregado de origem | `interno` | — |
| `event_type` | `varchar(100)` | NOT NULL | — | tipo do evento de domínio | `interno` | — |
| `event_version` | `varchar(20)` | NOT NULL | — | versão do evento (1–20 chars) | `interno` | Consumidor rejeita versão desconhecida; compatibilidade ≥ 1 versão |
| `payload` | `jsonb` | NOT NULL | — | payload do evento | `confidencial` | Pode conter `description` do evento (dado pessoal) |
| `occurred_at` | `timestamptz` | NOT NULL | `now()` | — | `interno` | Ordenação do relay |
| `published_at` | `timestamptz` | NULL | — | NULL enquanto não publicado | `interno` | Marca de publicação do relay |

Índices:
- `idx_outbox_event_unpublished_occurred_at`(`occurred_at`) WHERE `published_at` IS NULL — índice parcial do relay (FOR UPDATE SKIP LOCKED).
- `idx_outbox_event_aggregate`(`aggregate_type`, `aggregate_id`, `occurred_at`) — rastreabilidade de eventos de um agregado.

## 7. processed_event

Dedupe de entrega do consumidor (at-least-once). Retenção igual à janela de reentrega do broker (nunca infinita).

| Coluna | Tipo | Nulo | Default | Domínio / valores | Classificação (RN-17) | Regra protegida |
| --- | --- | --- | --- | --- | --- | --- |
| `consumer` | `varchar(100)` | NOT NULL | — | identificador do consumidor | `interno` | Parte da PK composta |
| `event_id` | `varchar(255)` | NOT NULL | — | identificador do evento processado | `interno` | Parte da PK composta |
| `processed_at` | `timestamptz` | NOT NULL | `now()` | — | `interno` | — |

Índices:
- `pk_processed_event` PK(`consumer`, `event_id`) — provê índice para dedupe.

## 8. purge_audit_log

Auditoria append-only de execuções de expurgo/anônimo (RN-18). Nunca se altera nem exclui linhas.

| Coluna | Tipo | Nulo | Default | Domínio / valores | Classificação (RN-17) | Regra protegida |
| --- | --- | --- | --- | --- | --- | --- |
| `id` | `bigint` | NOT NULL | `GENERATED ALWAYS AS IDENTITY` | PK interna | `interno` | PK da tabela |
| `executed_by` | `text` | NOT NULL | — | responsável pela execução | `interno` | — |
| `executed_at` | `timestamptz` | NOT NULL | `now()` | — | `interno` | — |
| `rule_id` | `text` | NOT NULL | — | ex.: `RN-18` | `interno` | — |
| `table_name` | `text` | NOT NULL | — | tabela alvo | `interno` | — |
| `rows_affected` | `integer` | NOT NULL | — | linhas afetadas no lote | `interno` | — |
| `description` | `text` | NOT NULL | — | descrição da operação | `interno` | — |
| `source_host` | `text` | NOT NULL | — | host de origem | `interno` | — |

## 9. Regras de segurança de dados

- Role de aplicação `events_app`: apenas `SELECT/INSERT/UPDATE/DELETE` nas tabelas acima; sem DDL, sem `SUPERUSER` (V003).
- RLS: não aplicável (módulo single-tenant, sem `tenant_id`). Revisar se multi-tenant for adotado.
- Backup criptografado e restore testado trimestralmente.
- Coluna de dado pessoal (`description`) classificada `confidencial` no dicionário; retenção 24 meses (RN-18) com expurgo auditável.
- `event_id` mantido após anonimização para integridade da trilha de auditoria (RN-18).
- `log_message` de `tb_audit_logs` também pode conter dado pessoal (evento QUALITY): classificado `interno` no dicionário; avaliar classificação `confidencial` se o conteúdo do log puder conter dado pessoal do titular.

## 10. Scripts relacionados

- Migrations: `backend/src/main/resources/db/migration/V001__schema_inicial.sql`, `V002__idempotencia_saga_outbox.sql`, `V003__seguranca_roles_grants_auditoria_purge.sql`.
- Undo (rollback): `U001__schema_inicial.sql`, `U002__idempotencia_saga_outbox.sql`, `U003__seguranca_roles_grants_auditoria_purge.sql`.
- Expurgo/anônimo: `backend/scripts/sql/purge_description_rn18_24meses.sql`.
- Evidências de plano: `docs/db/plans/` (EXPLAIN ANALYZE, BUFFERS antes/depois de cada índice).
