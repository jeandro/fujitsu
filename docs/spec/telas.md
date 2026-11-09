# Especificação de telas — events-api

Migração MVC → Hexagonal do módulo de eventos de fábrica. Define a experiência do usuário (sem código): objetivo, fluxo, hierarquia, layout, estados, mensagens finais por erro da API, tokens e checklist de acessibilidade WCAG 2.2 AA.

Referências: `docs/spec/regras-negocio.md` (RN-xx), `docs/spec/erros.md` (E-01..E-13), `AGENTS.md` (cinco estados por tela, tokens semânticos, orçamento de build).

---

## 1. Tela: Registro de evento (formulário)

**Objetivo:** o operador registra um evento de fábrica (PRODUCTION, MAINTENANCE ou QUALITY) com `eventId`, `eventType`, `description` e `timestamp`.

**Ação primária única:** botão "Registrar evento" (preenchimento sólido, único na dobra principal).

**Fluxo:**
- Vem de: menu/navegação principal ("Registrar evento") ou da tela de consulta (ação "Novo evento").
- Vai para: tela de consulta paginada ao receber 201, mantendo o filtro atual; ou permanece na mesma tela em erro.
- Cancelar: descarta o rascunho do formulário e retorna para a tela de origem sem confirmação (não há dado sensível em rascunho local).

**Hierarquia:**
1. Título h1: "Registrar evento de fábrica".
2. Resumo (legenda, uma linha): "Informe os dados do evento. A descrição pode conter dado pessoal; não insira senhas nem tokens."
3. Conteúdo: formulário de uma coluna (mobile) com os quatro campos, em ordem de preenchimento.
4. Ações secundárias: "Limpar campos" (texto, abaixo do botão primário); "Voltar à lista" (texto, cabeçalho).

**Layout responsivo (mobile-first):**
- Até 640 px: formulário empilhado em uma coluna, botão primário na largura total abaixo dos campos.
- Acima de 640 px: formulário em uma coluna centralizada (máx. 560 px); botão primário com largura do conteúdo, alinhado à esquerda dos campos.
- O campo `description` é uma área de texto com altura estável (mín. 4 linhas); nunca encolhe ao digitar.
- Nenhum conteúdo importante fica atrás de clique, toggle ou animação.

**Campos e validação na borda (mensagens finais, português):**

| Campo | Regras | Mensagem de erro (aparece abaixo do campo, `aria-describedby`) |
|-------|--------|--------------------------------------------------------------|
| `eventId` | Obrigatório, não vazio, até 100 caracteres | E-01: "O ID do evento é obrigatório." / "O ID do evento deve ter até 100 caracteres." |
| `eventType` | Obrigatório, um de: PRODUCTION, MAINTENANCE, QUALITY | E-01: "O tipo do evento é obrigatório." / "Selecione um tipo de evento válido." |
| `description` | Obrigatório, não vazio, até 1000 caracteres | E-01: "A descrição é obrigatória." / "A descrição deve ter até 1000 caracteres." |
| `timestamp` | Obrigatório, ISO 8601 válido | E-01: "O timestamp é obrigatório." / "Informe um timestamp válido (formato ISO 8601)." |

Erros de servidor aparecem em banner acima do formulário (`role="alert"`, `aria-live="polite"`):
- E-02 (409 duplicado): "O evento com este ID já foi processado anteriormente. Verifique o ID do evento." Ação: "Corrigir ID".
- E-03 (409 idempotency-key-reused): "Esta ação já foi processada com dados diferentes. Reinicie o registro com uma nova ação." Ação: "Novo registro".
- E-04 (409 idempotency-in-progress): "O registro já está em andamento. Aguarde a conclusão ou tente novamente em instantes." Sem ação de reenvio; botão permanece desabilitado.
- E-05 (500 falha de auditoria): ver seção 4 (tela de compensação).
- E-09 (413): "O corpo da requisição excede o limite de 1 MB. Reduza a descrição e tente novamente."
- E-10 (429): "Limite de 100 requisições por minuto excedido. Tente novamente mais tarde." Mostrar contador de tempo restante, se a API informar.
- E-11 (401): "Autenticação necessária. Faça login para registrar eventos." Ação: "Fazer login".
- E-12 (403): "Seu papel não tem permissão para registrar eventos. Solicitações devem ser feitas com o papel operador." Ação: "Voltar".
- E-13 (500): "Erro interno. Tente novamente ou informe o identificador da requisição ao suporte."

