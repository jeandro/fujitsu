-- =============================================================================
-- Flyway U001__schema_inicial.sql  (rollback de V001)
-- =============================================================================
-- Motivo: desfaz V001 (drop das tabelas criadas).
-- Atençăo: DROP TABLE apaga dados. Executar apenas em ambiente de dev/homolog
--         ou sob decisăo explícita do humano em produçăo.
-- Ordem: tb_audit_logs primeiro (possui FK para tb_factory_events).
-- =============================================================================

DROP TABLE IF EXISTS tb_audit_logs;
DROP TABLE IF EXISTS tb_factory_events;

-- Comentário de rollback (padrăo do squad): em produçăo a perda de dados é
-- decisăo humana; corrigir dado com script versionado e auditável, nunca TRUNCATE/DROP
-- para "resolver" erro de dado.
