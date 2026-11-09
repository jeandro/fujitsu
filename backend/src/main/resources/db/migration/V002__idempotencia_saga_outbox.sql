-- =============================================================================
-- Flyway V002__idempotencia_saga_outbox.sql  (PostgreSQL 17)
-- =============================================================================
-- Motivo: cria tabelas de consistência exigidas por ADR-0003 (idempotência) e
--         ADR-0004 (saga orquestrada + transactional outbox): idempotency_key,
--         saga_instance, saga_step, outbox_event, processed_event.
-- Impacto estimado: tabelas novas vazias -> índices criados em tabela vazia,
--         sem lock de leitura significativo. Sem ALTER sobre tabela grande.
-- Rollback: veja U002__idempotencia_saga_outbox.sql (DROP em ordem de FK).
-- Janela recomendada: qualquer janela (tabelas vazias).
-- Multi-tenant: este projeto năo é multi-tenant (sem tenant no modelo de negócio);
--         usa-se (scope, key) como namespace da idempotência, conforme ADR-0003.
--         Se multi-tenant for adicionado posteriormente, incluir tenant_id + RLS.
-- Padrőes: unicidade como mecanismo de idempotência (nunca "existe?" antes do
--         insert); índice parcial em outbox_event (published_at IS NULL) para o
--         relay com FOR UPDATE SKIP LOCKED; toda FK indexada; processed_event com
--         PK composta e retençăo finita (janela de reentrega do broker).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Tabela de idempotência (ADR-0003, RN-03/RN-04/RN-05).
-- Chave operacional (scope, key) com janela de retençăo 24 h (expires_at).
-- Única por (scope, key) = mecanismo de dedupe; năo se consulta "existe?" antes
--         do insert (claim atômico via UNIQUE).
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS idempotency_key (
  id             bigint       NOT NULL GENERATED ALWAYS AS IDENTITY,
  scope          varchar(100) NOT NULL,
  key            varchar(255) NOT NULL,
  request_hash   varchar(64)  NOT NULL,
  response_status smallint    NOT NULL,
  response_body  text         NOT NULL,
  expires_at     timestamptz  NOT NULL,
  created_at     timestamptz  NOT NULL DEFAULT now(),

  CONSTRAINT pk_idempotency_key PRIMARY KEY (id),
  CONSTRAINT uk_idempotency_key_scope_key UNIQUE (scope, key),
  CONSTRAINT chk_idempotency_key_response_status
    CHECK (response_status BETWEEN 100 AND 599)
);

-- Índice para o job de expurgo de registros expirados (lote).
-- Consulta real: DELETE FROM idempotency_key WHERE expires_at < $1 ORDER BY expires_at LIMIT :lote
CREATE INDEX IF NOT EXISTS idx_idempotency_key_expires_at
  ON idempotency_key (expires_at);

COMMENT ON TABLE idempotency_key IS
  'Resultado cacheado de requisiçőes repetíveis por Idempotency-Key. Janela de retençăo 24 h (expires_at).';
COMMENT ON COLUMN idempotency_key.scope IS
  'Namespace da chave (ex.: "events-api", "POST /api/events").';
COMMENT ON COLUMN idempotency_key.key IS
  'Valor do header Idempotency-Key (UUID v4 gerado pelo cliente).';
COMMENT ON COLUMN idempotency_key.request_hash IS
  'Hash do corpo da requisiçăo para detectar reuso de chave com payload diferente (RN-04).';
COMMENT ON COLUMN idempotency_key.response_status IS
  'Status HTTP da primeira resposta (replay devolve o mesmo status e corpo).';
COMMENT ON COLUMN idempotency_key.response_body IS
  'Corpo da primeira resposta (replay devolve o mesmo corpo).';
COMMENT ON COLUMN idempotency_key.expires_at IS
  'Expiraçăo = agora + 24 h; alvo do job de expurgo em lote.';
COMMENT ON COLUMN idempotency_key.created_at IS
  'Instante de criaçăo do registro.';