**Cinco estados (texto exato):**

| Estado | Texto principal | Texto secundário / ação |
|--------|----------------|-------------------------|
| Vazio | "Preencha os campos para registrar um novo evento." | Placeholders nos campos; botão "Registrar evento" ativo quando o formulário estiver válido. |
| Carregando | "Registrando evento…" | Botão primário desabilitado, rótulo "Registrando…"; não há segundo clique possível (duplo clique bloqueado pelo estado de envio visível). |
| Sucesso | "Evento registrado com sucesso." | "O evento foi salvo e está disponível na lista." Link de ação: "Ver na lista de eventos". |
| Erro | "Não foi possível registrar o evento." | Mensagem do `type` da API (tabela acima) em banner; ação de correção ou nova tentativa. |
| Sem permissão | "Você não tem permissão para registrar eventos." | "Acesse com o papel operador ou solicite permissão ao administrador." Botão: "Voltar". |

---

## 2. Tela: Consulta paginada de eventos

**Objetivo:** o operador ou gerente consulta o histórico de eventos, ordenado por `timestamp` decrescente.

**Ação primária única:** nenhuma ação primária na lista; a ação de maior destaque é o link/botão "Novo evento" no cabeçalho (navegação para o formulário).

**Fluxo:**
- Vem de: menu ("Consulta de eventos") ou do formulário após sucesso.
- Vai para: formulário de registro (clique em "Novo evento").
- Cancelar: não se aplica à listagem; limpar filtros retorna à consulta padrão.

**Hierarquia:**
1. Título h1: "Eventos de fábrica".
2. Resumo: "Mostrando X de Y eventos" (atualiza ao mudar página/tamanho).
3. Conteúdo: tabela de uma linha por evento (`eventId`, `eventType`, `timestamp`, `description` com mascaramento LGPD — seção 6).
4. Ações secundárias: seletor de tamanho de página (20/50/100), controles de paginação (anterior/próximo, números de página), ordenação fixa (`timestamp` decrescente, desempate por `id` interno decrescente — não exposto ao usuário como controle).

**Layout responsivo (mobile-first):**
- Até 640 px: tabela vira lista de cards empilhados; cada card mostra `eventId` (negrito), `eventType` (rótulo), `timestamp` e `description` mascarada ou visível conforme permissão. Controles de paginação embaixo, em uma linha.
- Acima de 640 px: tabela com cabeçalho fixo; coluna `description` com truncamento e tooltip ao passar/focar (sem depender só de cor).
- Página padrão = 20 itens; máximo = 100 itens por página (RN-09). Se o usuário selecionar acima de 100, o sistema mostra "Máximo de 100 itens por página" e aplica 100.

**Cinco estados (texto exato):**

| Estado | Texto principal | Texto secundário / ação |
|--------|----------------|-------------------------|
| Vazio | "Nenhum evento encontrado." | "Ainda não há eventos registrados. Registre o primeiro evento." Link: "Novo evento". |
| Carregando | "Carregando eventos…" | Skeleton com dimensão estável (mesma altura da lista carregada); nenhum salto de layout. |
| Sucesso | "Mostrando X de Y eventos." | Tabela/cards + paginação. |
| Erro | "Não foi possível carregar os eventos." | "Tente novamente ou informe o identificador da requisição ao suporte." Botão: "Tentar novamente". |
| Sem permissão | "Você não tem permissão para consultar eventos." | "Seu papel não tem acesso a esta tela. Solicite acesso ao administrador." Botão: "Voltar". |

---

## 3. Tela/Modal: Status da saga — falha de auditoria e compensação (RN-07)

**Objetivo:** informar ao usuário que o registro de evento QUALITY não foi concluído porque o log de auditoria falhou, que a compensação foi executada e auditada, e que nenhum efeito parcial permanece visível.

**Ação primária única:** "Tentar registro novamente" (leva ao formulário com uma nova Idempotency-Key).

**Fluxo:**
- Aparece como substituição da tela de sucesso do formulário quando a API retorna E-05 (500, `audit-compensation-required`).
- Pode ser acessada como "detalhe da falha" a partir de um banner de erro na consulta, se a infraestrutura expor o `instance`/`requestId` da requisição falha.
- Cancelar: fecha o diálogo/mensagem e retorna para a tela de origem.

