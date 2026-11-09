@CA-05 @CA-06
Funcionalidade: Idempotência por header Idempotency-Key

  Como cliente da API
  Quero que reenviar a mesma requisição seja seguro
  Para que um duplo clique ou retry não produza efeitos duplicados

  Regra: RN-03, RN-04, RN-05

  Cenário: Replay com mesma Idempotency-Key e mesmo payload devolve resposta idêntica
    Dado que enviei um evento com Idempotency-Key "IDEMP-K1", eventId "EVT-R1", tipo "PRODUCTION", description "a" e timestamp "2026-05-20T13:00:00Z"
    E recebi status 201 Created com corpo contendo eventId "EVT-R1"
    Quando envio novamente o mesmo corpo com Idempotency-Key "IDEMP-K1"
    Então o sistema responde com status 201 Created
    E o corpo da resposta é idêntico ao primeiro processamento
    E existe exatamente 1 evento com eventId "EVT-R1"

  Cenário: Idempotency-Key reutilizada com payload diferente é rejeitada (409)
    Dado que já processei um evento com Idempotency-Key "IDEMP-K2" e corpo A (eventId "EVT-R2")
    Quando envio um corpo B (eventId "EVT-RX", descrição diferente) com a mesma Idempotency-Key "IDEMP-K2"
    Então o sistema responde com status 409 Conflict
    E o tipo do erro é "idempotency-key-reused"
    E o evento "EVT-R2" permanece inalterado (não foi criado "EVT-RX")

  Cenário: Mesma Idempotency-Key em execução concorrente recebe 409 enquanto a primeira roda
    Dado que iniciei o processamento de um evento com Idempotency-Key "IDEMP-K3" e ainda não terminou
    Quando envio uma requisição concorrente com a mesma Idempotency-Key "IDEMP-K3"
    Então o sistema responde com status 409 Conflict
    E o tipo do erro é "idempotency-in-progress"
    E existe exatamente 1 evento processado para aquela chave
