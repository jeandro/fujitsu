# Eventos de fábrica — Java e Angular

Aplicação do teste Fullstack: recebe eventos PRODUCTION, MAINTENANCE e QUALITY, impede reprocessamento por ID e gera auditoria para QUALITY na mesma transação. A interface cadastra e consulta eventos, com feedback de validação, indisponibilidade e duplicidade.

## Pré-requisitos

- JDK **17** (definir JAVA_HOME). Não presumir compatibilidade desta versão de Spring Boot com JDK 25.
- Maven 3.9.x, ou os scripts Maven Wrapper incluídos (`mvnw.cmd` no Windows, `./mvnw` no Linux/macOS).
- Node.js 20.x para compatibilidade com Angular 17 e npm. O projeto mantém a stack original; uma atualização de versões deve ser planejada antes de uso em produção.
- Chrome ou Edge/Chromium para os testes frontend. No Windows o runner procura os locais comuns; em outros sistemas, se necessário, definir CHROME_BIN com o executável.
- Rede no primeiro build para baixar dependências.

## Executar

Terminal 1, na pasta backend:

```powershell
$env:JAVA_HOME = 'CAMINHO_DO_JDK_17'
.\mvnw.cmd spring-boot:run
```

Se Maven já estiver instalado, `mvn spring-boot:run` é equivalente.

Terminal 2, na pasta frontend:

```sh
npm ci
npm start
```

- Interface: http://localhost:4200
- API: http://localhost:8080/api/events
- Swagger: http://localhost:8080/swagger-ui/index.html
- OpenAPI: http://localhost:8080/v3/api-docs

Não distribua nem reutilize node_modules de outro sistema operacional. `npm ci` instala as dependências do package-lock na plataforma atual.

### Portas alternativas e publicação

