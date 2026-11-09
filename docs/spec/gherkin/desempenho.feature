@CA-13 @CA-14 @CA-15
Funcionalidade: Performance e eficiência de consulta de eventos

  Como equipe de operações de fábrica
  Quero que o registro e a consulta de eventos atendam aos orçamentos de performance
  Para que o sistema escale com o volume da planta sem degradação perceptível

  Regra: RN-10, RN-11, RN-12, RN-21

  Cenário: p95 do POST /api/events é no máximo 300 ms
    Dado que existem eventos cadastrados na base
    Quando executo 100 requisições POST /api/events com eventos válidos e autenticados
    Então o p95 do tempo de resposta é de no máximo 300 ms

  Cenário: p95 do GET /api/events com 1.000 registros é no máximo 800 ms
    Dado que existem 1.000 eventos cadastrados
    Quando executo consultas GET /api/events paginadas autenticado como operador ou gerente
    Então o p95 do tempo de resposta é de no máximo 800 ms

  Cenário: Cada requisição executa no máximo 3 consultas ao banco (sem N+1)
    Dado que registro um evento via POST /api/events
    Quando a requisição é processada
    Então o número de consultas SQL executadas é de no máximo 3
    E não há consulta N+1

  Cenário: Leitura reflete dado commitado imediatamente (frescor sem cache de leitura)
    Dado que acabei de registrar um evento com eventId "EVT-PERF-FRESH"
    Quando consulto a lista de eventos imediatamente após o commit
    Então o evento "EVT-PERF-FRESH" aparece na lista sem atraso de cache
