# Planos de execução — events-api

Este diretório armazena evidências `EXPLAIN (ANALYZE, BUFFERS)` antes/depois de cada índice novo, conforme o padrão do DBA (`AGENTS.md` §7 e `.opencode/standards/qualidade-codigo.md`).

## Status da fase 5

**Nenhum baseline existe ainda.** As tabelas são iniciais e vazias. Todas as justificativas de índice em `V001__schema_inicial.sql`, `V002__idempotencia_saga_outbox.sql` e `docs/db/data-dictionary.md` estão marcadas como "a medir na fase 6".

## O que deve ser entregue na fase 6

Para cada índice novo:

1. `EXPLAIN (ANALYZE, BUFFERS)` da consulta real **antes** do índice (baseline) — com volume representativo.
2. `EXPLAIN (ANALYZE, BUFFERS)` **depois** do índice.
3. Comparação: custo, linhas estimadas vs. reais, tipo de scan, ganho esperado e custo de escrita.
4. Confirmação de que a consulta é quente via `pg_stat_statements`.
5. Arquivo nomeado `<migration>__<indice>.txt` ou `<indice>.txt` neste diretório.

Índices a validar (fase 6):
- `idx_factory_events_timestamp_desc_id_desc` (listagem paginada RN-09/RN-11)
- `idx_audit_logs_event_id` (auditoria por eventId RN-06)
- `uk_idempotency_key_scope_key` + `idx_idempotency_key_expires_at` (idempotência/expurgo)
- `uq_saga_instance_active`, `idx_saga_instance_business_key` (saga)
- `idx_saga_step_saga_id_seq` (passos de saga)
- `idx_outbox_event_unpublished_occurred_at` (relay outbox, `FOR UPDATE SKIP LOCKED`)
- `idx_outbox_event_aggregate` (rastreabilidade outbox)
- `pk_processed_event` (dedupe consumidor)

## Convenção de arquivo

`V001__idx_factory_events_timestamp_desc_id_desc.txt` etc.
