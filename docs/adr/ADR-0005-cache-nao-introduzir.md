# ADR-0005: Cache adaptativo — decisão de NÃO introduzir cache nesta fase

Status: aceito
Data: 2026-11-05

## Contexto

O padrão de cache do squad exige que cache seja decisão de performance
medida, não hábito: sem plano de execução, métrica de acesso ou perfil de
leitura que justifique, a resposta é "sem cache". Requisitos aplicáveis:

- RN-21: frescor do dado — leitura direta, sem cache de leitura; dado
  commitado visível imediatamente.
- RN-11: p95 GET ≤ 800 ms com até 1.000 registros.
- RN-12: máximo 3 consultas/request; paginação obrigatória (máx. 100
  itens/página).
- Nenhum RN declara TTL. Volume pequeno (1.000 registros no cenário de teste
  de carga).
- `AGENTS.md`: cache apenas com ADR e hit ratio medido; nunca no domínio.

## Decisão

**Não introduzir cache agora** (nem Caffeine local, nem Redis distribuído,
nem `@Cacheable` em `domain`/`application`).

**Gatilho de reavaliação**: introduzir cache somente quando, com baseline
medido em homologação/produção:

- p95 de `GET /api/events` > **200 ms**, **ou**
- tabela de eventos > **10 000 registros**.

Quando introduzido, deve seguir o padrão: cache-aside com TTL explícito e
invalidação na escrita, chave com tenant, single-flight contra stampede, cache
negativo com TTL curto, métricas `cache.gets`/`cache.evictions` (Micrometer),
teste de hit/miss/invalidação (100% das chaves), hit ratio abaixo de 50% em
7 dias → remover o cache e registrar a decisão no ADR. HTTP: `ETag` +
`If-None-Match` (304) e `Cache-Control` para leitura estável; `no-store` em
resposta com dado pessoal (RN-17, RN-20).

## Números

- TTL: nenhum declarado agora (sem RN de TTL; número mágico proibido).
- Gatilho de reavaliação: p95 GET > 200 ms **ou** > 10 000 registros.
- Hit ratio mínimo: 50% em 7 dias; abaixo → remover.
- Cache introduzido nesta fase: zero instâncias.

## Alternativas descartadas

- **Cache-aside agora (Caffeine/Redis).** Rejeitado: sem medição de leitura,
  volume pequeno, RN-21 proíbe dado desatualizado, nenhum RN declara TTL;
  `@Cacheable` no domínio é violação de ArchUnit.
- **Cache de leitura para atender RN-11.** Rejeitado: p95 ≤ 800 ms com 1.000
  registros é atingível com leitura direta + índice + paginação (a validar com
  `EXPLAIN` pelo DBA); cache adicionaria complexidade e risco de frescor sem
  benefício mensurado.

## Referências

- `docs/spec/regras-negocio.md` — RN-11, RN-12, RN-21, RN-17, RN-20.
- `.opencode/standards/idempotencia-saga-cache.md` — padrão completo de cache
  (quando houver).
