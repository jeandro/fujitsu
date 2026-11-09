-- Flyway V003 - Segurança de dados, grants e auditoria de expurgo
-- Motivo: role de aplicação com menor privilégio (sem DDL, sem SUPERUSER) e
-- tabela append-only de auditoria de execuções de expurgo/anônimo (RN-18).
-- Depende de V001 (tb_factory_events, tb_audit_logs) e V002 (tabelas de
-- consistência). Rollback: U003__seguranca_roles_grants_auditoria_purge.sql.
-- RLS: não aplicável (módulo single-tenant, sem tenant_id). Revisar se
-- multi-tenant for adotado.

SET lock_timeout = '5s';

-- Role de aplicação com menor privilégio (padrão de segurança de dados).
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'events_app') THEN
        EXECUTE 'CREATE ROLE events_app LOGIN';
    END IF;
END $$;

GRANT USAGE ON SCHEMA public TO events_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON
    tb_factory_events, tb_audit_logs, idempotency_key, saga_instance, saga_step,
    outbox_event, processed_event TO events_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO events_app;

-- Tabela de auditoria de expurgo (append-only: quem, quando, o quê, de onde).
-- Nunca se altera nem exclui linhas desta tabela.
CREATE TABLE IF NOT EXISTS purge_audit_log (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    executed_by text NOT NULL,
    executed_at timestamptz NOT NULL DEFAULT now(),
    rule_id text NOT NULL,
    table_name text NOT NULL,
    rows_affected integer NOT NULL,
    description text NOT NULL,
    source_host text NOT NULL
);

GRANT SELECT, INSERT ON purge_audit_log TO events_app;

COMMENT ON TABLE purge_audit_log IS 'Auditoria append-only de execuções de expurgo/anônimo (RGPD/LGPD, RN-18). Nunca altera nem exclui linhas.';
COMMENT ON COLUMN purge_audit_log.executed_by IS 'Responsável pela execução do expurgo.';
COMMENT ON COLUMN purge_audit_log.executed_at IS 'Instant da execução.';
COMMENT ON COLUMN purge_audit_log.rule_id IS 'Regra de negócio executada (ex.: "RN-18").';
COMMENT ON COLUMN purge_audit_log.table_name IS 'Tabela alvo do expurgo.';
COMMENT ON COLUMN purge_audit_log.rows_affected IS 'Quantidade de linhas afetadas no lote.';
COMMENT ON COLUMN purge_audit_log.description IS 'Descrição da operação executada.';
COMMENT ON COLUMN purge_audit_log.source_host IS 'Host de origem da execução.';

-- Obrigatório: EXPLAIN (ANALYZE, BUFFERS) das consultas de expurgo (DELETE/UPDATE
-- em lote por expires_at ou created_at < corte) antes de aprovar em produção.
