# Regras de negócio — Registro, consulta e auditoria de eventos de fábrica

Especifica o comportamento verificável da migração MVC → Hexagonal do módulo `events-api`. Não descreve solução técnica; descreve o que o usuário observa e o que o sistema deve fazer.

## 1. História

Como **operador de fábrica**, quero **registrar um evento** de tipo PRODUCTION, MAINTENANCE ou QUALITY e **consultar os eventos paginados**, para que a planta tenha rastreabilidade dos eventos e um **log de auditoria automático** para eventos de qualidade.

## 2. Regras de negócio (RN-xx)

Cada regra é atômica e testável por um humano sem ler código.

| ID | Regra de negócio | Como testar (sem código) |
|----|------------------|--------------------------|
| **RN-01** | Para registrar um evento são obrigatórios: `eventId` (não vazio, até 100 caracteres), `eventType` (um dos valores PRODUCTION, MAINTENANCE, QUALITY), `description` (não vazio, até 1000 caracteres) e `timestamp` (instante ISO 8601 válido). Qualquer campo faltando ou inválido rejeita o registro. | Enviar POST sem um campo obrigatório → rejeitado (400). Enviar `eventId` com 101 caracteres → rejeitado. Enviar `eventType` = "OUTRO" → rejeitado. Enviar `timestamp` não-ISO → rejeitado. |
| **RN-02** | O `eventId` é único por evento. Tentar registrar um `eventId` que já existe é rejeitado com conflito (409), sem criar um segundo registro nem log de auditoria. | POST com `eventId` já cadastrado → 409; contagem de linhas com aquele `event_id` permanece 1; nenhuma nova linha em auditoria. |
| **RN-03** | Reenviar a mesma `Idempotency-Key` com o mesmo corpo (payload) dentro da janela de retenção devolve a resposta idêntica ao primeiro processamento (mesmo status e mesmo corpo), sem criar um segundo efeito. | Duplo clique com mesma `Idempotency-Key` → duas respostas iguais (ex.: 201 com mesmo corpo); uma linha no banco. |
| **RN-04** | Reutilizar uma `Idempotency-Key` já processada com um corpo (payload) diferente é rejeitado com 409 `idempotency-key-reused`. | POST com `Idempotency-Key` = K e corpo A → 201; POST com mesma `Idempotency-Key` = K e corpo B → 409. |
| **RN-05** | Enquanto a primeira execução de uma `Idempotency-Key` está em andamento, uma requisição concorrente com a mesma chave recebe 409 `idempotency-in-progress`. | Duas requisições simultâneas com mesma `Idempotency-Key` → uma processa; a outra recebe 409 enquanto a primeira roda. |
| **RN-06** | Um evento do tipo QUALITY gera um log de auditoria vinculado ao `eventId`, com mensagem descritiva e instant de criação, na mesma unidade de trabalho do registro do evento. | POST QUALITY → existe linha em `tb_audit_logs` com aquele `event_id`; POST PRODUCTION ou MAINTENANCE → nenhuma linha de auditoria. |
| **RN-07** | Se o log de auditoria QUALITY não puder ser gravado após o evento ser registrado, o sistema executa compensação: desfaz o registro do evento e grava um registro de compensação (com data, motivo e responsável "sistema"). O usuário recebe erro informando que o registro não foi concluído. Nenhum efeito parcial permanece visível. | Forçar falha na gravação de auditoria → evento não fica visível na consulta; existe registro de compensação; resposta é erro. |
| **RN-08** | Após criado, `eventId`, `eventType`, `description` e `timestamp` de um evento são imutáveis; não há operação de alteração de evento. | Não existe endpoint PUT/PATCH de evento; tentativa de alterar um evento registrado é rejeitada. |
| **RN-09** | A consulta de eventos é paginada, ordenada por `timestamp` decrescente e, no desempate, `id` interno decrescente. Página padrão = 20 itens; máximo = 100 itens por página. | GET `page=2&size=10` → retorna 10 itens da segunda página; GET `size=200` → limitado a 100 (ou rejeitado). |
| **RN-10** | O p95 do POST `/api/events` é de no máximo 300 ms. | Teste de carga mede p95 ≤ 300 ms. |
| **RN-11** | O p95 do GET `/api/events` é de no máximo 800 ms com até 1.000 registros cadastrados. | Teste de carga com 1.000 registros mede p95 ≤ 800 ms. |
| **RN-12** | Cada requisição executa no máximo 3 consultas ao banco de dados; não há consulta N+1. | Contador de consultas por requisição mostra ≤ 3 queries. |
| **RN-13** | Apenas usuários com o papel `operador` podem registrar eventos (POST `/api/events`). | Requisição sem autenticação → 401; com papel diferente de `operador` → 403. |
| **RN-14** | Usuários com os papéis `operador` ou `gerente` podem consultar eventos (GET `/api/events`). | Requisição com papel `gerente` → 200; com papel externo → 403; sem autenticação → 401. |
| **RN-15** | O limite de taxa é de 100 requisições por minuto por usuário autenticado (decisão proposta em PA-01). Exceder o limite recebe 429. | Enviar mais de 100 requisições em 1 minuto com o mesmo usuário → a 101ª recebe 429. |
| **RN-16** | O tamanho máximo do corpo da requisição POST é de 1 MB. Corpos maiores são rejeitados com 413. | Enviar corpo > 1 MB → 413. |
| **RN-17** | Classificação LGPD dos campos: `eventId` = interno; `eventType` = público; `description` = confidencial (texto livre que pode conter dado pessoal); `timestamp` = interno. | O dicionário de dados declara a classe de cada coluna/tabela. |
| **RN-18** | Base legal: interesse legítimo (rastreabilidade operacional da planta). Finalidade: registro e auditoria de eventos de fábrica. Retenção: 24 meses. Expurgo: anonimização do `description` após 24 meses; o `eventId` é mantido para integridade da trilha de auditoria. | O DBA executa expurgo/anônimo a cada 24 meses; a execução é auditável. |
| **RN-19** | Direito do titular: acesso (consulta os próprios dados via `description`), correção (não altera registro imutável; gera novo registro de correção vinculado), exclusão (anonimiza o `description`, não apaga o `eventId`), portabilidade (exporta os campos do evento em formato estruturado). | O `openapi.yaml` documenta o endpoint/operação de direito do titular. |
| **RN-20** | Mascaramento: o campo `description` é mascarado em logs de aplicação e não é devolvido em resposta a papel sem permissão; `timestamp` e `eventId` são internos, não expostos a público não autenticado. | Logs de aplicação não contêm `description` em claro; resposta pública não inclui dados confidenciais. |
| **RN-21** | Frescor do dado: a leitura reflete os dados commitados no momento da consulta (leitura direta, sem cache de leitura); não há dado em cache com atraso perceptível ao usuário. | GET após um commit bem-sucedido retorna o dado imediatamente. |
| **RN-22** | Operações irreversíveis: um evento já registrado (especialmente QUALITY com auditoria gerada) não é apagado; a correção se dá por novo registro vinculado, nunca por exclusão do original. | Não existe exclusão de evento; tentativa de excluir é rejeitada. |

