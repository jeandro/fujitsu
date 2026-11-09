-- ============================================================================
-- Script de expurgo/anônimo de description após 24 meses (RN-18).
-- LGPD: base legal interesse legítimo (rastreabilidade operacional da planta).
-- Anonimiza description de tb_factory_events mais antigos que 24 meses.
-- eventId é MANTIDO para integridade da trilha de auditoria (nunca tocado).
-- Execução auditável: cada lote é gravado em purge_audit_log (quem, quando,
-- o quê, de onde).
--
-- Tabelas alvo (alinhadas a V001/V002 e às entidades JPA):
--   tb_factory_events.description  (coluna confidencial, LGPD)
--   purge_audit_log                (auditoria append-only da execução)
--
-- Uso (psql):
--   psql -d eventsdb -U events_app \
--        -v executed_by="dba" \
--        -v source_host="host01" \
--        -v meses_retencao=24 \
--        -v anchor_col=created_at \
--        -v batch_size=1000 \
--        -f scripts/sql/purge_description_rn18_24meses.sql
--
-- anchor_col: 'created_at' (criação do registro) ou 'timestamp' (ocorrência).
--               Padrão de retenção: conta a partir de created_at.
-- ============================================================================

\set meses_retencao :meses_retencao
\set anchor_col :anchor_col
\set batch_size :batch_size
\set executed_by :'executed_by'
\set source_host :'source_host'

\set ON_ERROR_STOP on
\timing on

-- ---- DRY RUN (comente para executar de verdade) ----
-- SELECT count(*) AS linhas_que_seriam_anonimizadas
-- FROM tb_factory_events
-- WHERE description IS NOT NULL
--   AND :anchor_col < now() - interval ':meses_retencao months';
-- -----------------------------------------------------

DO $$
DECLARE
    v_cutoff      timestamptz := now() - interval ':' || :'meses_retencao' || ' months';
    v_total       integer := 0;
    v_lote        integer;
    v_executed_by text := :'executed_by';
    v_source_host text := :'source_host';
    v_batch       integer := :'batch_size';
    v_anchor      text := :'anchor_col';
BEGIN
    -- Valida coluna de âncora (whitelist -> injeção de SQL impossível).
    IF v_anchor NOT IN ('created_at','timestamp') THEN
        RAISE EXCEPTION 'anchor_col inválido: % (use created_at ou timestamp)', v_anchor;
    END IF;

    IF :'executed_by' IS NULL OR :'source_host' IS NULL THEN
        RAISE EXCEPTION 'executed_by e source_host são obrigatórios (auditoria RN-18).';
    END IF;

    RAISE NOTICE 'Iniciando expurgo RN-18: âncora=%, corte=%, lote=%, executado por % em %',
        v_anchor, v_cutoff, v_batch, v_executed_by, v_source_host;

    LOOP
        -- UPDATE em lote para não reter lock longo nem gerar bloat excessivo.
        EXECUTE format(
            'UPDATE tb_factory_events '
            || 'SET description = ''[ANONIMIZADO_RN_18_24_MESES]'' '
            || 'WHERE description IS NOT NULL '
            || '  AND %I < $1 '
            || 'ORDER BY %I '
            || 'LIMIT $2',
            v_anchor, v_anchor
        ) USING v_cutoff, v_batch;

        GET DIAGNOSTICS v_lote = ROW_COUNT;
        EXIT WHEN v_lote = 0;

        v_total := v_total + v_lote;

        -- Auditoria append-only do lote (quem, quando, o quê, de onde).
        INSERT INTO purge_audit_log (executed_by, rule_id, table_name, rows_affected, description, source_host)
        VALUES (
            v_executed_by,
            'RN-18',
            'tb_factory_events',
            v_lote,
            format('Anonimização de description após %s meses (âncora %s)', :'meses_retencao', v_anchor),
            v_source_host
        );

        RAISE NOTICE 'Lote processado: % linhas. Total acumulado: %.', v_lote, v_total;
    END LOOP;

    RAISE NOTICE 'Expurgo RN-18 concluído. Total de descrições anonimizadas: %.', v_total;
END $$;

-- Resumo final (consulta de leitura, sem modificar dados).
SELECT
    count(*) FILTER (WHERE description = '[ANONIMIZADO_RN_18_24_MESES]') AS descricoes_anonimas,
    count(*) FILTER (WHERE description IS NULL) AS descricoes_nulas,
    count(*) AS total_eventos,
    max(created_at) AS evento_mais_recente,
    min(created_at) AS evento_mais_antigo
FROM tb_factory_events;

-- Últimas execuções auditadas de expurgo.
SELECT id, executed_by, executed_at, rule_id, table_name, rows_affected, source_host
FROM purge_audit_log
ORDER BY executed_at DESC
LIMIT 10;
