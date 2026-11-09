@CA-16
Funcionalidade: Limite de tamanho do corpo da requisição

  Como cliente da API
  Quero que corpos de requisição muito grandes sejam rejeitados
  Para que o sistema seja protegido contra consumo excessivo de memória e ataque de negação de serviço

  Regra: RN-16

  Cenário: Corpo acima de 1 MB é rejeitado com 413
    Dado que sou um operador autenticado
    Quando envio um POST /api/events com corpo maior que 1 MB
    Então o sistema responde com status 413 Payload Too Large
    E o tipo do erro é "payload-too-large"
    E nenhum evento é criado
    E nenhum log de auditoria é gerado para aquela tentativa
