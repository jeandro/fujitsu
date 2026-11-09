-- =============================================================================
-- Flyway V001__schema_inicial.sql  (PostgreSQL 17)
-- =============================================================================
-- Motivo: cria o schema inicial do módulo events-api: tb_factory_events e
--         tb_audit_logs. Substitui H2 + ddl-auto=update (ADR-0002).
-- Impacto estimado: tabelas iniciais vazias -> sem lock de leitura, criaçăo
--         instantânea. Sem ALTER sobre tabela grande.
-- Rollback: veja U001__schema_inicial.sql (DROP das tabelas, em ordem de FK).
--         Em produçăo, perda de dados é decisăo humana; validar antes de executar.
-- Janela recomendada: qualquer janela (tabelas ainda vazias).
-- Padrőes aplicados: PK interna bigint identity; instante timestamptz; chave de
--         negócio event_id (varchar, năo é UUID exposto em URL); constraints no DB;
--         toda FK indexada; índice composto na ordem da consulta real (RN-09).
-- =============================================================================

CREATE TABLE IF NOT EXISTS tb_factory_events (
  id               bigint       NOT NULL GENERATED ALWAYS AS IDENTITY,
  event_id         varchar(100) NOT NULL,
  event_type       varchar(20)  NOT NULL,
  description      varchar(1000)NOT NULL,
  "timestamp"      timestamptz  NOT NULL,

  CONSTRAINT pk_factory_events PRIMARY KEY (id),
  CONSTRAINT uk_events_event_id UNIQUE (event_id),
  CONSTRAINT chk_factory_events_event_type
    CHECK (event_type IN ('PRODUCTION', 'MAINTENANCE', 'QUALITY')),
  CONSTRAINT chk_factory_events_event_id_len
    CHECK (length(event_id) BETWEEN 1 AND 100),
  CONSTRAINT chk_factory_events_description_len
    CHECK (length(description) BETWEEN 1 AND 1000)
);

-- Índice composto para listagem paginada (RN-09): ORDER BY timestamp DESC, id DESC.
-- Consulta real: SELECT ... FROM tb_factory_events ORDER BY "timestamp" DESC, id DESC LIMIT :size OFFSET :offset
CREATE INDEX IF NOT EXISTS idx_factory_events_timestamp_desc_id_desc
  ON tb_factory_events ("timestamp" DESC, id DESC);

COMMENT ON TABLE tb_factory_events IS
  'Eventos de fábrica recebidos dos equipamentos industriais. Imutáveis após criaçăo (RN-08, RN-22).';
COMMENT ON COLUMN tb_factory_events.id IS
  'Chave interna substituta (bigint identity). Nunca exposta ao cliente.';
COMMENT ON COLUMN tb_factory_events.event_id IS
  'Chave de negócio do evento (ex.: EVT-1001). Única por evento (RN-02). LGPD: interno.';
COMMENT ON COLUMN tb_factory_events.event_type IS
  'Tipo do evento: PRODUCTION, MAINTENANCE, QUALITY. LGPD: público.';
COMMENT ON COLUMN tb_factory_events.description IS
  'Descriçăo do ocorrido, até 1000 caracteres. Pode conter dado pessoal. LGPD: confidencial.';
COMMENT ON COLUMN tb_factory_events."timestamp" IS
  'Instante da ocorrência do evento (UTC). LGPD: interno.';

CREATE TABLE IF NOT EXISTS tb_audit_logs (
  id          bigint      NOT NULL GENERATED ALWAYS AS IDENTITY,
  event_id    varchar(100)NOT NULL,
  log_message varchar(2000)NOT NULL,
  created_at  timestamptz NOT NULL,

  CONSTRAINT pk_audit_logs PRIMARY KEY (id),
  CONSTRAINT fk_audit_logs_factory_events_event_id
    FOREIGN KEY (event_id) REFERENCES tb_factory_events (event_id)
    ON DELETE RESTRICT,
  CONSTRAINT chk_audit_logs_log_message_len
    CHECK (length(log_message) BETWEEN 1 AND 2000)
);

-- Índice para busca de auditoria por event_id (RN-06) e suporte ŕ FK (delete de pai verifica filhos).
-- Consulta real: SELECT ... FROM tb_audit_logs WHERE event_id = $1 ORDER BY created_at DESC
CREATE INDEX IF NOT EXISTS idx_audit_logs_event_id
  ON tb_audit_logs (event_id);

COMMENT ON TABLE tb_audit_logs IS
  'Log de auditoria automático para eventos QUALITY (RN-06). Imutável.';
COMMENT ON COLUMN tb_audit_logs.id IS
  'Chave interna substituta (bigint identity).';
COMMENT ON COLUMN tb_audit_logs.event_id IS
  'Referência ao event_id do evento gerador. FK para tb_factory_events(event_id). LGPD: interno.';
COMMENT ON COLUMN tb_audit_logs.log_message IS
  'Mensagem descritiva de auditoria, até 2000 caracteres. LGPD: confidencial.';
COMMENT ON COLUMN tb_audit_logs.created_at IS
  'Instante de criaçăo do log (UTC). LGPD: interno.';

-- =============================================================================
-- NOTA DE PERFORMANCE / EXPLAIN
-- idx_factory_events_timestamp_desc_id_desc: justificado por RN-09/RN-11 (listagem
-- paginada ordenada). Baseline ainda năo existe; EXPLAIN (ANALYZE, BUFFERS)
-- antes/depois a medir na fase 6 (docs/db/plans/).
-- idx_audit_logs_event_id: justificado por RN-06 (auditoria vinculada ao eventId).
-- =============================================================================
