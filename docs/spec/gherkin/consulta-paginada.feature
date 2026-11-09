@CA-10
Funcionalidade: Consulta paginada de eventos

  Como operador ou gerente
  Quero consultar os eventos paginados e ordenados
  Para que a listagem performe com volume de dados

  Regra: RN-09, RN-11, RN-12, RN-21

  Cenário: Listar eventos paginados e ordenados por timestamp decrescente
    Dado que existem eventos com timestamps diferentes cadastrados
    Quando consulto a página 1 com tamanho 20
    Então o sistema retorna no máximo 20 itens
    E os itens estão ordenados por timestamp decrescente (desempate por id interno decrescente)

  Cenário: Consultar segunda página respeita tamanho de página
    Dado que existem pelo menos 25 eventos cadastrados
    Quando consulto a página 2 com tamanho 10
    Então o sistema retorna 10 itens correspondentes à segunda página

  Cenário: Tamanho de página acima do máximo é limitado (ou rejeitado)
    Dado que existem eventos cadastrados
    Quando consulto com tamanho de página 200
    Então o sistema retorna no máximo 100 itens (ou rejeita a requisição)

  Cenário: Leitura reflete dado commitado imediatamente (frescor)
    Dado que acabei de registrar um evento com eventId "EVT-FRESH"
    Quando consulto a lista de eventos imediatamente
    Então o evento "EVT-FRESH" aparece na lista (sem atraso de cache)
