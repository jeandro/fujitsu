# Catálogo de erros — API de Eventos de Fábrica (docs/api)

Extraído de `docs/spec/erros.md` (fonte do negócio) e alinhado ao contrato `docs/api/openapi.yaml`.
Mantido pelo `negocio-analista` e revisado pelo `backend-java`; qualquer mudança de `type` é
mudança de contrato e exige ADR + aprovação humana.

Todo erro é retornado como `ProblemDetail` (RFC 9457, `application/problem+json`) com os campos `type`, `title`, `status`,
`detail`, `instance` e a extensão `code` (slug do catálogo). O `type` é um URI estável que identifica a classe de erro; o `RN-xx`
indica a regra de negócio correspondente; o `instance` é o identificador de correlação
(`requestId`) da requisição, útil para suporte.

Base dos URIs de erro: `https://api.factory/events/errors/`

| # | type (URI estável) | status HTTP | RN-xx | title | Quando ocorre (detail exemplar) |
|---|--------------------|-------------|-------|-------|---------------------------------|
| E-01 | `https://api.factory/events/errors/validation-error` | 400 | RN-01 | Erro de validação de entrada | Campo obrigatório/inválido no corpo (`eventId`, `eventType`, `description`, `timestamp`) ou cabeçalho `Idempotency-Key` ausente. Ex.: "O ID do evento (eventId) é obrigatório." |
| E-02 | `https://api.factory/events/errors/duplicate-event` | 409 | RN-02 | Evento duplicado | `eventId` já cadastrado: "O evento com ID 'EVT-DUP' já foi processado anteriormente." |
| E-03 | `https://api.factory/events/errors/idempotency-key-reused` | 409 | RN-04 | Chave de idempotência reutilizada com payload diferente | Mesma `Idempotency-Key` com corpo diferente do processado anteriormente. |
| E-04 | `https://api.factory/events/errors/idempotency-in-progress` | 409 | RN-05 | Execução de idempotência em andamento | Requisição concorrente com mesma `Idempotency-Key` enquanto a primeira execução roda. |
| E-05 | `https://api.factory/events/errors/audit-compensation-required` | 500 | RN-07 | Falha na auditoria com compensação executada | Log de auditoria QUALITY não pôde ser gravado; compensação executada, evento não persistido. |
| E-06 | `https://api.factory/events/errors/not-found` | 404 | — | Recurso não encontrado | Evento ou saga não existe. |
| E-07 | `https://api.factory/events/errors/method-not-allowed` | 405 | — | Método não permitido | Método HTTP não permitido para o recurso (ex.: PUT/PATCH/DELETE de evento — não existem, RN-08, RN-22). |
| E-08 | `https://api.factory/events/errors/unsupported-media-type` | 415 | — | Tipo de mídia não suportado | `Content-Type` diferente de `application/json`. |
| E-09 | `https://api.factory/events/errors/payload-too-large` | 413 | RN-16 | Corpo da requisição muito grande | Corpo POST > 1 MB. |
| E-10 | `https://api.factory/events/errors/too-many-requests` | 429 | RN-15 | Limite de taxa excedido | Mais de 100 requisições por minuto por usuário autenticado (PA-01: suposição a confirmar). |
| E-11 | `https://api.factory/events/errors/unauthorized` | 401 | RN-13, RN-14 | Não autenticado | Requisição sem token de acesso. |
| E-12 | `https://api.factory/events/errors/forbidden` | 403 | RN-13, RN-14 | Acesso negado | Papel sem permissão (`operador` para POST; `operador`/`gerente` para GET). |
| E-13 | `https://api.factory/events/errors/internal-error` | 500 | — | Erro interno | Exceção inesperada ou falha de infraestrutura; sem detalhes internos. |

## Exemplos de corpo (ProblemDetail — RFC 9457)

### E-01 — Erro de validação (400)

