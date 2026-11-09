@CA-07 @CA-08
Funcionalidade: Concorrência no registro de eventos

  Como sistema de fábrica
  Quero que requisições simultâneas não dupliquem eventos
  Para que a integridade dos dados seja mantida sob carga

  Regra: RN-02, RN-03

  Cenário: Duas requisições com mesmo eventId e chaves de idempotência diferentes → um efeito, um 201 e um 409
    Dado que não existe evento com eventId "EVT-RACE"
    Quando envio duas requisições simultâneas com eventId "EVT-RACE", tipo "QUALITY", description "d" e Idempotency-Key distintas "K-A" e "K-B"
    Então uma requisição recebe status 201 Created
    E a outra requisição recebe status 409 Conflict
    E existe exatamente 1 evento com eventId "EVT-RACE"
    E existe exatamente 1 log de auditoria para "EVT-RACE"

  Cenário: Duas requisições com mesma Idempotency-Key simultâneas → um efeito e duas respostas iguais
    Dado que envio duas requisições simultâneas com a mesma Idempotency-Key "IDEMP-RACE" e o mesmo payload (eventId "EVT-RACE2")
    Então apenas um evento é criado com eventId "EVT-RACE2"
    E ambas as requisições recebem a mesma resposta (mesmo status e mesmo corpo)
