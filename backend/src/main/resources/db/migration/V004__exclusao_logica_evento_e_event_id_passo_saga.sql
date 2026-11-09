-- =============================================================================
-- Flyway V004__exclusao_logica_evento_e_event_id_passo_saga.sql  (PostgreSQL 17)
-- =============================================================================
-- Motivo: suporta exclusão lógica de evento na compensação de saga (PA-05, RN-07)
--         e armazena o eventId do passo na tabela de passos de saga para que a
--         compensação tenha acesso ao eventId sem dependir do payload.
--         status = 'ACTIVE' (visível na consulta) ou 'CANCELLED' (compensado,
--         permanece para auditoria, não aparece na consulta normal).
-- Rollback: U004__exclusao_logica_evento_e_event_id_passo_saga.sql.
-- =============================================================================

ALTER TABLE tb_factory_events
  ADD COLUMN status varchar(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE tb_factory_events
  ADD CONSTRAINT chk_factory_events_status
    CHECK (status IN ('ACTIVE', 'CANCELLED'));

-- Índice parcial para consulta paginada apenas de eventos ativos (RN-09).
CREATE INDEX IF NOT EXISTS idx_factory_events_active_timestamp_desc_id_desc
  ON tb_factory_events ("timestamp" DESC, id DESC)
  WHERE status = 'ACTIVE';

COMMENT ON COLUMN tb_factory_events.status IS
  'Status do evento: ACTIVE (visível) ou CANCELLED (compensado por saga, invisível na consulta, mantido para auditoria). LGPD: interno.';

ALTER TABLE saga_step
  ADD COLUMN event_id varchar(100);

COMMENT ON COLUMN saga_step.event_id IS
  'eventId do evento registrado no passo (preenchido no passo "registrar-evento"; usado pela compensação PA-05).';

-- =============================================================================
-- NOTA DE PERFORMANCE / EXPLAIN
-- idx_factory_events_active_timestamp_desc_id_desc: justificado por RN-09/RN-11
--         (listagem paginada ordenada de eventos ativos). EXPLAIN (ANALYZE,
--         BUFFERS) a medir na fase 6 (docs/db/plans/).
-- =============================================================================