## 3. Critérios de aceite (CA-xx) — rastreamento

Cada CA está rastreado a pelo menos um teste automatizado que falha sem a implementação.

| ID | Cenário | Teste automatizado (falha sem implementação) |
|----|---------|----------------------------------------------|
| **CA-01** | Registro de evento com sucesso → 201 | `EventControllerTest.testReceiveEventSuccess` / `RegisterEventServiceTest.productionDoesNotAudit` |
| **CA-02** | Evento QUALITY gera log de auditoria | `EventApiTest.productionAndQualityPersistWithCorrectAudit` / `RegisterEventServiceTest.qualityRecordsContentAndSameClockInstant` |
| **CA-03** | Evento PRODUCTION/MAINTENANCE não gera auditoria | `EventApiTest.maintenanceDoesNotCreateAudit` / `RegisterEventServiceTest.productionDoesNotAudit` |
| **CA-04** | Duplicidade por `eventId` → 409, sem novo efeito | `EventApiTest.duplicateAndTrimReturnConflict` / `RegisterEventServiceTest.duplicateDoesNotWriteAnything` |
| **CA-05** | Replay idempotente (mesma `Idempotency-Key`, mesmo payload) → resposta idêntica, um efeito | `docs/spec/gherkin/idempotencia.feature` → `IdempotencyKeyTest.replaySameKeySamePayload` |
| **CA-06** | `Idempotency-Key` reutilizada com payload diferente → 409 `idempotency-key-reused` | `docs/spec/gherkin/idempotencia.feature` → `IdempotencyKeyTest.reusedKeyDifferentPayload` |
| **CA-07** | Concorrência: mesma `Idempotency-Key` simultânea → um efeito, duas respostas iguais | `docs/spec/gherkin/concorrencia.feature` → `IdempotencyKeyConcurrencyTest.sameKeySameResponse` |
| **CA-08** | Concorrência: mesmo `eventId`, chaves de idempotência diferentes → um 201, um 409 duplicado | `docs/spec/gherkin/concorrencia.feature` → `ConcurrentEventTest.oneEventIdTwoKeys` |
| **CA-09** | Saga: falha de auditoria QUALITY → compensação executada e auditada; evento não fica visível | `docs/spec/gherkin/saga-auditoria-qualidade.feature` → `QualityAuditSagaTest.auditFailureCompensates` |
| **CA-10** | Consulta paginada, ordenada, tamanho de página respeitado | `docs/spec/gherkin/consulta-paginada.feature` → `EventListPaginationTest.orderedAndPaged` |
| **CA-11** | Validação de entrada → 400 | `docs/spec/gherkin/registro-evento.feature` → `EventValidationTest.invalidInputsReturn400` |
| **CA-12** | Autorização por papel: 401 sem token, 403 papel errado por endpoint | `docs/spec/gherkin/autorizacao.feature` → `SecurityAuthorizationTest.roleBasedAccess` |
| **CA-13** | Performance p95 POST ≤ 300 ms | `docs/spec/gherkin/desempenho.feature` → `PerformanceTest.postP95` |
| **CA-14** | Performance p95 GET ≤ 800 ms com 1.000 registros | `docs/spec/gherkin/desempenho.feature` → `PerformanceTest.getP95With1000Records` |
| **CA-15** | Máximo 3 consultas por request (sem N+1) | `docs/spec/gherkin/desempenho.feature` → `QueryCountTest.maxThreeQueries` |
| **CA-16** | Corpo > 1 MB → 413 | `docs/spec/gherkin/payload-grande.feature` → `PayloadSizeTest.bodyOverOneMBReturns413` |
| **CA-17** | Mascaramento de `description` em logs | `docs/spec/gherkin/mascaramento-logs.feature` → `LogMaskingTest.descriptionMaskedInLogs` |

