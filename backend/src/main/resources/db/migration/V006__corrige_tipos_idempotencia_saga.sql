-- Rollback: converter ambas as colunas para smallint após verificar os valores de seq.
ALTER TABLE idempotency_key
ALTER COLUMN response_status TYPE integer;

ALTER TABLE saga_step
ALTER COLUMN seq TYPE integer;