-- -----------------------------------------------------------------------------
-- Tabela de instância de saga (ADR-0004, RN-06/RN-07).
-- Estado persistido da saga orquestrada (registro evento + auditoria QUALITY).
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS saga_instance (
  id          bigint       NOT NULL GENERATED ALWAYS AS IDENTITY,
  saga_type   varchar(100) NOT NULL,
  business_key varchar(255) NOT NULL,
  state       varchar(40)  NOT NULL,
  payload     jsonb        NOT NULL,
  attempts    integer      NOT NULL DEFAULT 0,
  error       text,
  created_at  timestamptz  NOT NULL DEFAULT now(),
  updated_at  timestamptz  NOT NULL DEFAULT now(),

  CONSTRAINT pk_saga_instance PRIMARY KEY (id),
  CONSTRAINT chk_saga_instance_state
    CHECK (state IN ('STARTED','IN_PROGRESS','SUCCEEDED','FAILED','FAILED_COMPENSATED','COMPENSATED'))
);

-- Unicidade parcial: apenas uma saga ATIVA por (saga_type, business_key).
-- Estados ativos: STARTED, IN_PROGRESS. Consulta real: lookup de saga ativa por
--         negócio (contrato de consulta de estado da saga).
CREATE UNIQUE INDEX IF NOT EXISTS uq_saga_instance_active
  ON saga_instance (saga_type, business_key)
  WHERE state IN ('STARTED', 'IN_PROGRESS');

-- Índice para consulta de saga por chave de negócio.
CREATE INDEX IF NOT EXISTS idx_saga_instance_business_key
  ON saga_instance (business_key);

COMMENT ON TABLE saga_instance IS
  'Estado persistido da saga orquestrada. Nunca variável de memória (ADR-0004).';
COMMENT ON COLUMN saga_instance.saga_type IS
  'Tipo de saga (ex.: "register-event-quality-audit").';
COMMENT ON COLUMN saga_instance.business_key IS
  'Chave de negócio de correlaçăo (ex.: eventId).';
COMMENT ON COLUMN saga_instance.state IS
  'Estado da saga (sealed: STARTED, IN_PROGRESS, SUCCEEDED, FAILED, FAILED_COMPENSATED, COMPENSATED).';
COMMENT ON COLUMN saga_instance.payload IS
  'Entrada da saga (JSONB).';
COMMENT ON COLUMN saga_instance.attempts IS
  'Número de tentativas do passo atual.';
COMMENT ON COLUMN saga_instance.error IS
  'Motivo da falha, quando aplicável.';
COMMENT ON COLUMN saga_instance.updated_at IS
  'Última atualizaçăo do estado.';

-- -----------------------------------------------------------------------------
-- Tabela de passos de saga (ADR-0004).
-- compensation_state guarda a auditoria de compensaçăo por passo (requisito,
-- năo log).
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS saga_step (
  id               bigint       NOT NULL GENERATED ALWAYS AS IDENTITY,
  saga_id          bigint       NOT NULL,
  seq              smallint     NOT NULL,
  name             varchar(100) NOT NULL,
  state            varchar(40)  NOT NULL,
  compensation_state varchar(40),
  updated_at       timestamptz  NOT NULL DEFAULT now(),

  CONSTRAINT pk_saga_step PRIMARY KEY (id),
  CONSTRAINT fk_saga_step_saga_instance
    FOREIGN KEY (saga_id) REFERENCES saga_instance (id)
    ON DELETE CASCADE,
  CONSTRAINT chk_saga_step_state
    CHECK (state IN ('STARTED','IN_PROGRESS','SUCCEEDED','FAILED','COMPENSATING','COMPENSATED')),
  CONSTRAINT chk_saga_step_compensation_state
    CHECK (compensation_state IS NULL OR compensation_state IN ('PENDING','IN_PROGRESS','DONE','FAILED'))
);

-- Toda FK tem índice. Consulta real: passos de uma saga ordenados por seq.
CREATE INDEX IF NOT EXISTS idx_saga_step_saga_id_seq
  ON saga_step (saga_id, seq);

COMMENT ON TABLE saga_step IS
  'Passos da saga orquestrada, com estado e estado de compensaçăo (auditável).';
COMMENT ON COLUMN saga_step.saga_id IS
  'FK para saga_instance(id).';
COMMENT ON COLUMN saga_step.seq IS
  'Ordem de execuçăo do passo.';
COMMENT ON COLUMN saga_step.name IS
  'Nome do passo (ex.: "registrar-evento", "gravar-auditoria-quality").';
COMMENT ON COLUMN saga_step.state IS
  'Estado do passo.';
COMMENT ON COLUMN saga_step.compensation_state IS
  'Estado da compensaçăo do passo (auditoria de compensaçăo, năo log).';
COMMENT ON COLUMN saga_step.updated_at IS
  'Última atualizaçăo do passo.';