## 4. Conflitos com decisões existentes (ADR-0001)

| Fonte | Conteúdo da decisão | Conflito com esta spec / AGENTS.md | Resolução necessária |
|-------|---------------------|------------------------------------|----------------------|
| ADR-0001 item 8 | Rejeita tabela `idempotency_key` e header `Idempotency-Key`; retenção = vida útil do registro | AGENTS.md stack obrigatória exige idempotência por chave; esta spec exige header `Idempotency-Key` com janela de retenção e 409 `idempotency-key-reused` (RN-03, RN-04, RN-05) | `arquitetura-hexagonal` revisa ADR-0001 |
| ADR-0001 item 9 | Rejeita saga orquestrada e transactional outbox | Esta spec exige saga de registro + auditoria com compensação (RN-06, RN-07) | `arquitetura-hexagonal` revisa ADR-0001 |
| ADR-0001 item 7 | Mantém H2 + `ddl-auto=update`; sem Flyway | AGENTS.md exige Flyway versionadas (PostgreSQL fica para ADR separado; H2 mantido nesta fase) | Sem conflito direto nesta fase |

## 5. Glossário de domínio

| Termo (português) | Nome no código / tabela (inglês) |
|-------------------|----------------------------------|
| ID do evento | `event_id` / `eventId` |
| Tipo do evento | `event_type` / `EventType` (PRODUCTION, MAINTENANCE, QUALITY) |
| Descrição | `description` / `description` |
| Timestamp do evento | `timestamp` / `Instant` (UTC) |
| Log de auditoria | `tb_audit_logs` / `AuditLog` (`logMessage`, `createdAt`) |
| Chave de idempotência (operacional) | Cabeçalho HTTP `Idempotency-Key` |
| Tabela de idempotência | `idempotency_key` (`scope`, `key`, `request_hash`, `response_status`, `response_body`, `expires_at`) |
| Registro de compensação | Registro de fato novo (data, motivo, responsável "sistema") |
| Papel operador | `operador` (Spring Security role) |
| Papel gerente | `gerente` (Spring Security role) |
| Problema da API (RFC 9457) | `Problem` (`type`, `title`, `status`, `detail`, `instance`) |

