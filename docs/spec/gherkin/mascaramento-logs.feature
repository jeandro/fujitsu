@CA-17
Funcionalidade: Mascaramento de dados confidenciais em logs de aplicação

  Como responsável pela LGPD
  Quero que o campo description seja mascarado nos logs de aplicação
  Para que dados pessoais possíveis no texto livre não vazem em logs

  Regra: RN-17, RN-20

  Cenário: Campo description é mascarado em logs de aplicação
    Dado que registro um evento QUALITY com description contendo dado pessoal "João Silva, CPF 123.456.789-00"
    Quando o evento é processado com sucesso
    Então os logs de aplicação não contêm o valor em claro de description
    E o campo description é mascarado (ex.: "[MASKED]" ou equivalente) nos logs
    E o valor original de description persiste no banco de dados para consulta autorizada

  Cenário: Campo description não é devolvido em resposta a papel sem permissão
    Dado que sou autenticado com papel que não tem permissão de leitura de dados confidenciais
    Quando consulto GET /api/events
    Então a resposta não inclui o campo description em claro para eventos confidenciais
