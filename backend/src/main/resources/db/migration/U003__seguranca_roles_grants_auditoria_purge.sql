-- Flyway UNDO U003 - Reverte V003__seguranca_roles_grants_auditoria_purge.sql
-- Motivo: remover tabela purge_audit_log e role events_app.
-- ATENÇÃO: perda de dados de auditoria de expurgo. Execução em produção só com
-- aprovação explícita do humano.
-- Execute em ordem inversa: U003, U002, U001.

DROP TABLE IF EXISTS purge_audit_log;
DROP ROLE IF EXISTS events_app;