## 6. Perguntas abertas (bloqueantes)

Máximo de cinco. Até serem respondidas, os itens marcados como "suposição" permanecem não confirmados.

| ID | Pergunta | Decisão proposta | Status | Por que bloqueia |
|----|----------|------------------|--------|------------------|
| **PA-01** | Qual é o limite de taxa exato (requisições por minuto por usuário/IP)? | **100 req/min por usuário autenticado** (RN-15 já usa este valor). Acima do limite → 429. RateLimiter e teste de 429 são de implementação (arquitetura-hexagonal / backend-java). | **Confirmado** — valor já fixado em RN-15 e E-10; não requer nova aprovação. | Define o `RateLimiter` e o teste de 429. |
| **PA-02** | Qual é a janela de retenção da `Idempotency-Key` (horas/dias)? | **24 horas** a partir do primeiro processamento da chave (proposta). Após expiração, replay com a mesma chave processa normalmente (novo efeito). | **Pendente - requer aprovação do humano** (aguardando confirmação do squad-lead/arquitetura). | Define `expires_at` da tabela de idempotência e o comportamento de replay expirado. |
| **PA-03** | Existem papéis além de `operador` e `gerente` no lançamento inicial (ex.: `analista`, `auditor`)? | **Não.** No lançamento inicial existem apenas os papéis `operador` e `gerente`. Matriz: operador (POST + GET), gerente (GET), demais papéis (403), não autenticado (401). | **Confirmado** — já especificado em RN-13, RN-14 e glossário. | Define a matriz de autorização completa (RN-13, RN-14). |
| **PA-04** | Quem executa o expurgo/anônimo de `description` após 24 meses e com que ferramenta (job, script manual)? | **Job agendado** executa anonimização do `description` após 24 meses; o `eventId` é mantido para integridade da trilha. Execução é auditável (data, responsável "sistema", quantidade de linhas anonimizadas). | **Confirmado** — já especificado em RN-18; agendador/cron é decisão operacional (dba-postgres). | Define a operação de retenção/LGPD (RN-18). |
| **PA-05** | A compensação de falha de auditoria exclui fisicamente o evento ou o marca como cancelado (exclusão lógica)? | **Exclusão lógica (marca como cancelado)** do evento + registro de compensação (data, motivo, responsável "sistema"). O evento cancelado não aparece na consulta normal mas permanece para auditoria. Não há exclusão física (RN-22 proíbe); compensação é comando novo, auditável e idempotente (lei de saga, AGENTS.md). Reconciliação com RN-07: "desfaz o registro" significa que não há efeito de negócio visível; "não foi persistido" (E-05) significa registro não concluído do ponto de vista de negócio, não exclusão física da linha. | **Confirmado** — consistente com RN-07, RN-22 e lei de saga. | Define o efeito visível da compensação e o que o usuário consulta depois (RN-07). |

## 6.1 Itens que requerem aprovação do humano

| Item | Decisão necessária | Justificativa | Quem aprova |
|------|--------------------|---------------|-------------|
| **PA-02** | Janela de retenção da `Idempotency-Key` (24 h proposta) | Afeta ciclo de vida do dado, comportamento de replay após expiração e tamanho da tabela `idempotency_key`. É decisão de negócio/infraestrutura, não suposição do desenvolvedor. Até confirmar, permanece como suposição não confirmada. | `squad-lead` / `arquitetura-hexagonal` |

PA-01, PA-03, PA-04 e PA-05 estão confirmados porque já estão especificados em RN-xx (respectivamente RN-15/E-10, RN-13/RN-14, RN-18 e RN-07/RN-22) e não dependem de nova decisão para prosseguir com a implementação.

---
Fim da spec de regras de negócio. Próximos arquivos: `docs/spec/gherkin/*.feature` e `docs/spec/erros.md`.