```json
{
  "type": "https://api.factory/events/errors/validation-error",
  "title": "Erro de validação de entrada",
  "status": 400,
  "detail": "O ID do evento (eventId) é obrigatório.",
  "instance": "/v1/events#req-550e8400-e29b-41d4-a716-446655440000",
  "code": "validation-error",
  "errors": {
    "eventId": "O ID do evento (eventId) é obrigatório."
  },
  "timestamp": "2026-11-05T13:00:00Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### E-02 — Evento duplicado (409)

```json
{
  "type": "https://api.factory/events/errors/duplicate-event",
  "title": "Evento duplicado",
  "status": 409,
  "detail": "O evento com ID 'EVT-DUP' já foi processado anteriormente.",
  "instance": "/v1/events#req-550e8400-e29b-41d4-a716-446655440001",
  "code": "duplicate-event",
  "timestamp": "2026-11-05T13:00:01Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440001"
}
```

### E-03 — Idempotency-Key reutilizada com payload diferente (409)

```json
{
  "type": "https://api.factory/events/errors/idempotency-key-reused",
  "title": "Chave de idempotência reutilizada com payload diferente",
  "status": 409,
  "detail": "A Idempotency-Key já foi processada com um corpo diferente. Reutilize a chave apenas com o mesmo payload.",
  "instance": "/v1/events#req-550e8400-e29b-41d4-a716-446655440002",
  "code": "idempotency-key-reused",
  "timestamp": "2026-11-05T13:00:02Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440002"
}
```

### E-04 — Idempotency-Key em andamento (409)

```json
{
  "type": "https://api.factory/events/errors/idempotency-in-progress",
  "title": "Execução de idempotência em andamento",
  "status": 409,
  "detail": "Uma execução com esta Idempotency-Key já está em andamento. Tente novamente em instantes.",
  "instance": "/v1/events#req-550e8400-e29b-41d4-a716-446655440003",
  "code": "idempotency-in-progress",
  "timestamp": "2026-11-05T13:00:03Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440003"
}
```

### E-05 — Falha na auditoria com compensação executada (500)

```json
{
  "type": "https://api.factory/events/errors/audit-compensation-required",
  "title": "Falha na auditoria com compensação executada",
  "status": 500,
  "detail": "Não foi possível concluir o registro: o log de auditoria QUALITY não pôde ser gravado. A compensação foi executada e o evento não foi persistido.",
  "instance": "/v1/events#req-550e8400-e29b-41d4-a716-446655440004",
  "code": "audit-compensation-required",
  "timestamp": "2026-11-05T13:00:04Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440004"
}
```

### E-06 — Recurso não encontrado (404)

```json
{
  "type": "https://api.factory/events/errors/not-found",
  "title": "Recurso não encontrado",
  "status": 404,
  "detail": "Recurso não encontrado.",
  "instance": "/v1/events/EVT-9999#req-550e8400-e29b-41d4-a716-446655440006",
  "code": "not-found",
  "timestamp": "2026-11-05T13:00:06Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440006"
}
```

### E-09 — Corpo muito grande (413)

```json
{
  "type": "https://api.factory/events/errors/payload-too-large",
  "title": "Corpo da requisição muito grande",
  "status": 413,
  "detail": "O corpo da requisição excede o limite de 1 MB.",
  "instance": "/v1/events#req-550e8400-e29b-41d4-a716-446655440007",
  "code": "payload-too-large",
  "timestamp": "2026-11-05T13:00:07Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440007"
}
```

### E-10 — Limite de taxa excedido (429)

```json
{
  "type": "https://api.factory/events/errors/too-many-requests",
  "title": "Limite de taxa excedido",
  "status": 429,
  "detail": "Limite de 100 requisições por minuto excedido. Tente novamente mais tarde.",
  "instance": "/v1/events#req-550e8400-e29b-41d4-a716-446655440008",
  "code": "too-many-requests",
  "timestamp": "2026-11-05T13:00:08Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440008"
}
```

### E-11 — Não autenticado (401)

```json
{
  "type": "https://api.factory/events/errors/unauthorized",
  "title": "Não autenticado",
  "status": 401,
  "detail": "Autenticação necessária para acessar este recurso.",
  "instance": "/v1/events#req-550e8400-e29b-41d4-a716-446655440005",
  "code": "unauthorized",
  "timestamp": "2026-11-05T13:00:05Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440005"
}
```

### E-12 — Acesso negado por papel (403)

```json
{
  "type": "https://api.factory/events/errors/forbidden",
  "title": "Acesso negado",
  "status": 403,
  "detail": "Seu papel não tem permissão para executar esta ação.",
  "instance": "/v1/events#req-550e8400-e29b-41d4-a716-446655440005",
  "code": "forbidden",
  "timestamp": "2026-11-05T13:00:05Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440005"
}
```

### E-13 — Erro interno (500)

```json
{
  "type": "https://api.factory/events/errors/internal-error",
  "title": "Erro interno",
  "status": 500,
  "detail": "Erro interno. Tente novamente ou informe o identificador ao suporte.",
  "instance": "/v1/events#req-550e8400-e29b-41d4-a716-446655440009",
  "code": "internal-error",
  "timestamp": "2026-11-05T13:00:09Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440009"
}
```

## Regras de uso do catálogo

- O campo `type` nunca muda para a mesma classe de erro: é o contrato estável para o cliente.
- O campo `detail` é a mensagem legível ao usuário e pode variar com o contexto (ex.: inclui o `eventId` conflitante).
- O campo `instance` é o `requestId` de correlação; nunca contém dado pessoal nem segredo.
- Erros 5xx não expõem detalhes internos (senhas, strings de conexão, nomes de classe) — veja `E-13`.
- O cabeçalho `Idempotency-Key` é obrigatório em `POST /v1/events`; sua ausência gera `E-01` (400).

## Perguntas abertas (herdadas de docs/spec/regras-negocio.md)

- **PA-01**: limite de taxa exato (100 req/min é suposição; define o `RateLimiter` e o teste de 429).
- **PA-02**: janela de retenção da `Idempotency-Key` (ADR-0003 adota 24 h; confirmar com `negocio-analista`).
- **PA-03**: papéis além de `operador` e `gerente` no lançamento inicial?
- **PA-05**: compensação de falha de auditoria exclui fisicamente o evento ou exclusão lógica?