**Hierarquia:**
1. Título: "Registro não concluído".
2. Resumo (texto exato): "Não foi possível concluir o registro: o log de auditoria QUALITY não pôde ser gravado. A compensação foi executada e auditada. O evento não foi persistido."
3. Conteúdo: identificador da requisição (`instance`/`requestId`) para suporte; linha "Compensação: executada e auditada pelo sistema"; linha "Efeito visível: nenhum — o evento não aparece na consulta".
4. Ações secundárias: "Copiar identificador"; "Contatar suporte".

**Estado de saga na tela:** não é spinner infinito. A tela mostra a etapa concluída ("Compensação executada") e o resultado final ("Registro não concluído"). Se o usuário sair da tela e voltar, o estado persiste por `instance` enquanto a sessão durar.

**Texto final ao usuário (substitui o `detail` técnico da API):**
- "O registro não foi concluído. A compensação foi executada e está registrada em auditoria. Nenhum efeito parcial permanece visível. Você pode tentar novamente ou entrar em contato com o suporte informando o identificador da requisição."

---

## 4. Tabela: erro da API → mensagem final → onde aparece

| # | `type` da API | Status | Mensagem final ao usuário (texto exato) | Onde aparece | Ação do usuário |
|---|---------------|--------|-----------------------------------------|--------------|-----------------|
| E-01 | `.../errors/validation-error` | 400 | "O [campo] é obrigatório." / "[Campo] deve ter até [N] caracteres." / "Selecione um tipo de evento válido." / "Informe um timestamp válido (formato ISO 8601)." | Abaixo do campo inválido (`aria-describedby`) | Corrigir o campo |
| E-02 | `.../errors/duplicate-event` | 409 | "O evento com este ID já foi processado anteriormente." | Banner acima do formulário | Corrigir ID do evento |
| E-03 | `.../errors/idempotency-key-reused` | 409 | "Esta ação já foi processada com dados diferentes. Reinicie o registro com uma nova ação." | Banner ou diálogo | Novo registro (nova Idempotency-Key) |
| E-04 | `.../errors/idempotency-in-progress` | 409 | "O registro já está em andamento. Aguarde a conclusão." | Banner; botão desabilitado | Aguardar / tentar depois |
| E-05 | `.../errors/audit-compensation-required` | 500 | "Não foi possível concluir o registro: o log de auditoria QUALITY não pôde ser gravado. A compensação foi executada e auditada. O evento não foi persistido." | Tela de status da saga (seção 3) | Tentar novamente ou contatar suporte |
| E-06 | `.../errors/not-found` | 404 | "Recurso não encontrado." | Banner / tela de erro | Voltar à lista |
| E-07 | `.../errors/method-not-allowed` | 405 | "Método não permitido para este recurso." | Banner / tela de erro | Voltar |
| E-08 | `.../errors/unsupported-media-type` | 415 | "Formato não suportado. Utilize application/json." | Banner / tela de erro | Voltar |
| E-09 | `.../errors/payload-too-large` | 413 | "O corpo da requisição excede o limite de 1 MB. Reduza a descrição e tente novamente." | Banner acima do formulário | Reduzir descrição |
| E-10 | `.../errors/too-many-requests` | 429 | "Limite de 100 requisições por minuto excedido. Tente novamente mais tarde." | Banner com tempo restante, se disponível | Aguardar |
| E-11 | `.../errors/unauthorized` | 401 | "Autenticação necessária para acessar este recurso." | Tela sem permissão | Fazer login |
| E-12 | `.../errors/forbidden` | 403 | "Seu papel não tem permissão para executar esta ação." | Tela sem permissão | Voltar / solicitar acesso |
| E-13 | `.../errors/internal-error` | 500 | "Erro interno. Tente novamente ou informe o identificador da requisição ao suporte." | Banner / tela de erro | Tentar novamente / suporte |

Regras de uso da mensagem: o `detail` técnico da API nunca é mostrado ao usuário; só o texto final desta tabela. O `instance` (requestId) é mostrado apenas para suporte, nunca dado pessoal nem segredo.

---

## 5. Tokens de design (semânticos)

Nunca cor crua, tamanho cruo ou fonte crua no componente. Valores são exemplos de implementação; o token é o contrato.

### Cores

