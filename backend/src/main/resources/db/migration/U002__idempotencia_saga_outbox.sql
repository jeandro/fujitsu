-- =============================================================================
-- Flyway U002__idempotencia_saga_outbox.sql  (rollback de V002)
-- =============================================================================
-- Motivo: desfaz V002 (drop das tabelas de consistęncia criadas).
-- Atençăo: DROP TABLE apaga dados (estado de saga, outbox, chaves de idempotęncia).
--         Executar apenas em dev/homolog ou sob decisăo explícita do humano.
-- Ordem: tabelas dependentes primeiro (saga_step depende de saga_instance).
-- =============================================================================

DROP TABLE IF EXISTS saga_step;
DROP TABLE IF EXISTS saga_instance;
DROP TABLE IF EXISTS outbox_event;
DROP TABLE IF EXISTS processed_event;
DROP TABLE IF EXISTS idempotency_key;

-- Comentário de rollback (padrăo do squad): em produçăo a perda de dados é
-- decisăo humana; corrigir dado com script versionado e auditável, nunca TRUNCATE/DROP
-- para "resolver" erro de dado.
