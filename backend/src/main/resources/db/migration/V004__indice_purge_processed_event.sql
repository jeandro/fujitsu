-- =============================================================================
-- Flyway V004__indice_purge_processed_event.sql  (PostgreSQL 17)
-- =============================================================================
-- Motivo: suportar expurgo em lote de processed_event (ADR-0004, RN-18).
--         O expurgo remove entregas antigas por processed_at < corte, dentro da
--         janela de reentrega do broker. Sem índice, cada lote de expurgo faz
--         seq scan em processed_event, que cresce com o volume de mensagens.
-- Impacto estimado: CREATE INDEX em tabela pequena nesta fase (fase 5, vazia).
--         Se aplicada em produção com tabela grande, executar como
--         CREATE INDEX CONCURRENTLY FORA de transação (Flyway default embrulha
--         a migration em transação; CONCURRENTLY não aceita transaction block),
--         para não bloquear gravações do consumidor (at-least-once).
-- Rollback: veja U004__indice_purge_processed_event.sql (DROP INDEX). Sem perda
--         de dados da tabela, apenas remove o índice de suporte ao expurgo.
-- Janela recomendada: qualquer janela (índice em tabela vazia/pequena; em
--         produção grande, janela de baixa gravação se não usar CONCURRENTLY).
-- Padrões: índice justificado por consulta real de expurgo; retenção finita
--         exige índice de suporte ao DELETE em lote (tabela que só cresce é
--         dívida operacional).
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_processed_event_processed_at
  ON processed_event (processed_at);

-- Consulta real do expurgo (RN-18; janela de reentrega do broker):
-- DELETE FROM processed_event
--  WHERE processed_at < $1
--  ORDER BY processed_at
--  LIMIT :lote;

-- EXPLAIN (ANALYZE, BUFFERS) do DELETE em lote: a medir na fase 6
-- (docs/db/plans/). Índice proposto sem medição real ainda; justificado pela
-- operação de retenção obrigatória (RN-18), não por leitura quente atual.