| Token | Uso | Valor exemplo (para implementar) |
|-------|-----|----------------------------------|
| `cor.bg.canvas` | Fundo geral da página | `#FFFFFF` |
| `cor.bg.surface` | Cartões, tabelas, diálogos, formulários | `#F7F9FA` |
| `cor.text.primary` | Títulos, rótulos, texto de leitura | `#111827` |
| `cor.text.secondary` | Legendas, texto auxiliar, placeholders | `#4B5563` |
| `cor.border.subtle` | Divisórias, bordas de campo | `#E5E7EB` |
| `cor.accent.default` | Botão primário, foco, links de ação | `#0F6B8C` |
| `cor.accent.hover` | Hover do botão primário | `#0C5570` |
| `cor.feedback.success` | Toast/banner de sucesso | `#15803D` (fundo `#ECFDF5`) |
| `cor.feedback.warning` | Atenção não-bloqueante | `#B45309` (fundo `#FFFBEB`) |
| `cor.feedback.danger` | Erro, banner de erro, texto de campo inválido | `#B91C1C` (fundo `#FEF2F2`) |

Contraste verificado: texto ≥ 4.5:1 sobre o fundo; componente (botão, campo, ícone informativo) ≥ 3:1. Nada depende só de cor (erro de campo tem ícone + texto; sucesso tem ícone + texto).

### Espaçamento (base 4px)

| Token | Valor | Uso típico |
|-------|-------|------------|
| `espaco.1` | 4px | Dentro de badge, ícone pequeno |
| `espaco.2` | 8px | Entre campo e rótulo de erro; padding de célula |
| `espaco.3` | 12px | Gap entre campos no formulário |
| `espaco.4` | 16px | Padding de cartão/tela; gap de seções |
| `espaco.5` | 20px | Margem de seção; padding de diálogo |
| `espaco.6` | 24px | Espaço entre seções principais |
| `espaco.7` | 28px | (reservado) |
| `espaco.8` | 32px | Padding generoso de tela cheia |

### Raio e elevação

| Token | Valor | Uso |
|-------|-------|-----|
| `raio.sm` | 4px | Botão, campo de entrada |
| `raio.md` | 8px | Cartão, diálogo |
| `raio.lg` | 12px | Modal de tela cheia |
| `elevacao.1` | sombra sutil | Cartão |
| `elevacao.2` | sombra média | Dropdown, tooltip |
| `elevacao.3` | sombra forte | Modal/dialogo sobreposto |

### Tipografia

| Token | Uso | Tamanho / peso |
|-------|-----|----------------|
| `tipografia.display` | Título de página (h1) | 28px, peso.forte |
| `tipografia.titulo` | Título de seção/cartão (h2/h3) | 20px, peso.forte |
| `tipografia.corpo` | Texto de leitura, rótulos, lista | 16px, peso.regular |
| `tipografia.legenda` | Ajuda, rodapé, placeholder, tabela pequena | 14px, peso.regular |

No máximo 2 famílias tipográficas, `font-display: swap`. Imagem sempre com dimensão declarada; prioridade de carregamento só na primeira dobra.

---

## 6. Regras de dados e privacidade na interface (LGPD + idempotência + saga)

### Idempotency-Key no cliente
- Cada ação de registro gera um UUID v4 no cliente no primeiro clique de "Registrar evento".
- A chave é enviada no cabeçalho HTTP `Idempotency-Key` e reutilizada se o usuário tentar reenviar enquanto a ação está em processamento (botão desabilitado — impossibilita duplo clique).
- Ao receber 409 `idempotency-key-reused`, a tela mostra a mensagem da tabela (seção 4) e oferece "Novo registro", que gera um novo UUID v4.
- A chave nunca é exibida ao usuário nem enviada por e-mail/log; é apenas um identificador de correlação da ação.

### Estado da saga na tela
- Registro QUALITY = duas etapas: registro do evento + log de auditoria.
- Se a segunda etapa falhar (E-05), a tela mostra o estado final "Registro não concluído" com compensação executada e auditada — nunca "processando" indefinido.
- Tempo estimado não se aplica (a falha é imediata); se a saga estivesse em processamento (202), a tela mostraria a etapa atual e permitiria sair da tela sem perder o acompanhamento por `instance`.

