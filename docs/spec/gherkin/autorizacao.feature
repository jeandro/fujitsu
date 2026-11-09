@CA-12
Funcionalidade: Autorização por papel nos eventos

  Como administrador de segurança
  Quero que apenas papéis autorizados registrem e consultem eventos
  Para que o acesso esteja sob controle

  Regra: RN-13, RN-14

  Cenário: Operador pode registrar evento (POST)
    Dado que sou autenticado com o papel "operador"
    Quando envio um evento válido via POST /api/events
    Então o sistema responde com status 201 Created

  Cenário: Gerente não pode registrar evento (POST) → 403
    Dado que sou autenticado com o papel "gerente"
    Quando envio um evento válido via POST /api/events
    Então o sistema responde com status 403 Forbidden

  Cenário: Usuário não autenticado não pode registrar evento → 401
    Dado que não estou autenticado
    Quando envio um evento válido via POST /api/events
    Então o sistema responde com status 401 Unauthorized

  Cenário: Operador pode consultar eventos (GET) → 200
    Dado que sou autenticado com o papel "operador"
    Quando consulto GET /api/events
    Então o sistema responde com status 200 OK

  Cenário: Gerente pode consultar eventos (GET) → 200
    Dado que sou autenticado com o papel "gerente"
    Quando consulto GET /api/events
    Então o sistema responde com status 200 OK

  Cenário: Papel externo não pode consultar eventos (GET) → 403
    Dado que sou autenticado com o papel "visitante"
    Quando consulto GET /api/events
    Então o sistema responde com status 403 Forbidden
