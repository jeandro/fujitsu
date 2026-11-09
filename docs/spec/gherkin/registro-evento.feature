@CA-01 @CA-02 @CA-03 @CA-11
Funcionalidade: Registro de evento de fábrica

  Como operador de fábrica
  Quero registrar um evento (PRODUCTION, MAINTENANCE ou QUALITY)
  Para que a planta tenha rastreabilidade e auditoria de eventos de qualidade

  Regra: RN-01, RN-02, RN-06, RN-08

  Cenário: Registrar evento PRODUCTION com sucesso (caminho feliz)
    Dado que sou um operador autenticado
    Quando envio um evento com eventId "EVT-1001", tipo "PRODUCTION", description "Linha Ativa" e timestamp "2026-05-20T13:00:00Z"
    Então o sistema responde com status 201 Created
    E o corpo de resposta contém eventId "EVT-1001"
    E nenhum log de auditoria é criado para "EVT-1001"

  Cenário: Registrar evento QUALITY gera log de auditoria
    Dado que sou um operador autenticado
    Quando envio um evento com eventId "EVT-QUAL-1", tipo "QUALITY", description "Falha de inspeção" e timestamp "2026-05-20T13:05:00Z"
    Então o sistema responde com status 201 Created
    E existe um log de auditoria vinculado a "EVT-QUAL-1" com data de criação preenchida

  Cenário: Registrar evento MAINTENANCE não gera log de auditoria
    Dado que sou um operador autenticado
    Quando envio um evento com eventId "EVT-MAN-1", tipo "MAINTENANCE", description "Revisão preventiva" e timestamp "2026-05-20T13:10:00Z"
    Então o sistema responde com status 201 Created
    E nenhum log de auditoria é criado para "EVT-MAN-1"

  Cenário: Registrar evento duplicado pelo eventId é rejeitado (409)
    Dado que já existe um evento com eventId "EVT-DUP" registrado
    Quando envio novamente um evento com eventId "EVT-DUP", tipo "QUALITY", description "d" e timestamp "2026-05-20T13:00:00Z"
    Então o sistema responde com status 409 Conflict
    E a contagem de eventos com eventId "EVT-DUP" permanece 1
    E nenhum novo log de auditoria é criado para "EVT-DUP"

  Cenário: Evento com campo obrigatório ausente é rejeitado (400)
    Dado que sou um operador autenticado
    Quando envio um evento sem o campo description
    Então o sistema responde com status 400 Bad Request
    E nenhum evento é criado

  Cenário: Evento com eventId excedendo 100 caracteres é rejeitado (400)
    Dado que sou um operador autenticado
    Quando envio um evento com eventId de 101 caracteres
    Então o sistema responde com status 400 Bad Request

  Cenário: Evento com eventType inválido é rejeitado (400)
    Dado que sou um operador autenticado
    Quando envio um evento com eventType "OUTRO"
    Então o sistema responde com status 400 Bad Request

  Cenário: Evento com timestamp inválido é rejeitado (400)
    Dado que sou um operador autenticado
    Quando envio um evento com timestamp "not-a-date"
    Então o sistema responde com status 400 Bad Request

  Cenário: Não existe operação de alteração de evento (imutabilidade)
    Dado que existe um evento registrado com eventId "EVT-IMUT"
    Quando tento alterar o eventId "EVT-IMUT" via PUT ou PATCH
    Então o sistema rejeita a operação (sem endpoint de alteração disponível)