### Descrição (`description`) — dado confidencial LGPD
- Classificação: confidencial (RN-17).
- No formulário de registro, o usuário digita o texto livre normalmente.
- Na tela de consulta, `description` é devolvido apenas a papéis com permissão (operador, gerente — RN-14). Papéis sem permissão veem "[dado restrito]" ou o campo não é devolvido; a tela nunca adivinha o conteúdo.
- `description` nunca aparece: em título da página, em breadcrumb, em toast de sucesso, em logs de aplicação, em URL, em `alt` de imagem.
- Dado sensível na tela é mascarado por padrão com ação explícita de revelar (se houver visualização detalhada futura); não há revelação automática.
- Tela com dado pessoal não fica em cache do navegador (`no-store` refletido no comportamento: refresh explícito refaz a chamada).

### Segurança de sessão
- Token de sessão nunca em `localStorage`/`sessionStorage`.
- Expiração de sessão: redireciona para login com feedback claro ("Sessão expirada. Faça login novamente.") preservando a intenção (volta para a mesma tela após login).
- Nenhum segredo, token ou payload de erro técnico na tela.

---

## 7. Checklist de acessibilidade WCAG 2.2 AA (verificação por automação + revisão)

- [ ] Foco visível em todos os elementos interativos (anel 2px, `cor.accent.default`, deslocamento 2px do elemento).
- [ ] Ordem de tabulação lógica segue a ordem visual (topo → baixo, esquerda → direita).
- [ ] `Esc` fecha diálogos e modais e devolve o foco ao elemento que os abriu.
- [ ] Todo campo tem `<label>` visível e associado (`for`/`id` ou `aria-labelledby`).
- [ ] Erro de campo tem `aria-describedby` apontando para a mensagem de erro; `aria-invalid="true"` no campo inválido.
- [ ] Erros assíncronos (banner) usam `role="alert"` e `aria-live="polite"`.
- [ ] Alvo de toque ≥ 24 × 24 px (botões, links, seletores de paginação).
- [ ] Nenhuma informação depende apenas de cor (ícone + texto; erro + ícone; sucesso + ícone).
- [ ] Contraste texto/fundo ≥ 4.5:1; componente/UI ≥ 3:1 (verificado, não estimado).
- [ ] 100% das tarefas navegáveis por teclado, sem armadilha de foco.
- [ ] Imagens decorativas com `alt=""`; imagens informativas com texto alternativo descritivo em português.
- [ ] Ordem de leitura (DOM) igual à ordem visual; sem uso de `position` ou `flex` para reordenar conteúdo sem sentido na tabulação.
- [ ] Texto em português claro, voz ativa, sem "ops!", sem jargão técnico (ex.: não dizer "400 Bad Request" ao usuário; dizer "Verifique os campos preenchidos").
- [ ] Redimensionamento de texto até 200% sem perda de conteúdo nem sobreposição.
- [ ] Animações respeitam `prefers-reduced-motion`; nada bloqueia leitura ou tabulação.
- [ ] Teste automatizado (axe) verde por tela; Lighthouse CI: LCP ≤ 2,5 s, INP ≤ 200 ms, CLS ≤ 0,1.

---

## 8. Orçamento de build (critério de aceite visual)

- Peso de mídia acima da dobra: declarado na spec da implementação; imagem do cabeçalho otimizada, sem hero pesado.
- Número de blocos acima da dobra: ≤ 4 (título, resumo, formulário/cabeçalho da lista, ação primária).
- Bundle inicial da aplicação: ≤ 200 KB gzip (regra do squad).
- Lighthouse CI por tela: LCP ≤ 2,5 s, INP ≤ 200 ms, CLS ≤ 0,1.

---

## 9. Pendentes / decisões necessárias ao humano

| Item | Por que bloqueia a tela | Quem decide |
|------|------------------------|-------------|
| Janela de retenção da `Idempotency-Key` (horas/dias) | Define se a tela permite reenviar com a mesma chave após expiração e o texto de "chave expirada" | `arquitetura-hexagonal` (PA-02) |
| Papéis além de `operador`/`gerente` e permissão sobre `description` | Define o que um papel sem permissão vê na consulta (`[dado restrito]` vs campo ausente) | `negocio-analista` (PA-03) |
| Efeito visível da compensação (exclusão física vs exclusão lógica) | Define o que o usuário consulta após E-05 (evento some da lista ou aparece como cancelado) | `arquitetura-hexagonal` (PA-05) |
| Limite de taxa exato (429) | Define o contador de tempo restante na tela de erro E-10 | `negocio-analista` (PA-01) |

Fim da spec de telas. Próximo: implementação dos componentes Angular com esses textos, tokens e estados; testes de componente validando cada erro da API contra esta tabela.