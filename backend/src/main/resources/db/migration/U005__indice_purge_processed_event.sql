-- =============================================================================
-- Flyway U004__indice_purge_processed_event.sql  (rollback de V004)
-- =============================================================================
-- Motivo: desfaz V004 (remove índice de suporte ao expurgo de processed_event).
-- Atenção: sem este índice, expurgo por processed_at < corte faz seq scan.
--         Reaplicar V004 antes de rodar o job de expurgo em produção.
-- Sem perda de dados da tabela processed_event.
-- =============================================================================

DROP INDEX IF EXISTS idx_processed_event_processed_at;
