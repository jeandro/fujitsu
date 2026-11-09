-- =============================================================================
-- Flyway U004__exclusao_logica_evento_e_event_id_passo_saga.sql  (rollback de V004)
-- =============================================================================
-- Motivo: desfaz V004 (remove colunas status e event_id).
-- =============================================================================

ALTER TABLE saga_step
  DROP COLUMN IF EXISTS event_id;

ALTER TABLE tb_factory_events
  DROP CONSTRAINT IF EXISTS chk_factory_events_status;

DROP INDEX IF EXISTS idx_factory_events_active_timestamp_desc_id_desc;

ALTER TABLE tb_factory_events
  DROP COLUMN IF EXISTS status;