No backend, `SERVER_PORT` configura a porta. No frontend, o serviço usa `/api/events` e o proxy de desenvolvimento lê `API_TARGET` (padrão http://localhost:8080).

```powershell
# Backend
$env:SERVER_PORT = '18080'
$env:CORS_ALLOWED_ORIGINS = 'http://localhost:14200'
.\mvnw.cmd spring-boot:run
# Frontend, em outro terminal
$env:API_TARGET = 'http://localhost:18080'
npm start -- --port 14200
```

O proxy do Angular existe apenas em desenvolvimento. Em produção, servir os arquivos de `frontend/dist/factory-events-frontend/browser` e encaminhar `/api` para o backend no mesmo domínio por reverse proxy. CORS direto, quando necessário, é configurado por `CORS_ALLOWED_ORIGINS`; o padrão permite apenas http://localhost:4200.

## Testes e cobertura

Backend:

```sh
cd backend
mvn test
```

Ou `mvnw.cmd test` / `./mvnw test`. O JaCoCo gera `backend/target/site/jacoco/index.html` e XML após os testes. Não há promessa de 100%: consulte a medição efetiva. Não use `forkCount=0` para medir cobertura pelo agente JaCoCo.

Frontend:

```sh
cd frontend
npm ci
npm test
npm test -- --code-coverage
npm run build
```

`npm run test:watch` inicia a modalidade interativa. Cobertura frontend em `frontend/coverage`. O runner usa Jasmine/Karma e ChromeHeadless; CHROME_BIN pode apontar para Edge. Os testes de serviço HTTP não dependem do backend ativo. As integrações backend usam H2 e dados próprios. O Mockito usa proxies de interfaces para os repositórios; o Surefire carrega o agente Byte Buddy ao iniciar a JVM de testes, evitando anexação dinâmica bloqueada em ambientes Windows restritos. O agente JaCoCo permanece ativo para medir cobertura.

## Arquitetura e decisões

- Controller valida o DTO e traduz respostas; Service aplica regras e delimita a transação; Repository persiste com JPA/H2. DTOs evitam expor entidades e esquema interno.
- A consulta retorna uma lista ordenada por timestamp e ID internos decrescentes. O frontend separa formulário, lista, serviço HTTP e interpretação de erros.
- H2 em memória atende ao enunciado. Os dados são perdidos ao encerrar o backend; isso é deliberado, não persistência durável.
- `eventId` recebe trim nas extremidades e permanece sensível a maiúsculas/minúsculas. Limite de 100 caracteres após trim. `description` é obrigatória, não pode conter apenas espaços e tem até 1.000 caracteres; seu texto é preservado.
- A verificação prévia melhora a resposta comum, mas a garantia de unicidade é a constraint `uk_events_event_id`. `saveAndFlush` permite converter exclusivamente essa violação em 409 quando há concorrência. Outros erros de integridade não são classificados como duplicidade.
- QUALITY grava evento e auditoria na mesma transação. A coluna de detalhes comporta o texto da descrição e metadados; uma falha de auditoria desfaz o evento. Não há endpoint público de auditoria porque não é exigido pelo teste.
- Datas são instantes UTC (`Instant`), serializadas com `Z`. Entradas com offset são normalizadas. Para compatibilidade com o exemplo do PDF, uma data sem offset é interpretada em **America/Sao_Paulo**. Equipamentos novos devem sempre enviar offset. A tabela exibe o fuso do navegador.
- Um `Clock` injetável gera uma única data da auditoria, permitindo testes determinísticos.
- Erros públicos incluem status, mensagem e requestId; validações incluem errors por campo. Exceções internas ficam no log com requestId, sem SQL/classe Java na resposta.
- Console H2 e SQL detalhado ficam desativados por padrão. Para inspeção local: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`; console em `/h2-console`, JDBC `jdbc:h2:mem:eventsdb`, usuário `sa`, senha vazia. Não habilitar esse perfil em ambiente exposto.

## Contrato e exemplo

```json
{
  "eventId": "EVT-1001",
  "eventType": "QUALITY",
  "description": "Inspeção de qualidade concluída",
  "timestamp": "2026-05-20T10:00:00-03:00"
}
```

POST /api/events: 201 criado; 400 entrada inválida; 409 ID previamente processado; 415 formato não suportado. GET /api/events: 200 com array. Métodos não aceitos retornam 405 com Allow; recursos inexistentes retornam 404. Falha interna inesperada retorna 500 com mensagem segura. Consulte Swagger para os esquemas.

## Requisitos do enunciado

Recepção, persistência, unicidade, tratamento por tipo, auditoria QUALITY e códigos HTTP estão implementados. A interface oferece cadastro, seleção de tipo, descrição, envio, consulta em tabela e mensagens. Diferenciais: testes unitários e integração com persistência, mocks de repositórios, regressão concorrente, tratamento estruturado, documentação OpenAPI, componentes separados e cobertura mensurável.

## Limitações e melhorias futuras

- Paginação e filtros são uma evolução para grandes volumes; a lista atual retorna todos os eventos e tem atualização manual. Atualização automática exigiria polling/SSE com uma política definida.
- Autenticação/autorização, banco durável, migrations, métricas e trilha de auditoria consultável são evoluções para produção, não exigências do PDF.
- Avaliar política de retenção e vínculo referencial da auditoria antes de introduzir exclusão de eventos (não existe exclusão nesta versão).
- Atualizar Spring Boot/Angular/JDK de maneira planejada, incluindo auditoria de dependências; esta correção não representa pentest ou certificação de segurança.
- Complementar a validação de acessibilidade com leitor de tela e testes em diferentes dispositivos reais.

## Entrega

Entregar fontes, README, pom.xml, package-lock.json e scripts de build. Excluir node_modules, target, dist, .angular, .idea e .local-backup. O ZIP corrigido gerado junto ao resumo de validação contém somente os arquivos necessários para reconstruir a aplicação.

## Referências

- Springdoc: https://springdoc.org/v2/
- JaCoCo Maven: https://www.jacoco.org/jacoco/trunk/doc/maven.html