-- -----------------------------------------------------------------------------
-- Tabela de outbox (transactional outbox, ADR-0004).
-- Evento publicado na mesma transaçăo do agregado; relay publica e marca
-- published_at. Publicar fora da transaçăo é proibido.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS outbox_event (
  id            bigint       NOT NULL GENERATED ALWAYS AS IDENTITY,
  aggregate_type varchar(100) NOT NULL,
  aggregate_id   varchar(255) NOT NULL,
  event_type     varchar(100) NOT NULL,
  event_version  varchar(20)  NOT NULL,
  payload        jsonb        NOT NULL,
  occurred_at    timestamptz  NOT NULL DEFAULT now(),
  published_at   timestamptz,

  CONSTRAINT pk_outbox_event PRIMARY KEY (id),
  CONSTRAINT chk_outbox_event_event_version
    CHECK (length(event_version) BETWEEN 1 AND 20)
);

-- ÍNDICE PARCIAL para o relay: eventos năo publicados, ordenados por occurred_at.
-- Consulta real: SELECT ... FROM outbox_event
--                WHERE published_at IS NULL ORDER BY occurred_at LIMIT :lote
--                FOR UPDATE SKIP LOCKED
CREATE INDEX IF NOT EXISTS idx_outbox_event_unpublished_occurred_at
  ON outbox_event (occurred_at)
  WHERE published_at IS NULL;

-- Índice para rastreabilidade de eventos de um agregado.
CREATE INDEX IF NOT EXISTS idx_outbox_event_aggregate
  ON outbox_event (aggregate_type, aggregate_id, occurred_at);

COMMENT ON TABLE outbox_event IS
  'Eventos de saída (transactional outbox). Publicados pelo relay na mesma transaçăo do agregado.';
COMMENT ON COLUMN outbox_event.aggregate_type IS
  'Tipo do agregado de origem (ex.: "FactoryEvent").';
COMMENT ON COLUMN outbox_event.aggregate_id IS
  'Identificador do agregado de origem.';
COMMENT ON COLUMN outbox_event.event_type IS
  'Tipo do evento de domínio.';
COMMENT ON COLUMN outbox_event.event_version IS
  'Versăo do evento; consumidor rejeita versăo desconhecida (ADR-0004).';
COMMENT ON COLUMN outbox_event.payload IS
  'Payload do evento (JSONB).';
COMMENT ON COLUMN outbox_event.occurred_at IS
  'Instante de ocorrência do evento.';
COMMENT ON COLUMN outbox_event.published_at IS
  'Instante de publicaçăo pelo relay; NULL enquanto năo publicado.';

-- -----------------------------------------------------------------------------
-- Tabela de eventos já processados (dedupe do consumidor, ADR-0004).
-- Retençăo igual ŕ janela de reentrega do broker (nunca infinita).
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS processed_event (
  consumer     varchar(100) NOT NULL,
  event_id     varchar(255) NOT NULL,
  processed_at timestamptz  NOT NULL DEFAULT now(),

  CONSTRAINT pk_processed_event PRIMARY KEY (consumer, event_id)
);

-- PK composta já provê índice para dedupe: WHERE consumer = $1 AND event_id = $2
COMMENT ON TABLE processed_event IS
  'Eventos já consumidos por consumidor (dedupe at-least-once). Retençăo = janela de reentrega do broker.';
COMMENT ON COLUMN processed_event.consumer IS
  'Identificador do consumidor.';
COMMENT ON COLUMN processed_event.event_id IS
  'Identificador do evento já processado.';
COMMENT ON COLUMN processed_event.processed_at IS
  'Instante de processamento.';

-- =============================================================================
-- NOTA DE PERFORMANCE / EXPLAIN
-- idx_outbox_event_unpublished_occurred_at: índice parcial justificado pelo
--         relay (at-least-once, FOR UPDATE SKIP LOCKED). Baseline a medir fase 6.
-- idx_idempotency_key_expires_at: justificado pelo job de expurgo (tabela năo
--         pode crescer infinitamente). Baseline a medir fase 6.
-- uq_saga_instance_active: justificado por "uma saga ativa por negócio".
-- idx_saga_step_saga_id_seq: justificado por listagem de passos de uma saga.
-- EXPLAIN (ANALYZE, BUFFERS) antes/depois a medir na fase 6 (docs/db/plans/).
-- =============================================================================
