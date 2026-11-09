@CA-09
Funcionalidade: Saga de registro + auditoria QUALITY com compensação

  Como operador de fábrica
  Quero que o registro de um evento QUALITY e seu log de auditoria sejam consistentes
  Para que, mesmo sob falha, não fique efeito parcial visível

  Regra: RN-06, RN-07

  Cenário: Registro QUALITY bem-sucedido cria evento e auditoria na mesma unidade de trabalho
    Dado que sou um operador autenticado
    Quando registro um evento tipo QUALITY com eventId "EVT-SAGA-OK"
    Então o evento "EVT-SAGA-OK" é criado
    E o log de auditoria vinculado a "EVT-SAGA-OK" é criado com mensagem descritiva e data de criação

  Cenário: Falha na gravação do log de auditoria dispara compensação e evento não fica visível
    Dado que o sistema está configurado para falhar na gravação do log de auditoria de QUALITY
    Quando registro um evento tipo QUALITY com eventId "EVT-SAGA-FAIL"
    Então o sistema responde com erro informando que o registro não foi concluído
    E o evento "EVT-SAGA-FAIL" não fica visível na consulta de eventos (compensação executada)
    E existe um registro de compensação com data, motivo e responsável "sistema"
    E nenhum efeito parcial permanece visível
