# Catálogo de erros — API de Eventos de Fábrica

Todo erro é retornado como `ProblemDetail` (RFC 9457) com os campos `type`, `title`, `status`, `detail` e `instance`. O `type` é um URI estável que identifica a classe de erro; o `RN-xx` indica a regra de negócio correspondente; `instance` é o identificador de correlação da requisição (útil para suporte).

Base dos URIs de erro: `https://api.factory/events/errors/`

| # | type (URI estável) | status HTTP | RN-xx | title | Quando ocorre (detail exemplar) |
|---|--------------------|-------------|-------|-------|---------------------------------|
| E-01 | `https://api.factory/events/errors/validation-error` | 400 | RN-01 | Erro de validação de entrada | "O ID do evento (eventId) é obrigatório." / "O tipo do evento (eventType) é obrigatório." / "A descrição deve ter até 1000 caracteres." / "O timestamp é obrigatório." |
| E-02 | `https://api.factory/events/errors/duplicate-event` | 409 | RN-02 | Evento duplicado | "O evento com ID 'EVT-DUP' já foi processado anteriormente." |
| E-03 | `https://api.factory/events/errors/idempotency-key-reused` | 409 | RN-04 | Chave de idempotência reutilizada com payload diferente | "A Idempotency-Key já foi processada com um corpo diferente. Reutilize a chave apenas com o mesmo payload." |
| E-04 | `https://api.factory/events/errors/idempotency-in-progress` | 409 | RN-05 | Execução de idempotência em andamento | "Uma execução com esta Idempotency-Key já está em andamento. Tente novamente em instantes." |
| E-05 | `https://api.factory/events/errors/audit-compensation-required` | 500 | RN-07 | Falha na auditoria com compensação executada | "Não foi possível concluir o registro: o log de auditoria QUALITY não pôde ser gravado. A compensação foi executada e o evento não foi persistido." |
| E-06 | `https://api.factory/events/errors/not-found` | 404 | — | Recurso não encontrado | "Recurso não encontrado." |
| E-07 | `https://api.factory/events/errors/method-not-allowed` | 405 | — | Método não permitido | "Método HTTP não permitido para este recurso." |
| E-08 | `https://api.factory/events/errors/unsupported-media-type` | 415 | — | Tipo de mídia não suportado | "Formato não suportado. Utilize Content-Type: application/json." |
| E-09 | `https://api.factory/events/errors/payload-too-large` | 413 | RN-16 | Corpo da requisição muito grande | "O corpo da requisição excede o limite de 1 MB." |
| E-10 | `https://api.factory/events/errors/too-many-requests` | 429 | RN-15 | Limite de taxa excedido | "Limite de 100 requisições por minuto excedido. Tente novamente mais tarde." |
| E-11 | `https://api.factory/events/errors/unauthorized` | 401 | RN-13, RN-14 | Não autenticado | "Autenticação necessária para acessar este recurso." |
| E-12 | `https://api.factory/events/errors/forbidden` | 403 | RN-13, RN-14 | Acesso negado | "Seu papel não tem permissão para executar esta ação." |
| E-13 | `https://api.factory/events/errors/internal-error` | 500 | — | Erro interno | "Erro interno. Tente novamente ou informe o identificador ao suporte." |

## Exemplos de corpo (ProblemDetail — RFC 9457)

### E-01 — Erro de validação (400)
```json
{
  "type": "https://api.factory/events/errors/validation-error",
  "title": "Erro de validação de entrada",
  "status": 400,
  "detail": "O ID do evento (eventId) é obrigatório.",
  "instance": "/api/events#req-550e8400-e29b-41d4-a716-446655440000",
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
  "instance": "/api/events#req-550e8400-e29b-41d4-a716-446655440001",
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
  "instance": "/api/events#req-550e8400-e29b-41d4-a716-446655440002",
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
  "instance": "/api/events#req-550e8400-e29b-41d4-a716-446655440003",
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
  "instance": "/api/events#req-550e8400-e29b-41d4-a716-446655440004",
  "timestamp": "2026-11-05T13:00:04Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440004"
}
```

### E-12 — Acesso negado por papel (403)
```json
{
  "type": "https://api.factory/events/errors/forbidden",
  "title": "Acesso negado",
  "status": 403,
  "detail": "Seu papel não tem permissão para executar esta ação.",
  "instance": "/api/events#req-550e8400-e29b-41d4-a716-446655440005",
  "timestamp": "2026-11-05T13:00:05Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440005"
}
```

## Regras de uso do catálogo

- O campo `type` nunca muda para a mesma classe de erro (é o contrato estável para o cliente).
- O campo `detail` é a mensagem legível ao usuário e pode variar com o contexto (ex.: inclui o `eventId` conflitante).
- O campo `instance` é o `requestId` de correlação; nunca contém dado pessoal nem segredo.
- Erros 5xx não expõem detalhes internos (senhas, strings de conexão, nomes de classe) — veja `E-13`.
